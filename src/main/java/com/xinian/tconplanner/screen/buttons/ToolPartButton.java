package com.xinian.tconplanner.screen.buttons;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.entity.ItemRenderer;
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
        super(x, y, 16, 16, Component.literal(""), button -> parent.setSelectedPart(index));
        this.index = index;
        this.part = part;
        this.parent = parent;
        this.material = material;
        stack = material == null ? new ItemStack(part.asItem()) : part.withMaterialForDisplay(material.getIdentifier());
    }

    @Override
    public void renderButton(PoseStack stack, int mouseX, int mouseY, float p_230431_4_) {
        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
        boolean selected = parent.selectedPart == index;
        PoseStack modelStack = RenderSystem.getModelViewStack();
        modelStack.pushPose();
        PlannerScreen.bindTexture();
        modelStack.translate(0, 0, 1);
        RenderSystem.setShaderColor(1f, 1f, 1f, 0.7f);
        RenderSystem.enableBlend();
        parent.blit(stack, x - 1, y - 1, 176 + (material == null ? 18 : 0), 41 + (selected ? 18 : 0), 18, 18);
        modelStack.popPose();
        renderer.renderGuiItem(this.stack, x, y);
        if(isHovered){
            renderToolTip(stack, mouseX, mouseY);
        }
    }

    @Override
    public void renderToolTip(PoseStack stack, int mouseX, int mouseY) {
        parent.postRenderTasks.add(() -> parent.renderItemTooltip(stack, this.stack, mouseX, mouseY));
    }


}
