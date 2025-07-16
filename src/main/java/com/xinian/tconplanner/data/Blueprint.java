package com.xinian.tconplanner.data;

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

public class Blueprint implements Cloneable {

    public final TCTool tool;
    public final ItemStack toolStack;
    public final IModifiable toolItem;
    public final ToolDefinition toolDefinition;
    public final IMaterial[] materials;
    public final Map<SlotType, Integer> creativeSlots = new HashMap<>();
    public final IToolPart[] toolParts;

    public ModifierStack modStack = new ModifierStack();

    public Blueprint(TCTool tool) {
        this.tool = tool;
        this.toolItem = tool.getModifiable();
        this.toolDefinition = toolItem.getToolDefinition();
        this.toolStack = ToolBuildHandler.buildToolForRendering(toolItem.asItem(), toolDefinition);
        this.toolParts = ToolPartsHook.parts(toolDefinition).toArray(new IToolPart[0]);
        List<MaterialStatsId> statList = ToolMaterialHook.stats(toolDefinition);

        MaterialStatsId[] requiredStats = statList.toArray(new MaterialStatsId[0]);
        this.materials = new IMaterial[requiredStats.length];
    }

    public ItemStack createOutput() {
        return createOutput(true);
    }

    public ItemStack createOutput(boolean applyMods) {
        if (!isComplete()) return ItemStack.EMPTY;

        MaterialNBT materialNBT = MaterialNBT.of(materials);
        ItemStack built = ToolBuildHandler.buildItemFromMaterials(toolItem, materialNBT);
        ToolStack stack = ToolStack.from(built);

        creativeSlots.forEach((slotType, amount) -> stack.getPersistentData().addSlots(slotType, amount));

        if (applyMods) {
            for (ModifierInfo info : modStack.getStack()) {
                stack.addModifier(info.modifier.getId(), 1);
                if (info.count != null) {
                    stack.getPersistentData().addSlots(info.count.type(), -info.count.count());
                }
            }
            modStack.applyIncrementals(stack);
        }
        stack.rebuildStats();
        return stack.createStack();
    }

    public void addCreativeSlot(SlotType type) {
        addCreativeSlot(type, 1);
    }

    public void addCreativeSlot(SlotType type, int amount) {
        creativeSlots.compute(type, (slotType, val) -> val == null ? amount : val + amount);
    }

    public void removeCreativeSlot(SlotType type) {
        addCreativeSlot(type, -1);
    }

    public boolean isComplete() {
        return Arrays.stream(materials).noneMatch(Objects::isNull);
    }

    @Override
    public Blueprint clone() {
        return fromNBT(toNBT());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Blueprint blueprint = (Blueprint) o;
        return toNBT().equals(blueprint.toNBT());
    }


    public RecipeResult<ItemStack> validate() {
        ToolStack ts = ToolStack.from(createOutput(false));
        RecipeResult<ItemStack> result = null;

        for (ModifierInfo info : modStack.getStack()) {
            IDisplayModifierRecipe recipe = info.recipe;
            // <<-- 变更点 3: 移除多余的 registryAccess 参数，只传递一个参数
            RecipeResult<ItemStack> rs = ((ITinkerStationRecipe) recipe).getValidatedResult(new DummyTinkersStationInventory(ts.createStack()));
            if (rs.hasError()) {
                result = rs;
                break;
            } else {
                ts.addModifier(info.modifier.getId(), 1);
                SlotType type = recipe.getSlotType();
                SlotType.SlotCount count = recipe.getSlots();
                if (type != null && count != null) {
                    ts.getPersistentData().addSlots(type, -count.count());
                }
            }
        }
        return result != null ? result : RecipeResult.success(ts.createStack());
    }

    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("tool", Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(tool.getItem())).toString());

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
