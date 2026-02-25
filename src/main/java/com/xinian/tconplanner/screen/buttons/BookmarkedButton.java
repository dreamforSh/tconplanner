package com.xinian.tconplanner.screen.buttons;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.CreateNarration;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import com.xinian.tconplanner.data.BaseBlueprint;
import com.xinian.tconplanner.screen.PlannerScreen;
import com.xinian.tconplanner.util.Icon;

public class BookmarkedButton extends Button {

    public static final Icon STAR_ICON = new Icon(6, 0);

    private final PlannerScreen parent;
    private final ItemStack stack;
    private final int index;
    private final BaseBlueprint<?> blueprint;
    private final boolean starred;
    private boolean selected;

    public BookmarkedButton(int index, BaseBlueprint<?> blueprint, boolean starred, PlannerScreen parent){
        super(0, 0, 18, 18, Component.literal(""), button -> parent.setBlueprint(blueprint.clone()), DEFAULT_NARRATION);
        this.index = index;
        this.blueprint = blueprint;
        this.starred = starred;
        this.parent = parent;
        stack = blueprint.createOutput();
        this.selected = parent.blueprint != null && parent.blueprint.equals(blueprint);
    }

    private static final CreateNarration DEFAULT_NARRATION = (p_253298_) -> p_253298_.get();

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PlannerScreen.bindTexture();
        RenderSystem.enableBlend();
        parent.blit(graphics, x, y, 213, 41 + (selected ? 18 : 0), 18, 18);
        graphics.renderItem(this.stack, x + 1, y + 1);
        if(starred){
            graphics.pose().pushPose();
            graphics.pose().translate(x + 11, y + 11, 105);
            graphics.pose().scale(0.5f, 0.5f, 0.5f);
            STAR_ICON.render(parent, graphics, 0, 0);
            graphics.pose().popPose();
        }
        if(isHovered){
            renderToolTip(graphics, mouseX, mouseY);
        }
    }

    public void renderToolTip(GuiGraphics graphics, int mouseX, int mouseY) {
        parent.postRenderTasks.add(() -> parent.renderItemTooltip(graphics, this.stack, mouseX, mouseY));
    }
}
