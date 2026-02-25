package com.xinian.tconplanner.screen.ext;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.CreateNarration;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import com.xinian.tconplanner.EventListener;
import com.xinian.tconplanner.screen.buttons.BookmarkedButton;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExtItemStackButton extends Button {
    public static ResourceLocation BACKGROUND = new ResourceLocation("tconstruct", "textures/gui/tinker_station.png");

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
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, BACKGROUND);
        graphics.blit(BACKGROUND, x - 1, y - 1, 194, 0, 18, 18);
        if(!isHoveredOrFocused()){
            graphics.fill(x, y, x + 16, y + 16, 0xff_a29b81);
        }
        graphics.renderItem(stack, x, y);
        graphics.pose().pushPose();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 0.6f);
        BookmarkedButton.STAR_ICON.render(screen, graphics, x + 2, y + 2);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        graphics.pose().popPose();
        if (this.isHoveredOrFocused()) {
            EventListener.postRenderQueue.offer(() -> {
                List<Component> result = Stream.concat(screen.getTooltipFromItem(mc, stack).stream(), tooltips.stream()).collect(Collectors.toList());
                graphics.renderTooltip(Minecraft.getInstance().font, result, Optional.empty(), mouseX, mouseY);
            });
        }
    }
}