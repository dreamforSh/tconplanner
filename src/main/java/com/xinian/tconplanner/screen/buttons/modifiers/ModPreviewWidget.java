package com.xinian.tconplanner.screen.buttons.modifiers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.xinian.tconplanner.screen.PlannerScreen;
import org.jetbrains.annotations.NotNull;

public class ModPreviewWidget extends AbstractWidget {
    private final ItemStack stack;
    private final boolean disabled;
    private final PlannerScreen parent;

    public ModPreviewWidget(int x, int y, ItemStack stack, PlannerScreen parent){
        super(x, y, 16, 16, Component.literal(""));
        this.parent = parent;
        this.disabled = stack.isEmpty();
        this.stack = disabled ? new ItemStack(Items.BARRIER) : stack;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PoseStack stack = graphics.pose();
        graphics.renderItem(this.stack, x, y);
        if(isHovered && !disabled){
            renderToolTip(graphics, mouseX, mouseY);
        }
    }

    public void renderToolTip(GuiGraphics graphics, int mouseX, int mouseY) {
        parent.postRenderTasks.add(() -> graphics.renderTooltip(Minecraft.getInstance().font, this.stack, mouseX, mouseY));
    }

    @Override
    public void playDownSound(@NotNull SoundManager sound) {}

    @Override
    public void updateWidgetNarration(NarrationElementOutput p_169152_) {

    }
}
