package com.xinian.tconplanner.screen.buttons;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import com.xinian.tconplanner.screen.PlannerScreen;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.tools.part.IToolPart;

public class ToolPartButton extends Button {

    private final ItemStack stack;
    private final IMaterial material;
    public final IToolPart part;
    private final PlannerScreen parent;
    public final int index;

    public ToolPartButton(int index, int x, int y, IToolPart part, IMaterial material, PlannerScreen parent){
        // 1. 更新 super() 调用，添加 DEFAULT_NARRATION
        super(x, y, 16, 16, Component.literal(""), button -> parent.setSelectedPart(index), DEFAULT_NARRATION);
        this.index = index;
        this.part = part;
        this.parent = parent;
        this.material = material;
        stack = material == null ? new ItemStack(part.asItem()) : part.withMaterialForDisplay(material.getIdentifier());
    }


    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack ms = guiGraphics.pose();
        ms.pushPose();
        ms.translate(0, 0, 50);
        boolean selected = parent.selectedPart == index;
        guiGraphics.setColor(1f, 1f, 1f, 0.7f);
        //RenderSystem.enableBlend();
        guiGraphics.blit(
                PlannerScreen.TEXTURE,
                this.getX() - 1,
                this.getY() - 1,
                176 + (material == null ? 18 : 0),
                41 + (selected ? 18 : 0),
                18,
                18
        );
        guiGraphics.setColor(1f, 1f, 1f, 1f);

        guiGraphics.renderItem(this.stack, this.getX(), this.getY());

        if(this.isHoveredOrFocused()){

            parent.postRenderTasks.add(() -> parent.renderItemTooltip(guiGraphics, this.stack, mouseX, mouseY));
        }
        ms.popPose();
    }


}
