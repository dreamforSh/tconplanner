package com.xinian.tconplanner.screen.buttons;

import com.mojang.blaze3d.vertex.PoseStack;
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


    public void addChild(AbstractWidget widget){
        allChildren.add((T)widget);
    }

    public void sort(Comparator<T> comparator){allChildren.sort(comparator);}

    public void refresh(){
        refresh(parent.getCacheValue(cachePrefix + ".page", 0));
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
        parent.setCacheValue(cachePrefix + ".page", page);
        refresh(page);
    }

    public void makeVisible(int index, boolean refresh){
        if(index >= 0 && index < allChildren.size()){
            int row = index/columns;
            int page = parent.getCacheValue(cachePrefix + ".page", 0);
            if(page > row){
                parent.setCacheValue(cachePrefix + ".page", row);
                if(refresh)refresh(row);
            }
            else if(page + rows - 1 < row){
                parent.setCacheValue(cachePrefix + ".page", Math.max(0, row - rows + 1));
                if(refresh)refresh(row);
            }
        }
    }

    @Override
    public void render(@NotNull PoseStack stack, int mouseX, int mouseY, float p_230430_4_) {
        super.render(stack, mouseX, mouseY, p_230430_4_);
        if(totalPages > 1) {
            int scrollX = x + width - 3;
            int page = parent.getCacheValue(cachePrefix + ".page", 0);
            Screen.fill(stack, scrollX, y, scrollX + 3, y + height, 0x0f_ffffff + (isHovered ? 0x0a_000000 : 0));
            Screen.fill(stack, scrollX, y + (int)(scrollPageHeight*page), scrollX + 3, y + (int)(scrollPageHeight*(page+rows)), 0x0f_ffffff + (isHovered ? 0x0f_000000 : 0));
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(totalPages > 1) {
            if (mouseX >= x + width - 3 && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                int clickedPage = (int) Math.min(((mouseY - y) / height) * totalPages, totalPages - 1);
                setPage(clickedPage);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        boolean result = false;
        double scrollAmount = scroll * Config.CONFIG.scrollDirection.get().mult;
        int currentPage = parent.getCacheValue(cachePrefix + ".page", 0);
        if(scrollAmount > 0 && currentPage < totalPages){
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
