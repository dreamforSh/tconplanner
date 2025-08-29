package com.xinian.tconplanner.util;

import com.xinian.tconplanner.TConPlanner;
import net.minecraft.client.gui.GuiGraphics; // 1. Import GuiGraphics
import net.minecraft.resources.ResourceLocation;

// Removed unused imports: PoseStack, Screen, RenderSystem, GameRenderer

public class Icon {
    private static final ResourceLocation ICONS = ResourceLocation.fromNamespaceAndPath(TConPlanner.MODID, "textures/gui/icons.png");

    private final int x, y;

    public Icon(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Renders the icon using the modern GuiGraphics context.
     * @param guiGraphics The graphics context to render with.
     * @param screenX The destination X coordinate on the screen.
     * @param screenY The destination Y coordinate on the screen.
     */
    // 2. Changed method signature to accept GuiGraphics.
    public void render(GuiGraphics guiGraphics, int screenX, int screenY) {
        // 3. Replaced screen.blit with guiGraphics.blit.
        // The new blit method takes the texture ResourceLocation as the first parameter,
        // which simplifies the call by removing the need for separate RenderSystem calls.
        guiGraphics.blit(ICONS, screenX, screenY, this.x * 12, this.y * 12, 12, 12);
    }
}
