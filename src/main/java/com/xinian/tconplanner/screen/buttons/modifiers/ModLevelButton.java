package com.xinian.tconplanner.screen.buttons.modifiers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.xinian.tconplanner.screen.PlannerScreen;
import com.xinian.tconplanner.util.ModifierStack;
import com.xinian.tconplanner.util.TranslationUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

public class ModLevelButton extends Button {

    private final int change;
    private boolean disabled = false;
    private final PlannerScreen parent;
    private Component tooltip;

    public ModLevelButton(int x, int y, int change, PlannerScreen parent) {

        super(x, y, 18, 17, Component.literal(""), (button) -> {}, DEFAULT_NARRATION);

        this.parent = parent;
        this.change = change;
        tooltip = TranslationUtil.createComponent(change < 0 ? "modifiers.removelevel" : "modifiers.addlevel")
                .setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
        this.setTooltip(Tooltip.create(tooltip));
    }

    public void disable(Component tooltip) {
        this.disabled = true;
        this.tooltip = tooltip;
    }

    public boolean isDisabled() {
        return disabled;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.setColor(1f, 1f, 1f, this.disabled ? 0.5f : 1f);

        int u = this.change > 0 ? 176 : 194;
        int v = this.disabled ? 146 : 163;

        guiGraphics.blit(PlannerScreen.TEXTURE, this.getX(), this.getY(), u, v, this.width, this.height, 256, 256);

        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    public void onPress() {
        if(!disabled) {
            ModifierStack stack = parent.blueprint.modStack;
            stack.setIncrementalDiff(parent.selectedModifier.modifier, 0);
            if(change > 0)stack.push(parent.selectedModifier);
            else stack.pop(parent.selectedModifier);
            parent.refresh();
        }
    }

    @Override
    public void playDownSound(@NotNull SoundManager handler) {
        if (!this.disabled) {
            super.playDownSound(handler);
        }
    }
}
