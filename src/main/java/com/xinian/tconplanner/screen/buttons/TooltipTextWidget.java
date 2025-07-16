package com.xinian.tconplanner.screen.buttons;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import com.xinian.tconplanner.screen.PlannerScreen;
import com.xinian.tconplanner.util.TextPosEnum;

import java.util.Collections;
import java.util.List;

public class TooltipTextWidget extends AbstractWidget {

    private final PlannerScreen parent;
    private int color = 0xff_ff_ff_ff;
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
        font = Minecraft.getInstance().font;
        setWidth(font.width(text));
        setHeight(font.lineHeight);
        if(pos == TextPosEnum.CENTER){
            this.x -= getWidth()/2;
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
    public void renderButton(PoseStack stack, int mouseX, int mouseY, float p_230431_4_) {
        drawString(stack, font, getMessage(), x, y, color);
        if(isHoveredOrFocused()){
            renderToolTip(stack, mouseX, mouseY);
        }
    }

    @Override
    public void renderToolTip(PoseStack stack, int mouseX, int mouseY) {
        parent.postRenderTasks.add(() -> parent.renderComponentTooltip(stack, tooltip, mouseX, mouseY));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (this.active && this.visible) {
            return clicked(mouseX, mouseY) && onClick != null && onClick.onClick(mouseX, mouseY, mouseButton);
        } else {
            return false;
        }
    }

    @Override
    public void updateNarration(NarrationElementOutput p_169152_) {}

    public static interface IOnTooltipTextWidgetClick {
        boolean onClick(double mouseX, double mouseY, int mouseButton);
    }
}
