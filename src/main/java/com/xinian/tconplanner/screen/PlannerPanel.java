package com.xinian.tconplanner.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PlannerPanel extends AbstractWidget {

    protected final List<AbstractWidget> children = new ArrayList<>();
    protected final PlannerScreen parent;

    public PlannerPanel(int x, int y, int width, int height, PlannerScreen parent) {
        super(x, y, width, height, Component.literal(""));  // 变更：new TextComponent("") -> Component.literal("")
        this.parent = parent;
    }

    public void addChild(AbstractWidget widget){
        widget.x += x;
        widget.y += y;
        children.add(widget);
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.isHovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
        for (AbstractWidget child : children) {
            child.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput p_169152_) {
        // No narration needed for panels
    }

    /**
     * Children get first refusal; the panel only claims the click if the cursor is actually inside it.
     * <p>
     * This used to seed {@code result} with {@link #isHoveredOrFocused()} and OR every child's answer,
     * which broke two ways. A panel stays focused after you click a text field in it, so it answered
     * "handled" for clicks anywhere on the screen - and because {@code Screen} stops at the first child
     * that returns true, whichever panel came earlier in the widget list swallowed every click meant for
     * a later one (click the material search box, and the modifier panel went dead). Not stopping at the
     * first consumer also let overlapping children both fire on a single click.
     * <p>
     * Children are asked before the bounds test on purpose: some are deliberately positioned outside
     * their parent, such as {@code MaterialSelectPanel}'s search box.
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (AbstractWidget child : children) {
            if(child.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return isMouseOver(mouseX, mouseY);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int p_231048_5_) {
        boolean result = false;
        for (AbstractWidget child : children) {
            if(child.mouseReleased(mouseX, mouseY, p_231048_5_))result = true;
        }
        return result;
    }

    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        boolean result = false;
        for (AbstractWidget child : children) {
            if(child.mouseDragged(mx, my, button, dx, dy))result = true;
        }
        return result;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        boolean result = false;
        for (AbstractWidget child : children) {
            if(child.isMouseOver(mouseX, mouseY)) {
                if (child.mouseScrolled(mouseX, mouseY, scroll)) result = true;
            }
        }
        return result;
    }

    public boolean keyPressed(int p_231046_1_, int p_231046_2_, int p_231046_3_) {
        boolean result = false;
        for (AbstractWidget child : children) {
            if(child.keyPressed(p_231046_1_, p_231046_2_, p_231046_3_))result = true;
        }
        return result;
    }

    public boolean keyReleased(int p_223281_1_, int p_223281_2_, int p_223281_3_) {
        boolean result = false;
        for (AbstractWidget child : children) {
            if(child.keyReleased(p_223281_1_, p_223281_2_, p_223281_3_))result = true;
        }
        return result;
    }

    public boolean charTyped(char p_231042_1_, int p_231042_2_) {
        boolean result = false;
        for (AbstractWidget child : children) {
            if(child.charTyped(p_231042_1_, p_231042_2_))result = true;
        }
        return result;
    }
}
