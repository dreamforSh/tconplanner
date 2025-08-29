package com.xinian.tconplanner.screen.buttons;

import com.xinian.tconplanner.data.BaseBlueprint;
import com.xinian.tconplanner.data.Blueprint;
import com.xinian.tconplanner.screen.PlannerScreen;
import com.xinian.tconplanner.util.Icon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class BookmarkedButton extends Button {

    public static final Icon STAR_ICON = new Icon(6, 0);

    private final PlannerScreen parent;
    private final int index;
    private final BaseBlueprint<?> blueprint;
    private final ItemStack stack;
    private final boolean starred;
    private boolean selected;

    public BookmarkedButton(int index, BaseBlueprint<?> blueprint, boolean starred, PlannerScreen parent) {
        super(0, 0, 18, 18, Component.literal(""),
                (button) -> parent.setBlueprint(blueprint.clone()),
                DEFAULT_NARRATION);
        this.index = index;
        this.blueprint = blueprint;
        this.starred = starred;
        this.parent = parent;
        this.stack = blueprint.createOutput();
        this.selected = parent.blueprint != null && parent.blueprint.equals(blueprint);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }


    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int vOffset = 41 + (this.selected ? 18 : 0);
        guiGraphics.blit(PlannerScreen.TEXTURE, getX(), getY(), 213, vOffset, this.width, this.height);
        guiGraphics.renderFakeItem(this.stack, getX() + 1, getY() + 1);

        if (this.starred) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(getX() + 11, getY() + 11, 105);
            guiGraphics.pose().scale(0.5f, 0.5f, 0.5f);
            STAR_ICON.render(guiGraphics, 0, 0);
            guiGraphics.pose().popPose();
        }

        if (this.isHoveredOrFocused()) {
            guiGraphics.renderTooltip(Minecraft.getInstance().font, this.stack, mouseX, mouseY);
        }
    }

    public BaseBlueprint<?> getBlueprint() {
        return blueprint;
    }

    public int getIndex() {
        return index;
    }

    public PlannerScreen getParent() {
        return parent;
    }
}
