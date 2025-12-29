package com.ace.cobbleboss;

import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.Natures;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Nature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
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
            Pokemon pokemon = createBossPokemon(species);
            
            if (pokemon == null) {
                CobblewardenBoss.LOGGER.error("Failed to create {} boss Pokemon - species not found", species);
                return;
            }
            
            // Configure boss stats, moves, and nature
            configureBossStats(pokemon, species);
            
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
     * Create a Pokemon with the appropriate properties based on species
     */
    private static Pokemon createBossPokemon(String species) {
        // Create properties string without aspects (Giratina doesn't need aspect specification)
        String propertiesString = String.format(
            "species=%s level=%d uncatchable=true",
            species,
            CobblewardenConfig.BOSS_LEVEL
        );
        
        CobblewardenBoss.LOGGER.debug("Creating boss Pokemon with properties: {}", propertiesString);
        
        PokemonProperties properties = PokemonProperties.Companion.parse(propertiesString);
        return properties.create();
    }
    
    /**
     * Determine which species to spawn based on location.
     * Ancient Cities (deep dark biome, Y < 0) get the configured ancient city species.
     * Overworld gets the configured overworld species.
     */
    private static String determineSpecies(ServerLevel level, BlockPos pos) {
        // Check if we're in an Ancient City by checking biome and Y level
        Holder<Biome> biomeHolder = level.getBiome(pos);
        
        // Get biome key for proper comparison
        boolean isDeepDark = biomeHolder.unwrapKey()
            .map(key -> key.location().getPath().equals("deep_dark"))
            .orElse(false);
        
        // Ancient Cities are in the deep dark biome and below Y=0
        boolean isAncientCity = isDeepDark && pos.getY() < 0;
        
        if (isAncientCity) {
            CobblewardenBoss.LOGGER.debug("Detected Ancient City location, spawning {}", CobblewardenConfig.ANCIENT_CITY_SPECIES);
            return CobblewardenConfig.ANCIENT_CITY_SPECIES;
        } else {
            CobblewardenBoss.LOGGER.debug("Detected overworld location, spawning {}", CobblewardenConfig.OVERWORLD_SPECIES);
            return CobblewardenConfig.OVERWORLD_SPECIES;
        }
    }
    
    private static void configureBossStats(Pokemon pokemon, String species) {
        // Set HP to Warden-equivalent
        pokemon.setCurrentHealth(CobblewardenConfig.BOSS_HP);
        
        // Boost IVs to maximum for stronger stats
        pokemon.getIvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.HP, 31);
        pokemon.getIvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.ATTACK, 31);
        pokemon.getIvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.DEFENCE, 31);
        pokemon.getIvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_ATTACK, 31);
        pokemon.getIvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_DEFENCE, 31);
        pokemon.getIvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPEED, 31);
        
        // Set species-specific EVs, moves, and nature
        switch (species) {
            case "giratina":
                configureGiratina(pokemon);
                break;
            case "exploud":
                configureExploud(pokemon);
                break;
            case "guzzlord":
                configureGuzzlord(pokemon);
                break;
            default:
                // Default EVs for unknown species
                pokemon.getEvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.HP, 252);
                pokemon.getEvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.ATTACK, 252);
                pokemon.getEvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_ATTACK, 6);
                break;
        }
        
        CobblewardenBoss.LOGGER.debug("Configured boss with Level: {}, HP: {}", pokemon.getLevel(), CobblewardenConfig.BOSS_HP);
    }
    
    /**
     * Configure Giratina with Mild nature, Sp ATK and ATK EVs, and specific moves
     */
    private static void configureGiratina(Pokemon pokemon) {
        // Set EVs: Full Sp ATK and ATK
        pokemon.getEvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_ATTACK, 252);
        pokemon.getEvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.ATTACK, 252);
        pokemon.getEvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.HP, 6);
        
        // Set Nature: Mild (+Sp ATK, -Def)
        Nature mild = Natures.INSTANCE.getNature("mild");
        if (mild != null) {
            pokemon.setNature(mild);
        }
        
        // Set Moves: shadowforce, dragonclaw, shadowball, earthpower
        setMoves(pokemon, "shadowforce", "dragonclaw", "shadowball", "earthpower");
    }
    
    /**
     * Configure Exploud with Rash nature, Sp ATK and ATK EVs, and specific moves
     */
    private static void configureExploud(Pokemon pokemon) {
        // Set EVs: Full Sp ATK and ATK
        pokemon.getEvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_ATTACK, 252);
        pokemon.getEvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.ATTACK, 252);
        pokemon.getEvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.HP, 6);
        
        // Set Nature: Rash (+Sp ATK, -Sp Def)
        Nature rash = Natures.INSTANCE.getNature("rash");
        if (rash != null) {
            pokemon.setNature(rash);
        }
        
        // Set Moves: crunch, boomburst, roar, supersonic (crunch first for Fight or Flight)
        setMoves(pokemon, "crunch", "boomburst", "roar", "supersonic");
    }
    
    /**
     * Configure Guzzlord with Adamant nature, ATK and HP EVs, and specific moves
     */
    private static void configureGuzzlord(Pokemon pokemon) {
        // Set EVs: Full ATK and HP
        pokemon.getEvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.ATTACK, 252);
        pokemon.getEvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.HP, 252);
        pokemon.getEvs().set(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_ATTACK, 6);
        
        // Set Nature: Adamant (+ATK, -Sp ATK)
        Nature adamant = Natures.INSTANCE.getNature("adamant");
        if (adamant != null) {
            pokemon.setNature(adamant);
        }
        
        // Set Moves: crunch, belch, dragonrush, hammerarm
        setMoves(pokemon, "crunch", "belch", "dragonrush", "hammerarm");
    }
    
    /**
     * Helper method to set moves on a Pokemon
     */
    private static void setMoves(Pokemon pokemon, String... moveNames) {
        pokemon.getMoveSet().clear();
        
        for (int i = 0; i < moveNames.length && i < 4; i++) {
            MoveTemplate move = Moves.INSTANCE.getByName(moveNames[i]);
            if (move != null) {
                pokemon.getMoveSet().add(move.create());
            } else {
                CobblewardenBoss.LOGGER.warn("Move {} not found", moveNames[i]);
            }
        }
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
            // Use Pokemon's persistent data through Cobblemon API
            entity.getPokemon().getPersistentData().putBoolean("unbattleable", true);
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
