package com.ace.cobbleboss;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * Handles animated portal creation and destruction when Warden would spawn.
 * 
 * Timeline:
 * - Ticks 0-99: Animate portal building (100 ticks = 5 seconds)
 * - Tick 100: Portal completes and activates
 * - Ticks 100-159: Portal remains active (3 seconds)
 * - Tick 160: Spawn boss Pokemon at portal center
 * - Ticks 160-199: Boss spawned, portal still active (2 seconds)
 * - Tick 200: Destroy portal and cleanup
 */
public class PortalAnimator {
    
    private static final Map<UUID, PortalInstance> activePortals = new HashMap<>();
    private static boolean tickListenerRegistered = false;
    
    /**
     * Represents a single portal being animated
     */
    private static class PortalInstance {
        final ServerLevel level;
        final BlockPos centerPos;
        final String species;
        final List<BlockPos> frameBlocks;
        int ticksElapsed = 0;
        
        PortalInstance(ServerLevel level, BlockPos centerPos, String species, List<BlockPos> frameBlocks) {
            this.level = level;
            this.centerPos = centerPos;
            this.species = species;
            this.frameBlocks = frameBlocks;
        }
    }
    
    /**
     * Register the tick listener for portal animations
     */
    public static void register() {
        if (!tickListenerRegistered) {
            TickEvent.SERVER_POST.register(server -> {
                // Process all active portals
                Iterator<Map.Entry<UUID, PortalInstance>> iterator = activePortals.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<UUID, PortalInstance> entry = iterator.next();
                    PortalInstance portal = entry.getValue();
                    
                    if (processPortalTick(portal)) {
                        // Portal completed its lifecycle, remove it
                        iterator.remove();
                    }
                }
            });
            tickListenerRegistered = true;
            CobblewardenBoss.LOGGER.info("Portal animator tick listener registered");
        }
    }
    
    /**
     * Start creating an animated portal at the given position
     * @param level The server level
     * @param triggerPos The position where the Warden would have spawned
     * @param species The Pokemon species to spawn
     */
    public static void startPortalCreation(ServerLevel level, BlockPos triggerPos, String species) {
        // Find the best location within 10-15 blocks
        BlockPos portalPos = findSafeLocation(level, triggerPos);
        if (portalPos == null) {
            CobblewardenBoss.LOGGER.warn("Could not find safe location for portal near {}, falling back to direct spawn", triggerPos);
            // Fallback to direct spawn
            BossSpawner.spawnBoss(level, triggerPos);
            AncientCityTracker.markAsSpawned(level, triggerPos);
            return;
        }
        
        // Clear terrain at the chosen location
        clearTerrainForPortal(level, portalPos);
        
        // Calculate all frame positions
        List<BlockPos> frameBlocks = calculateFramePositions(portalPos);
        
        // Create portal instance
        PortalInstance portal = new PortalInstance(level, portalPos, species, frameBlocks);
        activePortals.put(UUID.randomUUID(), portal);
        
        CobblewardenBoss.LOGGER.info("Started portal animation at {} for species {}", portalPos, species);
    }
    
    /**
     * Find the best location for portal within 10-15 block range
     * Prioritizes areas away from lava and with good terrain
     */
    private static BlockPos findSafeLocation(ServerLevel level, BlockPos triggerPos) {
        BlockPos bestLocation = null;
        int bestScore = Integer.MIN_VALUE;
        
        // Search within 10-15 block radius
        for (int radius = 10; radius <= 15; radius += 2) {
            // Check 8 directions at each radius
            int[][] directions = {
                {radius, 0}, {-radius, 0}, {0, radius}, {0, -radius},
                {radius, radius}, {radius, -radius}, {-radius, radius}, {-radius, -radius}
            };
            
            for (int[] dir : directions) {
                BlockPos candidate = triggerPos.offset(dir[0], 0, dir[1]);
                
                // Find ground level
                BlockPos groundPos = findGroundLevel(level, candidate);
                if (groundPos != null) {
                    int score = scorePortalLocation(level, groundPos);
                    if (score > bestScore) {
                        bestScore = score;
                        bestLocation = groundPos;
                    }
                }
            }
        }
        
        // If we found any location (even if not ideal), use it
        return bestLocation;
    }
    
    /**
     * Score a potential portal location
     * Higher score = better location
     * Avoids lava and prefers clear areas
     */
    private static int scorePortalLocation(ServerLevel level, BlockPos pos) {
        int score = 0;
        
        // Check 10x10 horizontal area with 10 blocks vertical clearance
        int lavaBlocks = 0;
        int airBlocks = 0;
        int solidBlocks = 0;
        
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                for (int y = 0; y < 10; y++) {
                    BlockPos checkPos = pos.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);
                    
                    if (state.is(Blocks.LAVA)) {
                        lavaBlocks++;
                    } else if (state.isAir()) {
                        airBlocks++;
                    } else {
                        solidBlocks++;
                    }
                }
            }
        }
        
        // Heavily penalize lava (avoid at all costs)
        score -= lavaBlocks * 1000;
        
        // Prefer areas with more air (less clearing needed)
        score += airBlocks * 10;
        
        // Slightly prefer areas that need some clearing (more stable ground)
        score += Math.min(solidBlocks, 50);
        
        return score;
    }
    
    /**
     * Find the ground level at the given X/Z position
     */
    private static BlockPos findGroundLevel(ServerLevel level, BlockPos pos) {
        // Start from trigger Y and search down (max 30 blocks)
        int searchLimit = Math.max(level.getMinBuildHeight(), pos.getY() - 30);
        for (int y = pos.getY(); y > searchLimit; y--) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
            if (!level.getBlockState(checkPos).isAir() && 
                level.getBlockState(checkPos.above()).isAir()) {
                return checkPos.above();
            }
        }
        return null;
    }
    
    /**
     * Clear terrain for portal placement
     * Destroys all blocks in the 10x10x10 area needed for the portal
     * Avoids destroying bedrock or other indestructible blocks
     */
    private static void clearTerrainForPortal(ServerLevel level, BlockPos pos) {
        CobblewardenBoss.LOGGER.info("Clearing terrain for portal at {}", pos);
        
        // Clear 10x10 horizontal area (5 blocks in each direction from center)
        // with 10 blocks of vertical clearance for the portal frame
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                for (int y = 0; y < 10; y++) {
                    BlockPos checkPos = pos.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);
                    
                    // Don't destroy bedrock or other indestructible blocks
                    if (!state.isAir() && state.getDestroySpeed(level, checkPos) >= 0) {
                        level.setBlock(checkPos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }
    
    /**
     * Calculate all obsidian frame positions for a 10x10 portal
     * Returns positions ordered from bottom to top for smooth building animation
     * Creates a proper nether portal frame: rectangular perimeter only (1 block thick)
     * Only the edges (top, bottom, left, right) have obsidian, interior is hollow
     */
    private static List<BlockPos> calculateFramePositions(BlockPos center) {
        List<BlockPos> positions = new ArrayList<>();
        
        // Build a 10-block tall, 10-block wide portal frame
        // This is a FLAT rectangular frame (like a picture frame), not a 3D cube
        int width = 10;
        int height = 10;
        
        // Calculate half-width for centering (5 blocks in each direction)
        int halfWidth = width / 2;
        
        // Build frame from bottom to top for smooth animation
        // We only place blocks on a SINGLE PLANE (z = 0), creating a flat rectangular frame
        for (int y = 0; y < height; y++) {
            for (int x = -halfWidth; x <= halfWidth; x++) {
                // Only place blocks on the PERIMETER of the rectangle:
                // - Bottom row (y == 0) - full width
                // - Top row (y == height - 1) - full width
                // - Left edge (x == -halfWidth) - only if not already placed by top/bottom
                // - Right edge (x == halfWidth) - only if not already placed by top/bottom
                boolean isBottomOrTop = (y == 0 || y == height - 1);
                boolean isLeftOrRight = (x == -halfWidth || x == halfWidth);
                
                // Place block if it's on the perimeter (on a single plane, z=0)
                if (isBottomOrTop || isLeftOrRight) {
                    positions.add(center.offset(x, y, 0));
                }
            }
        }
        
        return positions;
    }
    
    /**
     * Process one tick for a portal instance
     * @return true if portal should be removed, false otherwise
     */
    private static boolean processPortalTick(PortalInstance portal) {
        portal.ticksElapsed++;
        
        if (portal.ticksElapsed <= 100) {
            // Phase 2: Build animation (ticks 0-100)
            animatePortalBuild(portal);
        } else if (portal.ticksElapsed == 100) {
            // Phase 3: Activate portal (tick 100)
            activatePortal(portal);
        } else if (portal.ticksElapsed == 160) {
            // Phase 4: Spawn boss (tick 160 = 3 seconds after completion)
            spawnBossAtPortal(portal);
        } else if (portal.ticksElapsed >= 200) {
            // Phase 5: Destroy portal (tick 200 = 2 seconds after boss spawn)
            destroyPortal(portal);
            return true; // Remove from active portals
        }
        
        return false; // Keep processing
    }
    
    /**
     * Animate portal building - place blocks gradually
     */
    private static void animatePortalBuild(PortalInstance portal) {
        int totalBlocks = portal.frameBlocks.size();
        int blocksPerTick = Math.max(1, totalBlocks / 100);
        
        int startIdx = (portal.ticksElapsed - 1) * blocksPerTick;
        int endIdx = Math.min(startIdx + blocksPerTick, totalBlocks);
        
        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
        
        for (int i = startIdx; i < endIdx; i++) {
            BlockPos blockPos = portal.frameBlocks.get(i);
            portal.level.setBlock(blockPos, obsidian, 3); // Flag 3: notify neighbors and clients
        }
    }
    
    /**
     * Activate the portal using CustomPortalAPI or fire
     */
    private static void activatePortal(PortalInstance portal) {
        // For now, we'll use fire to ignite the portal
        // In the future, this could use CustomPortalAPI if available
        
        // Find the interior of the portal and place fire
        BlockPos firePos = portal.centerPos.offset(0, 1, 0);
        portal.level.setBlock(firePos, Blocks.FIRE.defaultBlockState(), 3);
        
        CobblewardenBoss.LOGGER.info("Portal activated at {}", portal.centerPos);
    }
    
    /**
     * Spawn the boss Pokemon inside the portal center
     */
    private static void spawnBossAtPortal(PortalInstance portal) {
        // Spawn at portal center, slightly elevated (y+2 to be inside the portal)
        BlockPos spawnPos = portal.centerPos.offset(0, 2, 0);
        CobblewardenBoss.LOGGER.info("Spawning {} boss inside portal at {}", portal.species, spawnPos);
        BossSpawner.spawnBoss(portal.level, spawnPos, portal.species);
        
        // Mark location as spawned
        AncientCityTracker.markAsSpawned(portal.level, portal.centerPos);
    }
    
    /**
     * Destroy the portal by removing all frame blocks
     */
    private static void destroyPortal(PortalInstance portal) {
        CobblewardenBoss.LOGGER.info("Destroying portal at {}", portal.centerPos);
        
        // Remove all obsidian blocks
        for (BlockPos blockPos : portal.frameBlocks) {
            portal.level.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 3);
        }
        
        // Remove any fire
        BlockPos firePos = portal.centerPos.offset(0, 1, 0);
        if (portal.level.getBlockState(firePos).is(Blocks.FIRE)) {
            portal.level.setBlock(firePos, Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
