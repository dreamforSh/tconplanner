package com.xinian.tconplanner.screen.buttons.modifiers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Colours and drawing helpers for the modifier panel.
 * <p>
 * Every control here is drawn with {@link GuiGraphics#fill} rather than blitted from planner.png, so
 * rows, chips and tabs are crisp at any size and none of them inherit the atlas plate's fixed 100x18
 * geometry or its built-in 16px icon well. The accent is the atlas plate's own blue ({@code #2B98FF}),
 * which keeps the panel tied to the rest of the GUI even though nothing here is textured.
 * <p>
 * The banner at the top is still the atlas sprite - it is a title, not a control.
 */
public final class ModifierTheme {

    private ModifierTheme(){}

    /** The blue planner.png uses for its button plates; everything else is derived from it */
    public static final int ACCENT = 0xff_2b98ff;
    public static final int DANGER = 0xff_e02121;

    //Rows
    public static final int ROW_BG = 0x66_1c2a38;
    public static final int ROW_BG_HOVER = 0x99_2f5f8f;
    public static final int ROW_BG_APPLIED = 0x66_1e5233;
    public static final int ROW_BG_APPLIED_HOVER = 0x99_2f8a54;
    public static final int ROW_BG_ERROR = 0x66_5a2222;
    public static final int ROW_BG_ERROR_HOVER = 0x99_8f3535;

    //Buttons and tabs
    public static final int BUTTON_BG = 0x55_16222e;
    public static final int BUTTON_BG_HOVER = 0x88_2f5f8f;
    public static final int BUTTON_BG_SELECTED = 0x99_1d4f7a;

    //Edges
    public static final int HIGHLIGHT = 0x40_ffffff;
    public static final int SHADOW = 0x50_000000;
    /** Painted behind the inline +/- cluster so it stays legible over a long modifier name */
    public static final int SCRIM = 0xdd_16222e;

    //Text
    public static final int TEXT = 0xff_ffffff;
    public static final int TEXT_DIM = 0xa0_ffffff;
    public static final int TEXT_FAINT = 0x70_ffffff;
    public static final int TEXT_ERROR = 0xff_ff6b6b;
    public static final int TEXT_OK = 0xff_7be87b;

    //Glyphs
    public static final int GLYPH = 0xc0_ffffff;
    public static final int GLYPH_HOVER = 0xff_ffffff;
    public static final int GLYPH_DISABLED = 0x35_ffffff;

    /** Names never shrink past this; beyond it they are clipped, so text stays readable */
    private static final float MIN_TEXT_SCALE = 0.7f;

    /** Flat plate with a 1px top highlight and bottom shadow - the base for every row, chip and button */
    public static void plate(GuiGraphics graphics, int x, int y, int width, int height, int background){
        graphics.fill(x, y, x + width, y + height, background);
        graphics.fill(x, y, x + width, y + 1, HIGHLIGHT);
        graphics.fill(x, y + height - 1, x + width, y + height, SHADOW);
    }

    /** 2px accent bar along the bottom edge, marking the selected tab */
    public static void accentBar(GuiGraphics graphics, int x, int y, int width, int height, int color){
        graphics.fill(x, y + height - 2, x + width, y + height, color);
    }

    /**
     * Draws text that shrinks to fit, but never below {@link #MIN_TEXT_SCALE} - past that it is clipped.
     * The old code scaled without a floor, which is why long names became unreadable sub-pixel mush.
     */
    public static void fittedString(GuiGraphics graphics, Font font, Component text, int x, int y, int maxWidth, int color){
        int textWidth = font.width(text);
        if(textWidth <= maxWidth){
            graphics.drawString(font, text, x, y, color);
            return;
        }
        float scale = Math.max(maxWidth / (float) textWidth, MIN_TEXT_SCALE);
        boolean clip = textWidth * scale > maxWidth + 0.5f;
        if(clip) graphics.enableScissor(x, y - 1, x + maxWidth, y + font.lineHeight + 1);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y + (font.lineHeight * (1 - scale)) / 2f, 0);
        pose.scale(scale, scale, 1);
        graphics.drawString(font, text, 0, 0, color);
        pose.popPose();
        if(clip) graphics.disableScissor();
    }

    /** {@link #fittedString} centred on {@code centreX} */
    public static void fittedCenteredString(GuiGraphics graphics, Font font, Component text, int centreX, int y, int maxWidth, int color){
        int drawn = Math.min(font.width(text), maxWidth);
        fittedString(graphics, font, text, centreX - drawn / 2, y, maxWidth, color);
    }

    /** Half-size text, used for the slot-cost sub-line */
    public static void smallString(GuiGraphics graphics, Font font, Component text, int x, int y, int color){
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(0.5f, 0.5f, 1);
        graphics.drawString(font, text, 0, 0, color);
        pose.popPose();
    }

    /** Plus glyph centred in a {@code size} box */
    public static void plus(GuiGraphics graphics, int x, int y, int size, int color){
        int mid = size / 2;
        graphics.fill(x + 1, y + mid, x + size - 1, y + mid + 1, color);
        graphics.fill(x + mid, y + 1, x + mid + 1, y + size - 1, color);
    }

    /** Minus glyph centred in a {@code size} box */
    public static void minus(GuiGraphics graphics, int x, int y, int size, int color){
        int mid = size / 2;
        graphics.fill(x + 1, y + mid, x + size - 1, y + mid + 1, color);
    }

    /** Rows a {@link #triangle} of this width occupies, so callers can centre it */
    public static int triangleHeight(int width){
        return (width + 1) / 2;
    }

    /**
     * Solid 45-degree pixel triangle, {@code width} across and {@link #triangleHeight} rows tall;
     * {@code up} puts the apex at the top.
     * <p>
     * The height is derived rather than passed because the previous signature took both and clipped any
     * row whose half-width computed to zero - at 7x5 that silently dropped two rows from each arrow and
     * left the up and down arrows sitting 2px apart.
     */
    public static void triangle(GuiGraphics graphics, int x, int y, int width, boolean up, int color){
        int rows = triangleHeight(width);
        int centre = x + width / 2;
        for(int row = 0; row < rows; row++){
            int half = up ? row : rows - 1 - row;
            graphics.fill(centre - half, y + row, centre + half + 1, y + row + 1, color);
        }
    }

    /** Diagonal cross, for "remove" */
    public static void cross(GuiGraphics graphics, int x, int y, int size, int color){
        for(int i = 0; i < size; i++){
            graphics.fill(x + i, y + i, x + i + 1, y + i + 1, color);
            graphics.fill(x + size - 1 - i, y + i, x + size - i, y + i + 1, color);
        }
    }
}
