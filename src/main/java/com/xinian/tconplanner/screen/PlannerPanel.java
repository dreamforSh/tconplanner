package com.xinian.tconplanner.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class PlannerPanel extends AbstractWidget {

    protected final List<AbstractWidget> children = new ArrayList<>();
    protected final PlannerScreen parent;

    public PlannerPanel(int x, int y, int width, int height, PlannerScreen parent) {
        super(x, y, width, height, Component.empty());
        this.parent = parent;
    }

    public void addChild(AbstractWidget widget) {

        widget.setX(this.getX() + widget.getX());
        widget.setY(this.getY() + widget.getY());
        this.children.add(widget);
    }


    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

        for (AbstractWidget child : children) {

            child.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }


    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput p_259886_) {

    }



    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (AbstractWidget child : children) {
            if (child.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (AbstractWidget child : children) {
            if (child.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        for (AbstractWidget child : children) {
            if (child.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        for (AbstractWidget child : children) {
            if (child.isMouseOver(mouseX, mouseY)) {
                if (child.mouseScrolled(mouseX, mouseY, delta)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (AbstractWidget child : children) {
            if (child.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        for (AbstractWidget child : children) {
            if (child.keyReleased(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        for (AbstractWidget child : children) {
            if (child.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return false;
    }
}
