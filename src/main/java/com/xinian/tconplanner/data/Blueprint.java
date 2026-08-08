package com.xinian.tconplanner.data;

import com.xinian.tconplanner.api.TCTool;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.tools.SlotType;

import java.util.Objects;
import java.util.Optional;

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
        ResourceLocation toolRL = new ResourceLocation(tag.getString("tool"));
        Optional<TCTool> optional = TCTool.getTools().stream()
                .filter(tool -> Objects.equals(ForgeRegistries.ITEMS.getKey(tool.getItem()), toolRL)).findFirst();
        if (optional.isEmpty()) return null;
        Blueprint bp = new Blueprint(optional.get());
        ListTag materials = tag.getList("materials", 8);
        for (int i = 0; i < materials.size(); i++) {
            if (i < bp.materials.length) {
                //null when unresolvable, so isComplete() reports false instead of silently
                //rewriting the blueprint with tconstruct:unknown
                bp.materials[i] = resolveMaterial(materials.getString(i));
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