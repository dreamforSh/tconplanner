package com.xinian.tconplanner.screen.buttons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import com.xinian.tconplanner.screen.PlannerScreen;
import com.xinian.tconplanner.util.TextPosEnum;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class TooltipTextWidget extends AbstractWidget {

    private final PlannerScreen parent;
    private int color = 0xffffffff;
    private final Font font;
    private final List<Component> tooltip;

    private IOnTooltipTextWidgetClick onClick;

    public TooltipTextWidget(int x, int y, Component text, Component tooltip, PlannerScreen parent) {
        this(x, y, TextPosEnum.LEFT, text, tooltip, parent);
    }

    public TooltipTextWidget(int x, int y, TextPosEnum pos, Component text, Component tooltip, PlannerScreen parent) {
        this(x, y, pos, text, Collections.singletonList(tooltip), parent);
    }

    public TooltipTextWidget(int x, int y, TextPosEnum pos, Component text, List<Component> tooltip, PlannerScreen parent) {
        super(x, y, 0, 0, text);
        this.parent = parent;
        this.tooltip = tooltip;
        this.font = Minecraft.getInstance().font;
        this.setWidth(this.font.width(text));
        this.setHeight(this.font.lineHeight);

        // 1. 使用 setX() 和 getX() 来修改坐标
        if(pos == TextPosEnum.CENTER){
            this.setX(this.getX() - this.getWidth() / 2);
        }
    }

    public TooltipTextWidget withColor(int color){
        this.color = color;
        return this;
    }

    public TooltipTextWidget withClickHandler(IOnTooltipTextWidgetClick onClick){
        this.onClick = onClick;
        return this;
    }


    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

        guiGraphics.drawString(this.font, getMessage(), this.getX(), this.getY(), this.color);


        if(this.isHoveredOrFocused()){

            parent.postRenderTasks.add(() -> parent.renderComponentTooltip(guiGraphics, tooltip, mouseX, mouseY));
        }
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (this.active && this.visible) {

            return clicked(mouseX, mouseY) && onClick != null && onClick.onClick(mouseX, mouseY, mouseButton);
        } else {
            return false;
        }
    }


    public interface IOnTooltipTextWidgetClick {
        boolean onClick(double mouseX, double mouseY, int mouseButton);
    }


    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
         //this.defaultButtonNarrationText(narrationElementOutput)

    }
}
