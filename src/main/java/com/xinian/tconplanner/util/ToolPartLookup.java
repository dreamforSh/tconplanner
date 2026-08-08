package com.xinian.tconplanner.util;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.library.tools.part.IToolPart;
import slimeknights.tconstruct.tools.TinkerToolParts;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Finds something to draw for a material slot that has no part behind it.
 * <p>
 * A tool assembled from parts renders each slot as that part in the chosen material. Tools whose
 * definition carries a {@code material_stats} module but no {@code part_stats} module - Tinkers'
 * ancient tools, and its travelers/slime armour - have material slots and no parts, so there is
 * nothing to draw. Picking any registered part that takes the same stat type gives the slot an icon
 * that both renders in the right material colour and tells the player what the slot is for: a war
 * pick's three slots show as a pick head, a bow limb and a bowstring.
 */
public final class ToolPartLookup {

    /**
     * Stat type to the part that represents it, or null before the first lookup.
     * <p>
     * Built from a datapack-driven tag, so it belongs to the world it was built in and is dropped on
     * logout alongside the tool and armour lists.
     */
    @Nullable
    private static Map<MaterialStatsId, IToolPart> byStatType;

    private ToolPartLookup() {}

    /** Drops the cached map; see {@code TCTool.invalidate()} */
    public static void invalidate() {
        byStatType = null;
    }

    /**
     * A registered tool part taking the given stat type, or null if nothing does.
     * <p>
     * Candidates are resolved in registry-name order so the same stat type always yields the same
     * icon; tag iteration order is not guaranteed stable across loads, and a slot icon that changed
     * between sessions would read as a bug.
     */
    @Nullable
    public static IToolPart representative(MaterialStatsId statType) {
        Map<MaterialStatsId, IToolPart> map = byStatType;
        if (map == null) {
            map = build();
            byStatType = map;
        }
        return map.get(statType);
    }

    private static Map<MaterialStatsId, IToolPart> build() {
        Map<MaterialStatsId, IToolPart> resolved = new HashMap<>();
        Map<MaterialStatsId, Map<String, IToolPart>> candidates = new HashMap<>();
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(TinkerTags.Items.TOOL_PARTS)) {
            Item item = holder.value();
            if (item instanceof IToolPart part) {
                candidates.computeIfAbsent(part.getStatType(), id -> new TreeMap<>())
                        .putIfAbsent(BuiltInRegistries.ITEM.getKey(item).toString(), part);
            }
        }
        candidates.forEach((statType, sorted) -> resolved.put(statType, sorted.values().iterator().next()));
        return resolved;
    }

    /**
     * The stack to draw for a material slot.
     *
     * @param statType stat type of the slot
     * @param part the slot's own part, or null when the definition has none
     * @param material material chosen for the slot, or null while it is still empty
     * @return a stack to render; never empty, so the icon is never invisible
     */
    public static ItemStack display(MaterialStatsId statType, @Nullable IToolPart part,
                                    @Nullable IMaterial material) {
        IMaterialItem item = part != null ? part : representative(statType);
        if (item == null) {
            //Some stat types have no part at all - travelers armour's tconstruct:cuirass is one Tinkers
            //itself ships - so the slot needs something neutral rather than an invisible button. The
            //fake ingot is a material item that renders as a tinted ingot, which reads as "this slot is
            //made of X" instead of naming some unrelated part. withMaterialForDisplay goes through
            //setMaterialForced, so it takes any material regardless of the item's own canUseMaterial.
            item = TinkerToolParts.fakeIngot.get();
        }
        return material == null
                ? new ItemStack(item.asItem())
                : item.withMaterialForDisplay(material.getIdentifier());
    }
}
