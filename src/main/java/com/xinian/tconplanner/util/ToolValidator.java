package com.xinian.tconplanner.util;

import com.xinian.tconplanner.data.Blueprint;
import com.xinian.tconplanner.data.ModifierInfo;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.recipe.RecipeResult;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

public final class ToolValidator {

    /**
     * Validate if a modifier is able to be removed from a tool
     * @param bp      The blueprint to validate against
     * @param tool    The tool to try to remove a modifier from
     * @param modInfo The modifier to remove
     * @return        A RecipeResult containing the new stack on success, or an error message on failure.
     */
    public static RecipeResult<ItemStack> validateModRemoval(Blueprint bp, ToolStack tool, ModifierInfo modInfo){
        ToolStack toolClone = tool.copy();
        int toolBaseLevel = ToolStack.from(bp.createOutput(false)).getModifierLevel(modInfo.modifier);
        int minLevel = Math.max(0, toolBaseLevel);

        if(bp.modStack.getLevel(modInfo.modifier) + toolBaseLevel <= minLevel || !bp.modStack.isRecipeUsed((ITinkerStationRecipe) modInfo.recipe)) {
            return RecipeResult.failure("gui.tconplanner.modifiers.error.minlevel");
        }

        toolClone.removeModifier(modInfo.modifier.getId(), 1);

        Blueprint bpClone = bp.clone();
        bpClone.modStack.pop(modInfo);

        RecipeResult<?> bpResult = bpClone.validate();
        if(bpResult.hasError()) {
            return RecipeResult.failure(bpResult.getMessage());
        }

        return RecipeResult.success(toolClone.createStack());
    }
}
