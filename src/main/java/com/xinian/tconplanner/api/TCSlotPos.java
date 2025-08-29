package com.xinian.tconplanner.api;

public class TCSlotPos {
    public static final int partsOffsetX = 15, partsOffsetY = 15;

    private final int x, y;

    public TCSlotPos(int x, int y){
        this.x = x;
        this.y = y;
    }

    public int getX(){
        return x + partsOffsetX;
    }

    public int getY(){
        return y + partsOffsetY;
    }

}