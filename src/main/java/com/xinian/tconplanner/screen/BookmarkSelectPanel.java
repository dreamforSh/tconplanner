package com.xinian.tconplanner.screen;

import com.xinian.tconplanner.data.Blueprint;
import com.xinian.tconplanner.data.PlannerData;
import com.xinian.tconplanner.screen.buttons.BannerWidget;
import com.xinian.tconplanner.screen.buttons.BookmarkedButton;
import com.xinian.tconplanner.screen.buttons.PaginatedPanel;
import com.xinian.tconplanner.util.TranslationUtil;
import com.xinian.tconplanner.data.BaseBlueprint;

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
    }
}
