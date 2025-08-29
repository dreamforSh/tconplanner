package com.xinian.tconplanner.screen;

import com.xinian.tconplanner.api.TCArmor;
import com.xinian.tconplanner.screen.buttons.ArmorTypeButton;
import com.xinian.tconplanner.screen.buttons.BannerWidget;
import com.xinian.tconplanner.screen.buttons.PaginatedPanel;
import com.xinian.tconplanner.util.TranslationUtil;

import java.util.List;

public class ArmorSelectPanel extends PlannerPanel{

    public ArmorSelectPanel(int x, int y, int width, int height, List<TCArmor> armors, PlannerScreen parent) {
        super(x, y, width, height, parent);
        addChild(new BannerWidget(5, 0, TranslationUtil.createComponent("banner.armors"), parent));

        PaginatedPanel<ArmorTypeButton> armorsGroup = new PaginatedPanel<>(0, 23, 18, 18, 5, 3, 2, "armorsgroup", parent);
        addChild(armorsGroup);
        for (int i = 0; i < armors.size(); i++) {
            TCArmor armor = armors.get(i);
            ArmorTypeButton button = new ArmorTypeButton(i, armor, parent);
            armorsGroup.addPageChild(button);
            //armorsGroup.addChild(new ArmorTypeButton(i, armor, parent));
        }
        armorsGroup.refresh();
    }
}
