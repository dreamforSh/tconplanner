package com.xinian.tconplanner.screen.buttons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import com.xinian.tconplanner.screen.PlannerScreen;
import com.xinian.tconplanner.screen.buttons.modifiers.ModifierTheme;
import org.jetbrains.annotations.NotNull;

public class BannerWidget extends AbstractWidget {

    private final PlannerScreen parent;

    public BannerWidget(int x, int y, Component text, PlannerScreen parent) {
        super(x, y, 90, 19, text);
        this.parent = parent;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PlannerScreen.bindTexture();
        parent.blit(graphics, x, y, 0, 205, width, height);
        //The banner is a fixed 90px sprite, so a long title (ru_ru's "Занесено в закладки") used to
        //spill past both the banner and the 100px panel holding it
        ModifierTheme.fittedCenteredString(graphics, Minecraft.getInstance().font, getMessage(),
                x + width / 2, y + 5, width - 6, 0xff_90_90_ff);
    }

    @Override
    public void playDownSound(@NotNull SoundManager SoundManager) {}

    @Override
    public void updateWidgetNarration(NarrationElementOutput p_169152_) {

    }
}
