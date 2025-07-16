package com.xinian.tconplanner.screen;

import com.xinian.tconplanner.data.Blueprint;
import com.xinian.tconplanner.data.ModifierInfo;
import com.xinian.tconplanner.screen.buttons.*;
import com.xinian.tconplanner.screen.buttons.modifiers.*;
import com.xinian.tconplanner.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.impl.DurabilityShieldModifier;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.recipe.RecipeResult;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.*;

public class ModifierPanel extends PlannerPanel {
    public static final String KEY_MAX_LEVEL = TConstruct.makeTranslationKey("recipe", "modifier.max_level");
    private static final SlotType[] ValidSlots = new SlotType[]{SlotType.UPGRADE, SlotType.ABILITY};

    public ModifierPanel(int x, int y, int width, int height, ItemStack result, ToolStack tool, List<IDisplayModifierRecipe> modifiers, PlannerScreen parent) {
        super(x, y, width, height, parent);

        int slotIndex = 0;
        for (SlotType slotType : ValidSlots) {
            int slots = tool.getFreeSlots(slotType);
            List<Component> tooltips = new ArrayList<>();
            Component coloredName = Component.literal("")
                    .withStyle(Style.EMPTY.withColor(slotType.getColor()))
                    .append(slotType.getDisplayName())
                    .append(Component.literal("").withStyle(ChatFormatting.RESET));
            tooltips.add(TranslationUtil.createComponent("slots.available", coloredName));
            tooltips.add(Component.literal(""));
            tooltips.add(TranslationUtil.createComponent("modifiers.addcreativeslot").copy().withStyle(ChatFormatting.GREEN));
            MutableComponent removeCreativeSlotTextComponent = TranslationUtil.createComponent("modifiers.removecreativeslot").copy().withStyle(ChatFormatting.RED);
            if (slots == 0) {
                removeCreativeSlotTextComponent.withStyle(removeCreativeSlotTextComponent.getStyle().applyFormats(ChatFormatting.STRIKETHROUGH));
            }
            tooltips.add(removeCreativeSlotTextComponent);
            MutableComponent slotsRemaining = Component.literal("" + slots);
            int creativeSlots = parent.blueprint.creativeSlots.getOrDefault(slotType, 0);
            if (creativeSlots > 0) {
                slotsRemaining.append(" (+" + parent.blueprint.creativeSlots.get(slotType) + ")");
            }
            addChild(new TooltipTextWidget(108, 23 + slotIndex * 12, TextPosEnum.LEFT, slotsRemaining, tooltips, parent)
                    .withColor(slotType.getColor().getValue() + 0xff_00_00_00)
                    .withClickHandler((mouseX, mouseY, mouseButton) -> handleCreativeSlotButton(slotType, slots, creativeSlots, mouseButton)));
            slotIndex++;
        }
        addChild(new BannerWidget(7, 0, TranslationUtil.createComponent("banner.modifiers")));
        int modGroupStartY = 23;
        int modGroupStartX = 2;
        Blueprint blueprint = parent.blueprint;
        ModifierInfo selectedModifier = parent.selectedModifier;
        ModifierStack modifierStack = parent.modifierStack;


        if (modifierStack != null) {
            HashMap<ModifierId, Integer> levelCount = new HashMap<>();
            PaginatedPanel<ModifierStackButton> stackGroup = new PaginatedPanel<>(modGroupStartX, modGroupStartY, 100, 18, 1, 5, 2, "modifierstackgroup", parent);
            addChild(stackGroup);
            ToolStack displayStack = ToolStack.from(blueprint.createOutput(false));
            List<ModifierInfo> modStack = modifierStack.getStack();
            Blueprint resultingBlueprint = parent.blueprint.clone();
            resultingBlueprint.modStack = modifierStack;
            RecipeResult<?> validatedResult = resultingBlueprint.validate();
            boolean isValid = !validatedResult.hasError();
            for (int i = 0; i < modStack.size(); i++) {
                ModifierInfo info = modStack.get(i);
                int newLevel = levelCount.getOrDefault(info.modifier.getId(), 0) + 1;
                levelCount.put(info.modifier.getId(), newLevel);
                displayStack.addModifier(info.modifier.getId(), 1);
                if (info.count != null) {
                    displayStack.getPersistentData().addSlots(info.count.type(), -info.count.count());
                }
                displayStack.rebuildStats();
                stackGroup.addPageChild(new ModifierStackButton(info, i, newLevel, displayStack.copy().createStack(), parent));
            }
            stackGroup.refresh();
            addChild(new TextButton(2 + 50 - 58 / 2, 158, TranslationUtil.createComponent("modifierstack.save"), () -> {
                if (isValid) {
                    parent.blueprint.modStack = parent.modifierStack;
                    parent.modifierStack = null;
                    parent.refresh();
                }
            }, parent).withColor(isValid ? 0x50ff50 : 0x1a0000).withTooltip(isValid ? null : validatedResult.getMessage()));
            addChild(new TextButton(2 + 50 - 58 / 2, 180, TranslationUtil.createComponent("modifierstack.cancel"), () -> {
                parent.modifierStack = null;
                parent.refresh();
            }, parent).withColor(0xe02121));

            if (parent.selectedModifierStackIndex != -1) {
                addChild(new StackMoveButton(2 + 50 - 9, 130, true, stackGroup, parent));
                addChild(new StackMoveButton(2 + 50 - 9, 141, false, stackGroup, parent));
            }
        } else if (selectedModifier == null) {
            PaginatedPanel<ModifierSelectButton> modifiersGroup = new PaginatedPanel<>(modGroupStartX, modGroupStartY, 100, 18, 1, 9, 2, "modifiersgroup", parent);
            addChild(modifiersGroup);
            for (IDisplayModifierRecipe recipe : modifiers) {
                if (recipe.getToolWithoutModifier().stream().anyMatch(stack -> ToolStack.from(stack).getDefinition() == blueprint.toolDefinition)) {
                    modifiersGroup.addPageChild(ModifierSelectButton.create(recipe, tool, result, parent));
                }
            }
            modifiersGroup.sort(Comparator.comparingInt(value -> value.state.ordinal()));
            modifiersGroup.refresh();
            addChild(new IconButton(100, 0, new Icon(5, 0),
                    TranslationUtil.createComponent("editmodifierstack"), e -> {
                parent.modifierStack = blueprint.clone().modStack;
                parent.selectedModifierStackIndex = -1;
                parent.refresh();
            }));
        } else {
            ModifierSelectButton modSelectButton = ModifierSelectButton.create(selectedModifier.recipe, tool, result, parent);
            modSelectButton.setX(modGroupStartX);
            modSelectButton.setY(modGroupStartY);
            addChild(modSelectButton);

            Modifier modifier = selectedModifier.modifier;
            ITinkerStationRecipe tsrecipe = (ITinkerStationRecipe) selectedModifier.recipe;

            addChild(new ModPreviewWidget(2 + 50 - 9, 50, result));
            int arrowOffset = 11;
            RegistryAccess registryAccess = Minecraft.getInstance().level.registryAccess();
            ModLevelButton addButton = new ModLevelButton(2 + 50 + arrowOffset - 2, 50, 1, parent);
            RecipeResult<?> validatedResultAdd = (modifier instanceof NoLevelsModifier || modifier instanceof DurabilityShieldModifier) && tool.getModifierLevel(modifier) >= 1 ?
                    RecipeResult.failure(KEY_MAX_LEVEL, modifier.getDisplayName(), 1) : tsrecipe.getValidatedResult(new DummyTinkersStationInventory(result),registryAccess);
            if (!validatedResultAdd.isSuccess()) {
                addButton.disable(validatedResultAdd.getMessage().copy().setStyle(Style.EMPTY.withColor(ChatFormatting.RED)));

                addChild(new ModPreviewWidget(addButton.getX() + addButton.getWidth() + 2, 50, ItemStack.EMPTY));
            } else if (blueprint.modStack.getIncrementalDiff(modifier) > 0) {
                addButton.disable(TranslationUtil.createComponent("modifiers.error.incrementnotmax").copy().setStyle(Style.EMPTY.withColor(ChatFormatting.RED)));

                addChild(new ModPreviewWidget(addButton.getX() + addButton.getWidth() + 2, 50, ItemStack.EMPTY));
            } else {
                Blueprint copy = blueprint.clone();
                copy.modStack.push(selectedModifier);

                addChild(new ModPreviewWidget(addButton.getX() + addButton.getWidth() + 2, 50, copy.createOutput()));
            }
            addChild(addButton);

            ModLevelButton subtractButton = new ModLevelButton(2 + 50 - arrowOffset - 18, 50, -1, parent);
            RecipeResult<ItemStack> validatedResultSubtract = ToolValidator.validateModRemoval(blueprint, tool, selectedModifier);
            if (validatedResultSubtract.hasError()) {
                subtractButton.disable(((MutableComponent) validatedResultSubtract.getMessage()).setStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
            }

            addChild(new ModPreviewWidget(subtractButton.getX() - 2 - 18, 50, subtractButton.isDisabled() ? ItemStack.EMPTY : validatedResultSubtract.getResult()));
            addChild(subtractButton);
            int perLevel = ModifierRecipeLookup.getNeededPerLevel(modifier.getId());
            if (perLevel > 0 && blueprint.modStack.getLevel(modifier) > 0) {
                addChild(new SliderWidget(2 + 10, 70, 80, 20, val -> {
                    blueprint.modStack.setIncrementalDiff(modifier, perLevel - val);
                    parent.refresh();
                }, 1, perLevel, perLevel - blueprint.modStack.getIncrementalDiff(modifier)));
            }

            addChild(new TextButton(2 + 50 - 58 / 2, 115, TranslationUtil.createComponent("modifiers.exit"), () -> {
                parent.selectedModifier = null;
                parent.refresh();
            }, parent).withColor(0xe02121));
        }
    }


    private boolean handleCreativeSlotButton(SlotType type, int remainingSlots, int creativeSlots, int mb) {
        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        if (mb == 0) {
            parent.blueprint.addCreativeSlot(type);
            parent.refresh();
            soundManager.play(SimpleSoundInstance.forUI(SoundEvents.ANVIL_PLACE, 2f, 0.08f));
            return true;
        }
        if (mb == 1) {
            if (creativeSlots > 0 && remainingSlots > 0) {
                parent.blueprint.removeCreativeSlot(type);
                parent.refresh();
                soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_TAKE_RESULT, 2f, 0.08f));
                return true;
            }
            soundManager.play(SimpleSoundInstance.forUI(SoundEvents.BAMBOO_FALL, 2f, 0.08f));
        }
        return false;
    }
}
