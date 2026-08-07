package com.xinian.tconplanner.screen.buttons.modifiers;

import com.xinian.tconplanner.screen.PlannerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * Flat button for the modifier panel, used both for the three view tabs and for the reset action.
 * <p>
 * Drawn entirely with {@link ModifierTheme#plate}, so it is crisp at the 42px tab width the atlas
 * plate could not have covered anyway. A selected tab is marked with a 2px accent bar rather than a
 * different plate, which keeps all three tabs reading as one control group.
 */
public class ModifierButton extends AbstractWidget {

    private final PlannerScreen parent;
    private final Runnable onPress;
    private boolean selected;
    private boolean dangerous;
    @Nullable
    private Component tooltip;

    public ModifierButton(int x, int y, int width, int height, Component label, Runnable onPress, PlannerScreen parent){
        super(x, y, width, height, label);
        this.parent = parent;
        this.onPress = onPress;
    }

    /** Marks this as the active view; draws the accent bar */
    public ModifierButton selected(boolean selected){
        this.selected = selected;
        return this;
    }

    /** Destructive action styling - red plate and red label */
    public ModifierButton dangerous(){
        this.dangerous = true;
        return this;
    }

    public ModifierButton withTooltip(@Nullable Component tooltip){
        this.tooltip = tooltip;
        return this;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick){
        boolean hovered = isHoveredOrFocused();
        int background;
        if(dangerous){
            background = hovered ? ModifierTheme.ROW_BG_ERROR_HOVER : ModifierTheme.ROW_BG_ERROR;
        } else if(selected){
            background = ModifierTheme.BUTTON_BG_SELECTED;
        } else {
            background = hovered ? ModifierTheme.BUTTON_BG_HOVER : ModifierTheme.BUTTON_BG;
        }
        ModifierTheme.plate(graphics, x, y, width, height, background);
        if(selected) ModifierTheme.accentBar(graphics, x, y, width, height, ModifierTheme.ACCENT);

        int textColor = dangerous ? (hovered ? ModifierTheme.TEXT : ModifierTheme.TEXT_ERROR)
                : selected || hovered ? ModifierTheme.TEXT : ModifierTheme.TEXT_DIM;
        ModifierTheme.fittedCenteredString(graphics, Minecraft.getInstance().font, getMessage(),
                x + width / 2, y + (height - 8) / 2, width - 4, textColor);

        if(hovered && tooltip != null){
            parent.postRenderTasks.add(() -> graphics.renderTooltip(Minecraft.getInstance().font, tooltip, mouseX, mouseY));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button){
        if(!active || !visible || button != 0) return false;
        if(mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) return false;
        if(!selected){
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            onPress.run();
        }
        return true;
    }

    @Override
    public void updateWidgetNarration(@NotNull NarrationElementOutput output){
        // No narration needed
    }
}
