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
        // Find a safe location ~10 blocks away
        BlockPos portalPos = findSafeLocation(level, triggerPos);
        if (portalPos == null) {
            CobblewardenBoss.LOGGER.warn("Could not find safe location for portal near {}, falling back to direct spawn", triggerPos);
            // Fallback to direct spawn
            BossSpawner.spawnBoss(level, triggerPos);
            AncientCityTracker.markAsSpawned(level, triggerPos);
            return;
        }
        
        // Calculate all frame positions
        List<BlockPos> frameBlocks = calculateFramePositions(portalPos);
        
        // Create portal instance
        PortalInstance portal = new PortalInstance(level, portalPos, species, frameBlocks);
        activePortals.put(UUID.randomUUID(), portal);
        
        CobblewardenBoss.LOGGER.info("Started portal animation at {} for species {}", portalPos, species);
    }
    
    /**
     * Find a safe 10x10 area near the trigger position
     */
    private static BlockPos findSafeLocation(ServerLevel level, BlockPos triggerPos) {
        // Try positions in a radius around the trigger point
        int[] offsets = {10, -10, 15, -15, 5, -5};
        
        for (int xOffset : offsets) {
            for (int zOffset : offsets) {
                BlockPos candidate = triggerPos.offset(xOffset, 0, zOffset);
                
                // Find ground level
                BlockPos groundPos = findGroundLevel(level, candidate);
                if (groundPos != null && isSafeForPortal(level, groundPos)) {
                    return groundPos;
                }
            }
        }
        
        return null; // No safe location found
    }
    
    /**
     * Find the ground level at the given X/Z position
     */
    private static BlockPos findGroundLevel(ServerLevel level, BlockPos pos) {
        // Start from trigger Y and search down
        for (int y = pos.getY(); y > level.getMinY(); y--) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
            if (!level.getBlockState(checkPos).isAir() && 
                level.getBlockState(checkPos.above()).isAir()) {
                return checkPos.above();
            }
        }
        return null;
    }
    
    /**
     * Check if a 10x10 area is safe for portal placement
     */
    private static boolean isSafeForPortal(ServerLevel level, BlockPos pos) {
        // Check 10x10 area for air blocks
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                for (int y = 0; y < 5; y++) {
                    BlockPos checkPos = pos.offset(x, y, z);
                    if (!level.getBlockState(checkPos).isAir()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    /**
     * Calculate all obsidian frame positions for a 10x10 portal
     * Returns positions ordered from bottom to top for smooth building animation
     */
    private static List<BlockPos> calculateFramePositions(BlockPos center) {
        List<BlockPos> positions = new ArrayList<>();
        
        // Build a 10-block tall, 10-block wide frame
        int width = 10;
        int height = 10;
        
        // Bottom to top, layer by layer
        for (int y = 0; y < height; y++) {
            for (int x = -width/2; x <= width/2; x++) {
                for (int z = -width/2; z <= width/2; z++) {
                    // Only add perimeter blocks (frame, not interior)
                    if (x == -width/2 || x == width/2 || z == -width/2 || z == width/2 || y == 0 || y == height - 1) {
                        positions.add(center.offset(x, y, z));
                    }
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
     * Spawn the boss Pokemon at the portal center
     */
    private static void spawnBossAtPortal(PortalInstance portal) {
        CobblewardenBoss.LOGGER.info("Spawning {} boss at portal center {}", portal.species, portal.centerPos);
        BossSpawner.spawnBoss(portal.level, portal.centerPos);
        
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
