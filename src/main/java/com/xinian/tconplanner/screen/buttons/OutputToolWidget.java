package com.xinian.tconplanner.screen.buttons;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import com.xinian.tconplanner.screen.PlannerScreen;
import org.jetbrains.annotations.NotNull;

public class OutputToolWidget extends AbstractWidget {

    private final ItemStack stack;
    private final PlannerScreen parent;

    public OutputToolWidget(int x, int y, ItemStack stack, PlannerScreen parent){
        super(x, y, 16, 16, Component.literal(""));
        this.parent = parent;
        this.stack = stack;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PlannerScreen.bindTexture();
        parent.blit(graphics, x - 6, y - 6, 176, 117, 28, 28);
        graphics.renderItem(this.stack, x, y);
        if(isHovered){
            renderToolTip(graphics, mouseX, mouseY);
        }
    }

    public void renderToolTip(GuiGraphics graphics, int mouseX, int mouseY) {
        parent.postRenderTasks.add(() -> parent.renderItemTooltip(graphics, this.stack, mouseX, mouseY));
    }

    @Override
    public void playDownSound(@NotNull SoundManager sound) {}

    @Override
    public void updateWidgetNarration(NarrationElementOutput p_169152_) {
        // No narration needed
    }
}
