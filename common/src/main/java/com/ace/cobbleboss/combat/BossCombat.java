package com.ace.cobbleboss.combat;

import com.ace.cobbleboss.CobblewardenBoss;
import com.ace.cobbleboss.spawn.BossSpawner;
import com.cobblemon.mod.common.CobblemonMemories;
import com.cobblemon.mod.common.api.events.battles.BattleStartedEvent;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.properties.UncatchableProperty;
import dev.architectury.platform.Platform;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;

import java.util.Comparator;

/** Capture and battle policies apply to this addon's marked wild replacements. */
public final class BossCombat {
    public static final String TRIGGER_KEY = CobblewardenBoss.MOD_ID + ":triggering_player";
    public static final String CITY_ENCOUNTER_KEY = CobblewardenBoss.MOD_ID + ":city_encounter";
    private static final String TARGET_KEY = CobblewardenBoss.MOD_ID + ":controls_target";
    public static final double TARGET_RANGE = 64;

    private BossCombat() {}

    public static boolean isBoss(PokemonEntity entity) {
        return isReplacement(entity)
                && !entity.getPokemon().getPersistentData().getString(BossSpawner.CITY_KEY).isEmpty();
    }

    public static boolean isReplacement(PokemonEntity entity) {
        return entity.getPokemon().isWild() && entity.getTags().contains(BossSpawner.BOSS_TAG);
    }

    private static boolean isCityEncounter(PokemonEntity entity) {
        // CITY_KEY also recognizes city bosses saved by older versions of the addon.
        return entity.getPokemon().getPersistentData().getBoolean(CITY_ENCOUNTER_KEY) || isBoss(entity);
    }

    public static boolean isUnbattleable(PokemonEntity entity) {
        return isReplacement(entity) && CobblewardenBoss.config().unbattleableFor(isCityEncounter(entity), false);
    }

    public static boolean usesRealTimeCombat(PokemonEntity entity) {
        return isUnbattleable(entity) && Platform.isModLoaded("fightorflight");
    }

    public static boolean forcesAggression(PokemonEntity entity) {
        return isBoss(entity) && Platform.isModLoaded("fightorflight")
                && CobblewardenBoss.config().bossAlwaysAggressive();
    }

    public static ServerPlayer chooseTarget(PokemonEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return null;
        var data = entity.getPokemon().getPersistentData();
        if (data.hasUUID(TRIGGER_KEY)) {
            ServerPlayer trigger = (ServerPlayer) level.getPlayerByUUID(data.getUUID(TRIGGER_KEY));
            if (eligible(entity, trigger)) return trigger;
        }
        return level.players().stream().filter(player -> eligible(entity, player))
                .min(Comparator.comparingDouble(entity::distanceToSqr)).orElse(null);
    }

    private static boolean eligible(PokemonEntity entity, ServerPlayer player) {
        return player != null && player.level() == entity.level() && player.isAlive() && !player.isRemoved()
                && !player.isSpectator() && !player.getAbilities().invulnerable
                && entity.distanceToSqr(player) <= TARGET_RANGE * TARGET_RANGE;
    }

    public static void update(PokemonEntity entity) {
        if (entity.level().isClientSide || !isReplacement(entity)) return;
        boolean uncatchable = CobblewardenBoss.config().uncatchableFor(isCityEncounter(entity), false);
        if (entity.isUncatchable() != uncatchable) {
            (uncatchable ? UncatchableProperty.INSTANCE.uncatchable() : UncatchableProperty.INSTANCE.catchable())
                    .apply(entity.getPokemon());
        }
        // Native Cobblemon battle protection works independently of Reborn.
        entity.getEntityData().set(PokemonEntity.getUNBATTLEABLE(), isUnbattleable(entity));
        var data = entity.getPokemon().getPersistentData();
        if (forcesAggression(entity)) {
            data.putBoolean(TARGET_KEY, true);
            ServerPlayer target = chooseTarget(entity);
            if (target != null && !entity.isBusy()) {
                entity.getBrain().eraseMemory(MemoryModuleType.AVOID_TARGET);
                entity.getBrain().eraseMemory(CobblemonMemories.POKEMON_SLEEPING);
                entity.getBrain().setMemory(MemoryModuleType.ANGRY_AT, target.getUUID());
                entity.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
                entity.setTarget(target);
                entity.getBrain().setActiveActivityIfPossible(Activity.FIGHT);
            } else if (target == null) {
                clearTarget(entity);
            }
        } else if (data.getBoolean(TARGET_KEY)) {
            clearTarget(entity);
            data.remove(TARGET_KEY);
        }
    }

    private static void clearTarget(PokemonEntity entity) {
        entity.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        entity.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
        entity.setTarget(null);
    }

    public static void preventBattle(BattleStartedEvent.Pre event) {
        for (var actor : event.getBattle().getActors()) {
            for (var pokemon : actor.getPokemonList()) {
                PokemonEntity entity = pokemon.getOriginalPokemon().getEntity();
                if (entity != null && isUnbattleable(entity)) {
                    event.setReason(Component.literal("Pokémon battles are disabled for this Warden replacement in the Cobblewarden config."));
                    event.cancel();
                    return;
                }
            }
        }
    }
}
