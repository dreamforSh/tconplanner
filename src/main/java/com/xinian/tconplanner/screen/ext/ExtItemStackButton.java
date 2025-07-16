package com.xinian.tconplanner.screen.ext;

import com.mojang.blaze3d.systems.RenderSystem;
import com.xinian.tconplanner.EventListener;
import com.xinian.tconplanner.screen.buttons.BookmarkedButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class ExtItemStackButton extends Button {
    public static final ResourceLocation BACKGROUND = new ResourceLocation("tconstruct", "textures/gui/tinker_station.png");

    private final ItemStack stack;
    private final List<Component> tooltips;

    public ExtItemStackButton(int x, int y, ItemStack stack, List<Component> tooltips, Button.OnPress action) {
        super(x, y, 16, 16, Component.literal(""), action, DEFAULT_NARRATION);
        this.stack = stack;
        this.tooltips = tooltips == null ? Collections.emptyList() : tooltips;
    }


    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();


        guiGraphics.blit(BACKGROUND, this.getX() - 1, this.getY() - 1, 194, 0, 18, 18);

        if (!isHoveredOrFocused()) {

            guiGraphics.fill(this.getX(), this.getY(), this.getX() + 16, this.getY() + 16, 0xff_a29b81);
        }


        guiGraphics.renderItem(stack, this.getX(), this.getY());


        guiGraphics.pose().pushPose();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 0.6f);


        BookmarkedButton.STAR_ICON.render(guiGraphics, this.getX() + 2, this.getY() + 2);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        guiGraphics.pose().popPose();

        if (this.isHoveredOrFocused()) {

            EventListener.postRenderQueue.offer(() -> {

                List<Component> combinedTooltips = Stream.concat(
                        Screen.getTooltipFromItem(mc, stack).stream(),
                        tooltips.stream()
                ).toList();


                guiGraphics.renderComponentTooltip(mc.font, combinedTooltips, mouseX, mouseY);
            });
        }
    }
}
