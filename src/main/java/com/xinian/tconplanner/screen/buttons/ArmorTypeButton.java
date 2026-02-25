package com.xinian.tconplanner.screen.buttons;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.CreateNarration;
import com.xinian.tconplanner.api.TCArmor;
import com.xinian.tconplanner.screen.PlannerScreen;

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

    private static final CreateNarration DEFAULT_NARRATION = (p_253298_) -> p_253298_.get();

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PlannerScreen.bindTexture();
        RenderSystem.enableBlend();
        parent.blit(graphics, x, y, 213, 41 + (selected ? 18 : 0), 18, 18);
        graphics.renderItem(armor.getRenderStack(), x + 1, y + 1);
        if(isHovered){
            renderToolTip(graphics, mouseX, mouseY);
        }
    }

    public void renderToolTip(GuiGraphics graphics, int mouseX, int mouseY) {
        parent.postRenderTasks.add(() -> parent.renderItemTooltip(graphics, armor.getRenderStack(), mouseX, mouseY));
    }
}