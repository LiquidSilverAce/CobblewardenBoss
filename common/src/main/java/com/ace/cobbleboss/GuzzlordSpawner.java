package com.ace.cobbleboss;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class GuzzlordSpawner {
    
    public static void spawnGuzzlord(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            CobblewardenBoss.LOGGER.warn("Cannot spawn Guzzlord on client side");
            return;
        }
        
        try {
            // Create Guzzlord Pokemon with properties
            PokemonProperties properties = PokemonProperties.Companion.parse(
                "guzzlord level=" + CobblewardenConfig.GUZZLORD_LEVEL + " uncatchable=true"
            );
            
            Pokemon guzzlord = properties.create();
            
            if (guzzlord == null) {
                CobblewardenBoss.LOGGER.error("Failed to create Guzzlord Pokemon - species not found");
                return;
            }
            
            // Configure Guzzlord stats
            configureGuzzlordStats(guzzlord);
            
            // Create Pokemon entity
            PokemonEntity guzzlordEntity = new PokemonEntity(serverLevel, guzzlord, null);
            
            // Set position (slightly above ground to prevent spawning in blocks)
            guzzlordEntity.moveTo(
                pos.getX() + 0.5, 
                pos.getY() + 1.0, 
                pos.getZ() + 0.5, 
                serverLevel.getRandom().nextFloat() * 360F, 
                0.0F
            );
            
            // Configure entity behavior
            configureGuzzlordEntity(guzzlordEntity);
            
            // Spawn the entity
            serverLevel.addFreshEntity(guzzlordEntity);
            
            CobblewardenBoss.LOGGER.info("Successfully spawned Guzzlord boss at position: {}", pos);
            
        } catch (Exception e) {
            CobblewardenBoss.LOGGER.error("Failed to spawn Guzzlord", e);
        }
    }
    
    private static void configureGuzzlordStats(Pokemon guzzlord) {
        // Set HP to Warden-equivalent
        guzzlord.setCurrentHealth(CobblewardenConfig.GUZZLORD_HP);
        
        // Boost IVs to maximum for stronger stats
        guzzlord.getIvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.HP, 31);
        guzzlord.getIvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.ATTACK, 31);
        guzzlord.getIvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.DEFENCE, 31);
        guzzlord.getIvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_ATTACK, 31);
        guzzlord.getIvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_DEFENCE, 31);
        guzzlord.getIvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPEED, 31);
        
        // Boost EVs for HP and Attack
        guzzlord.getEvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.HP, 252);
        guzzlord.getEvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.ATTACK, 252);
        guzzlord.getEvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_ATTACK, 6);
        
        CobblewardenBoss.LOGGER.debug("Configured Guzzlord with Level: {}, HP: {}", guzzlord.getLevel(), CobblewardenConfig.GUZZLORD_HP);
    }
    
    private static void configureGuzzlordEntity(PokemonEntity entity) {
        // Make the entity persistent (won't despawn naturally)
        entity.setPersistenceRequired();
        
        // The Fight or Flight Reborn mod should automatically make this Pokemon aggressive
        // when it encounters players, so no additional configuration is needed
        
        // Mark as boss using NBT tag
        GuzzlordBossUtil.markAsBoss(entity);
        
        // Set custom name to indicate this is a boss
        entity.setCustomName(net.minecraft.network.chat.Component.literal(CobblewardenConfig.BOSS_NAME_FORMATTED));
        entity.setCustomNameVisible(true);
        
        // Set as invulnerable to non-battle damage (like fall damage, fire, etc.)
        // Players must defeat it in battle
        entity.setInvulnerable(false); // Keep vulnerable to allow defeat
        
        CobblewardenBoss.LOGGER.debug("Configured Guzzlord entity behavior");
    }
}
