package com.xinian.tconplanner.screen.buttons;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import com.xinian.tconplanner.screen.PlannerScreen;

public class BannerWidget extends AbstractWidget {

    private final PlannerScreen parent;

    public BannerWidget(int x, int y, Component text, PlannerScreen parent) {
        super(x, y, 90, 19, text);
        this.parent = parent;
    }

    @Override
    public void renderButton(PoseStack stack, int mouseX, int mouseY, float p_230431_4_) {
        PlannerScreen.bindTexture();
        parent.blit(stack, x, y, 0, 205, width, height);
        drawCenteredString(stack, Minecraft.getInstance().font, getMessage(), x + width/2, y + 5, 0xff_90_90_ff);
    }

    @Override
    public void playDownSound(SoundManager SoundManager) {}

    @Override
    public void updateNarration(NarrationElementOutput p_169152_) {

    }
}
