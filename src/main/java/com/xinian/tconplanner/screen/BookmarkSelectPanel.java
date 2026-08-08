package com.xinian.tconplanner.screen;

import com.xinian.tconplanner.TConPlanner;
import com.xinian.tconplanner.data.BlueprintIO;
import com.xinian.tconplanner.data.PlannerData;
import com.xinian.tconplanner.screen.buttons.BannerWidget;
import com.xinian.tconplanner.screen.buttons.BookmarkedButton;
import com.xinian.tconplanner.screen.buttons.PaginatedPanel;
import com.xinian.tconplanner.screen.buttons.TextButton;
import com.xinian.tconplanner.util.TranslationUtil;
import com.xinian.tconplanner.data.BaseBlueprint;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class BookmarkSelectPanel extends PlannerPanel {

    private static final int GRID_Y = 23;
    private static final int CELL = 18;
    private static final int GAP = 2;
    private static final int GRID_COLUMNS = 5;
    /**
     * Four, not five. At five the grid ran to y=121 while the export/import row sat at y=112..130, so
     * the buttons covered the whole bottom row of bookmarks - and because the old
     * {@code PlannerPanel.mouseClicked} did not stop at the first consumer, one click fired the hidden
     * bookmark (replacing the blueprint you were editing) and then the button.
     */
    private static final int GRID_ROWS = 4;
    private static final int GRID_HEIGHT = (CELL + GAP) * GRID_ROWS - GAP;
    private static final int BUTTON_HEIGHT = 18;
    private static final int BUTTON_Y = GRID_Y + GRID_HEIGHT + 3;

    /** Exported so PlannerScreen cannot size this panel out of step with its own layout again */
    public static final int HEIGHT = BUTTON_Y + BUTTON_HEIGHT;

    public BookmarkSelectPanel(int x, int y, int width, int height, PlannerData data, PlannerScreen parent) {
        super(x, y, width, height, parent);
        addChild(new BannerWidget(5, 0, TranslationUtil.createComponent("banner.bookmarked"), parent));

        PaginatedPanel<BookmarkedButton> bookmarkGroup = new PaginatedPanel<>(
                0, GRID_Y, CELL, CELL, GRID_COLUMNS, GRID_ROWS, GAP, "bookmarkedgroup", parent);
        addChild(bookmarkGroup);
        for (int i = 0; i < data.saved.size(); i++) {
            BaseBlueprint<?> bookmarked = data.saved.get(i);
            boolean starred = bookmarked.equals(data.starred);
            bookmarkGroup.addChild(new BookmarkedButton(i, bookmarked, starred, parent));
        }
        bookmarkGroup.refresh();

        addChild(new TextButton(5, BUTTON_Y, TranslationUtil.createComponent("export.current"),
                () -> exportCurrent(parent), parent).withWidth(45));
        addChild(new TextButton(52, BUTTON_Y, TranslationUtil.createComponent("import"),
                () -> importFromClipboard(data, parent), parent).withWidth(45));
    }

    private static void exportCurrent(PlannerScreen parent) {
        BaseBlueprint<?> blueprint = parent.blueprint;
        String code = blueprint != null && blueprint.isComplete() ? BlueprintIO.exportToCode(blueprint) : null;
        if (code == null) {
            //Every failure path here used to be an empty if, so a failed export looked identical to no click
            tell(TranslationUtil.createComponent("export.fail").withStyle(ChatFormatting.RED));
            return;
        }
        Minecraft.getInstance().keyboardHandler.setClipboard(code);
        //The code is now the full portable payload, so it goes to the clipboard and not into chat
        tell(TranslationUtil.createComponent("export.success"));
    }

    private static void importFromClipboard(PlannerData data, PlannerScreen parent) {
        String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
        BaseBlueprint<?> imported = clip == null || clip.isEmpty() ? null : BlueprintIO.importFromCode(clip);
        if (imported == null || !imported.isComplete() || !data.addBlueprint(imported)) {
            tell(TranslationUtil.createComponent("import.fail").withStyle(ChatFormatting.RED));
            return;
        }
        try {
            data.refresh();
        } catch (Exception e) {
            TConPlanner.LOGGER.error("Failed to refresh data", e);
        }
        tell(TranslationUtil.createComponent("import.success"));
        parent.refresh();
    }

    private static void tell(Component message) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(message, false);
        }
    }
}
