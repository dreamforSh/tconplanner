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
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

public class BookmarkSelectPanel extends PlannerPanel {

    public BookmarkSelectPanel(int x, int y, int width, int height, PlannerData data, PlannerScreen parent) {
        super(x, y, width, height, parent);
        addChild(new BannerWidget(5, 0, TranslationUtil.createComponent("banner.bookmarked"), parent));
        
        PaginatedPanel<BookmarkedButton> bookmarkGroup = new PaginatedPanel<>(0, 23, 18, 18, 5, 5, 2, "bookmarkedgroup", parent);
        addChild(bookmarkGroup);
        for (int i = 0; i < data.saved.size(); i++) {
            BaseBlueprint<?> bookmarked = data.saved.get(i);
            boolean starred = bookmarked.equals(data.starred);
            bookmarkGroup.addChild(new BookmarkedButton(i, bookmarked, starred, parent));
        }
        bookmarkGroup.refresh();
        
        int buttonY = height - 15;
        addChild(new TextButton(5, buttonY, TranslationUtil.createComponent("export.current"), () -> {
            BaseBlueprint<?> blueprint = parent.blueprint;
            if (blueprint != null && blueprint.isComplete()) {
                String shortCode = BlueprintIO.exportToShortCode(blueprint);
                if (shortCode != null) {
                    Minecraft.getInstance().keyboardHandler.setClipboard(shortCode);
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.displayClientMessage(
                            Component.literal(String.format(TranslationUtil.createComponent("export.success").getString(), shortCode)), 
                            false
                        );
                    }
                }
            }
        }, parent).withWidth(45));
        
        addChild(new TextButton(52, buttonY, TranslationUtil.createComponent("import"), () -> {
            String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (clip != null && !clip.isEmpty()) {
                BaseBlueprint<?> imported = BlueprintIO.importFromCode(clip);
                if (imported != null && imported.isComplete()) {
                    if (data.addBlueprint(imported)) {
                        try {
                            data.refresh();
                        } catch (Exception e) {
                            TConPlanner.LOGGER.error("Failed to refresh data", e);
                        }
                        if (Minecraft.getInstance().player != null) {
                            Minecraft.getInstance().player.displayClientMessage(
                                TranslationUtil.createComponent("import.success").append("1"),
                                false
                            );
                        }
                        parent.refresh();
                    }
                }
            }
        }, parent).withWidth(45));
    }
}
