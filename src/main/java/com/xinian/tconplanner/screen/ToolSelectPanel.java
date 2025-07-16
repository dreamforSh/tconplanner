package com.xinian.tconplanner.screen;

import com.xinian.tconplanner.api.TCTool;
import com.xinian.tconplanner.screen.buttons.BannerWidget;
import com.xinian.tconplanner.screen.buttons.PaginatedPanel;
import com.xinian.tconplanner.screen.buttons.ToolTypeButton;
import com.xinian.tconplanner.util.TranslationUtil;

import java.util.List;

public class ToolSelectPanel extends PlannerPanel{

    public ToolSelectPanel(int x, int y, int width, int height, List<TCTool> tools, PlannerScreen parent) {
        super(x, y, width, height, parent);
        addChild(new BannerWidget(5, 0, TranslationUtil.createComponent("banner.tools")));

        PaginatedPanel<ToolTypeButton> toolsGroup = new PaginatedPanel<>(0, 23, 18, 18, 5, 3, 2,"toolsgroup", parent);
        addChild(toolsGroup);
        for (int i = 0; i < tools.size(); i++) {
            TCTool tool = tools.get(i);
            ToolTypeButton button = new ToolTypeButton(i, tool, parent);
//            toolsGroup.addChild(new ToolTypeButton(i, tool, parent));
            toolsGroup.addPageChild(button);
        }
        toolsGroup.refresh();
    }
}
