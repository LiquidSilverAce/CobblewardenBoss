package com.ace.cobbleboss.state;

import com.ace.cobbleboss.CobblewardenBoss;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

/** Shared across dimensions, scoped to one save, and keyed by dimension plus structure start. */
public final class DefeatedCities extends SavedData {
    private static final Factory<DefeatedCities> FACTORY =
            new Factory<>(DefeatedCities::new, DefeatedCities::load, null);
    private final Set<String> defeated = new HashSet<>();

    public static DefeatedCities get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, CobblewardenBoss.MOD_ID + "_cities");
    }

    public boolean isDefeated(String city) {
        return defeated.contains(city);
    }

    public boolean markDefeated(String city) {
        if (defeated.add(city)) {
            setDirty();
            return true;
        }
        return false;
    }

    public static DefeatedCities load(CompoundTag tag, HolderLookup.Provider registries) {
        DefeatedCities data = new DefeatedCities();
        ListTag cities = tag.getList("defeatedCities", Tag.TAG_STRING);
        for (int i = 0; i < cities.size(); i++) {
            data.defeated.add(cities.getString(i));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag cities = new ListTag();
        defeated.stream().sorted().forEach(city -> cities.add(StringTag.valueOf(city)));
        tag.put("defeatedCities", cities);
        return tag;
    }
}
