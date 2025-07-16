package com.xinian.tconplanner.screen.buttons;

import com.xinian.tconplanner.screen.PlannerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class SliderWidget extends AbstractWidget {

    private final Consumer<Integer> listener;
    private final int min, max;
    private double percent;
    private int value;

    // 已移除未使用的 'parent' 参数
    public SliderWidget(int x, int y, int width, int height, Consumer<Integer> listener, int min, int max, int value) {
        super(x, y, width, height, Component.literal(""));
        this.listener = listener;
        this.min = min;
        this.max = max;
        this.value = value;
        // 添加一个检查防止除以零
        this.percent = (this.max == this.min) ? 0.0 : (double)(this.value - this.min) / (this.max - this.min);
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int center = this.getY() + this.getHeight() / 2;
        for (int dx = this.getX() - 2; dx < this.getX() + this.getWidth() + 2; dx++) {
            graphics.blit(PlannerScreen.TEXTURE, dx, center - 2, 176, 78, 1, 4);
        }
        int sliderX = this.getX() + (int) (this.getWidth() * this.percent);
        graphics.blit(PlannerScreen.TEXTURE, sliderX - 2, this.getY(), 178, 78, 4, 20);

        Font font = Minecraft.getInstance().font;
        String minStr = String.valueOf(this.min);
        String maxStr = String.valueOf(this.max);
        String valueStr = String.valueOf(this.value);

        int minValSize = font.width(minStr);
        graphics.drawString(font, minStr, this.getX() - minValSize - 5, this.getY() + 6, 0xFFFFFFFF);
        graphics.drawString(font, maxStr, this.getX() + this.getWidth() + 5, this.getY() + 6, 0xFFFFFFFF);
        graphics.drawString(font, valueStr, sliderX - font.width(valueStr) / 2, this.getY() + 22, 0xFFFFFFFF);
    }

    @Override
    public void onClick(double mx, double my) {
        this.updateVal(mx);
    }

    @Override
    protected void onDrag(double mx, double my, double dx, double dy) {
        if (mx >= this.getX() - 5 && my >= this.getY() && mx <= this.getX() + this.getWidth() + 5 && my <= this.getY() + this.getHeight()) {
            this.updateVal(mx);
        }
    }

    private void updateVal(double mouseX) {
        this.percent = Mth.clamp((mouseX - this.getX()) / this.getWidth(), 0, 1);
        int oldVal = this.value;
        this.value = (int)Math.round(Mth.lerp(this.percent, this.min, this.max));
        if (this.value != oldVal) {
            this.listener.accept(this.value);
        }
    }


    @Override
    public void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {

    }
}
