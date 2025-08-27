package com.xinian.tconplanner.util;

import com.google.common.collect.Lists;
import net.minecraftforge.common.TierSortingRegistry;
import slimeknights.tconstruct.library.materials.IMaterialRegistry;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.stats.IMaterialStats;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.tools.stats.GripMaterialStats;
import slimeknights.tconstruct.tools.stats.HandleMaterialStats;
import slimeknights.tconstruct.tools.stats.HeadMaterialStats;
import slimeknights.tconstruct.tools.stats.PlatingMaterialStats;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public record MaterialSort<T extends IMaterialStats>(Comparator<T> comparator, String text, Icon icon) {

    public static final HashMap<Class<? extends IMaterialStats>, List<MaterialSort<?>>> MAP = new HashMap<>();

    @SuppressWarnings("unchecked")
    public int compare(IMaterialStats stats1, IMaterialStats stats2) {
        return comparator.compare((T) stats1, (T) stats2);
    }

    public int compare(IMaterial mat1, IMaterial mat2, MaterialStatsId statsId) {
        IMaterialRegistry registry = MaterialRegistry.getInstance();
        Optional<IMaterialStats> ostats1 = registry.getMaterialStats(mat1.getIdentifier(), statsId);
        Optional<IMaterialStats> ostats2 = registry.getMaterialStats(mat2.getIdentifier(), statsId);
        if (ostats1.isPresent() && ostats2.isEmpty()) return -1;
        if (ostats1.isEmpty() && ostats2.isPresent()) return 1;
        if (ostats1.isEmpty()) return 0;
        IMaterialStats stats1 = ostats1.get();
        IMaterialStats stats2 = ostats2.get();
        return compare(stats1, stats2);
    }

    private static <T extends IMaterialStats> void add(Class<T> type, MaterialSort<T> sort) {
        List<MaterialSort<?>> list;
        if ((list = MAP.putIfAbsent(type, Lists.newArrayList(sort))) != null) list.add(sort);
    }

    static {
        add(HandleMaterialStats.class, new MaterialSort<>(Comparator.comparingDouble(HandleMaterialStats::durability), "Durability Multiplier", new Icon(0, 1)));
        add(HandleMaterialStats.class, new MaterialSort<>(Comparator.comparingDouble(HandleMaterialStats::miningSpeed), "Mining Speed", new Icon(2, 1)));
        add(HandleMaterialStats.class, new MaterialSort<>(Comparator.comparingDouble(HandleMaterialStats::meleeSpeed), "Attack Speed", new Icon(3, 1)));
        add(HandleMaterialStats.class, new MaterialSort<>(Comparator.comparingDouble(HandleMaterialStats::attackDamage), "Attack Damage", new Icon(4, 1)));

        add(HeadMaterialStats.class, new MaterialSort<>(Comparator.comparingInt(HeadMaterialStats::durability), "Durability", new Icon(1, 1)));
        add(HeadMaterialStats.class, new MaterialSort<>(Comparator.comparingInt(value -> TierSortingRegistry.getSortedTiers().indexOf(value.tier())), "Harvest Level", new Icon(5, 1)));
        add(HeadMaterialStats.class, new MaterialSort<>(Comparator.comparingDouble(HeadMaterialStats::miningSpeed), "Mining Speed", new Icon(2, 1)));
        add(HeadMaterialStats.class, new MaterialSort<>(Comparator.comparingDouble(HeadMaterialStats::attack), "Attack Damage", new Icon(4, 1)));

        add(PlatingMaterialStats.class, new MaterialSort<>(Comparator.comparingInt(PlatingMaterialStats::durability), "Durability", new Icon(1, 1)));
        add(PlatingMaterialStats.class, new MaterialSort<>(Comparator.comparingDouble(PlatingMaterialStats::toughness), "Armor Toughness", new Icon(6, 1)));
        add(PlatingMaterialStats.class, new MaterialSort<>(Comparator.comparingDouble(PlatingMaterialStats::knockbackResistance), "Knockback Resistance", new Icon(7, 1)));

        add(GripMaterialStats.class, new MaterialSort<>(Comparator.comparingDouble(GripMaterialStats::durability), "Durability", new Icon(1, 1)));
    }
}