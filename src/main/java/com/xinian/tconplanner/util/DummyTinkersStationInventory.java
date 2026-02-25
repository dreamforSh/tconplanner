package com.xinian.tconplanner.util;

import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationContainer;

import javax.annotation.Nullable;

public class DummyTinkersStationInventory implements ITinkerStationContainer {

    private final ItemStack stack;

    public DummyTinkersStationInventory(ItemStack stack){
        this.stack = stack;
    }

    @Override
    public ItemStack getTinkerableStack() {
        return stack;
    }

    @Override
    public ItemStack getInput(int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getInputCount() {
        return 0;
    }

    @Nullable
    @Override
    public MaterialRecipe getInputMaterial(int i) {
        return null;
    }
}
