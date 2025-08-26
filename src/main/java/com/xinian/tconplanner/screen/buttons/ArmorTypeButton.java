package com.xinian.tconplanner.screen.buttons;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import com.xinian.tconplanner.api.TCArmor;
import com.xinian.tconplanner.screen.PlannerScreen;

public class ArmorTypeButton extends Button {

    private final TCArmor armor;
    private final boolean selected;
    public final int index;
    private final PlannerScreen parent;

    public ArmorTypeButton(int index, TCArmor armor, PlannerScreen parent) {
        super(0, 0, 18, 18, armor.getDescription(), button -> parent.setSelectedArmor(index));
        this.armor = armor;
        this.index = index;
        this.parent = parent;
        this.selected = parent.blueprint != null && armor == parent.blueprint.plannable;
    }

    @Override
    public void renderButton(PoseStack stack, int mouseX, int mouseY, float p_230431_4_) {
        PlannerScreen.bindTexture();
        RenderSystem.enableBlend();
        parent.blit(stack, x, y, 213, 41 + (selected ? 18 : 0), 18, 18);
        Minecraft.getInstance().getItemRenderer().renderGuiItem(armor.getRenderStack(), x + 1, y + 1);
        if(isHovered){
            renderToolTip(stack, mouseX, mouseY);
        }
    }

    @Override
    public void renderToolTip(PoseStack stack, int mouseX, int mouseY) {
        parent.postRenderTasks.add(() -> parent.renderItemTooltip(stack, armor.getRenderStack(), mouseX, mouseY));
    }
}
