package com.xinian.tconplanner.screen.buttons.modifiers;

import com.xinian.tconplanner.data.ModifierInfo;
import com.xinian.tconplanner.screen.PlannerScreen;
import com.xinian.tconplanner.util.ModifierEvaluator;
import com.xinian.tconplanner.util.ModifierStateEnum;
import com.xinian.tconplanner.util.ToolValidator;
import com.xinian.tconplanner.util.TranslationUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.modifiers.ModifierIconManager;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.impl.DurabilityShieldModifier;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.recipe.RecipeResult;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * One modifier in the browse list, with the level controls inline.
 * <p>
 * This replaces the old {@code ModifierSelectButton} and the whole add/remove sub-page it used to open.
 * Clicking never navigates away: left-click (or the {@code +} glyph) adds a level, right-click (or
 * {@code -}) removes one, and hovering either glyph shows the full vanilla tooltip of the tool you
 * would end up with - which is what the old three-way {@code ModPreviewWidget} row existed for.
 */
public class ModifierRow extends AbstractWidget {

    public static final int HEIGHT = 16;
    private static final String KEY_MAX_LEVEL = TConstruct.makeTranslationKey("recipe", "modifier.max_level");

    private static final int TEXT_X = 18;
    private static final int STEP_SIZE = 11;
    /** Offsets from the row's right edge, so the row works at any width */
    private static final int PLUS_FROM_RIGHT = 12;
    private static final int MINUS_FROM_RIGHT = 35;
    private static final int LEVEL_FROM_RIGHT = 18;
    private static final int CLUSTER_FROM_RIGHT = 37;

    private final PlannerScreen parent;
    private final ModifierEvaluator.Candidate candidate;
    private final ModifierInfo info;
    private final Modifier modifier;
    private final IDisplayModifierRecipe recipe;
    private final ToolStack tool;
    private final ItemStack result;
    private final List<ItemStack> recipeStacks = new ArrayList<>();
    private final boolean hasLevels;
    private final boolean incremental;

    /** Resolved on first hover - {@link ToolValidator#validateModRemoval} replays the whole stack */
    @Nullable
    private RecipeResult<ItemStack> removal;

    public ModifierRow(ModifierEvaluator.Candidate candidate, int width, ToolStack tool, ItemStack result, PlannerScreen parent){
        super(0, 0, width, HEIGHT, Component.empty());
        this.parent = parent;
        this.candidate = candidate;
        this.recipe = candidate.recipe();
        this.modifier = candidate.modifier();
        this.info = new ModifierInfo(recipe);
        this.tool = tool;
        this.result = result;
        this.hasLevels = !(modifier instanceof NoLevelsModifier);
        this.incremental = recipe.isIncremental();
        for(int i = 0; i < recipe.getInputCount(); i++){
            recipeStacks.addAll(recipe.getDisplayItems(i));
        }
    }

    public ModifierStateEnum getState(){
        return evaluate().state();
    }

    private ModifierEvaluator.Evaluation evaluate(){
        return parent.modifierEvaluator.evaluate(tool, result, recipe);
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick){
        ModifierEvaluator.Evaluation eval = evaluate();
        Font font = Minecraft.getInstance().font;
        boolean hovered = isHoveredOrFocused();

        ModifierTheme.plate(graphics, x, y, width, HEIGHT, switch(eval.state()){
            case APPLIED -> hovered ? ModifierTheme.ROW_BG_APPLIED_HOVER : ModifierTheme.ROW_BG_APPLIED;
            case UNAVAILABLE -> hovered ? ModifierTheme.ROW_BG_ERROR_HOVER : ModifierTheme.ROW_BG_ERROR;
            default -> hovered ? ModifierTheme.ROW_BG_HOVER : ModifierTheme.ROW_BG;
        });

        //Cycle through the recipe's inputs while hovered so you can see what it actually costs
        if(hovered && !recipeStacks.isEmpty()){
            graphics.renderItem(recipeStacks.get((int) ((System.currentTimeMillis() / 1000) % recipeStacks.size())), x, y);
        } else {
            ModifierIconManager.renderIcon(graphics, modifier, x, y, 0, 16);
        }

        //Name is always laid out against the un-hovered width, so hovering never reflows the row
        ModifierTheme.fittedString(graphics, font, candidate.displayName(), x + TEXT_X, y + 1,
                width - TEXT_X - LEVEL_FROM_RIGHT + 6,
                eval.state() == ModifierStateEnum.UNAVAILABLE ? ModifierTheme.TEXT_DIM : ModifierTheme.TEXT);

        SlotType.SlotCount count = recipe.getSlots();
        if(count != null){
            graphics.fill(x + TEXT_X, y + 10, x + TEXT_X + 2, y + 14, count.type().getColor().getValue() + 0xff_000000);
            ModifierTheme.smallString(graphics, font, slotCost(count), x + TEXT_X + 4, y + 10, ModifierTheme.TEXT_FAINT);
        }

        if(hovered){
            renderControls(graphics, font, eval, mouseX, mouseY);
        } else if(hasLevels && eval.level() > 0){
            Component level = Component.literal(String.valueOf(eval.level()));
            graphics.drawString(font, level, x + width - 3 - font.width(level), y + 4, ModifierTheme.TEXT_OK);
        }
    }

    static MutableComponent slotCost(SlotType.SlotCount count){
        return count.count() == 1
                ? TranslationUtil.createComponent("modifiers.usedslot", count.type().getDisplayName())
                : TranslationUtil.createComponent("modifiers.usedslots", count.count(), count.type().getDisplayName());
    }

