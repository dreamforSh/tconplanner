package com.xinian.tconplanner.screen.tooltip;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xinian.tconplanner.Config;
import com.xinian.tconplanner.util.TranslationUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Every tooltip the planner draws goes through here, so that all of them fit the window they are drawn
 * in at any vanilla GUI Scale.
 * <p>
 * The widgets used to call {@link GuiGraphics#renderTooltip} directly, which lays a tooltip out at a
 * fixed size and only nudges it back inside the window with {@code DefaultTooltipPositioner}. That is
 * fine at GUI Scale 1 or 2, but the planner shows whole-tool tooltips: a tool with a dozen modifiers is
 * forty lines tall, and at GUI Scale 3 or 4 the window is only 180-270 pixels tall, so the tooltip was
 * clamped to the top edge and everything past the bottom of the screen was simply unreachable. Long
 * modifier descriptions had the same problem sideways - nothing wraps them, so they ran off the edge.
 * <p>
 * This class fixes the geometry in three steps and leaves the drawing to vanilla:
 * <ol>
 *   <li>every line is wrapped to a width the window can actually show,</li>
 *   <li>the whole tooltip is scaled down until it fits vertically, no further than
 *       {@link #MIN_SCALE},</li>
 *   <li>what still does not fit at that floor is dropped, with a line saying how much.</li>
 * </ol>
 * Vanilla still renders: {@link GuiGraphics#renderTooltip(Font, List, ClientTooltipPositioner, int,
 * int)} draws the frame through {@code TooltipRenderUtil} and fires {@code RenderTooltipEvent}, so
 * resource packs and other mods keep their say over how a tooltip looks. Only where and how large it
 * lands is ours, supplied as a {@link ClientTooltipPositioner}.
 * <p>
 * Sizes below are in <em>tooltip space</em>: the window as it looks once the scale is applied. That is
 * the space the text is measured in, the positioner works in, and the one the cursor is converted into.
 */
public final class PlannerTooltip {

    private PlannerTooltip(){}

    /** Frame {@code TooltipRenderUtil} draws around the text, on every side */
    private static final int FRAME = 3;
    /** Kept between that frame and the window edge, so a fitted tooltip does not touch it */
    private static final int EDGE_GAP = 1;
    /** Vanilla's offset from the cursor, so an unscaled tooltip sits exactly where it always did */
    private static final int CURSOR_GAP = 12;
    /** Height one wrapped line occupies, matching {@code ClientTooltipComponent#getHeight} */
    private static final int LINE_HEIGHT = 10;
    /** Text never wraps wider than this, so a tooltip on a 4K window is still readable in one glance */
    private static final int MAX_TEXT_WIDTH = 220;
    /** ...nor narrower, because below it wrapping degenerates into one word per line */
    private static final int MIN_TEXT_WIDTH = 90;
    /** Text stops shrinking here; past this it is too small to read, so lines are dropped instead */
    private static final float MIN_SCALE = 0.5f;

    /**
     * A laid-out tooltip.
     *
     * @param verbatim nothing had to be changed to make it fit, so the caller may hand the whole thing
     *                 to vanilla untouched
     */
    private record Layout(List<FormattedCharSequence> lines, int width, int height, float scale, boolean verbatim) {}

    /** Draws {@code lines}, wrapping and shrinking them as needed */
    public static void render(GuiGraphics graphics, Font font, List<Component> lines, int mouseX, int mouseY){
        if(lines.isEmpty()) return;
        draw(graphics, font, layout(font, lines), mouseX, mouseY);
    }

    /** Single-line counterpart of {@link #render(GuiGraphics, Font, List, int, int)} */
    public static void render(GuiGraphics graphics, Font font, Component line, int mouseX, int mouseY){
        render(graphics, font, List.of(line), mouseX, mouseY);
    }

    /** The vanilla tooltip of {@code stack}, fitted to the window */
    public static void renderItem(GuiGraphics graphics, Font font, ItemStack stack, int mouseX, int mouseY){
        renderItem(graphics, font, stack, List.of(), mouseX, mouseY);
    }

    /**
     * The vanilla tooltip of {@code stack} followed by {@code extraLines}, fitted to the window.
     * <p>
     * A tooltip that already fits is handed to vanilla with the stack attached, which keeps the entire
     * Forge pipeline - {@code gatherTooltipComponents}, item tooltip images, per-item frame colours -
     * working as it does everywhere else in the game. Only a tooltip that has to be wrapped, shrunk or
     * truncated takes the path below, which can carry text but not tooltip images.
     */
    public static void renderItem(GuiGraphics graphics, Font font, ItemStack stack, List<Component> extraLines,
                                  int mouseX, int mouseY){
        List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), stack));
        lines.addAll(extraLines);
        Layout layout = layout(font, lines);
        if(layout.verbatim()){
            //The image goes under the first line, so it only makes sense while the stack owns every line
            Optional<TooltipComponent> image = extraLines.isEmpty() ? stack.getTooltipImage() : Optional.empty();
            graphics.renderTooltip(font, lines, image, stack, mouseX, mouseY);
            return;
        }
        draw(graphics, font, layout, mouseX, mouseY);
    }

    private static Layout layout(Font font, List<Component> source){
        Minecraft minecraft = Minecraft.getInstance();
        int windowWidth = minecraft.getWindow().getGuiScaledWidth();
        int windowHeight = minecraft.getWindow().getGuiScaledHeight();
        float requested = Mth.clamp(Config.CONFIG.tooltipScale.get().factor, MIN_SCALE, 1f);

        int wrapWidth = Mth.clamp(Math.round(windowWidth / requested) - 2 * (FRAME + EDGE_GAP) - CURSOR_GAP,
                MIN_TEXT_WIDTH, MAX_TEXT_WIDTH);
        List<FormattedCharSequence> lines = new ArrayList<>();
        boolean wrapped = false;
        for(Component component : source){
            //Font#split drops empty text, and the empty lines are load-bearing - the panels use them to
            //separate a modifier's description from the click hints below it
            if(component.getString().isEmpty()){
                lines.add(FormattedCharSequence.EMPTY);
                continue;
            }
            List<FormattedCharSequence> split = font.split(component, wrapWidth);
            wrapped |= split.size() > 1;
            lines.addAll(split);
        }

        //Shrink to fit whichever axis runs out of room first, then stop at the floor. Wrapping is not
        //redone at the smaller scale: the extra width it would win back is not worth a layout that
        //reflows as the tooltip grows.
        int width = measure(font, lines);
        float box = FRAME + EDGE_GAP;
        float fit = Math.min(windowWidth / (width + 2 * box), windowHeight / (height(lines.size()) + 2 * box));
        float scale = Mth.clamp(Math.min(requested, fit), MIN_SCALE, requested);

        //Only a tooltip held up by the floor can still overflow - any larger scale was chosen to fit
        //exactly - and gating on that also keeps a rounding error in `fit` from dropping a line that
        //would have fitted
        int maxLines = (int) ((windowHeight / scale - 2 * box) / LINE_HEIGHT);
        boolean truncated = scale <= MIN_SCALE && lines.size() > maxLines && maxLines >= 2;
        if(truncated){
            int dropped = lines.size() - (maxLines - 1);
            lines = new ArrayList<>(lines.subList(0, maxLines - 1));
            lines.add(TranslationUtil.createComponent("tooltip.overflow", dropped)
                    .withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
            width = measure(font, lines);
        }
        return new Layout(lines, width, height(lines.size()), scale, scale == 1f && !wrapped && !truncated);
    }

    private static int measure(Font font, List<FormattedCharSequence> lines){
        int width = 0;
        for(FormattedCharSequence line : lines){
            width = Math.max(width, font.width(line));
        }
        return width;
    }

    /**
     * Height vanilla gives a text-only tooltip of {@code lines} lines: one {@link #LINE_HEIGHT} each,
     * less the gap it reserves under the title when there is no second line to separate.
     */
    private static int height(int lines){
        return lines * LINE_HEIGHT - (lines == 1 ? 2 : 0);
    }

    private static void draw(GuiGraphics graphics, Font font, Layout layout, int mouseX, int mouseY){
        Vector2ic position = position(layout, mouseX, mouseY);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.scale(layout.scale(), layout.scale(), 1f);
        //The mouse position is passed as the origin because the positioner already accounts for it; a
        //positioner is the only way to place a tooltip without vanilla re-clamping it to window bounds
        //it measures in unscaled pixels
        graphics.renderTooltip(font, layout.lines(), (w, h, x, y, tw, th) -> position, 0, 0);
        pose.popPose();
    }

    private static Vector2ic position(Layout layout, int mouseX, int mouseY){
        Minecraft minecraft = Minecraft.getInstance();
        float scale = layout.scale();
        int windowWidth = Math.round(minecraft.getWindow().getGuiScaledWidth() / scale);
        int windowHeight = Math.round(minecraft.getWindow().getGuiScaledHeight() / scale);
        int cursorX = Math.round(mouseX / scale);
        int cursorY = Math.round(mouseY / scale);
        int margin = FRAME + EDGE_GAP;

        //Flip to the other side of the cursor rather than clamping against the edge: clamping is what
        //leaves a wide tooltip parked on top of the very row it is describing
        int x = cursorX + CURSOR_GAP;
        if(x + layout.width() + margin > windowWidth){
            x = cursorX - CURSOR_GAP - layout.width();
        }
        int y = cursorY - CURSOR_GAP;
        return new Vector2i(
                Math.max(margin, Math.min(x, windowWidth - margin - layout.width())),
                Math.max(margin, Math.min(y, windowHeight - margin - layout.height())));
    }
}
