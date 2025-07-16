package com.xinian.tconplanner.api;

import slimeknights.tconstruct.library.tools.layout.LayoutSlot;

public class TCSlotPos {
    public static final int partsOffsetX = 13, partsOffsetY = 15;

    private final LayoutSlot pos;

    TCSlotPos(LayoutSlot pos){
        this.pos = pos;
    }

    public int getX(){
        return pos.getX() + partsOffsetX;
    }

    public int getY(){
        return pos.getY() + partsOffsetY;
    }

}
