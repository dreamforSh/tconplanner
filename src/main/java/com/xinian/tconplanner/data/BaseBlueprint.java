package com.xinian.tconplanner.data;

import com.xinian.tconplanner.api.IPlannable;
import com.xinian.tconplanner.util.DummyTinkersStationInventory;
import com.xinian.tconplanner.util.ModifierStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.modifiers.ModifierId;

import javax.annotation.Nullable;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.recipe.RecipeResult;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolMaterialHook;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolPartsHook;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.LazyToolStack;
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

    @Nullable
    private ItemStack cachedOutput;
    @Nullable
    private CompoundTag cachedOutputSignature;

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

    /**
     * Whether the planner can represent a tool definition at all.
     * <p>
     * {@code toolParts} comes from {@link ToolPartsHook} and {@code materials} from
     * {@link ToolMaterialHook}; every screen that walks them assumes the two are the same length.
     * Nothing in Tinkers guarantees that - a definition with a {@code material_stats} module but no
     * {@code part_stats} module has zero parts and a non-zero material count, and {@code ToolPartsHook}
     * defaults to an empty list. Tinkers' own travelers_* and slime_* armour is exactly that shape, and
     * the only thing keeping it out of the planner was a hardcoded blacklist of eight item ids - which
     * covered nothing any addon adds. Checking the shape instead covers all of them.
     */
    public static boolean isPlannable(ToolDefinition definition) {
        int parts = ToolPartsHook.parts(definition).size();
        return parts > 0 && parts == ToolMaterialHook.stats(definition).size();
    }

    /**
     * Looks up a saved material id, following renames and refusing the placeholder.
     * <p>
     * {@code MaterialRegistry.getMaterial} answers {@link IMaterial#UNKNOWN} rather than null for an id
     * it does not know, and {@link #isComplete()} only rejects nulls - so an unresolvable material used
     * to sail through as a complete blueprint and get written back to disk as "tconstruct:unknown",
     * permanently losing which material it had been. It also never consulted the redirect table, so the
     * five materials Tinkers ships as redirect-only definitions (bloodbone, chain, platinum,
     * rotten_flesh, tungsten) failed on a stock install.
     *
     * @return the material, or null if the id cannot be resolved - which makes the blueprint incomplete
     */
    @Nullable
    public static IMaterial resolveMaterial(String id) {
        if (id == null || id.isEmpty()) return null;
        MaterialId materialId = new MaterialId(id);
        try {
            materialId = MaterialRegistry.getInstance().resolve(materialId);
        } catch (RuntimeException e) {
            //Registry not ready; fall through to the plain lookup rather than losing the blueprint
        }
        IMaterial material = MaterialRegistry.getMaterial(materialId);
        return material == null || material == IMaterial.UNKNOWN ? null : material;
    }

    /** One replayed modifier step: the tool it produced, and why the recipe refused it if it did */
    public record ModifierStep(ModifierInfo info, ToolStack tool, @Nullable Component error) {}

    /**
     * The finished tool, memoised against this blueprint's serialised state.
     * <p>
     * Building it is expensive - parts, {@code rebuildStats}, and one recipe validation per applied
     * modifier - and it is called once per bookmark on every screen rebuild, plus several more times per
     * refresh. The signature is the same {@code toNBT()} identity {@link #equals} uses, so any edit to
     * materials, modifiers or creative slots invalidates it, and a bookmark that never changes is built
     * exactly once.
     */
    public ItemStack createOutput() {
        CompoundTag signature = toNBT();
        if (cachedOutput != null && signature.equals(cachedOutputSignature)) {
            //Copy: callers hand this to renderers, tooltips and giveItemstack, which may mutate it
            return cachedOutput.copy();
        }
        ItemStack built = createOutput(true);
        cachedOutputSignature = signature;
        cachedOutput = built;
        return built.copy();
    }

    public ItemStack createOutput(boolean applyMods) {
        if (!isComplete()) return ItemStack.EMPTY;

        ToolStack stack = buildBase();
        if (applyMods && !modStack.isEmpty()) {
            List<ModifierStep> steps = replay(stack, modStack, false);
            stack = steps.get(steps.size() - 1).tool();
        }
        stack.rebuildStats();
        return stack.createStack();
    }

    /**
     * The tool with its parts and creative slots but no modifiers applied.
     * <p>
     * Stats are built before it is handed to any recipe, because a recipe's prerequisite checks read
     * the modifier list that {@code rebuildStats} populates from the material traits.
     */
    private ToolStack buildBase() {
        ToolStack stack = ToolStack.from(ToolBuildHandler.buildItemFromMaterials(toolItem, MaterialNBT.of(materials)));
        creativeSlots.forEach((slotType, amount) -> stack.getPersistentData().addSlots(slotType, amount));
        stack.rebuildStats();
        return stack;
    }

    /** Replays {@code order} against this blueprint's parts, one step per applied modifier level */
    public List<ModifierStep> replayModifiers(ModifierStack order) {
        if (!isComplete()) return List.of();
        return replay(buildBase(), order, false);
    }

    /**
     * Applies each modifier by asking its recipe for the resulting tool, instead of adding the modifier
     * and subtracting a cached slot cost by hand.
     * <p>
     * {@link ModifierInfo} caches {@code recipe.getSlots()} once, but for a multi-level recipe that is
     * only ever <em>level one's</em> cost - {@code MultilevelModifierRecipe} passes {@code levels.get(0)}
     * to its superclass while charging {@code LevelEntry.find(levels, newLevel).slots()} at craft time.
     * So {@code returning} (level 1 = one ability slot, levels 2-4 = one upgrade slot each) was billed
     * the wrong amount, of the wrong type, for every level past the first. Taking the recipe's own
     * result also keeps third-party recipe types correct for free.
     * <p>
     * A step the recipe refuses is still force-applied, so the preview keeps showing what the player
     * built rather than collapsing; the error rides along so the UI can mark the exact failing step.
     *
     * @param stopOnError stop after the first refused step. Callers that only need a verdict pass true;
     *                    callers that render a snapshot per step need every step and pass false.
     */
    private static List<ModifierStep> replay(ToolStack base, ModifierStack order, boolean stopOnError) {
        List<ModifierStep> steps = new ArrayList<>();
        Minecraft minecraft = Minecraft.getInstance();
        RegistryAccess access = minecraft.level == null ? null : minecraft.level.registryAccess();
        ToolStack stack = base;

        for (ModifierInfo info : order.getStack()) {
            Component error = null;
            ToolStack next = null;
            if (access != null) {
                RecipeResult<LazyToolStack> result = ((ITinkerStationRecipe) info.recipe)
                        .getValidatedResult(new DummyTinkersStationInventory(stack.createStack()), access);
                if (result.hasError()) {
                    error = result.getMessage();
                } else if (result.isSuccess()) {
                    next = result.getResult().getTool();
                }
            }
            ModifierId id = info.modifier.getId();
            if (next == null) {
                next = stack.copy();
                next.addModifier(id, 1);
                if (info.count != null) {
                    next.getPersistentData().addSlots(info.count.type(), -info.count.count());
                }
            } else if (next.getUpgrades().getLevel(id) <= stack.getUpgrades().getLevel(id)) {
                //An incremental recipe spends the slot but adds the modifier through
                //addModifierAmount(id, availableAmount, neededPerLevel), and availableAmount comes from
                //the container's inputs. DummyTinkersStationInventory reports none, so the amount is 0
                //and addModifierAmount returns immediately - the slot is charged and the modifier never
                //appears. Force the level here, keeping the recipe's own (correct, per-level) slot
                //accounting. 46 incremental_modifier and 2 multilevel_incremental_modifier recipes ship
                //with Tinkers, so this covers a large slice of the modifier list.
                next.addModifier(id, 1);
            }
            next.rebuildStats();
            steps.add(new ModifierStep(info, next, error));
            stack = next;
            if (stopOnError && error != null) break;
        }
        return steps;
    }

    /**
     * The tool this blueprint would produce with a different modifier stack.
     * <p>
     * Deliberately not {@code clone().createOutput()}: {@link #clone} round-trips through NBT, and
     * {@code ModifierStack.fromNBT} needs the recipe index - far too heavy for something the UI calls
     * while the cursor merely hovers a row.
     */
    public ItemStack previewWith(ModifierStack override) {
        ModifierStack saved = this.modStack;
        try {
            this.modStack = override;
            return createOutput();
        } finally {
            this.modStack = saved;
        }
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
        return validateWith(modStack);
    }

    /**
     * Validates this blueprint with one part swapped for a different material, without copying it.
     * <p>
     * {@code MaterialSelectPanel} builds one button per material and each used to {@link #clone} the
     * blueprint first - a full NBT round-trip, 27 times per page turn - purely to change one array slot.
     * Swapping in place and restoring in a {@code finally} is exactly equivalent, since nothing on this
     * path caches against the blueprint.
     */
    public RecipeResult<ItemStack> validateWithMaterial(int slot, IMaterial material) {
        if (slot < 0 || slot >= materials.length) return validate();
        IMaterial previous = materials[slot];
        try {
            materials[slot] = material;
            return validate();
        } finally {
            materials[slot] = previous;
        }
    }

    /** Replays an arbitrary modifier order against this blueprint's parts, in application order */
    public RecipeResult<ItemStack> validateWith(ModifierStack order) {
        if (!isComplete()) return RecipeResult.pass();
        ToolStack stack = buildBase();
        //Only the verdict matters here, so stop at the first refusal - MaterialSelectPanel validates
        //once per material tile and would otherwise keep replaying steps that can no longer succeed
        List<ModifierStep> steps = replay(stack, order, true);
        for (ModifierStep step : steps) {
            if (step.error() != null) return RecipeResult.failure(step.error());
        }
        if (!steps.isEmpty()) stack = steps.get(steps.size() - 1).tool();
        stack.rebuildStats();
        return RecipeResult.success(stack.createStack());
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
