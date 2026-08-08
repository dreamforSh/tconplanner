package com.xinian.tconplanner.screen.buttons;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import com.xinian.tconplanner.Config;
import com.xinian.tconplanner.screen.PlannerPanel;
import com.xinian.tconplanner.screen.PlannerScreen;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PaginatedPanel<T extends AbstractWidget> extends PlannerPanel {

    private final List<T> allChildren = new ArrayList<>();
    private final String cachePrefix;
    private final int childWidth, childHeight, spacing, columns, rows, pageSize;
    private int totalRows;
    private int totalPages;
    private float scrollPageHeight;
    private boolean isDragging = false;

    public PaginatedPanel(int x, int y, int childWidth, int childHeight, int columns, int rows, int spacing, String cachePrefix, PlannerScreen parent) {
        super(x, y, (childWidth+spacing) * columns - spacing + 4, (childHeight+spacing) * rows - spacing, parent);
        this.childWidth = childWidth;
        this.childHeight = childHeight;
        this.spacing = spacing;
        this.columns = columns;
        this.rows = rows;
        this.pageSize = columns  * rows;
        this.cachePrefix = cachePrefix;
    }


    /**
     * Where the current page is stored in the screen cache.
     * <p>
     * Exposed because callers that want to scroll a list back to the top have to write the same key, and
     * spelling it out at the call site is how the modifier search box ended up resetting a key nothing
     * read - so the list never returned to the top on a new query.
     */
    public static String pageCacheKey(String cachePrefix){
        return cachePrefix + ".page";
    }

    /** Scrolls the list identified by {@code cachePrefix} back to the top on its next build */
    public static void resetPage(PlannerScreen parent, String cachePrefix){
        parent.setCacheValue(pageCacheKey(cachePrefix), 0);
    }

    public void addChild(AbstractWidget widget){
        allChildren.add((T)widget);
    }

    public void sort(Comparator<T> comparator){allChildren.sort(comparator);}

    public void refresh(){
        refresh(parent.getCacheValue(pageCacheKey(cachePrefix), 0));
    }

    public void refresh(int page){
        totalRows = (int)Math.ceil(allChildren.size()/(double)columns);
        totalPages = allChildren.size() > pageSize ? totalRows - rows + 1 : 1;
        if(page >= totalPages){
            setPage(totalPages - 1);
            return;
        }
        children.clear();
        children.addAll(allChildren.subList(page*columns, Math.min(allChildren.size(), pageSize + page*columns)));
        scrollPageHeight = height/(float)(totalPages+rows-1);
        for (int i = 0; i < children.size(); i++) {
            AbstractWidget widget = children.get(i);
            widget.x = x + (i % columns) * (childWidth+spacing);
            widget.y = y + (i / columns) * (childHeight+spacing);
        }
    }

    private void setPage(int page){
        parent.setCacheValue(pageCacheKey(cachePrefix), page);
        refresh(page);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float p_230430_4_) {
        super.render(graphics, mouseX, mouseY, p_230430_4_);
        if(totalPages > 1) {
            int scrollX = x + width - 3;
            int page = parent.getCacheValue(pageCacheKey(cachePrefix), 0);
            graphics.fill(scrollX, y, scrollX + 3, y + height, 0x0f_ffffff + (isHovered ? 0x0a_000000 : 0));
            graphics.fill(scrollX, y + (int)(scrollPageHeight*page), scrollX + 3, y + (int)(scrollPageHeight*(page+rows)), 0x0f_ffffff + (isHovered || isDragging ? 0x0f_000000 : 0));
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(totalPages > 1) {
            if (mouseX >= x + width - 3 && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                isDragging = true;
                int clickedPage = (int) Math.min(Math.max(((mouseY - y) / height) * totalPages, 0), totalPages - 1);
                setPage(clickedPage);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        //Release outside the panel never reaches mouseReleased, so a stale drag would hijack the next
        //press anywhere on the screen; re-check the button here rather than trusting the release
        if (button != 0) {
            isDragging = false;
        }
        if (isDragging) {
            int clickedPage = (int) Math.min(Math.max(((mouseY - y) / height) * totalPages, 0), totalPages - 1);
            setPage(clickedPage);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        boolean result = false;
        double scrollAmount = scroll * Config.CONFIG.scrollDirection.get().mult;
        int currentPage = parent.getCacheValue(pageCacheKey(cachePrefix), 0);
        if(scrollAmount > 0 && currentPage < totalPages - 1){
            setPage(currentPage + 1);
            result = true;
        }else if(scrollAmount < 0 && currentPage > 0){
            setPage(currentPage - 1);
            result = true;
        }
        if(super.mouseScrolled(mouseX, mouseY, scroll))result = true;
        return result;
    }
}
