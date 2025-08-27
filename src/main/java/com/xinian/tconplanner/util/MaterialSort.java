package com.xinian.tconplanner.util;

import com.google.common.collect.Lists;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.TierSortingRegistry;
import slimeknights.tconstruct.library.materials.IMaterialRegistry;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.stats.IMaterialStats;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.tools.stats.*;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public record MaterialSort<T extends IMaterialStats>(Comparator<T> comparator, Component text, Icon icon) {

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
        // Handle stats
        add(HandleMaterialStats.class, new MaterialSort<>(Comparator.comparingDouble(HandleMaterialStats::durability),
                TranslationUtil.createComponent("durability_multiplier"), new Icon(0, 1)));
        add(HandleMaterialStats.class, new MaterialSort<>(Comparator.comparingDouble(HandleMaterialStats::miningSpeed),
                TranslationUtil.createComponent("mining_speed"), new Icon(2, 1)));
        add(HandleMaterialStats.class, new MaterialSort<>(Comparator.comparingDouble(HandleMaterialStats::meleeSpeed),
                TranslationUtil.createComponent("attack_speed"), new Icon(3, 1)));
        add(HandleMaterialStats.class, new MaterialSort<>(Comparator.comparingDouble(HandleMaterialStats::attackDamage),
                TranslationUtil.createComponent("attack_damage"), new Icon(4, 1)));

        // Head stats
        add(HeadMaterialStats.class, new MaterialSort<>(Comparator.comparingInt(HeadMaterialStats::durability),
                TranslationUtil.createComponent("durability"), new Icon(1, 1)));
        add(HeadMaterialStats.class, new MaterialSort<>(Comparator.comparingInt(value -> TierSortingRegistry.getSortedTiers().indexOf(value.tier())),
                TranslationUtil.createComponent("harvest_level"), new Icon(5, 1)));
        add(HeadMaterialStats.class, new MaterialSort<>(Comparator.comparingDouble(HeadMaterialStats::miningSpeed),
                TranslationUtil.createComponent("mining_speed"), new Icon(2, 1)));
        add(HeadMaterialStats.class, new MaterialSort<>(Comparator.comparingDouble(HeadMaterialStats::attack),
                TranslationUtil.createComponent("attack_damage"), new Icon(4, 1)));

        // Plating stats
        add(PlatingMaterialStats.class, new MaterialSort<>(Comparator.comparingInt(PlatingMaterialStats::durability),
                TranslationUtil.createComponent("durability"), new Icon(1, 1)));
        add(PlatingMaterialStats.class, new MaterialSort<>(Comparator.comparingDouble(PlatingMaterialStats::armor),
                TranslationUtil.createComponent("armor"), new Icon(6, 1)));
        add(PlatingMaterialStats.class, new MaterialSort<>(Comparator.comparingDouble(PlatingMaterialStats::toughness),
                TranslationUtil.createComponent("armor_toughness"), new Icon(7, 1)));
        add(PlatingMaterialStats.class, new MaterialSort<>(Comparator.comparingDouble(PlatingMaterialStats::knockbackResistance),
                TranslationUtil.createComponent("knockback_resistance"), new Icon(8, 1)));

        // Grip stats
        add(GripMaterialStats.class, new MaterialSort<>(Comparator.comparingDouble(GripMaterialStats::durability),
                TranslationUtil.createComponent("durability_multiplier"), new Icon(1, 1)));
        add(GripMaterialStats.class, new MaterialSort<>(Comparator.comparingDouble(GripMaterialStats::accuracy),
                TranslationUtil.createComponent("accuracy"), new Icon(11, 1)));

        // Limb stats
        add(LimbMaterialStats.class, new MaterialSort<>(Comparator.comparingDouble(LimbMaterialStats::durability),
                TranslationUtil.createComponent("durability"), new Icon(1, 1)));
        add(LimbMaterialStats.class, new MaterialSort<>(Comparator.comparingDouble(LimbMaterialStats::drawSpeed),
                TranslationUtil.createComponent("draw_speed"), new Icon(9, 1)));
        add(LimbMaterialStats.class, new MaterialSort<>(Comparator.comparingDouble(LimbMaterialStats::velocity),
                TranslationUtil.createComponent("velocity"), new Icon(10, 1)));
        add(LimbMaterialStats.class, new MaterialSort<>(Comparator.comparingDouble(LimbMaterialStats::accuracy),
                TranslationUtil.createComponent("accuracy"), new Icon(11, 1)));
    }
}