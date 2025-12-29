package com.ace.cobbleboss;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.world.entity.LivingEntity;

public class BossDefeatHandler {
    
    public static void register() {
        // Register entity death event listener
        EntityEvent.LIVING_DEATH.register((entity, damageSource) -> {
            // Check if the entity is a boss Pokemon using utility method
            if (BossUtil.isBoss(entity)) {
                CobblewardenBoss.LOGGER.info("Boss Pokemon defeated at position: {}", entity.blockPosition());
                
                // Handle defeat behavior
                handleBossDefeat(entity);
                
                // Allow the death to proceed (entity will be removed)
                return EventResult.pass();
            }
            
            return EventResult.pass();
        });
        
        CobblewardenBoss.LOGGER.info("Boss defeat handler registered");
    }
    
    private static void handleBossDefeat(LivingEntity entity) {
        if (entity instanceof PokemonEntity pokemonEntity) {
            // Entity will be removed by the death event
            // Loot drops are prevented by the LivingEntityMixin
            
            CobblewardenBoss.LOGGER.info("Boss Pokemon will despawn without dropping loot");
        }
    }
}
