package com.ace.cobbleboss;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

/**
 * Utility class for identifying and tagging boss Pokemon entities.
 */
public class BossUtil {
    
    private static final String BOSS_TAG = "CobblewardenBoss";
    
    /**
     * Mark a Pokemon entity as a boss using NBT tags.
     */
    public static void markAsBoss(PokemonEntity entity) {
        CompoundTag persistentData = entity.getPersistentData();
        persistentData.putBoolean(BOSS_TAG, true);
    }
    
    /**
     * Check if an entity is a boss Pokemon.
     * Uses NBT tags for reliable identification.
     */
    public static boolean isBoss(LivingEntity entity) {
        if (!(entity instanceof PokemonEntity pokemonEntity)) {
            return false;
        }
        
        // Check NBT tag first (most reliable)
        CompoundTag persistentData = entity.getPersistentData();
        if (persistentData.contains(BOSS_TAG)) {
            return persistentData.getBoolean(BOSS_TAG);
        }
        
        // Fallback to name check for backwards compatibility
        String customName = entity.getCustomName() != null ? entity.getCustomName().getString() : "";
        return customName.contains(CobblewardenConfig.BOSS_NAME_PLAIN);
    }
}
