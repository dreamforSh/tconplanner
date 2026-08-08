package com.xinian.tconplanner.util;

import com.google.common.collect.ImmutableList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import com.xinian.tconplanner.TConPlanner;
import com.xinian.tconplanner.data.ModifierInfo;
import com.xinian.tconplanner.screen.PlannerScreen;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ModifierStack {
    private final LinkedList<ModifierInfo> stack = new LinkedList<>();
    /**
     * Per-level material amounts for incremental modifiers. Always empty: Tinkers keeps
     * {@code IncrementalModifierRecipe.neededPerLevel} protected with no accessor, and both
     * {@code ModifierEntry.getNeeded()} and the entry built by {@code AbstractModifierRecipe
     * .getDisplayResult()} return 0, so the planner can never learn the real value. The map and its
     * NBT are kept because {@link com.xinian.tconplanner.data.BaseBlueprint#equals} compares serialised
     * blueprints - dropping the "diff" tag would stop every existing bookmark from matching itself.
     */
    private final HashMap<ModifierId, Integer> incrementalDiffMap = new HashMap<>();

    /**
     * The exact {@code mods} list this stack was read from, kept only when some id in it did not
     * resolve. While the player has not edited the stack, {@link #toNBT} re-emits it verbatim.
     * <p>
     * Without this, a recipe id the current game does not know was dropped on load and the shortened
     * stack was written straight back - so a temporarily absent addon permanently deleted the modifiers
     * that came from it. Re-emitting byte-for-byte also keeps {@code BaseBlueprint.equals}, and with it
     * bookmark identity, intact.
     */
    private ListTag unresolvedSource;
    private boolean edited;

    public void push(ModifierInfo info){
        stack.add(info);
        edited = true;
    }

    public void pop(ModifierInfo info){
        if(stack.removeLastOccurrence(info)) edited = true;
    }

    public void moveDown(int index){
        if(index < 0 || index >= stack.size() - 1) return;
        ModifierInfo info = stack.remove(index);
        stack.add(index + 1, info);
        edited = true;
    }

    public void moveUp(int index){
        moveDown(index - 1);
    }

    /** Removes one specific entry by position, so duplicate levels of the same modifier stay distinguishable */
    public void removeAt(int index){
        if(index < 0 || index >= stack.size()) return;
        stack.remove(index);
        edited = true;
    }

    public void clear(){
        stack.clear();
        incrementalDiffMap.clear();
        edited = true;
    }

    /** True when this stack was read with ids the current game cannot resolve */
    public boolean hasUnresolved(){
        return unresolvedSource != null;
    }

    public int size(){
        return stack.size();
    }

    public boolean isEmpty(){
        return stack.isEmpty();
    }

    /** Shallow copy - {@link ModifierInfo} is immutable, so the entries can be shared */
    public ModifierStack copy(){
        ModifierStack copy = new ModifierStack();
        copy.stack.addAll(stack);
        copy.incrementalDiffMap.putAll(incrementalDiffMap);
        copy.unresolvedSource = unresolvedSource;
        copy.edited = edited;
        return copy;
    }

    public boolean isRecipeUsed(ITinkerStationRecipe recipe){
        return stack.stream().anyMatch(info -> ((ITinkerStationRecipe)info.recipe).getId().equals(recipe.getId()));
    }

    public int getLevel(Modifier modifier){
        return (int) stack.stream().filter(info1 -> info1.modifier.equals(modifier)).count();
    }

    public List<ModifierInfo> getStack(){
        return ImmutableList.copyOf(stack);
    }

    public CompoundTag toNBT(){
        CompoundTag tag = new CompoundTag();
        ListTag modList;
        if (unresolvedSource != null && !edited) {
            //Nothing has been changed, so hand back exactly what was read rather than a stripped copy
            modList = unresolvedSource;
        } else {
            modList = new ListTag();
            for (ModifierInfo info : stack) {
                modList.add(StringTag.valueOf(((ITinkerStationRecipe) info.recipe).getId().toString()));
            }
        }
        tag.put("mods", modList);
        ListTag diffList = new ListTag();
        for (Map.Entry<ModifierId, Integer> entry : incrementalDiffMap.entrySet()) {
            CompoundTag diffNBT = new CompoundTag();
            diffNBT.putString("mod", entry.getKey().toString());
            diffNBT.putInt("amount", entry.getValue());
            diffList.add(diffNBT);
        }
        tag.put("diff", diffList);
        return tag;
    }

    public void fromNBT(CompoundTag tag){
        stack.clear();
        incrementalDiffMap.clear();
        unresolvedSource = null;
        ListTag modList = tag.getList("mods", 8);
        //Memoised index - this used to rescan every recipe in the game once per deserialised blueprint,
        //so opening the planner with N bookmarks cost N full recipe scans
        Map<ResourceLocation, IDisplayModifierRecipe> recipesMap = PlannerScreen.getModifierRecipeIndex();
        boolean allResolved = true;
        for(int i = 0; i < modList.size(); i++){
            String raw = modList.getString(i);
            IDisplayModifierRecipe recipe = null;
            try {
                recipe = recipesMap.get(new ResourceLocation(raw));
            } catch (RuntimeException e) {
                //Malformed id in the saved data; treat it like any other unresolvable entry
            }
            if(recipe != null) {
                stack.add(new ModifierInfo(recipe));
            } else {
                allResolved = false;
                TConPlanner.LOGGER.warn("Modifier recipe '{}' is not available; it will be preserved but not shown", raw);
            }
        }
        if(!allResolved) unresolvedSource = modList.copy();
        edited = false;
        ListTag diffList = tag.getList("diff", 10);
        for(int i = 0; i < diffList.size(); i++){
            CompoundTag diffNBT = diffList.getCompound(i);
            ModifierId modId = new ModifierId(diffNBT.getString("mod"));
            int amount = diffNBT.getInt("amount");
            incrementalDiffMap.put(modId, amount);
        }
    }
}
