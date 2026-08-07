package com.xinian.tconplanner.util;

import com.google.common.collect.ImmutableList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
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

    public void push(ModifierInfo info){
        stack.add(info);
    }

    public void pop(ModifierInfo info){
        stack.removeLastOccurrence(info);
    }

    public void moveDown(int index){
        if(index < 0 || index >= stack.size() - 1) return;
        ModifierInfo info = stack.remove(index);
        stack.add(index + 1, info);
    }

    public void moveUp(int index){
        moveDown(index - 1);
    }

    /** Removes one specific entry by position, so duplicate levels of the same modifier stay distinguishable */
    public void removeAt(int index){
        if(index < 0 || index >= stack.size()) return;
        stack.remove(index);
    }

    public void clear(){
        stack.clear();
        incrementalDiffMap.clear();
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
        ListTag modList = new ListTag();
        for (ModifierInfo info : stack) {
            modList.add(StringTag.valueOf(((ITinkerStationRecipe)info.recipe).getId().toString()));
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
        ListTag modList = tag.getList("mods", 8);
        //Memoised index - this used to rescan every recipe in the game once per deserialised blueprint,
        //so opening the planner with N bookmarks cost N full recipe scans
        Map<ResourceLocation, IDisplayModifierRecipe> recipesMap = PlannerScreen.getModifierRecipeIndex();
        for(int i = 0; i < modList.size(); i++){
            ResourceLocation resourceLocation = new ResourceLocation(modList.getString(i));
            if(recipesMap.containsKey(resourceLocation)) {
                push(new ModifierInfo(recipesMap.get(resourceLocation)));
            }
        }
        ListTag diffList = tag.getList("diff", 10);
        for(int i = 0; i < diffList.size(); i++){
            CompoundTag diffNBT = diffList.getCompound(i);
            ModifierId modId = new ModifierId(diffNBT.getString("mod"));
            int amount = diffNBT.getInt("amount");
            incrementalDiffMap.put(modId, amount);
        }
    }
}
