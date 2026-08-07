package com.xinian.tconplanner.screen.buttons.modifiers;

import com.xinian.tconplanner.data.ModifierInfo;
import com.xinian.tconplanner.screen.PlannerScreen;
import com.xinian.tconplanner.util.TranslationUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.Modifier;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * One step of the applied-modifier stack, in application order.
 * <p>
 * Replaces {@code ModifierStackButton} plus the select-a-row-then-press-the-fixed-arrows dance and the
 * Save/Cancel modal that went with it. Reordering and removal happen inline and take effect straight
 * away; if the resulting order cannot be crafted, the offending row is the one that turns red.
 */
public class AppliedModifierRow extends AbstractWidget {

    public static final int HEIGHT = 16;

    private static final int TEXT_X = 18;
    private static final int BUTTON_SIZE = 11;
    private static final int UP_FROM_RIGHT = 36;
    private static final int DOWN_FROM_RIGHT = 24;
    private static final int REMOVE_FROM_RIGHT = 12;

    private final PlannerScreen parent;
    private final ModifierInfo info;
    private final Modifier modifier;
    private final ItemStack snapshot;
    private final Component displayName;
    private final int index;
    private final int lastIndex;
    @Nullable
    private final Component error;

    /**
     * @param snapshot the tool as it looks after this step, so the stack reads top to bottom
     * @param error    set only on the first step that fails validation
     */
    public AppliedModifierRow(ModifierInfo info, int index, int lastIndex, int level, int width, ItemStack snapshot,
                              @Nullable Component error, PlannerScreen parent){
        super(0, 0, width, HEIGHT, Component.empty());
        this.parent = parent;
        this.info = info;
        this.modifier = info.modifier;
        this.index = index;
        this.lastIndex = lastIndex;
        this.snapshot = snapshot;
        this.error = error;
        this.displayName = modifier.getDisplayName(level);
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick){
        Font font = Minecraft.getInstance().font;
        boolean hovered = isHoveredOrFocused();

        ModifierTheme.plate(graphics, x, y, width, HEIGHT, error != null
                ? (hovered ? ModifierTheme.ROW_BG_ERROR_HOVER : ModifierTheme.ROW_BG_ERROR)
                : (hovered ? ModifierTheme.ROW_BG_HOVER : ModifierTheme.ROW_BG));

        if(!snapshot.isEmpty()) graphics.renderItem(snapshot, x, y);

        Component ordinal = Component.literal((index + 1) + ".");
        int ordinalWidth = font.width(ordinal) + 2;
        graphics.drawString(font, ordinal, x + TEXT_X, y + 1, ModifierTheme.TEXT_FAINT);
        ModifierTheme.fittedString(graphics, font, displayName, x + TEXT_X + ordinalWidth, y + 1,
                width - UP_FROM_RIGHT - TEXT_X - ordinalWidth - 3,
                error != null ? ModifierTheme.TEXT_ERROR : ModifierTheme.TEXT);

        if(info.count != null){
            graphics.fill(x + TEXT_X, y + 10, x + TEXT_X + 2, y + 14, info.count.type().getColor().getValue() + 0xff_000000);
            ModifierTheme.smallString(graphics, font, ModifierRow.slotCost(info.count), x + TEXT_X + 4, y + 10, ModifierTheme.TEXT_FAINT);
        }

        ModifierTheme.triangle(graphics, x + width - UP_FROM_RIGHT + 2, y + 5, 7, 5, true,
                glyph(index > 0, inBox(mouseX, mouseY, width - UP_FROM_RIGHT)));
        ModifierTheme.triangle(graphics, x + width - DOWN_FROM_RIGHT + 2, y + 5, 7, 5, false,
                glyph(index < lastIndex, inBox(mouseX, mouseY, width - DOWN_FROM_RIGHT)));
        ModifierTheme.cross(graphics, x + width - REMOVE_FROM_RIGHT + 3, y + 5, 6,
                inBox(mouseX, mouseY, width - REMOVE_FROM_RIGHT) ? ModifierTheme.TEXT_ERROR : ModifierTheme.GLYPH);

        if(hovered) queueTooltip(graphics, mouseX, mouseY);
    }

    private void queueTooltip(GuiGraphics graphics, int mouseX, int mouseY){
        Component hint = inBox(mouseX, mouseY, width - UP_FROM_RIGHT) ? TranslationUtil.createComponent("modifiers.moveup")
                : inBox(mouseX, mouseY, width - DOWN_FROM_RIGHT) ? TranslationUtil.createComponent("modifiers.movedown")
                : inBox(mouseX, mouseY, width - REMOVE_FROM_RIGHT) ? TranslationUtil.createComponent("modifiers.remove")
                : null;
        parent.postRenderTasks.add(() -> {
            if(hint != null){
                graphics.renderTooltip(Minecraft.getInstance().font, hint, mouseX, mouseY);
                return;
            }
            List<Component> lines = new ArrayList<>();
            lines.add(displayName);
            lines.addAll(modifier.getDescriptionList());
            if(error != null){
                lines.add(Component.empty());
                lines.add(TranslationUtil.createComponent("modifiers.order.invalid", index + 1).withStyle(ChatFormatting.RED));
                lines.add(error.copy().withStyle(ChatFormatting.RED));
            }
            parent.renderComponentTooltip(graphics, lines, mouseX, mouseY);
        });
    }

    private static int glyph(boolean enabled, boolean hovered){
        if(!enabled) return ModifierTheme.GLYPH_DISABLED;
        return hovered ? ModifierTheme.GLYPH_HOVER : ModifierTheme.GLYPH;
    }

    private boolean inBox(int mouseX, int mouseY, int boxX){
        return mouseX >= x + boxX && mouseX < x + boxX + BUTTON_SIZE && mouseY >= y && mouseY < y + HEIGHT;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button){
        if(!active || !visible || button != 0) return false;
        if(mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + HEIGHT) return false;

        if(within(mouseX, width - UP_FROM_RIGHT)){
            if(index <= 0) return deny();
            parent.blueprint.modStack.moveUp(index);
            return apply();
        }
        if(within(mouseX, width - DOWN_FROM_RIGHT)){
            if(index >= lastIndex) return deny();
            parent.blueprint.modStack.moveDown(index);
            return apply();
        }
        if(within(mouseX, width - REMOVE_FROM_RIGHT)){
            parent.blueprint.modStack.removeAt(index);
            return apply();
        }
        return false;
    }

    private boolean within(double mouseX, int boxX){
        return mouseX >= x + boxX && mouseX < x + boxX + BUTTON_SIZE;
    }

    private boolean apply(){
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        parent.refresh();
        return true;
    }

    private boolean deny(){
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ANVIL_HIT, 1.0F));
        return true;
    }

    @Override
    public void updateWidgetNarration(@NotNull NarrationElementOutput output){
        // No narration needed
    }
}
