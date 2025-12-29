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
    private static final String DATA_NAME = "cobblewarden_boss_ancient_cities";
    private final Set<String> spawnedCities = new HashSet<>();
    
    public AncientCityTracker() {
    }
    
    public static AncientCityTracker load(CompoundTag tag) {
        AncientCityTracker tracker = new AncientCityTracker();
        ListTag list = tag.getList("SpawnedCities", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            tracker.spawnedCities.add(list.getString(i));
        }
        return tracker;
    }
    
    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (String city : spawnedCities) {
            list.add(net.minecraft.nbt.StringTag.valueOf(city));
        }
        tag.put("SpawnedCities", list);
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
        String cityKey = getCityKey(pos);
        return tracker.spawnedCities.contains(cityKey);
    }
    
    public static void markAsSpawned(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        
        AncientCityTracker tracker = get(serverLevel);
        String cityKey = getCityKey(pos);
        tracker.spawnedCities.add(cityKey);
        tracker.setDirty();
        
        CobblewardenBoss.LOGGER.info("Marked Ancient City at {} as having spawned Guzzlord", cityKey);
    }
    
    /**
     * Generate a unique key for an Ancient City based on its approximate location.
     * We use chunk coordinates divided by 8 to group nearby spawns into the same "city"
     * This assumes Ancient Cities are at least 128 blocks apart.
     */
    private static String getCityKey(BlockPos pos) {
        int regionX = pos.getX() >> 7; // Divide by 128
        int regionZ = pos.getZ() >> 7; // Divide by 128
        int regionY = pos.getY() >> 6; // Divide by 64 for Y coordinate
        return regionX + "_" + regionY + "_" + regionZ;
    }
}
