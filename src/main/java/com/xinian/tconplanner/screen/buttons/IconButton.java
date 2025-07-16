package com.xinian.tconplanner.screen.buttons;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import com.xinian.tconplanner.screen.PlannerScreen;
import com.xinian.tconplanner.util.Icon;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class IconButton extends Button {

    private final Icon icon;
    private final PlannerScreen parent;
    private SoundEvent pressSound = SoundEvents.UI_BUTTON_CLICK;
    private Color color = Color.WHITE;

    public IconButton(int x, int y, Icon icon, Component tooltip, PlannerScreen parent, Button.OnPress action) {
        super(x, y, 12, 12, tooltip, action);
        this.icon = icon;
        this.parent = parent;
    }

    public IconButton withSound(SoundEvent sound){
        this.pressSound = sound;
        return this;
    }

    public IconButton withColor(Color color){
        this.color = color;
        return this;
    }

    @Override
    public void renderButton(@NotNull PoseStack stack, int mouseX, int mouseY, float p_230431_4_) {
        PlannerScreen.bindTexture();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f, isHovered ? 1 : 0.8F);
        icon.render(parent, stack, x, y);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        if(isHovered){
            renderToolTip(stack, mouseX, mouseY);
        }
    }

    @Override
    public void renderToolTip(@NotNull PoseStack stack, int mouseX, int mouseY) {
        parent.postRenderTasks.add(() -> parent.renderTooltip(stack, this.getMessage(), mouseX, mouseY));
    }

    @Override
    public void playDownSound(@NotNull SoundManager handler) {
        if(pressSound != null)handler.play(SimpleSoundInstance.forUI(pressSound, 1.0F));
    }
}
