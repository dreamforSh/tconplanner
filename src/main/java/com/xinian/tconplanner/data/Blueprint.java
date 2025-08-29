package com.xinian.tconplanner.data;

import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import com.xinian.tconplanner.api.TCTool;
import com.xinian.tconplanner.util.DummyTinkersStationInventory;
import com.xinian.tconplanner.util.ModifierStack;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;
import slimeknights.tconstruct.library.recipe.RecipeResult;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolMaterialHook;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolPartsHook;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.part.IToolPart;

import java.util.*;

public class Blueprint extends BaseBlueprint<TCTool> {

    public Blueprint(TCTool tool) {
        super(tool);
    }

    @Override
    public Blueprint clone() {
        return fromNBT(toNBT());
    }

    @Override
    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("tool", Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(plannable.getItem())).toString());

        ListTag matList = new ListTag();
        for (IMaterial material : materials) {
            matList.add(StringTag.valueOf(material == null ? "" : material.getIdentifier().toString()));
        }
        nbt.put("materials", matList);
        nbt.put("modifiers", modStack.toNBT());

        if (!creativeSlots.isEmpty()) {
            CompoundTag creativeSlotsNbt = new CompoundTag();
            creativeSlots.forEach((slotType, amount) -> {
                if (amount > 0) creativeSlotsNbt.putInt(slotType.getName(), amount);
            });
            if (!creativeSlotsNbt.isEmpty()) {
                nbt.put("creativeSlots", creativeSlotsNbt);
            }
        }
        return nbt;
    }

    public static Blueprint fromNBT(CompoundTag tag) {
        ResourceLocation toolRL = ResourceLocation.parse(tag.getString("tool"));
        Optional<TCTool> optional = TCTool.getTools().stream()
                .filter(tool -> Objects.equals(ForgeRegistries.ITEMS.getKey(tool.getItem()), toolRL)).findFirst();
        if (optional.isEmpty()) return null;
        Blueprint bp = new Blueprint(optional.get());
        ListTag materials = tag.getList("materials", 8);
        for (int i = 0; i < materials.size(); i++) {
            String id = materials.getString(i);
            if (id.isEmpty()) continue;
            IMaterial material = MaterialRegistry.getMaterial(new MaterialId(id));
            if (i < bp.materials.length) {
                bp.materials[i] = material;
            }
        }

        CompoundTag modifiers = tag.getCompound("modifiers");
        bp.modStack.fromNBT(modifiers);

        if (tag.contains("creativeSlots")) {
            CompoundTag creativeSlotsTag = tag.getCompound("creativeSlots");
            for (String key : creativeSlotsTag.getAllKeys()) {
                SlotType type = SlotType.getIfPresent(key);
                if (type != null) {
                    bp.creativeSlots.put(type, creativeSlotsTag.getInt(key));
                }
            }
        }

        return bp;
    }
}