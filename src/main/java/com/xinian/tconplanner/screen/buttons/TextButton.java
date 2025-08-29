package com.xinian.tconplanner.screen.buttons;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import com.xinian.tconplanner.screen.PlannerScreen;

import java.util.List; // 导入 List 类

public class TextButton extends Button {

    private final PlannerScreen parent;;
    private final Runnable onPress;
    private int color = 0xff_ff_ff;
    private Component tooltip = null;

    public TextButton(int x, int y, Component text, Runnable onPress, PlannerScreen parent) {
        super(x, y, 58, 18, text, button -> {}, DEFAULT_NARRATION);
        this.parent = parent;
        this.onPress = onPress;
    }

    public TextButton withColor(int color) {
        this.color = color;
        return this;
    }

    public TextButton withTooltip(Component tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    public TextButton withWidth(int width) {
        this.width = width;
        return this;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        //RenderSystem.enableBlend();

        guiGraphics.setColor(
                ((color & 0xff0000) >> 16) / 255f,
                ((color & 0x00ff00) >> 8) / 255f,
                (color & 0x0000ff) / 255f,
                1f
        );


        guiGraphics.blit(PlannerScreen.TEXTURE, this.getX(), this.getY(), 176, 183, this.width, this.height);

        guiGraphics.setColor(1f, 1f, 1f, 1f);

        guiGraphics.drawCenteredString(
                Minecraft.getInstance().font,
                getMessage(),
                this.getX() + this.width / 2,
                this.getY() + (this.height - 8) / 2,
                isHoveredOrFocused() ? 0xffffffff : 0xa0ffffff
        );

        if (this.isHoveredOrFocused() && this.tooltip != null) {

            parent.postRenderTasks.add(() -> parent.renderComponentTooltip(guiGraphics, List.of(tooltip), mouseX, mouseY));
        }
    }

    @Override
    public void onPress() {
        onPress.run();
    }
}
