package com.xinian.tconplanner.screen.buttons;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import com.xinian.tconplanner.api.TCTool;
import com.xinian.tconplanner.screen.PlannerScreen;

public class ToolTypeButton extends Button {

    private final TCTool tool;
    private final boolean selected;
    public final int index;
    private final PlannerScreen parent;

    public ToolTypeButton(int index, TCTool tool, PlannerScreen parent) {
        super(0, 0, 18, 18, tool.getDescription(), button -> parent.setSelectedTool(index));
        this.tool = tool;
        this.index = index;
        this.parent = parent;
        this.selected = parent.blueprint != null && tool == parent.blueprint.tool;
    }

    @Override
    public void renderButton(PoseStack stack, int mouseX, int mouseY, float p_230431_4_) {
        PlannerScreen.bindTexture();
        RenderSystem.enableBlend();
        parent.blit(stack, x, y, 213, 41 + (selected ? 18 : 0), 18, 18);
        Minecraft.getInstance().getItemRenderer().renderGuiItem(tool.getRenderTool(), x + 1, y + 1);
        if(isHovered){
            renderToolTip(stack, mouseX, mouseY);
        }
    }

    @Override
    public void renderToolTip(PoseStack stack, int mouseX, int mouseY) {
        parent.postRenderTasks.add(() -> parent.renderItemTooltip(stack, tool.getRenderTool(), mouseX, mouseY));
    }
}
