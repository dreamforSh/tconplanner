package com.xinian.tconplanner.screen.buttons;

import com.xinian.tconplanner.screen.PlannerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class MatPageButton extends Button {
    private final boolean right;

    public MatPageButton(int x, int y, int change, PlannerScreen parent) {
        super(x, y, 38, 20, Component.literal(""), button -> {
            parent.materialPage += change;
            parent.refresh();
        }, DEFAULT_NARRATION);
        this.right = change > 0;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

        int u = this.right ? 176 : 214;
        int v = this.active ? 20 : 0;


        guiGraphics.blit(
                PlannerScreen.TEXTURE,
                this.getX(),
                this.getY(),
                u,
                v,
                this.width,
                this.height
        );
    }
}
