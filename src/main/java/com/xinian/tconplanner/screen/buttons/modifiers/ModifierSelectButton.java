package com.xinian.tconplanner.screen.buttons.modifiers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import com.xinian.tconplanner.data.ModifierInfo;
import com.xinian.tconplanner.screen.PlannerScreen;
import com.xinian.tconplanner.util.DummyTinkersStationInventory;
import com.xinian.tconplanner.util.ModifierStateEnum;
import com.xinian.tconplanner.util.TranslationUtil;
import slimeknights.tconstruct.library.client.modifiers.ModifierIconManager;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.recipe.RecipeResult;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ModifierSelectButton extends Button {

    private static final Style ERROR_STYLE = Style.EMPTY.withColor(ChatFormatting.RED);

    private final IDisplayModifierRecipe recipe;
    private final Modifier modifier;
    private final Component error;
    private final Component displayName;
    public final ModifierStateEnum state;
    private final PlannerScreen parent;
    private final List<ItemStack> recipeStacks = new ArrayList<>();
    private final Component levelText;


    public ModifierSelectButton(IDisplayModifierRecipe recipe, ModifierStateEnum state, @Nullable Component error, int level, PlannerScreen parent) {
        super(0, 0, 100, 18, Component.literal(""), e -> {});
        this.recipe = recipe;
        this.modifier = recipe.getDisplayResult().getModifier();
        this.parent = parent;
        this.state = state;
        this.error = error;
        for (int i = 0; i < recipe.getInputCount(); i++) {
            recipeStacks.addAll(recipe.getDisplayItems(i));
        }

        boolean hasLevels = !(modifier instanceof NoLevelsModifier);
        this.displayName = level == 0 || !hasLevels ? modifier.getDisplayName() : modifier.getDisplayName(level);


        MutableComponent tempLevelText = Component.literal(hasLevels ? String.valueOf(level) : "");
        if (error != null) {
            tempLevelText.withStyle(ChatFormatting.DARK_RED);
        }
        this.levelText = tempLevelText;
    }

    @Override
    public void renderButton(@Nonnull PoseStack stack, int mouseX, int mouseY, float p_230431_4_) {
        PlannerScreen.bindTexture();
        RenderSystem.enableBlend();
        switch (state) {
            case APPLIED -> RenderSystem.setShaderColor(0.5f, 1f, 0.5f, 1f);
            case UNAVAILABLE -> RenderSystem.setShaderColor(1f, 0.5f, 0.5f, 1f);
            default -> RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
        parent.blit(stack, x, y, 0, 224, 100, 18);
        if (isHoveredOrFocused()) {
            Minecraft.getInstance().getItemRenderer().renderGuiItem(recipeStacks.get((int) ((System.currentTimeMillis() / 1000) % recipeStacks.size())), x + 1, y + 1);
        } else {
            ModifierIconManager.renderIcon(stack, modifier, x + 1, y + 1, 0, 16);
        }
        Font font = Minecraft.getInstance().font;

        stack.pushPose();
        stack.translate(x + 20, y + 2, 0);
        float nameWidth = font.width(displayName);
        int maxWidth = width - 22;
        if (nameWidth > maxWidth) {
            float scale = maxWidth / nameWidth;
            stack.scale(scale, scale, 1);
        }
        Screen.drawString(stack, font, displayName, 0, 0, 0xff_ff_ff_ff);
        stack.popPose();

        stack.pushPose();
        stack.translate(x + 20, y + 11, 0);
        stack.scale(0.5f, 0.5f, 1);
        if (recipe.getSlots() != null) {
            SlotType.SlotCount count = recipe.getSlots();
            MutableComponent text = count.count() == 1 ? TranslationUtil.createComponent("modifiers.usedslot", count.type().getDisplayName()) :
                    TranslationUtil.createComponent("modifiers.usedslots", count.count(), count.type().getDisplayName());
            Screen.drawString(stack, font, text, 0, 0, 0xff_ff_ff_ff);
        }
        stack.popPose();

        stack.pushPose();
        stack.translate(x + width - 1, y + 11, 0);
        stack.scale(0.5f, 0.5f, 1);
        Screen.drawString(stack, font, levelText, -font.width(levelText), 0, 0xff_ff_ff_ff);
        stack.popPose();
        if (isHovered) {
            renderToolTip(stack, mouseX, mouseY);
        }
    }

    @Override
    public void renderToolTip(@Nonnull PoseStack stack, int mouseX, int mouseY) {
        parent.postRenderTasks.add(() -> {
            List<Component> tooltips = new ArrayList<>(modifier.getDescriptionList());
            if (error != null) tooltips.add(error.copy().withStyle(ERROR_STYLE));
            parent.renderComponentTooltip(stack, tooltips, mouseX, mouseY);
        });
    }

    @Override
    public void onPress() {
        switch (state) {
            case AVAILABLE, APPLIED -> {
                if (error == null) { // 只在没有错误（比如等级已满）的情况下执行操作
                    parent.selectedModifier = new ModifierInfo(recipe);
                    parent.refresh();
                }
            }
        }
    }

    @Override
    public void playDownSound(@Nonnull SoundManager sound) {
        if (state == ModifierStateEnum.UNAVAILABLE || error != null) {
            sound.play(SimpleSoundInstance.forUI(SoundEvents.ANVIL_HIT, 1.0F));
        } else {
            super.playDownSound(sound);
        }
    }


    public static ModifierSelectButton create(IDisplayModifierRecipe recipe, ToolStack tstack, ItemStack stack, PlannerScreen screen) {
        ITinkerStationRecipe tsrecipe = (ITinkerStationRecipe) recipe;
        Modifier modifier = recipe.getDisplayResult().getModifier();
        int currentLevel = tstack.getModifierLevel(modifier);

        RecipeResult<ItemStack> recipeResult = tsrecipe.getValidatedResult(new DummyTinkersStationInventory(stack));

        ModifierStateEnum mstate;
        Component error = null;

        if (currentLevel > 0) {

            mstate = ModifierStateEnum.APPLIED;

            if (!recipeResult.isSuccess() && recipeResult.hasError()) {
                error = recipeResult.getMessage();
            }
        } else {

            if (recipeResult.isSuccess()) {
                mstate = ModifierStateEnum.AVAILABLE;
            } else {
                mstate = ModifierStateEnum.UNAVAILABLE;
                if (recipeResult.hasError()) {
                    error = recipeResult.getMessage();
                }
            }
        }

        return new ModifierSelectButton(recipe, mstate, error, currentLevel, screen);
    }
}
