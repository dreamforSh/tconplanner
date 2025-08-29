package com.xinian.tconplanner.screen.buttons.modifiers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
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
import java.util.Collections;
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
        super(0, 0, 100, 18, Component.empty(), button -> {
            ModifierSelectButton self = (ModifierSelectButton) button;
            switch (self.state) {
                case AVAILABLE, APPLIED -> {
                    if (self.error == null) {
                        self.parent.selectedModifier = new ModifierInfo(self.recipe);
                        self.parent.refresh();
                    }
                }
            }
        }, DEFAULT_NARRATION);

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
    public void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

        RenderSystem.setShaderTexture(0, PlannerScreen.TEXTURE);
        RenderSystem.enableBlend();

        switch (state) {
            case APPLIED -> RenderSystem.setShaderColor(0.5f, 1f, 0.5f, 1f);
            case UNAVAILABLE -> RenderSystem.setShaderColor(1f, 0.5f, 0.5f, 1f);
            default -> RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }


        guiGraphics.blit(PlannerScreen.TEXTURE, getX(), getY(), 0, 224, this.width, this.height);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f); // Reset color after blit

        if (isHoveredOrFocused()) {

            guiGraphics.renderItem(recipeStacks.get((int) ((System.currentTimeMillis() / 1000) % recipeStacks.size())), getX() + 1, getY() + 1);
        } else {

            ModifierIconManager.renderIcon(guiGraphics, modifier, getX() + 1, getY() + 1, 100, 16);
        }

        Font font = Minecraft.getInstance().font;
        PoseStack poseStack = guiGraphics.pose();

        poseStack.pushPose();

        poseStack.translate(getX() + 20, getY() + 2, 0);
        float nameWidth = font.width(displayName);
        int maxWidth = this.width - 22;
        if (nameWidth > maxWidth) {
            float scale = maxWidth / nameWidth;
            poseStack.scale(scale, scale, 1);
        }

        guiGraphics.drawString(font, displayName, 0, 0, 0xff_ff_ff_ff, false); // No shadow for better looks on custom bg
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(getX() + 20, getY() + 11, 0);
        poseStack.scale(0.5f, 0.5f, 1);
        if (recipe.getSlots() != null) {
            SlotType.SlotCount count = recipe.getSlots();
            MutableComponent text = count.count() == 1 ? TranslationUtil.createComponent("modifiers.usedslot", count.type().getDisplayName()) :
                    TranslationUtil.createComponent("modifiers.usedslots", count.count(), count.type().getDisplayName());
            guiGraphics.drawString(font, text, 0, 0, 0xff_ff_ff_ff, false);
        }
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(getX() + this.width - 1, getY() + 11, 0);
        poseStack.scale(0.5f, 0.5f, 1);
        guiGraphics.drawString(font, levelText, -font.width(levelText), 0, 0xff_ff_ff_ff, false);
        poseStack.popPose();


        if (this.isHovered) {
            this.renderTooltip(guiGraphics, mouseX, mouseY);
        }
    }


    public void renderTooltip(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<Component> tooltips = new ArrayList<>(modifier.getDescriptionList());
        if (error != null) tooltips.add(error.copy().withStyle(ERROR_STYLE));

        parent.renderComponentTooltip(guiGraphics, tooltips, mouseX, mouseY);
    }

    @Override
    public void onPress() {

        super.onPress();
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
        RegistryAccess registryAccess = Minecraft.getInstance().level.registryAccess();
        RecipeResult<?> recipeResult = tsrecipe.getValidatedResult(new DummyTinkersStationInventory(stack),registryAccess);

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
