package com.xinian.tconplanner.util;

import com.google.common.collect.ImmutableList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import com.xinian.tconplanner.data.ModifierInfo;
import com.xinian.tconplanner.screen.PlannerScreen;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ModifierStack {
    private final LinkedList<ModifierInfo> stack = new LinkedList<>();
    private final HashMap<ModifierId, Integer> incrementalDiffMap = new HashMap<>();

    public void push(ModifierInfo info){
        stack.add(info);
    }

    public void pop(ModifierInfo info){
        stack.removeLastOccurrence(info);
    }

    public void moveDown(int index){
        ModifierInfo info = stack.remove(index);
        stack.add(index + 1, info);
    }

    public void setIncrementalDiff(Modifier modifier, int amount){
        incrementalDiffMap.put(modifier.getId(), Mth.clamp(amount,0, ModifierRecipeLookup.getNeededPerLevel(modifier.getId())));
    }

    public int getIncrementalDiff(Modifier modifier){
        return incrementalDiffMap.getOrDefault(modifier.getId(), 0);
    }

    public boolean isRecipeUsed(ITinkerStationRecipe recipe){
        return stack.stream().anyMatch(info -> ((ITinkerStationRecipe)info.recipe).getId().equals(recipe.getId()));
    }

    public int getLevel(Modifier modifier){
        return (int) stack.stream().filter(info1 -> info1.modifier.equals(modifier)).count();
    }

    // 1. Correct the typo in the method name for better readability.
    public void applyIncrements(ToolStack tool){
        stack.stream().distinct().forEach(info -> {
            Modifier mod = info.modifier;
            int amount = ModifierRecipeLookup.getNeededPerLevel(mod.getId());
            if(amount > 0){
                //
                int needed = ModifierRecipeLookup.getNeededPerLevel(mod.getId());
                int currentDiff = getIncrementalDiff(mod);
                int applyAmount = needed - currentDiff;

                tool.addModifierAmount(mod.getId(), applyAmount, needed);
            }
        });
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
        ListTag modList = tag.getList("mods", 8); // TAG_STRING
        Map<ResourceLocation, IDisplayModifierRecipe> recipesMap = PlannerScreen.getModifierRecipes().stream().collect(Collectors.toMap(recipe -> ((ITinkerStationRecipe)recipe).getId(), recipe -> recipe));
        for(int i = 0; i < modList.size(); i++){
            // 2. Replace ResourceLocation.parse() with the constructor new ResourceLocation().
            ResourceLocation resourceLocation = new ResourceLocation(modList.getString(i));
            if(recipesMap.containsKey(resourceLocation)) {
                push(new ModifierInfo(recipesMap.get(resourceLocation)));
            }
        }
        ListTag diffList = tag.getList("diff", 10); // TAG_COMPOUND
        for(int i = 0; i < diffList.size(); i++){
            CompoundTag diffNBT = diffList.getCompound(i);
            ModifierId modId = new ModifierId(diffNBT.getString("mod"));
            int amount = diffNBT.getInt("amount");
            incrementalDiffMap.put(modId, amount);
        }
    }
}
