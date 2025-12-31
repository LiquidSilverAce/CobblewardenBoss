package com.ace.cobbleboss;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.world.entity.LivingEntity;

/**
 * Utility class for identifying and tagging boss Pokemon entities.
 */
public class BossUtil {
    
    /**
     * Mark a Pokemon entity as a boss.
     * Uses the Pokemon's persistent data through Cobblemon API.
     */
    public static void markAsBoss(PokemonEntity entity) {
        Pokemon pokemon = entity.getPokemon();
        // Store boss flag in the Pokemon's persistent data
        pokemon.getPersistentData().putBoolean("CobblewardenBoss", true);
    }
    
    /**
     * Check if an entity is a boss Pokemon.
     * Uses Pokemon's persistent data for reliable identification.
     */
    public static boolean isBoss(LivingEntity entity) {
        if (!(entity instanceof PokemonEntity pokemonEntity)) {
            return false;
        }
        
        Pokemon pokemon = pokemonEntity.getPokemon();
        
        // Check persistent data tag first (most reliable)
        if (pokemon.getPersistentData().contains("CobblewardenBoss")) {
            return pokemon.getPersistentData().getBoolean("CobblewardenBoss");
        }
        
        // Fallback to name check for backwards compatibility
        String customName = entity.getCustomName() != null ? entity.getCustomName().getString() : "";
        return customName.contains(CobblewardenConfig.BOSS_NAME_PLAIN);
    }
}
