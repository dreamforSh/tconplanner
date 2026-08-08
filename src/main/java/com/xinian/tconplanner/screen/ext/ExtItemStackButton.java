package com.xinian.tconplanner.screen.ext;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.CreateNarration;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import com.xinian.tconplanner.EventListener;
import com.xinian.tconplanner.screen.PlannerScreen;
import com.xinian.tconplanner.screen.buttons.BookmarkedButton;
import com.xinian.tconplanner.screen.tooltip.PlannerTooltip;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class ExtItemStackButton extends Button {

    /**
     * The planner's own 18x18 slot frame; v=41 is the resting frame, v=59 the highlighted one.
     * <p>
     * This used to blit {@code tconstruct:textures/gui/tinker_station.png}, which does not exist in
     * Tinkers 3.11 - the jar only ships {@code gui/jei/tinker_station.png} - so the button rendered as
     * the missing-texture magenta, masked in the resting state by an opaque beige fill.
     */
    private static final int PLATE_U = 213;
    private static final int PLATE_V = 41;
    private static final int PLATE_V_HOVER = 59;

    private final ItemStack stack;
    private final Screen screen;
    private final List<Component> tooltips;

    public ExtItemStackButton(int x, int y, ItemStack stack, List<Component> tooltips, Button.OnPress action, Screen screen) {
        super(x, y, 16, 16, Component.literal(""), action, DEFAULT_NARRATION);
        this.stack = stack;
        this.screen = screen;
        this.tooltips = tooltips == null ? Collections.emptyList() : tooltips;
    }

    private static final CreateNarration DEFAULT_NARRATION = (p_253298_) -> p_253298_.get();

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        PlannerScreen.bindTexture();
        RenderSystem.enableBlend();
        graphics.blit(PlannerScreen.TEXTURE, x - 1, y - 1, PLATE_U, isHoveredOrFocused() ? PLATE_V_HOVER : PLATE_V, 18, 18);
        graphics.renderItem(stack, x, y);
        graphics.pose().pushPose();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 0.6f);
        BookmarkedButton.STAR_ICON.render(screen, graphics, x + 2, y + 2);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        graphics.pose().popPose();
        if (this.isHoveredOrFocused()) {
            //The star hint lines are appended to the stack's own tooltip by PlannerTooltip, which also
            //keeps the whole thing inside the station screen at high GUI Scale
            EventListener.postRenderQueue.offer(() ->
                    PlannerTooltip.renderItem(graphics, mc.font, stack, tooltips, mouseX, mouseY));
        }
    }
}