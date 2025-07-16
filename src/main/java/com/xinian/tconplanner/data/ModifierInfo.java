package com.xinian.tconplanner.data;

import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;
import slimeknights.tconstruct.library.tools.SlotType;

import java.util.Objects;

public class ModifierInfo {

    public final IDisplayModifierRecipe recipe;
    public final Modifier modifier;
    public final SlotType.SlotCount count;

    public ModifierInfo(IDisplayModifierRecipe recipe){
        this.recipe = recipe;
        this.modifier = recipe.getDisplayResult().getModifier();
        this.count = recipe.getSlots();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModifierInfo info = (ModifierInfo) o;
        return ((ITinkerStationRecipe)recipe).getId().equals(((ITinkerStationRecipe)info.recipe).getId()) && modifier.getId().equals(info.modifier.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(recipe, modifier);
    }
}
