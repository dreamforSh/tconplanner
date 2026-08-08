package com.xinian.tconplanner.screen;

import com.xinian.tconplanner.data.BaseBlueprint;
import com.xinian.tconplanner.data.ModifierInfo;
import com.xinian.tconplanner.screen.buttons.BannerWidget;
import com.xinian.tconplanner.screen.buttons.PaginatedPanel;
import com.xinian.tconplanner.screen.buttons.modifiers.AppliedModifierRow;
import com.xinian.tconplanner.screen.buttons.modifiers.ModifierButton;
import com.xinian.tconplanner.screen.buttons.modifiers.ModifierRow;
import com.xinian.tconplanner.screen.buttons.modifiers.ModifierTheme;
import com.xinian.tconplanner.screen.buttons.modifiers.SlotBarWidget;
import com.xinian.tconplanner.util.JECharactersIntegration;
import com.xinian.tconplanner.util.ModifierEvaluator;
import com.xinian.tconplanner.util.ModifierStateEnum;
import com.xinian.tconplanner.util.TranslationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The modifier side panel: slot chips, a search box, three views and one list.
 * <p>
 * The previous version was a three-state machine - browse, an add/remove sub-page that replaced the
 * whole list, and a hidden reorder mode behind an unlabelled icon with its own Save/Cancel. All three
 * are gone. There is one list, always visible; levels change inline (see {@link ModifierRow}) and the
 * stack is reordered inline (see {@link AppliedModifierRow}).
 */
public class ModifierPanel extends PlannerPanel {

    /** Wider than the old 115 so names stop being squashed; needs ~448px of effective screen width */
    public static final int WIDTH = 136;
    /** Fallback for screens too narrow for {@link #WIDTH} */
    public static final int NARROW_WIDTH = 115;

    public static final int TAB_ALL = 0;
    public static final int TAB_AVAILABLE = 1;
    public static final int TAB_APPLIED = 2;
    private static final int TAB_COUNT = 3;

    private static final int MARGIN = 2;
    private static final int SLOT_BAR_Y = 21;
    private static final int SEARCH_Y = 36;
    private static final int SEARCH_HEIGHT = 14;
    private static final int TAB_Y = 52;
    //13 rather than 12 so the selected tab's 2px accent bar clears the label
    private static final int TAB_HEIGHT = 13;
    private static final int LIST_Y = 66;
    private static final int BROWSE_ROWS = 8;
    private static final int APPLIED_ROWS = 7;
    private static final int RESET_Y = 186;
    private static final int RESET_HEIGHT = 16;

    private final EditBox searchBox;
    @Nullable
    private Component emptyMessage;

    public ModifierPanel(int x, int y, int width, int height, ItemStack result, ToolStack tool,
                         List<IDisplayModifierRecipe> modifiers, PlannerScreen parent) {
        super(x, y, width, height, parent);
        BaseBlueprint<?> blueprint = parent.blueprint;
        int contentWidth = width - MARGIN * 2;
        int rowWidth = contentWidth - 3;

        //Everything cached against a different blueprint dies here, before any lookup below
        parent.modifierEvaluator.sync(blueprint);

        addChild(new BannerWidget((width - 90) / 2, 0, TranslationUtil.createComponent("banner.modifiers"), parent));
        addChild(new SlotBarWidget(MARGIN, SLOT_BAR_Y, contentWidth, tool, parent));

        searchBox = obtainSearchBox(parent, contentWidth);
        //addChild rebases coordinates, so a re-mounted widget must have them re-set, never accumulated
        searchBox.x = MARGIN;
        searchBox.y = SEARCH_Y;
        searchBox.width = contentWidth;
        addChild(searchBox);

        addTabs(blueprint, contentWidth);

        if (parent.modifierTab == TAB_APPLIED) {
            buildAppliedList(blueprint, rowWidth);
        } else {
            buildBrowseList(blueprint, tool, result, modifiers, rowWidth);
        }
    }

