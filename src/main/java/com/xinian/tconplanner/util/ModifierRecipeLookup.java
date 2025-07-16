package com.xinian.tconplanner.util;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.resources.ResourceLocation;

public class ModifierRecipeLookup {
    private static final Object2IntMap<ResourceLocation> INCREMENTAL_PER_LEVEL = new Object2IntOpenHashMap<>();

    public static void setNeededPerLevel(ResourceLocation modifier, int neededPerLevel) {
        INCREMENTAL_PER_LEVEL.put(modifier, neededPerLevel);
    }

    public static int getNeededPerLevel(ResourceLocation modifier) {
        return INCREMENTAL_PER_LEVEL.getOrDefault(modifier, 0);
    }
}
