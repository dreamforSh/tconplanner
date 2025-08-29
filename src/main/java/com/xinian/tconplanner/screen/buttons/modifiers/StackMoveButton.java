package com.xinian.tconplanner.screen.buttons.modifiers;

import com.xinian.tconplanner.screen.PlannerScreen;
import com.xinian.tconplanner.screen.buttons.PaginatedPanel;
import com.xinian.tconplanner.util.TranslationUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class StackMoveButton extends Button {
    private static final Component MOVE_UP = TranslationUtil.createComponent("modifierstack.moveup");
    private static final Component MOVE_DOWN = TranslationUtil.createComponent("modifierstack.movedown");
    private final PaginatedPanel<ModifierStackButton> scrollPanel;
    private final PlannerScreen parent;
    private final boolean moveUp;

    public StackMoveButton(int x, int y, boolean moveUp, PaginatedPanel<ModifierStackButton> scrollPanel, PlannerScreen parent) {
        super(x, y, 18, 10, Component.literal(""), (button) -> {}, DEFAULT_NARRATION);
        this.parent = parent;
        this.scrollPanel = scrollPanel;
        this.moveUp = moveUp;
        this.setTooltip(Tooltip.create(moveUp ? MOVE_UP : MOVE_DOWN));
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int vOffset = 145 + (this.moveUp ? 0 : this.height);
        guiGraphics.blit(PlannerScreen.TEXTURE, this.getX(), this.getY(), 214, vOffset, this.width, this.height, 256, 256);
    }

    @Override
    public void onPress() {
        if(moveUp){
            if(parent.selectedModifierStackIndex > 0){
                parent.modifierStack.moveDown(parent.selectedModifierStackIndex - 1);
                parent.selectedModifierStackIndex--;
                scrollPanel.makeVisible(parent.selectedModifierStackIndex, false);
                parent.refresh();
            }
        }else{
            if(parent.selectedModifierStackIndex < parent.modifierStack.getStack().size() - 1){
                parent.modifierStack.moveDown(parent.selectedModifierStackIndex);
                parent.selectedModifierStackIndex++;
                scrollPanel.makeVisible(parent.selectedModifierStackIndex, false);
                parent.refresh();
            }
        }
    }
}
