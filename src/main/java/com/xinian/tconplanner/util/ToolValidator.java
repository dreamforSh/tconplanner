package com.xinian.tconplanner.util;

import com.xinian.tconplanner.data.BaseBlueprint;
import com.xinian.tconplanner.data.ModifierInfo;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.recipe.RecipeResult;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;

public final class ToolValidator {

    private ToolValidator(){}

    /**
     * Validate whether one level of a modifier can be taken back off the blueprint.
     * <p>
     * The previous version cloned the blueprint and validated it <em>without removing anything from the
     * clone</em>, so the replay always described the stack you already had and the check could never
     * fail. It also returned a preview built by calling {@code removeModifier} on the tool directly,
     * which never refunded the modifier's slots. Both are fixed by replaying the real remaining order.
     *
     * @param bp      the blueprint to validate against
     * @param modInfo the modifier level to remove
     * @return the resulting tool on success, or the reason it cannot be removed
     */
    public static RecipeResult<ItemStack> validateModRemoval(BaseBlueprint<?> bp, ModifierInfo modInfo){
        if(bp.modStack.getLevel(modInfo.modifier) <= 0 || !bp.modStack.isRecipeUsed((ITinkerStationRecipe) modInfo.recipe)){
            return RecipeResult.failure("gui.tconplanner.modifiers.error.minlevel");
        }

        ModifierStack remaining = bp.modStack.copy();
        remaining.pop(modInfo);

        //Every later modifier is re-run in order, so one that depended on this level still reports it
        RecipeResult<ItemStack> replayed = bp.validateWith(remaining);
        if(replayed.hasError()){
            return replayed;
        }
        return RecipeResult.success(bp.previewWith(remaining));
    }
}
