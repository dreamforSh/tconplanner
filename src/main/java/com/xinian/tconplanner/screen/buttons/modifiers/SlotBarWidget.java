package com.xinian.tconplanner.screen.buttons.modifiers;

import com.xinian.tconplanner.data.BaseBlueprint;
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
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Free modifier slots, one chip per slot type.
 * <p>
 * The old panel drew two bare numbers hard against the right edge with no label, and the only way to
 * discover that left-clicking a number adds a creative slot was to read the tooltip you had no reason
 * to hover. Chips are labelled, highlight under the cursor and spell the click actions out.
 * <p>
 * Slot types are also worked out from the tool now instead of being hardcoded to
 * {@code {UPGRADE, ABILITY}}, so armour's DEFENSE slots finally show up.
 */
public class SlotBarWidget extends AbstractWidget {

    public static final int HEIGHT = 13;
    private static final int GAP = 2;
    private static final int BAR_WIDTH = 3;
    /** Types that stay visible even at zero, because "you have none left" is information too */
    private static final SlotType[] ALWAYS_SHOWN = {SlotType.UPGRADE, SlotType.ABILITY};
    private static final int MAX_CHIPS = 3;

    private record Chip(SlotType type, int free, int creative) {}

    private final PlannerScreen parent;
    private final List<Chip> chips = new ArrayList<>();
    private final int chipWidth;

    public SlotBarWidget(int x, int y, int width, ToolStack tool, PlannerScreen parent){
        super(x, y, width, HEIGHT, Component.empty());
        this.parent = parent;
        BaseBlueprint<?> blueprint = parent.blueprint;

        for(SlotType type : ALWAYS_SHOWN){
            chips.add(chipFor(type, tool, blueprint));
        }
        for(SlotType type : SlotType.getAllSlotTypes()){
            if(chips.size() >= MAX_CHIPS) break;
            if(type == SlotType.UPGRADE || type == SlotType.ABILITY) continue;
            if(isRelevant(type, tool, blueprint)) chips.add(chipFor(type, tool, blueprint));
        }
        this.chipWidth = (width - GAP * (chips.size() - 1)) / chips.size();
    }

    private static Chip chipFor(SlotType type, ToolStack tool, BaseBlueprint<?> blueprint){
        return new Chip(type, tool.getFreeSlots(type), blueprint.creativeSlots.getOrDefault(type, 0));
    }

    private static boolean isRelevant(SlotType type, ToolStack tool, BaseBlueprint<?> blueprint){
        if(tool.getFreeSlots(type) > 0) return true;
        if(blueprint.creativeSlots.getOrDefault(type, 0) > 0) return true;
        for(ModifierInfo info : blueprint.modStack.getStack()){
            if(info.count != null && info.count.type() == type) return true;
        }
        return false;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick){
        Font font = Minecraft.getInstance().font;
        int hovered = chipAt(mouseX, mouseY);
        for(int i = 0; i < chips.size(); i++){
            Chip chip = chips.get(i);
            int cx = x + i * (chipWidth + GAP);
            int color = chip.type().getColor().getValue() + 0xff_000000;

            ModifierTheme.plate(graphics, cx, y, chipWidth, HEIGHT,
                    i == hovered ? ModifierTheme.ROW_BG_HOVER : ModifierTheme.ROW_BG);
            graphics.fill(cx + 1, y + 2, cx + 1 + BAR_WIDTH, y + HEIGHT - 2, color);

            Component count = countText(chip);
            int countWidth = font.width(count);
            graphics.drawString(font, count, cx + chipWidth - 2 - countWidth, y + 3,
                    chip.free() == 0 ? ModifierTheme.TEXT_FAINT : ModifierTheme.TEXT);
            ModifierTheme.fittedString(graphics, font, chip.type().getDisplayName(), cx + BAR_WIDTH + 3, y + 3,
                    chipWidth - BAR_WIDTH - countWidth - 8, color);
        }
        if(hovered >= 0) queueTooltip(graphics, chips.get(hovered), mouseX, mouseY);
    }

    private static Component countText(Chip chip){
        MutableComponent text = Component.literal(String.valueOf(chip.free()));
        if(chip.creative() > 0) text.append(Component.literal("+" + chip.creative()).withStyle(ChatFormatting.AQUA));
        return text;
    }

    private void queueTooltip(GuiGraphics graphics, Chip chip, int mouseX, int mouseY){
        parent.postRenderTasks.add(() -> {
            Component coloredName = Component.literal("")
                    .withStyle(Style.EMPTY.withColor(chip.type().getColor()))
                    .append(chip.type().getDisplayName())
                    .append(Component.literal("").withStyle(ChatFormatting.RESET));
            List<Component> lines = new ArrayList<>();
            lines.add(TranslationUtil.createComponent("slots.available", coloredName));
            if(chip.creative() > 0){
                lines.add(TranslationUtil.createComponent("slots.creative", chip.creative()).withStyle(ChatFormatting.AQUA));
            }
            lines.add(Component.empty());
            lines.add(TranslationUtil.createComponent("modifiers.addcreativeslot").withStyle(ChatFormatting.GREEN));
            MutableComponent remove = TranslationUtil.createComponent("modifiers.removecreativeslot").withStyle(ChatFormatting.RED);
            if(chip.creative() == 0 || chip.free() == 0){
                remove.withStyle(remove.getStyle().applyFormats(ChatFormatting.STRIKETHROUGH));
            }
            lines.add(remove);
            parent.renderComponentTooltip(graphics, lines, mouseX, mouseY);
        });
    }

    private int chipAt(double mouseX, double mouseY){
        if(mouseY < y || mouseY >= y + HEIGHT) return -1;
        for(int i = 0; i < chips.size(); i++){
            int cx = x + i * (chipWidth + GAP);
            if(mouseX >= cx && mouseX < cx + chipWidth) return i;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button){
        if(!active || !visible) return false;
        int index = chipAt(mouseX, mouseY);
        if(index < 0) return false;
        Chip chip = chips.get(index);
        SoundManager sound = Minecraft.getInstance().getSoundManager();
        if(button == 0){
            parent.blueprint.addCreativeSlot(chip.type());
            sound.play(SimpleSoundInstance.forUI(SoundEvents.ANVIL_PLACE, 2f, 0.08f));
            parent.refresh();
            return true;
        }
        if(button == 1){
            if(chip.creative() > 0 && chip.free() > 0){
                parent.blueprint.removeCreativeSlot(chip.type());
                sound.play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_TAKE_RESULT, 2f, 0.08f));
                parent.refresh();
                return true;
            }
            sound.play(SimpleSoundInstance.forUI(SoundEvents.BAMBOO_FALL, 2f, 0.08f));
            return true;
        }
        return false;
    }

    @Override
    public void updateWidgetNarration(@NotNull NarrationElementOutput output){
        // No narration needed
    }
}
