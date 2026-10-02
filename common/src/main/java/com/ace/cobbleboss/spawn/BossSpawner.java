package com.ace.cobbleboss.spawn;

import com.ace.cobbleboss.CobblewardenBoss;
import com.ace.cobbleboss.config.BossConfig;
import com.ace.cobbleboss.combat.BossCombat;
import com.ace.cobbleboss.mixin.SpawnUtilAccessor;
import com.ace.cobbleboss.state.DefeatedCities;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.activestate.SentOutState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class BossSpawner {
    public static final String BOSS_TAG = CobblewardenBoss.MOD_ID + ":replacement";
    public static final String CITY_KEY = CobblewardenBoss.MOD_ID + ":ancient_city";
    private static final int SPAWN_ATTEMPTS = 20;
    private static final int HORIZONTAL_RANGE = 5;
    private static final int VERTICAL_RANGE = 6;

    private BossSpawner() {}

    public static boolean hasNearbyReplacement(ServerLevel level, BlockPos pos) {
        // Vanilla uses a 48-block-wide box, not a 48-block radius.
        return !level.getEntitiesOfClass(PokemonEntity.class, nearbyBox(pos),
                entity -> entity.isAlive() && entity.getTags().contains(BOSS_TAG)
                        && entity.getPokemon().isWild()).isEmpty();
    }

    private static AABB nearbyBox(BlockPos pos) {
        return AABB.ofSize(Vec3.atCenterOf(pos), 48, 48, 48);
    }

    /** Returns null outside the bounds of an actual minecraft:ancient_city structure. */
    public static String ancientCityAt(ServerLevel level, BlockPos pos) {
        Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(BuiltinStructures.ANCIENT_CITY);
        if (structure == null) {
            return null;
        }
        StructureStart start = level.structureManager().getStructureAt(pos, structure);
        if (!start.isValid()) {
            return null;
        }
        return level.dimension().location() + "/" + start.getChunkPos().x + "/" + start.getChunkPos().z;
    }

    public static boolean trySpawn(ServerLevel level, BlockPos shriekerPos) {
        return trySpawn(level, shriekerPos, null);
    }

    public static boolean trySpawn(ServerLevel level, BlockPos shriekerPos, UUID triggeringPlayer) {
        // Recheck at spawn time: two shriekers can finish shrieking on the same tick.
        if (hasNearbyReplacement(level, shriekerPos)
                || !level.getEntitiesOfClass(Warden.class, nearbyBox(shriekerPos)).isEmpty()) {
            return false;
        }
        BossConfig config = CobblewardenBoss.config();
        String city = ancientCityAt(level, shriekerPos);
        boolean defeated = city != null && DefeatedCities.get(level.getServer()).isDefeated(city);
        String species = config.speciesFor(city != null, defeated);
        // The property API falls back to a random species if an unknown name is supplied.
        if (PokemonSpecies.getByName(species) == null) {
            CobblewardenBoss.LOGGER.error("Cannot replace Warden: Cobblemon species '{}' is unavailable", species);
            return false;
        }
        PokemonProperties properties = new PokemonProperties();
        properties.setSpecies(species);
        properties.setLevel(config.pokemonLevel());
        PokemonEntity entity = properties.createEntity(level);
        entity.addTag(BOSS_TAG);
        entity.getPokemon().getPersistentData().putBoolean(BossCombat.CITY_ENCOUNTER_KEY, city != null && !defeated);
        if (city != null && (species.equals("giratina") || species.equals("guzzlord"))) {
            entity.getPokemon().getPersistentData().putString(CITY_KEY, city);
            if (triggeringPlayer != null) entity.getPokemon().getPersistentData().putUUID(BossCombat.TRIGGER_KEY, triggeringPlayer);
        }
        BossCombat.update(entity);

        BlockPos.MutableBlockPos candidate = shriekerPos.mutable();
        for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
            candidate.setWithOffset(shriekerPos,
                    Mth.randomBetweenInclusive(level.random, -HORIZONTAL_RANGE, HORIZONTAL_RANGE), VERTICAL_RANGE,
                    Mth.randomBetweenInclusive(level.random, -HORIZONTAL_RANGE, HORIZONTAL_RANGE));
            if (!level.hasChunkAt(candidate) || !level.getWorldBorder().isWithinBounds(candidate)
                    || !SpawnUtilAccessor.cobblewarden$moveToPossibleSpawnPosition(level, VERTICAL_RANGE,
                    candidate, SpawnUtil.Strategy.ON_TOP_OF_COLLIDER)) {
                continue;
            }
            entity.moveTo(candidate.getX() + 0.5, candidate.getY(), candidate.getZ() + 0.5,
                    level.random.nextFloat() * 360.0F, 0);
            AABB bounds = entity.getBoundingBox();
            if (bounds.minY < level.getMinBuildHeight() || bounds.maxY > level.getMaxBuildHeight()
                    || !level.getWorldBorder().isWithinBounds(bounds)
                    || !level.hasChunksAt(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ),
                    BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ))
                    || !level.noCollision(entity) || level.containsAnyLiquid(bounds)) {
                continue;
            }
            if (level.addFreshEntity(entity)) {
                // Link immediately so even damage before the first entity tick can credit the city.
                entity.getPokemon().setState(new SentOutState(entity));
                BossCombat.update(entity);
                return true;
            }
            // Respect another mod rejecting the spawn instead of repeatedly submitting it.
            break;
        }
        entity.discard();
        return false;
    }

    public static void recordDefeat(Pokemon pokemon) {
        PokemonEntity entity = pokemon.getEntity();
        if (!pokemon.isWild() || entity == null || !entity.getTags().contains(BOSS_TAG)
                || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        String city = pokemon.getPersistentData().getString(CITY_KEY);
        if (!city.isEmpty()) {
            // Cobblemon or addons may dispatch battle callbacks off the server thread.
            level.getServer().execute(() -> {
                if (DefeatedCities.get(level.getServer()).markDefeated(city)) {
                    CobblewardenBoss.LOGGER.info("Ancient City {} cleared; future spawns use the default species", city);
                }
            });
        }
    }
}
