package com.xinian.tconplanner.screen.buttons;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.xinian.tconplanner.api.TCArmor;
import com.xinian.tconplanner.screen.PlannerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;

public class ArmorTypeButton extends Button {

    private final TCArmor armor;
    private final boolean selected;
    public final int index;
    private final PlannerScreen parent;

    public ArmorTypeButton(int index, TCArmor armor, PlannerScreen parent) {
        super(0, 0, 18, 18, armor.getDescription(), button -> parent.setSelectedArmor(index), DEFAULT_NARRATION);
        this.armor = armor;
        this.index = index;
        this.parent = parent;
        this.selected = parent.blueprint != null && armor == parent.blueprint.plannable;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float p_230431_4_) {
        guiGraphics.blit(PlannerScreen.TEXTURE, this.getX(), this.getY(), 213, 41 + (selected ? 18 : 0), 18, 18);
        guiGraphics.renderFakeItem(armor.getRenderStack(), this.getX() + 1, this.getY() + 1);
        if(isHovered){
            renderToolTip(guiGraphics, mouseX, mouseY);
        }
    }

    public void renderToolTip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            parent.postRenderTasks.add(() -> parent.renderItemTooltip(guiGraphics, armor.getRenderStack(), mouseX, mouseY));
    }
}