    /**
     * A single EditBox lives on the screen and is re-mounted on each build, so its text, caret,
     * selection and focus survive the panel rebuild that every keystroke triggers. Building a fresh box
     * per rebuild dropped the caret to the end of the line after every character.
     */
    private static EditBox obtainSearchBox(PlannerScreen parent, int width) {
        EditBox box = parent.modifierSearchBox;
        if (box == null) {
            box = new EditBox(Minecraft.getInstance().font, 0, 0, width, SEARCH_HEIGHT,
                    TranslationUtil.createComponent("modifiers.search"));
            box.setMaxLength(50);
            box.setValue(parent.modifierSearch);
            box.setResponder(text -> {
                //Also stops the setValue below from re-entering
                if (text.equals(parent.modifierSearch)) return;
                parent.modifierSearch = text;
                PaginatedPanel.resetPage(parent, pageKey(parent.modifierTab));
                //Swap only this panel so the box keeps focus while typing
                parent.refreshModifierPanel();
            });
            parent.modifierSearchBox = box;
        } else if (!box.getValue().equals(parent.modifierSearch)) {
            //setBlueprint clears the query; push that into the surviving widget
            box.setValue(parent.modifierSearch);
        }
        return box;
    }

    private void addTabs(BaseBlueprint<?> blueprint, int contentWidth) {
        int gap = 2;
        int tabWidth = (contentWidth - gap * (TAB_COUNT - 1)) / TAB_COUNT;
        Component[] labels = {
                TranslationUtil.createComponent("modifiers.tab.all"),
                TranslationUtil.createComponent("modifiers.tab.available"),
                TranslationUtil.createComponent("modifiers.tab.applied", blueprint.modStack.size())
        };
        for (int tab = 0; tab < TAB_COUNT; tab++) {
            int target = tab;
            addChild(new ModifierButton(MARGIN + tab * (tabWidth + gap), TAB_Y, tabWidth, TAB_HEIGHT, labels[tab], () -> {
                parent.modifierTab = target;
                parent.refresh();
            }, parent).selected(parent.modifierTab == tab));
        }
    }

    private void buildBrowseList(BaseBlueprint<?> blueprint, ToolStack tool, ItemStack result,
                                 List<IDisplayModifierRecipe> modifiers, int rowWidth) {
        PaginatedPanel<ModifierRow> list = new PaginatedPanel<>(MARGIN, LIST_Y, rowWidth, ModifierRow.HEIGHT,
                1, BROWSE_ROWS, 1, pageKey(parent.modifierTab), parent);
        addChild(list);

        String search = parent.modifierSearch.toLowerCase(Locale.ROOT).trim();
        boolean availableOnly = parent.modifierTab == TAB_AVAILABLE;
        int shown = 0;
        for (ModifierEvaluator.Candidate candidate : parent.modifierEvaluator.candidatesFor(blueprint.toolDefinition, modifiers)) {
            if (!matches(candidate.searchKey(), candidate.displayName(), search)) continue;
            ModifierRow row = new ModifierRow(candidate, rowWidth, tool, result, parent);
            //Only this branch pays for a full validation sweep; results are cached for later refreshes
            if (availableOnly && row.getState() == ModifierStateEnum.UNAVAILABLE) continue;
            list.addChild(row);
            shown++;
        }
        list.refresh();

        if (shown == 0) {
            emptyMessage = TranslationUtil.createComponent(search.isEmpty() ? "modifiers.empty" : "modifiers.empty.search");
        }
    }

