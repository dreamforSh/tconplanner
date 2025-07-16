package com.xinian.tconplanner.screen.buttons;

import com.xinian.tconplanner.Config;
import com.xinian.tconplanner.screen.PlannerPanel;
import com.xinian.tconplanner.screen.PlannerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PaginatedPanel<T extends AbstractWidget> extends PlannerPanel {

    private final List<T> allChildren = new ArrayList<>();
    private final List<T> currentWidgets = new ArrayList<>();
    private final String cachePrefix;
    private final int childWidth, childHeight, spacing, columns, rows, pageSize;
    private int totalRows;
    private int totalPages;
    private float scrollPageHeight;

    public PaginatedPanel(int x, int y, int childWidth, int childHeight, int columns, int rows, int spacing, String cachePrefix, PlannerScreen parent) {
        super(x, y, (childWidth + spacing) * columns - spacing + 4, (childHeight + spacing) * rows - spacing, parent);
        this.childWidth = childWidth;
        this.childHeight = childHeight;
        this.spacing = spacing;
        this.columns = columns;
        this.rows = rows;
        this.pageSize = columns * rows;
        this.cachePrefix = cachePrefix;
    }


    public void addPageChild(T widget) {
        allChildren.add(widget);
    }

    public void sort(Comparator<T> comparator) {
        allChildren.sort(comparator);
    }

    public void refresh() {
        refresh(parent.getCacheValue(cachePrefix + ".page", 0));
    }

    public void refresh(int page) {
        totalPages = allChildren.size() > pageSize ? (int)Math.ceil(allChildren.size() / (float)pageSize) : 1;
        if (page >= totalPages) {
            setPage(Math.max(0, totalPages - 1));
            return;
        }

        // 清除旧组件
        for (T widget : currentWidgets) {
            parent.removeWidget(widget);
        }
        currentWidgets.clear();

        children.clear();

        // 当前页布局
        int start = page * pageSize;
        int end = Math.min(allChildren.size(), start + pageSize);
        for (int i = start; i < end; i++) {
            T widget = allChildren.get(i);
            int index = i - start;
            int col = index % columns;
            int row = index / columns;

            int x = this.getX() + col * (childWidth + spacing);
            int y = this.getY() + row * (childHeight + spacing);
            widget.setX(x);
            widget.setY(y);

            parent.addRenderableWidget(widget);
            currentWidgets.add(widget);
            children.add(widget);
        }

        scrollPageHeight = this.getHeight() / (float) totalPages;
    }
//    public void refresh(int page) {
//        totalRows = (int) Math.ceil(allChildren.size() / (double) columns);
//        totalPages = allChildren.size() > pageSize ? (int)Math.ceil(allChildren.size() / (double)columns) - rows + 1 : 1;
//        if (page >= totalPages) {
//            setPage(Math.max(0, totalPages - 1));
//            return;
//        }
//        children.clear();
//        children.addAll(allChildren.subList(page * columns, Math.min(allChildren.size(), pageSize + page * columns)));
//        scrollPageHeight = this.getHeight() / (float) (totalPages + rows - 1);
//        for (int i = 0; i < children.size(); i++) {
//            AbstractWidget widget = children.get(i);
//            widget.setX(this.getX() + (i % columns) * (childWidth + spacing));
//            widget.setY(this.getY() + (i / columns) * (childHeight + spacing) - page * (childHeight+spacing));
//        }
//    }

    private void setPage(int page) {
        parent.setCacheValue(cachePrefix + ".page", page);
        refresh(page);
    }


    public void makeVisible(int index, boolean refresh) {
        if (index >= 0 && index < allChildren.size()) {
            int row = index / columns;
            int page = parent.getCacheValue(cachePrefix + ".page", 0);
            if (page > row) {
                setPage(row);
                if(refresh) refresh(row);
            } else if (page + rows - 1 < row) {
                setPage(Math.max(0, row - rows + 1));
                if(refresh) refresh(row);
            }
        }
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        if (totalPages > 1) {
            int scrollX = this.getX() + this.getWidth() - 3;
            int page = parent.getCacheValue(cachePrefix + ".page", 0);
            guiGraphics.fill(scrollX, this.getY(), scrollX + 3, this.getY() + this.getHeight(), 0x0f_ffffff + (isHovered() ? 0x0a_000000 : 0));
            guiGraphics.fill(scrollX, this.getY() + (int) (scrollPageHeight * page), scrollX + 3, this.getY() + (int) (scrollPageHeight * (page + rows)), 0x0f_ffffff + (isHovered() ? 0x0f_000000 : 0));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(super.mouseClicked(mouseX, mouseY, button)) return true;

        if (totalPages > 1) {
            if (mouseX >= this.getX() + this.getWidth() - 3 && mouseX <= this.getX() + this.getWidth() && mouseY >= this.getY() && mouseY <= this.getY() + this.getHeight()) {
                int clickedPage = (int) Math.min(((mouseY - this.getY()) / this.getHeight()) * totalPages, totalPages - 1);
                setPage(clickedPage);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if(isMouseOver(mouseX, mouseY)) {
            double scrollAmount = delta * Config.CONFIG.scrollDirection.get().mult;
            int currentPage = parent.getCacheValue(cachePrefix + ".page", 0);
            int newPage = currentPage + (int)Math.round(scrollAmount);
            newPage = Math.max(0, Math.min(newPage, totalPages - 1));
            if(newPage != currentPage) {
                setPage(newPage);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
}
