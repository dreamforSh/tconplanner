package com.xinian.tconplanner.screen.buttons;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.CreateNarration;
import net.minecraft.network.chat.Component;
import com.xinian.tconplanner.screen.PlannerScreen;

public class TextButton extends Button {

    private final PlannerScreen parent;
    private final Runnable onPress;
    private int color = 0xff_ff_ff;
    private Component tooltip = null;

    public TextButton(int x, int y, Component text, Runnable onPress, PlannerScreen parent) {
        super(x, y, 58, 18, text, e -> onPress.run(), DEFAULT_NARRATION);
        this.parent = parent;
        this.onPress = onPress;
    }

    private static final CreateNarration DEFAULT_NARRATION = (p_253298_) -> p_253298_.get();

    public TextButton withColor(int color){
        this.color = color;
        return this;
    }

    public TextButton withTooltip(Component tooltip){
        this.tooltip = tooltip;
        return this;
    }

    public TextButton withWidth(int width) {
        this.width = width;
        return this;
    }

    //planner.png's button plate: border at u=176, uniform body u=177..222, border at u=223 - only 48
    //wide in total, so a plain blit at the default 58 used to leave 10px unpainted
    private static final int PLATE_U = 176;
    private static final int PLATE_BODY_WIDTH = 46;
    private static final int PLATE_V = 183;

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PlannerScreen.bindTexture();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(((color & 0xff0000) >> 16)/255f, ((color & 0x00ff00) >> 8)/255f, (color & 0x0000ff)/255f,1f);
        parent.blit(graphics, x, y, PLATE_U, PLATE_V, 1, height);
        for(int drawn = 0; drawn < width - 2; drawn += PLATE_BODY_WIDTH){
            parent.blit(graphics, x + 1 + drawn, y, PLATE_U + 1, PLATE_V, Math.min(PLATE_BODY_WIDTH, width - 2 - drawn), height);
        }
        parent.blit(graphics, x + width - 1, y, PLATE_U + 47, PLATE_V, 1, height);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(), x + width/2, y + 5, isHovered ? 0xffffffff : 0xa0ffffff);
        if(isHovered){
            renderToolTip(graphics, mouseX, mouseY);
        }
    }

    public void renderToolTip(GuiGraphics graphics, int mouseX, int mouseY) {
        if(tooltip != null) {
            parent.postRenderTasks.add(() -> graphics.renderTooltip(Minecraft.getInstance().font, tooltip, mouseX, mouseY));
        }
    }

    @Override
    public void onPress() {
        onPress.run();
    }
}
