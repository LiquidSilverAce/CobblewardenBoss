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
            if (entity instanceof Warden) {
                CobblewardenBoss.LOGGER.info("Warden spawn detected at position: {}", entity.blockPosition());
                
                // Check if this location has already spawned a boss
                if (AncientCityTracker.hasSpawned(level, entity.blockPosition())) {
                    CobblewardenBoss.LOGGER.info("This location has already spawned a boss. Cancelling Warden spawn.");
                    return EventResult.interruptFalse(); // Cancel the spawn
                }
                
                // Spawn boss Pokemon instead
                BossSpawner.spawnBoss(level, entity.blockPosition());
                
                // Mark this location as having spawned
                AncientCityTracker.markAsSpawned(level, entity.blockPosition());
                
                // Cancel the Warden spawn
                return EventResult.interruptFalse();
            }
            
            return EventResult.pass();
        });
        
        CobblewardenBoss.LOGGER.info("Warden spawn interceptor registered");
    }
}
