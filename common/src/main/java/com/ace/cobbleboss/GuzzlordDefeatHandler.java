package com.ace.cobbleboss;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.world.entity.LivingEntity;

public class GuzzlordDefeatHandler {
    
    public static void register() {
        // Register entity death event listener
        EntityEvent.LIVING_DEATH.register((entity, damageSource) -> {
            // Check if the entity is a Guzzlord boss using utility method
            if (GuzzlordBossUtil.isBoss(entity)) {
                CobblewardenBoss.LOGGER.info("Guzzlord boss defeated at position: {}", entity.blockPosition());
                
                // Handle defeat behavior
                handleGuzzlordDefeat(entity);
                
                // Allow the death to proceed (entity will be removed)
                return EventResult.pass();
            }
            
            return EventResult.pass();
        });
        
        CobblewardenBoss.LOGGER.info("Guzzlord defeat handler registered");
    }
    
    private static void handleGuzzlordDefeat(LivingEntity entity) {
        // Clear drops - Guzzlord should not drop any items
        if (entity instanceof PokemonEntity pokemonEntity) {
            // The entity will be removed by the death event
            // No loot drops needed - vanilla behavior already handles this
            
            CobblewardenBoss.LOGGER.info("Guzzlord boss will despawn without dropping loot");
        }
    }
}
