package com.ace.cobbleboss;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public class BossSpawner {
    
    /**
     * Spawn a boss Pokemon (species depends on location)
     */
    public static void spawnBoss(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            CobblewardenBoss.LOGGER.warn("Cannot spawn boss Pokemon on client side");
            return;
        }
        
        // Determine which species to spawn based on location
        String species = determineSpecies(serverLevel, pos);
        
        try {
            // Create boss Pokemon with properties
            PokemonProperties properties = PokemonProperties.Companion.parse(
                species + " level=" + CobblewardenConfig.BOSS_LEVEL + " uncatchable=true" + 
                (species.equals("giratina_altered") ? " form=altered" : "")
            );
            
            Pokemon pokemon = properties.create();
            
            if (pokemon == null) {
                CobblewardenBoss.LOGGER.error("Failed to create {} boss Pokemon - species not found", species);
                return;
            }
            
            // Configure boss stats
            configureBossStats(pokemon);
            
            // Create Pokemon entity
            PokemonEntity pokemonEntity = new PokemonEntity(serverLevel, pokemon, null);
            
            // Set position (slightly above ground to prevent spawning in blocks)
            pokemonEntity.moveTo(
                pos.getX() + 0.5, 
                pos.getY() + 1.0, 
                pos.getZ() + 0.5, 
                serverLevel.getRandom().nextFloat() * 360F, 
                0.0F
            );
            
            // Configure entity behavior
            configureBossEntity(pokemonEntity);
            
            // Spawn the entity
            serverLevel.addFreshEntity(pokemonEntity);
            
            CobblewardenBoss.LOGGER.info("Successfully spawned {} boss at position: {}", species, pos);
            
        } catch (Exception e) {
            CobblewardenBoss.LOGGER.error("Failed to spawn {} boss", species, e);
        }
    }
    
    /**
     * Determine which species to spawn based on location.
     * Ancient Cities (deep dark biome, Y < 0) get the configured ancient city species.
     * Overworld gets the configured overworld species.
     */
    private static String determineSpecies(ServerLevel level, BlockPos pos) {
        // Check if we're in an Ancient City by checking biome and Y level
        Holder<Biome> biomeHolder = level.getBiome(pos);
        String biomeName = biomeHolder.unwrapKey()
            .map(key -> key.location().toString())
            .orElse("");
        
        // Ancient Cities are in the deep dark biome and below Y=0
        boolean isAncientCity = biomeName.contains("deep_dark") && pos.getY() < 0;
        
        if (isAncientCity) {
            CobblewardenBoss.LOGGER.debug("Detected Ancient City location, spawning {}", CobblewardenConfig.ANCIENT_CITY_SPECIES);
            return CobblewardenConfig.ANCIENT_CITY_SPECIES;
        } else {
            CobblewardenBoss.LOGGER.debug("Detected overworld location, spawning {}", CobblewardenConfig.OVERWORLD_SPECIES);
            return CobblewardenConfig.OVERWORLD_SPECIES;
        }
    }
    
    private static void configureBossStats(Pokemon pokemon) {
        // Set HP to Warden-equivalent
        pokemon.setCurrentHealth(CobblewardenConfig.BOSS_HP);
        
        // Boost IVs to maximum for stronger stats
        pokemon.getIvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.HP, 31);
        pokemon.getIvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.ATTACK, 31);
        pokemon.getIvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.DEFENCE, 31);
        pokemon.getIvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_ATTACK, 31);
        pokemon.getIvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_DEFENCE, 31);
        pokemon.getIvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPEED, 31);
        
        // Boost EVs for HP and Attack
        pokemon.getEvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.HP, 252);
        pokemon.getEvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.ATTACK, 252);
        pokemon.getEvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_ATTACK, 6);
        
        CobblewardenBoss.LOGGER.debug("Configured boss with Level: {}, HP: {}", pokemon.getLevel(), CobblewardenConfig.BOSS_HP);
    }
    
    private static void configureBossEntity(PokemonEntity entity) {
        // Make the entity persistent (won't despawn naturally)
        entity.setPersistenceRequired();
        
        // The Fight or Flight Reborn mod should automatically make this Pokemon aggressive
        // when it encounters players, so no additional configuration is needed
        
        // Mark as boss using NBT tag
        BossUtil.markAsBoss(entity);
        
        // Prevent normal battles if configured
        if (CobblewardenConfig.PREVENT_NORMAL_BATTLE) {
            entity.getPersistentData().putBoolean("unbattleable", true);
        }
        
        // Set custom name to indicate this is a boss
        entity.setCustomName(net.minecraft.network.chat.Component.literal(CobblewardenConfig.BOSS_NAME_FORMATTED));
        entity.setCustomNameVisible(true);
        
        // Keep entity vulnerable to damage so it can be defeated
        // The LivingEntityMixin will prevent loot drops
        entity.setInvulnerable(false);
        
        CobblewardenBoss.LOGGER.debug("Configured boss entity behavior");
    }
}
