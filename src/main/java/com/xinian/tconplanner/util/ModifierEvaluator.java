package com.xinian.tconplanner.util;

import com.xinian.tconplanner.data.BaseBlueprint;
import com.xinian.tconplanner.data.ModifierInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.recipe.RecipeResult;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import javax.annotation.Nullable;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Memoises the expensive parts of building the modifier list.
 * <p>
 * {@link ITinkerStationRecipe#getValidatedResult} used to run once per recipe on every single
 * {@code PlannerScreen.refresh()} - and refresh happens on every click anywhere in the planner, so a
 * pack with a few hundred modifier recipes paid a few hundred validations per click. Results only
 * depend on the blueprint, so they stay good until the blueprint's NBT changes; that is the same
 * identity {@link BaseBlueprint#equals} already uses.
 * <p>
 * Lives as a field on {@code PlannerScreen} so it survives {@code refresh()}, which only clears widgets.
 */
public class ModifierEvaluator {

    /** State of one modifier relative to the current blueprint */
    public record Evaluation(ModifierStateEnum state, @Nullable Component error, int level) {}

    /**
     * One applicable modifier recipe with everything the list needs to filter and sort it.
     * {@code plainName} is resolved once so sorting does not re-render the Component O(n log n) times.
     */
    public record Candidate(IDisplayModifierRecipe recipe, Modifier modifier, Component displayName,
                            String plainName, String searchKey) {}

    private CompoundTag signature;
    private final Map<String, RecipeResult<?>> validations = new HashMap<>();
    private final Map<String, Evaluation> evaluations = new HashMap<>();
    private final Map<String, ItemStack> previews = new HashMap<>();
    /** Keyed by tool definition - depends only on the tool, so it outlives blueprint edits */
    private final Map<ResourceLocation, List<Candidate>> candidates = new HashMap<>();

    /**
     * Drops everything cached against a stale blueprint. Call once per panel build, before any lookup.
     */
    public void sync(BaseBlueprint<?> blueprint){
        CompoundTag current = blueprint.toNBT();
        if(!current.equals(signature)){
            signature = current;
            validations.clear();
            evaluations.clear();
            previews.clear();
        }
    }

    /**
     * Forces the blueprint-scoped caches to miss. The candidate lists are keyed by tool definition and
     * do not depend on the blueprint at all, so they are deliberately kept - and they die with the
     * screen anyway, which is also when a language or datapack change could have staled them.
     */
    public void invalidate(){
        signature = null;
    }

    /**
     * Every recipe that applies to this tool, pre-sorted by localised name.
     * <p>
     * The tool match walks {@code getToolWithoutModifier()} and builds a {@link ToolStack} per entry,
     * and the sort needs a localised name per modifier - neither depends on the blueprint's materials
     * or modifiers, so both are done once per tool definition instead of once per refresh.
     */
    public List<Candidate> candidatesFor(ToolDefinition definition, List<IDisplayModifierRecipe> recipes){
        return candidates.computeIfAbsent(definition.getId(), id -> {
            List<Candidate> found = new ArrayList<>();
            for(IDisplayModifierRecipe recipe : recipes){
                boolean applies = recipe.getToolWithoutModifier().stream().anyMatch(stack ->
                        !stack.isEmpty() && stack.getItem() instanceof IModifiable && ToolStack.from(stack).getDefinition() == definition);
                if(!applies) continue;
                Modifier modifier = recipe.getDisplayResult().getModifier();
                Component name = modifier.getDisplayName();
                String plain = name.getString();
                String searchKey = (plain + '\n' + modifier.getId()).toLowerCase(Locale.ROOT);
                found.add(new Candidate(recipe, modifier, name, plain, searchKey));
            }
            //Collator so Chinese names sort by pinyin rather than by codepoint
            Collator collator = Collator.getInstance();
            found.sort((a, b) -> collator.compare(a.plainName(), b.plainName()));
            return found;
        });
    }

    public RecipeResult<?> validate(ItemStack result, IDisplayModifierRecipe recipe){
        return validations.computeIfAbsent(key(recipe), key -> {
            Minecraft mc = Minecraft.getInstance();
            if(mc.level == null) return RecipeResult.pass();
            return ((ITinkerStationRecipe) recipe).getValidatedResult(new DummyTinkersStationInventory(result), mc.level.registryAccess());
        });
    }

    /**
     * Applied / available / unavailable plus the reason it cannot be applied.
     * The applied check is cheap, the rest needs a full recipe validation - so only ask for rows you
     * actually draw.
     */
    public Evaluation evaluate(ToolStack tool, ItemStack result, IDisplayModifierRecipe recipe){
        return evaluations.computeIfAbsent(key(recipe), key -> {
            Modifier modifier = recipe.getDisplayResult().getModifier();
            int level = tool.getModifierLevel(modifier);
            RecipeResult<?> validated = validate(result, recipe);
            if(level > 0){
                Component error = !validated.isSuccess() && validated.hasError() ? validated.getMessage() : null;
                return new Evaluation(ModifierStateEnum.APPLIED, error, level);
            }
            if(validated.isSuccess()){
                return new Evaluation(ModifierStateEnum.AVAILABLE, null, 0);
            }
            return new Evaluation(ModifierStateEnum.UNAVAILABLE, validated.hasError() ? validated.getMessage() : null, 0);
        });
    }

    /**
     * The tool you would end up with after adding one more level, for the +/- hover preview.
     * <p>
     * Uses {@link BaseBlueprint#previewWith} rather than {@code clone()} on purpose - cloning goes
     * through NBT and would drag the whole recipe index in on every hovered row.
     */
    public ItemStack previewAdd(BaseBlueprint<?> blueprint, ModifierInfo info){
        return previews.computeIfAbsent("add/" + key(info.recipe), key -> {
            ModifierStack next = blueprint.modStack.copy();
            next.push(info);
            return blueprint.previewWith(next);
        });
    }

    /** The tool you would end up with after removing one level */
    public ItemStack previewRemove(BaseBlueprint<?> blueprint, ModifierInfo info){
        return previews.computeIfAbsent("remove/" + key(info.recipe), key -> {
            ModifierStack next = blueprint.modStack.copy();
            next.pop(info);
            return blueprint.previewWith(next);
        });
    }

    private static String key(IDisplayModifierRecipe recipe){
        return ((ITinkerStationRecipe) recipe).getId().toString();
    }
}
