package com.xinian.tconplanner.screen.buttons;

import com.mojang.blaze3d.systems.RenderSystem;
import com.xinian.tconplanner.data.Blueprint;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import com.xinian.tconplanner.api.TCTool;
import com.xinian.tconplanner.screen.PlannerScreen;

public class ToolTypeButton extends Button {

    private final TCTool tool;
    private final boolean selected;
    public final int index;
    private final PlannerScreen parent;

    public ToolTypeButton(int index, TCTool tool, PlannerScreen parent) {

        super(0, 0, 18, 18, tool.getDescription(), button -> parent.setSelectedTool(index), DEFAULT_NARRATION);
        this.tool = tool;
        this.index = index;
        this.parent = parent;
        this.selected = parent.blueprint instanceof Blueprint && tool == ((Blueprint) parent.blueprint).plannable;
    }


    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

        RenderSystem.enableBlend();


        guiGraphics.blit(
                PlannerScreen.TEXTURE,
                this.getX(),
                this.getY(),
                213,
                41 + (selected ? 18 : 0),
                18,
                18
        );


        guiGraphics.renderFakeItem(tool.getRenderStack(), this.getX() + 1, this.getY() + 1);


        if(isHoveredOrFocused()){

            parent.postRenderTasks.add(() -> parent.renderItemTooltip(guiGraphics, tool.getRenderStack(), mouseX, mouseY));
        }

        RenderSystem.disableBlend();
    }

}
