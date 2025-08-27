package com.xinian.tconplanner.util;

import net.minecraft.client.gui.components.EditBox;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

public class ContingameApiHelper {

    private static boolean initialized = false;
    private static boolean isLoaded = false;
    private static Method setEditBoxMethod;
    private static Method clearEditBoxMethod;

    private static void initialize() {
        if (initialized) {
            return;
        }
        isLoaded = ModList.get().isLoaded("contingameime");
        if (isLoaded) {
            try {
                Class<?> apiClass = Class.forName("com.thinkingstudios.contingame.api.ContingameApi");
                setEditBoxMethod = apiClass.getMethod("setEditBox", EditBox.class);
                clearEditBoxMethod = apiClass.getMethod("clearEditBox");
            } catch (Exception e) {
                isLoaded = false;
            }
        }
        initialized = true;
    }

    public static void setEditBox(EditBox box) {
        initialize();
        if (isLoaded && setEditBoxMethod != null) {
            try {
                setEditBoxMethod.invoke(null, box);
            } catch (Exception e) {
                // ignore
            }
        }
    }

    public static void clearEditBox() {
        initialize();
        if (isLoaded && clearEditBoxMethod != null) {
            try {
                clearEditBoxMethod.invoke(null);
            } catch (Exception e) {
                // ignore
            }
        }
    }
    
    public static boolean isLoaded() {
        initialize();
        return isLoaded;
    }
}
