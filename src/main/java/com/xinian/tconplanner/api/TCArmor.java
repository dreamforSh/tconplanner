package com.xinian.tconplanner.api;

import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolPartsHook;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.layout.LayoutSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TCArmor implements IPlannable {
    private static List<TCArmor> ALL_ARMORS = null;

    private final IModifiable modifiable;
    private final ToolDefinition definition;
    private final EquipmentSlot equipmentSlot;
    private final ItemStack renderArmor;

    private static final Set<ResourceLocation> ARMOR_BLACKLIST = Set.of(
            ResourceLocation.fromNamespaceAndPath(TConstruct.MOD_ID, "slime_helmet"),
            ResourceLocation.fromNamespaceAndPath(TConstruct.MOD_ID, "slime_chestplate"),
            ResourceLocation.fromNamespaceAndPath(TConstruct.MOD_ID, "slime_leggings"),
            ResourceLocation.fromNamespaceAndPath(TConstruct.MOD_ID, "slime_boots"),
            ResourceLocation.fromNamespaceAndPath(TConstruct.MOD_ID, "travelers_helmet"),
            ResourceLocation.fromNamespaceAndPath(TConstruct.MOD_ID, "travelers_chestplate"),
            ResourceLocation.fromNamespaceAndPath(TConstruct.MOD_ID, "travelers_leggings"),
            ResourceLocation.fromNamespaceAndPath(TConstruct.MOD_ID, "travelers_boots")
    );

    private TCArmor(Item item, EquipmentSlot equipmentSlot) {
        if (!(item instanceof IModifiable)) {
            throw new IllegalArgumentException("Item " + item.toString() + " is not an instance of IModifiable.");
        }
        this.modifiable = (IModifiable) item;
        this.definition = this.modifiable.getToolDefinition();
        this.equipmentSlot = equipmentSlot;
        this.renderArmor = ToolBuildHandler.buildToolForRendering(item, this.definition);
    }

    @Override
    public Component getName() {
        return this.renderArmor.getHoverName();
    }

    @Override
    public Component getDescription() {
        return getName();
    }

    @Override
    public ItemStack getRenderStack() {
        return renderArmor.copy();
    }

    @Override
    public IModifiable getModifiable() {
        return modifiable;
    }

    @Override
    public Item getItem() {
        return modifiable.asItem();
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return definition;
    }

    @Override
    public List<TCSlotPos> getSlotPos() {

        List<TCSlotPos> pos = new ArrayList<>();
        int parts = ToolPartsHook.parts(getToolDefinition()).size();
        for (int i = 0; i < parts; i++) {
            pos.add(new TCSlotPos(i * 18, 0));
        }
        return pos;
    }

    public EquipmentSlot getEquipmentSlot() {
        return equipmentSlot;
    }

    public static List<TCArmor> getArmors() {
        if (ALL_ARMORS == null) {
            ALL_ARMORS = new ArrayList<>();
            findArmorsForSlot(ALL_ARMORS, EquipmentSlot.HEAD, TinkerTags.Items.HELMETS);
            findArmorsForSlot(ALL_ARMORS, EquipmentSlot.CHEST, TinkerTags.Items.CHESTPLATES);
            findArmorsForSlot(ALL_ARMORS, EquipmentSlot.LEGS, TinkerTags.Items.LEGGINGS);
            findArmorsForSlot(ALL_ARMORS, EquipmentSlot.FEET, TinkerTags.Items.BOOTS);
        }
        return ALL_ARMORS;
    }

    private static void findArmorsForSlot(List<TCArmor> armorList, EquipmentSlot slot, TagKey<Item> tag) {
        Registry.ITEM.getTagOrEmpty(tag).forEach(itemHolder -> {
            Item item = itemHolder.value();
            ResourceLocation id = Registry.ITEM.getKey(item);
            if (item instanceof IModifiable && !ARMOR_BLACKLIST.contains(id)) {
                armorList.add(new TCArmor(item, slot));
            }
        });
    }
}