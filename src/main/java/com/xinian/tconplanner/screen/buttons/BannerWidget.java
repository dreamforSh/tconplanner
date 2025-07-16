package com.xinian.tconplanner.screen.buttons;

import com.xinian.tconplanner.screen.PlannerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class BannerWidget extends AbstractWidget {

    public BannerWidget(int x, int y, Component text) {
        super(x, y, 90, 19, text);
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

        guiGraphics.blit(PlannerScreen.TEXTURE, this.getX(), this.getY(), 0, 205, this.getWidth(), this.getHeight());

        Font font = Minecraft.getInstance().font;
        guiGraphics.drawCenteredString(font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + 5, 0xff_90_90_ff);
    }

    @Override
    public void playDownSound(@NotNull SoundManager soundManager) {
    }


    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
    }
}
