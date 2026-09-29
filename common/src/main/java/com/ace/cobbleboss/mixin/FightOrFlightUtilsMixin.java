package com.ace.cobbleboss.mixin;

import com.ace.cobbleboss.combat.BossCombat;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional Reborn 0.11 integration; its classes are never linked when it is absent. */
@Pseudo
@Mixin(targets = "me.rufia.fightorflight.utils.PokemonUtils", remap = false)
public abstract class FightOrFlightUtilsMixin {
    @Inject(method = "shouldFightTarget", at = @At("HEAD"), cancellable = true)
    private static void cobblewarden$forceBossCombat(PokemonEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (BossCombat.forcesAggression(entity)) {
            cir.setReturnValue(entity.isAlive() && !entity.isBusy() && BossCombat.chooseTarget(entity) != null);
        }
    }

    @Inject(method = "shouldAvoid", at = @At("HEAD"), cancellable = true)
    private static void cobblewarden$bossDoesNotFlee(PokemonEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (BossCombat.forcesAggression(entity)) cir.setReturnValue(false);
    }

    @Inject(method = "pokemonTryForceEncounter", at = @At("HEAD"), cancellable = true)
    private static void cobblewarden$keepCombatInWorld(PokemonEntity attacker, Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (BossCombat.usesRealTimeCombat(attacker)
                || (target instanceof PokemonEntity pokemon && BossCombat.usesRealTimeCombat(pokemon))) {
            cir.setReturnValue(false);
        }
    }

    // Both overloads accept a player (or their Pokémon), followed by the wild Pokémon.
    @Inject(method = "pokemonForceEncounterPvE", at = @At("HEAD"), cancellable = true)
    private static void cobblewarden$preventBattlePrompt(@Coerce Object playerOrPokemon, PokemonEntity wild,
                                                        CallbackInfoReturnable<Boolean> cir) {
        if (BossCombat.usesRealTimeCombat(wild)) cir.setReturnValue(false);
    }
}