    private void renderControls(GuiGraphics graphics, Font font, ModifierEvaluator.Evaluation eval, int mouseX, int mouseY){
        graphics.fill(x + width - CLUSTER_FROM_RIGHT, y + 1, x + width - 1, y + HEIGHT - 1, ModifierTheme.SCRIM);

        Component addError = addError(eval);
        Component removeError = removeError();
        int minusX = width - MINUS_FROM_RIGHT;
        int plusX = width - PLUS_FROM_RIGHT;
        boolean overMinus = inBox(mouseX, mouseY, minusX);
        boolean overPlus = inBox(mouseX, mouseY, plusX);

        ModifierTheme.minus(graphics, x + minusX, y + 2, STEP_SIZE,
                removeError != null ? ModifierTheme.GLYPH_DISABLED : overMinus ? ModifierTheme.GLYPH_HOVER : ModifierTheme.GLYPH);
        ModifierTheme.plus(graphics, x + plusX, y + 2, STEP_SIZE,
                addError != null ? ModifierTheme.GLYPH_DISABLED : overPlus ? ModifierTheme.GLYPH_HOVER : ModifierTheme.GLYPH);

        if(hasLevels){
            Component level = Component.literal(String.valueOf(eval.level()));
            graphics.drawString(font, level, x + width - LEVEL_FROM_RIGHT + 2 - font.width(level) / 2, y + 4,
                    eval.level() > 0 ? ModifierTheme.TEXT_OK : ModifierTheme.TEXT_DIM);
        }

        if(overPlus){
            queueStepTooltip(graphics, mouseX, mouseY, addError, "modifiers.result.after_add",
                    () -> parent.modifierEvaluator.previewAdd(parent.blueprint, info));
        } else if(overMinus){
            queueStepTooltip(graphics, mouseX, mouseY, removeError, "modifiers.result.after_remove",
                    () -> parent.modifierEvaluator.previewRemove(parent.blueprint, info));
        } else {
            queueDescriptionTooltip(graphics, mouseX, mouseY, eval);
        }
    }

    /** Shows the resulting tool if the step is legal, or the reason it is not */
    private void queueStepTooltip(GuiGraphics graphics, int mouseX, int mouseY, @Nullable Component error,
                                  String headerKey, Supplier<ItemStack> preview){
        parent.postRenderTasks.add(() -> {
            if(error != null){
                parent.renderTooltip(graphics, error.copy().withStyle(ChatFormatting.RED), mouseX, mouseY);
                return;
            }
            ItemStack stack = preview.get();
            if(stack.isEmpty()){
                parent.renderTooltip(graphics, TranslationUtil.createComponent(headerKey), mouseX, mouseY);
                return;
            }
            parent.renderItemTooltip(graphics, stack, mouseX, mouseY);
        });
    }

    private void queueDescriptionTooltip(GuiGraphics graphics, int mouseX, int mouseY, ModifierEvaluator.Evaluation eval){
        parent.postRenderTasks.add(() -> {
            List<Component> lines = new ArrayList<>();
            lines.add(hasLevels && eval.level() > 0 ? modifier.getDisplayName(eval.level()) : modifier.getDisplayName());
            lines.addAll(modifier.getDescriptionList());
            if(incremental){
                lines.add(TranslationUtil.createComponent("modifiers.incremental").withStyle(ChatFormatting.AQUA));
            }
            lines.add(Component.empty());
            if(eval.error() != null){
                lines.add(eval.error().copy().withStyle(ChatFormatting.RED));
            } else {
                lines.add(TranslationUtil.createComponent("modifiers.hint.add").withStyle(ChatFormatting.GREEN));
                if(eval.level() > 0){
                    lines.add(TranslationUtil.createComponent("modifiers.hint.remove").withStyle(ChatFormatting.GRAY));
                }
            }
            parent.renderComponentTooltip(graphics, lines, mouseX, mouseY);
        });
    }

    /** Null when a level may be added, otherwise the reason it may not */
    @Nullable
    private Component addError(ModifierEvaluator.Evaluation eval){
        if((modifier instanceof NoLevelsModifier || modifier instanceof DurabilityShieldModifier) && eval.level() >= 1){
            return Component.translatable(KEY_MAX_LEVEL, modifier.getDisplayName(), 1);
        }
        return eval.error();
    }

    /** Null when a level may be removed, otherwise the reason it may not */
    @Nullable
    private Component removeError(){
        if(removal == null){
            removal = ToolValidator.validateModRemoval(parent.blueprint, info);
        }
        return removal.hasError() ? removal.getMessage() : null;
    }

    private boolean inBox(int mouseX, int mouseY, int boxX){
        return mouseX >= x + boxX && mouseX < x + boxX + STEP_SIZE && mouseY >= y && mouseY < y + HEIGHT;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button){
        if(!active || !visible) return false;
        if(mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + HEIGHT) return false;

        boolean onPlus = within(mouseX, width - PLUS_FROM_RIGHT);
        boolean onMinus = within(mouseX, width - MINUS_FROM_RIGHT);

        if(onPlus || (!onMinus && button == 0)) return addLevel();
        if(onMinus || button == 1) return removeLevel();
        return false;
    }

    private boolean within(double mouseX, int boxX){
        return mouseX >= x + boxX && mouseX < x + boxX + STEP_SIZE;
    }

    private boolean addLevel(){
        if(addError(evaluate()) != null){
            deny();
            return true;
        }
        parent.blueprint.modStack.push(info);
        click();
        parent.refresh();
        return true;
    }

    private boolean removeLevel(){
        if(removeError() != null){
            deny();
            return true;
        }
        parent.blueprint.modStack.pop(info);
        click();
        parent.refresh();
        return true;
    }

    private void click(){
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void deny(){
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ANVIL_HIT, 1.0F));
    }

    @Override
    public void updateWidgetNarration(@NotNull NarrationElementOutput output){
        // No narration needed
    }
}
