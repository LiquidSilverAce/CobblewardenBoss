package com.ace.cobbleboss;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashSet;
import java.util.Set;

public class AncientCityTracker extends SavedData {
    private static final String DATA_NAME = "cobblewarden_boss_spawn_locations";
    private final Set<String> spawnedLocations = new HashSet<>();
    
    public AncientCityTracker() {
    }
    
    public static AncientCityTracker load(CompoundTag tag) {
        AncientCityTracker tracker = new AncientCityTracker();
        ListTag list = tag.getList("SpawnedLocations", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            tracker.spawnedLocations.add(list.getString(i));
        }
        return tracker;
    }
    
    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (String location : spawnedLocations) {
            list.add(net.minecraft.nbt.StringTag.valueOf(location));
        }
        tag.put("SpawnedLocations", list);
        return tag;
    }
    
    private static AncientCityTracker get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(
            new SavedData.Factory<>(AncientCityTracker::new, AncientCityTracker::load),
            DATA_NAME
        );
    }
    
    public static boolean hasSpawned(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        
        AncientCityTracker tracker = get(serverLevel);
        String locationKey = getLocationKey(pos);
        return tracker.spawnedLocations.contains(locationKey);
    }
    
    public static void markAsSpawned(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        
        AncientCityTracker tracker = get(serverLevel);
        String locationKey = getLocationKey(pos);
        tracker.spawnedLocations.add(locationKey);
        tracker.setDirty();
        
        CobblewardenBoss.LOGGER.info("Marked location at {} as having spawned boss", locationKey);
    }
    
    /**
     * Generate a unique key for a spawn location based on its approximate coordinates.
     * We use configurable region size to group nearby spawns into the same location.
     */
    private static String getLocationKey(BlockPos pos) {
        int regionSize = CobblewardenConfig.ANCIENT_CITY_REGION_SIZE;
        int regionX = pos.getX() / regionSize;
        int regionZ = pos.getZ() / regionSize;
        int regionY = pos.getY() / (regionSize / 2); // Smaller Y regions since spawns are at specific Y levels
        return regionX + "_" + regionY + "_" + regionZ;
    }
}
