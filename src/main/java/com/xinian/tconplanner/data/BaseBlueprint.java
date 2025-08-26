package com.xinian.tconplanner.data;

import com.xinian.tconplanner.api.IPlannable;
import com.xinian.tconplanner.util.DummyTinkersStationInventory;
import com.xinian.tconplanner.util.ModifierStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.recipe.RecipeResult;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;
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

public abstract class BaseBlueprint<T extends IPlannable> implements Cloneable {

    public final T plannable;
    public final ItemStack toolStack;
    public final IModifiable toolItem;
    public final ToolDefinition toolDefinition;
    public final IMaterial[] materials;
    public final Map<SlotType, Integer> creativeSlots = new HashMap<>();
    public final IToolPart[] toolParts;

    public ModifierStack modStack = new ModifierStack();

    public BaseBlueprint(T plannable) {
        this.plannable = plannable;
        this.toolItem = plannable.getModifiable();
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

    public RecipeResult<ItemStack> validate() {
        ToolStack ts = ToolStack.from(createOutput(false));
        RecipeResult<ItemStack> result = null;

        for (ModifierInfo info : modStack.getStack()) {
            IDisplayModifierRecipe recipe = info.recipe;
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

    public abstract CompoundTag toNBT();

    @Override
    public abstract BaseBlueprint<T> clone();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseBlueprint<?> that = (BaseBlueprint<?>) o;
        return toNBT().equals(that.toNBT());
    }
}
