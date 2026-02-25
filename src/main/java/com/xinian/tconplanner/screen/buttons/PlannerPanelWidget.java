package com.xinian.tconplanner.screen.buttons;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xinian.tconplanner.screen.PlannerScreen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;


public abstract class PlannerPanelWidget extends AbstractWidget {

    protected final PlannerScreen parent;

    public PlannerPanelWidget(int x, int y, int width, int height, PlannerScreen parent) {
        super(x, y, width, height, Component.empty());
        this.parent = parent;
    }


}
