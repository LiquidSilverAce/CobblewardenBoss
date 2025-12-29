package com.ace.cobbleboss;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;

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
            // Get spawn command for the species
            String command = getSpawnCommand(species, pos);
            
            // Execute the command from the server
            CommandSourceStack source = serverLevel.getServer()
                .createCommandSourceStack()
                .withPosition(new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5))
                .withLevel(serverLevel)
                .withPermission(4); // Op permission level
            
            serverLevel.getServer().getCommands().performPrefixedCommand(source, command);
            
            CobblewardenBoss.LOGGER.info("Successfully executed spawn command for {} boss at position: {}", species, pos);
            
        } catch (Exception e) {
            CobblewardenBoss.LOGGER.error("Failed to spawn {} boss", species, e);
        }
    }
    
    /**
     * Get the spawn command for a specific species
     */
    private static String getSpawnCommand(String species, BlockPos pos) {
        // Position 10 blocks in front (positive X direction)
        int spawnX = pos.getX() + 10;
        int spawnY = pos.getY();
        int spawnZ = pos.getZ();
        
        String command;
        switch (species) {
            case "giratina":
                command = String.format(
                    "pokespawnat giratina level=100 nature=mild special_attack_ev=252 attack_ev=252 special_attack_iv=31 attack_iv=31 uncatchable=true %d %d %d",
                    spawnX, spawnY, spawnZ
                );
                break;
            case "exploud":
                command = String.format(
                    "pokespawnat exploud level=100 nature=rash special_attack_ev=252 attack_ev=252 special_attack_iv=31 attack_iv=31 uncatchable=true %d %d %d",
                    spawnX, spawnY, spawnZ
                );
                break;
            case "guzzlord":
                command = String.format(
                    "pokespawnat guzzlord level=100 nature=adamant attack_ev=252 hp_ev=252 attack_iv=31 hp_iv=31 uncatchable=true %d %d %d",
                    spawnX, spawnY, spawnZ
                );
                break;
            default:
                command = String.format(
                    "pokespawnat %s level=100 uncatchable=true %d %d %d",
                    species, spawnX, spawnY, spawnZ
                );
                break;
        }
        
        CobblewardenBoss.LOGGER.debug("Generated spawn command: {}", command);
        return command;
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
}
