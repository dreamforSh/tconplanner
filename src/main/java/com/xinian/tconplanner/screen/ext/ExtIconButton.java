package com.xinian.tconplanner.screen.ext;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.CreateNarration;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import com.xinian.tconplanner.EventListener;
import com.xinian.tconplanner.screen.PlannerScreen;
import com.xinian.tconplanner.util.Icon;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.function.Supplier;

public class ExtIconButton extends Button {

    private static final Supplier<Boolean> ALWAYS_TRUE = () -> true;

    private final Icon icon;
    private final Screen screen;
    private final Component tooltip;
    private net.minecraft.sounds.SoundEvent pressSound = null;
    private java.awt.Color color = java.awt.Color.WHITE;

    private Supplier<Boolean> enabledFunc = ALWAYS_TRUE;

    public ExtIconButton(int x, int y, Icon icon, Component tooltip, Button.OnPress action, Screen screen) {
        super(x, y, 16, 16, Component.literal(""), action, DEFAULT_NARRATION);
        this.icon = icon;
        this.screen = screen;
        this.tooltip = tooltip;
    }

    private static final CreateNarration DEFAULT_NARRATION = (p_253298_) -> p_253298_.get();

    public ExtIconButton withSound(SoundEvent sound){
        this.pressSound = sound;
        return this;
    }

    public ExtIconButton withColor(Color color){
        this.color = color;
        return this;
    }

    public ExtIconButton withEnabledFunc(Supplier<Boolean> func){
        this.enabledFunc = func;
        return this;
    }

    @Override
    public boolean mouseClicked(double p_231044_1_, double p_231044_3_, int p_231044_5_) {
        if(!enabledFunc.get())return false;
        return super.mouseClicked(p_231044_1_, p_231044_3_, p_231044_5_);
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if(!enabledFunc.get())return;
        PlannerScreen.bindTexture();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f, isHovered ? 1 : 0.8F);
        icon.render(screen, graphics, x, y);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        
        if (this.isHoveredOrFocused()) {
            EventListener.postRenderQueue.offer(() -> graphics.renderTooltip(Minecraft.getInstance().font, tooltip, mouseX, mouseY));
        }
    }

    @Override
    public void playDownSound(@NotNull SoundManager handler) {
        if(pressSound != null)handler.play(SimpleSoundInstance.forUI(pressSound, 1.0F));
    }
}
