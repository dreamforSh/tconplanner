package com.xinian.tconplanner.screen.ext;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics; // 1. Import GuiGraphics
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.Holder; // 2. Import Holder
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import com.xinian.tconplanner.EventListener;
import com.xinian.tconplanner.screen.PlannerScreen;
import com.xinian.tconplanner.util.Icon;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List; // 3. Import List for tooltip
import java.util.function.Supplier;

public class ExtIconButton extends Button {

    private static final Supplier<Boolean> ALWAYS_TRUE = () -> true;

    private final Icon icon;
    private final Screen screen;
    private final Component tooltip;
    private Holder<SoundEvent> pressSound = SoundEvents.UI_BUTTON_CLICK;
    private Color color = Color.WHITE;
    private Supplier<Boolean> enabledFunc = ALWAYS_TRUE;

    public ExtIconButton(int x, int y, Icon icon, Component tooltip, Button.OnPress action, Screen screen) {
        // 6. Simplify super() call, using the default narration. Tooltip is now handled manually.
        super(x, y, 12, 12, Component.literal(""), action, DEFAULT_NARRATION);
        this.icon = icon;
        this.screen = screen;
        this.tooltip = tooltip;
    }

    // 7. Update withSound to accept a Holder
    public ExtIconButton withSound(Holder<SoundEvent> sound) {
        this.pressSound = sound;
        return this;
    }

    public ExtIconButton withColor(Color color) {
        this.color = color;
        return this;
    }

    public ExtIconButton withEnabledFunc(Supplier<Boolean> func) {
        this.enabledFunc = func;
        return this;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!enabledFunc.get()) return false;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // 8. Replace renderButton with renderWidget and update its logic
    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.active || !this.visible || !enabledFunc.get()) {
            return;
        }

        // 9. Remove bindTexture invocation. Icon's render method should handle it.
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, isHoveredOrFocused() ? 1 : 0.8F);

        // 10. Assume icon.render is updated to use GuiGraphics and use getX()/getY()
        // The screen parameter might not be needed anymore, depending on Icon's implementation.
        // If icon.render is `render(GuiGraphics, int, int)`, this is correct.
        icon.render(guiGraphics, this.getX(), this.getY());

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        // 11. Handle tooltip rendering here, calling the method we added to PlannerScreen
        if (this.isHoveredOrFocused()) {
            // Your custom queue system. The lambda now uses GuiGraphics.
            EventListener.postRenderQueue.offer(() -> {
                if (this.screen instanceof PlannerScreen plannerScreen) {
                    plannerScreen.renderComponentTooltip(guiGraphics, List.of(this.tooltip), mouseX, mouseY);
                }
            });
        }
    }

    @Override
    public void playDownSound(@NotNull SoundManager handler) {
        if (pressSound != null) {
            // 12. Call .value() to get the actual SoundEvent from the Holder
            handler.play(SimpleSoundInstance.forUI(pressSound.value(), 1.0F));
        }
    }
}
