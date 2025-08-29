package com.xinian.tconplanner.data;

import com.xinian.tconplanner.api.TCArmor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.tools.SlotType;

import java.util.Objects;
import java.util.Optional;

public class ArmorBlueprint extends BaseBlueprint<TCArmor> {

    public ArmorBlueprint(TCArmor armor) {
        super(armor);
    }

    @Override
    public ArmorBlueprint clone() {
        return fromNBT(toNBT());
    }

    @Override
    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("armor", Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(plannable.getItem())).toString());

        ListTag matList = new ListTag();
        for (IMaterial material : materials) {
            matList.add(StringTag.valueOf(material == null ? "" : material.getIdentifier().toString()));
        }
        nbt.put("materials", matList);
        nbt.put("modifiers", modStack.toNBT());

        if (!creativeSlots.isEmpty()) {
            CompoundTag creativeSlotsNbt = new CompoundTag();
            creativeSlots.forEach((slotType, amount) -> {
                if (amount > 0) {
                    String slotName = slotType.toString();
                    slotName = slotName.substring(9, slotName.length() - 1);
                    creativeSlotsNbt.putInt(slotName, amount);
                }
            });
            if (!creativeSlotsNbt.isEmpty()) {
                nbt.put("creativeSlots", creativeSlotsNbt);
            }
        }
        return nbt;
    }

    public static ArmorBlueprint fromNBT(CompoundTag tag) {
        ResourceLocation armorRL = ResourceLocation.parse(tag.getString("armor"));
        Optional<TCArmor> optional = TCArmor.getArmors().stream()
                .filter(armor -> Objects.equals(ForgeRegistries.ITEMS.getKey(armor.getItem()), armorRL)).findFirst();
        if (optional.isEmpty()) return null;
        ArmorBlueprint bp = new ArmorBlueprint(optional.get());
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