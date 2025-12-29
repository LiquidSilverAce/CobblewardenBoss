package com.ace.cobbleboss;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.warden.Warden;

public class WardenSpawnInterceptor {
    
    public static void register() {
        // Register entity spawn event listener
        EntityEvent.ADD.register((entity, level) -> {
            // Check if the entity being added is a Warden
            if (entity instanceof Warden warden) {
                CobblewardenBoss.LOGGER.info("Warden spawn detected at position: {}", entity.blockPosition());
                
                // Check if this location has already spawned a boss
                if (AncientCityTracker.hasSpawned(level, entity.blockPosition())) {
                    CobblewardenBoss.LOGGER.info("This location has already spawned a boss. Cancelling Warden spawn.");
                    return EventResult.interruptFalse(); // Cancel the spawn
                }
                
                // Schedule portal animation for next tick
                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    net.minecraft.core.BlockPos spawnPos = entity.blockPosition();
                    
                    // Determine which species to spawn
                    String species = determineSpecies(serverLevel, spawnPos);
                    
                    serverLevel.getServer().tell(new net.minecraft.server.TickTask(
                        serverLevel.getServer().getTickCount() + 1,
                        () -> {
                            // Start animated portal creation instead of direct spawn
                            PortalAnimator.startPortalCreation(serverLevel, spawnPos, species);
                        }
                    ));
                }
                
                // Cancel the Warden spawn
                return EventResult.interruptFalse();
            }
            
            return EventResult.pass();
        });
        
        CobblewardenBoss.LOGGER.info("Warden spawn interceptor registered");
    }
    
    /**
     * Determine which species to spawn based on location.
     * Moved from BossSpawner to be accessible here.
     */
    private static String determineSpecies(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos) {
        // Check if we're in an Ancient City by checking biome and Y level
        net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biomeHolder = level.getBiome(pos);
        
        // Get biome key for proper comparison
        boolean isDeepDark = biomeHolder.unwrapKey()
            .map(key -> key.location().getPath().equals("deep_dark"))
            .orElse(false);
        
        // Ancient Cities are in the deep dark biome and below Y=0
        boolean isAncientCity = isDeepDark && pos.getY() < 0;
        
        if (isAncientCity) {
            return CobblewardenConfig.ANCIENT_CITY_SPECIES;
        } else {
            return CobblewardenConfig.OVERWORLD_SPECIES;
        }
    }
}
