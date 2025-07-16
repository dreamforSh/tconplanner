package com.xinian.tconplanner.screen.buttons;

import com.xinian.tconplanner.screen.PlannerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class OutputToolWidget extends AbstractWidget {

    private final ItemStack stack;

    public OutputToolWidget(int x, int y, ItemStack stack) {
        super(x, y, 16, 16, Component.literal(""));
        this.stack = stack;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

        guiGraphics.blit(PlannerScreen.TEXTURE, this.getX() - 6, this.getY() - 6, 176, 117, 28, 28);


        guiGraphics.renderFakeItem(this.stack, this.getX(), this.getY());


        if (this.isHoveredOrFocused()) {
            guiGraphics.renderTooltip(Minecraft.getInstance().font, this.stack, mouseX, mouseY);
        }
    }


    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput pNarrationElementOutput) {

    }
}
