package com.xinian.tconplanner.screen.buttons;

import com.xinian.tconplanner.util.Icon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;

public class IconButton extends Button {

    private final Icon icon;
    private Holder<SoundEvent> pressSound = SoundEvents.UI_BUTTON_CLICK;
    private int color = 0xFFFFFFFF;

    public IconButton(int x, int y, Icon icon, Component tooltip, Button.OnPress action) {
        super(x, y, 12, 12, tooltip, action, DEFAULT_NARRATION);
        this.icon = icon;
    }

    public IconButton withSound(Holder<SoundEvent> sound) {
        this.pressSound = sound;
        return this;
    }


    public IconButton withColor(int color) {
        this.color = color;
        return this;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

        float alpha = (this.isHoveredOrFocused() ? 1.0F : 0.8F) * ((color >> 24) & 0xFF) / 255.0F;
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;


        guiGraphics.setColor(red, green, blue, alpha);
        this.icon.render(guiGraphics, getX(), getY());


        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (this.isHoveredOrFocused()) {
            guiGraphics.renderTooltip(Minecraft.getInstance().font, this.getMessage(), mouseX, mouseY);
        }
    }

    @Override
    public void playDownSound(@NotNull SoundManager handler) {
        if (this.pressSound != null) {
            handler.play(SimpleSoundInstance.forUI(this.pressSound, 1.0F));
        }
    }
}
