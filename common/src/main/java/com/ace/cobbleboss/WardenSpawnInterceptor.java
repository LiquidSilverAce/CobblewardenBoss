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
                
                // Schedule boss spawn for next tick to ensure it happens after event processing
                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    net.minecraft.core.BlockPos spawnPos = entity.blockPosition();
                    serverLevel.getServer().tell(new net.minecraft.server.TickTask(
                        serverLevel.getServer().getTickCount() + 1,
                        () -> {
                            // Spawn boss Pokemon
                            BossSpawner.spawnBoss(serverLevel, spawnPos);
                            
                            // Mark this location as having spawned
                            AncientCityTracker.markAsSpawned(serverLevel, spawnPos);
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
}
