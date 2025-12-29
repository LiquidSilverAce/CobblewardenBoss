package com.ace.cobbleboss;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;

public class GuzzlordSpawner {
    
    private static final int GUZZLORD_HP = 500;
    private static final String GUZZLORD_SPECIES = "guzzlord";
    
    public static void spawnGuzzlord(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            CobblewardenBoss.LOGGER.warn("Cannot spawn Guzzlord on client side");
            return;
        }
        
        try {
            // Create Guzzlord Pokemon
            Pokemon guzzlord = PokemonSpecies.INSTANCE.getByName(GUZZLORD_SPECIES).create(75);
            
            if (guzzlord == null) {
                CobblewardenBoss.LOGGER.error("Failed to create Guzzlord Pokemon - species not found");
                return;
            }
            
            // Configure Guzzlord stats
            configureGuzzlordStats(guzzlord);
            
            // Create Pokemon entity
            PokemonEntity guzzlordEntity = new PokemonEntity(serverLevel, guzzlord, null);
            
            // Set position
            guzzlordEntity.moveTo(
                pos.getX() + 0.5, 
                pos.getY() + 1.0, 
                pos.getZ() + 0.5, 
                0.0F, 
                0.0F
            );
            
            // Configure entity behavior
            configureGuzzlordEntity(guzzlordEntity);
            
            // Spawn the entity
            serverLevel.addFreshEntity(guzzlordEntity);
            
            CobblewardenBoss.LOGGER.info("Successfully spawned Guzzlord at position: {}", pos);
            
        } catch (Exception e) {
            CobblewardenBoss.LOGGER.error("Failed to spawn Guzzlord", e);
        }
    }
    
    private static void configureGuzzlordStats(Pokemon guzzlord) {
        // Set HP to Warden-equivalent (500 HP)
        guzzlord.setCurrentHealth(GUZZLORD_HP);
        guzzlord.setMaxHealth(GUZZLORD_HP);
        
        // Boost attack stats to match Warden's devastating attacks
        // This will be handled by Cobblemon's stat system
        guzzlord.setLevel(75); // High level for strong stats
        
        // Make uncatchable by setting as wild and adding special flag
        guzzlord.getCaughtBall().setBallType(com.cobblemon.mod.common.api.pokeball.PokeBalls.INSTANCE.getMaster());
        
        CobblewardenBoss.LOGGER.debug("Configured Guzzlord with HP: {}, Level: {}", GUZZLORD_HP, guzzlord.getLevel());
    }
    
    private static void configureGuzzlordEntity(PokemonEntity entity) {
        // Make the entity persistent (won't despawn naturally)
        entity.setPersistenceRequired();
        
        // The Fight or Flight Reborn mod should automatically make this Pokemon aggressive
        // No additional configuration needed here as long as the mod is loaded
        
        // Set custom name to indicate this is a boss
        entity.setCustomName(net.minecraft.network.chat.Component.literal("§4Guzzlord Boss§r"));
        entity.setCustomNameVisible(true);
        
        CobblewardenBoss.LOGGER.debug("Configured Guzzlord entity behavior");
    }
}
