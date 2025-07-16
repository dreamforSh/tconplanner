package com.xinian.tconplanner.screen.buttons.modifiers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

public class ModPreviewWidget extends AbstractWidget {
    private final ItemStack stack;
    private final boolean disabled;

    public ModPreviewWidget(int x, int y, ItemStack stack) {
        super(x, y, 16, 16, Component.literal(""));
        this.disabled = stack.isEmpty();
        this.stack = disabled ? new ItemStack(Items.BARRIER) : stack;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.renderFakeItem(this.stack, this.getX(), this.getY());

        if (this.isHoveredOrFocused() && !this.disabled) {
            Font font = Minecraft.getInstance().font;
            guiGraphics.renderTooltip(font, this.stack, mouseX, mouseY);
        }
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
        // 作为一个纯展示控件，此处留空，不提供旁白。
    }
}