    /**
     * Walks the stack applying one modifier at a time, so each row can show the tool as of that step
     * and the first step that cannot be crafted can be marked.
     */
    private void buildAppliedList(BaseBlueprint<?> blueprint, int rowWidth) {
        PaginatedPanel<AppliedModifierRow> list = new PaginatedPanel<>(MARGIN, LIST_Y, rowWidth, AppliedModifierRow.HEIGHT,
                1, APPLIED_ROWS, 1, pageKey(TAB_APPLIED), parent);
        addChild(list);

        List<ModifierInfo> stack = blueprint.modStack.getStack();
        if (stack.isEmpty()) {
            emptyMessage = TranslationUtil.createComponent("modifiers.empty.applied");
            list.refresh();
            return;
        }
        String search = parent.modifierSearch.toLowerCase(Locale.ROOT).trim();
        int shown = 0;

        //Shared with createOutput and validateWith, so the per-step snapshots, the failing-step marker
        //and the tool the rest of the planner shows can never disagree about slot costs
        List<BaseBlueprint.ModifierStep> steps = blueprint.replayModifiers(blueprint.modStack);
        Map<ModifierId, Integer> levels = new HashMap<>();
        boolean alreadyMarked = false;

        for (int i = 0; i < steps.size(); i++) {
            BaseBlueprint.ModifierStep step = steps.get(i);
            ModifierInfo info = step.info();
            int level = levels.merge(info.modifier.getId(), 1, Integer::sum);

            //Only the first failure is the cause; the ones after it are consequences
            Component error = null;
            if (!alreadyMarked && step.error() != null) {
                error = step.error();
                alreadyMarked = true;
            }

            //Every step is replayed even while filtering, so snapshots stay correct. Only the row is
            //skipped, and its index stays the real stack index so reordering acts on the right entry.
            if (matches(info.modifier, search)) {
                list.addChild(new AppliedModifierRow(info, i, steps.size() - 1, level, rowWidth,
                        step.tool().createStack(), error, parent));
                shown++;
            }
        }
        list.refresh();
        if (shown == 0) {
            emptyMessage = TranslationUtil.createComponent("modifiers.empty.search");
        }

        addChild(new ModifierButton(MARGIN + rowWidth / 2 - 29, RESET_Y, 58, RESET_HEIGHT,
                TranslationUtil.createComponent("modifiers.reset"), () -> {
            blueprint.modStack.clear();
            parent.refresh();
        }, parent).dangerous().withTooltip(TranslationUtil.createComponent("modifiers.reset.tooltip", stack.size())));
    }

    private static boolean matches(String searchKey, Component displayName, String search) {
        if (search.isEmpty()) return true;
        if (searchKey.contains(search)) return true;
        return JECharactersIntegration.isLoaded() && JECharactersIntegration.matches(displayName.getString(), search);
    }

    private static boolean matches(Modifier modifier, String search) {
        if (search.isEmpty()) return true;
        Component name = modifier.getDisplayName();
        return matches((name.getString() + '\n' + modifier.getId()).toLowerCase(Locale.ROOT), name, search);
    }

    private static String pageKey(int tab) {
        return "modifiers.page." + tab;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        if (emptyMessage != null) {
            Font font = Minecraft.getInstance().font;
            ModifierTheme.fittedCenteredString(graphics, font, emptyMessage, x + width / 2, y + LIST_Y + 26,
                    width - 8, ModifierTheme.TEXT_FAINT);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        //Only ever SETS the flag; PlannerScreen.mouseClicked clears both flags before dispatch, so a
        //click that lands anywhere else already took the keyboard away
        if (searchBox.mouseClicked(mouseX, mouseY, button)) {
            parent.modifierSearchFocused = true;
            searchBox.setFocused(true);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Drives the search box from {@code parent.modifierSearchFocused} on BOTH edges.
     * <p>
     * {@code AbstractContainerEventHandler.setFocused} always calls {@code setFocused(false)} on the
     * outgoing listener before {@code setFocused(true)} on the incoming one, and the screen re-issues
     * that pair even when the incoming panel is the one already focused. Clearing the box on the false
     * edge alone therefore unfocused it during the very handoff meant to keep it - the search accepted
     * exactly one character and then went deaf, and clicking it again did not recover.
     */
    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        searchBox.setFocused(focused && parent.modifierSearchFocused);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox.isFocused()) {
            if (searchBox.keyPressed(keyCode, scanCode, modifiers)) return true;
            return searchBox.canConsumeInput();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchBox.isFocused()) {
            return searchBox.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }
}
