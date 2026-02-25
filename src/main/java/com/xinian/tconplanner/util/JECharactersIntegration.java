package com.xinian.tconplanner.util;

import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

public class JECharactersIntegration {

    private static Method matchesMethod = null;
    private static boolean jecharactersLoaded = false;
    private static boolean initialized = false;

    private static void initialize() {
        if (initialized) {
            return;
        }
        jecharactersLoaded = ModList.get().isLoaded("jecharacters");
        if (jecharactersLoaded) {
            try {
                Class<?> matchClass = Class.forName("me.towdium.jecharacters.utils.Match");
                matchesMethod = matchClass.getMethod("matches", String.class, String.class);
            } catch (Exception e) {

                jecharactersLoaded = false;
            }
        }
        initialized = true;
    }

    public static boolean matches(String name, String query) {
        initialize();
        if (jecharactersLoaded && matchesMethod != null) {
            try {
                return (boolean) matchesMethod.invoke(null, name, query);
            } catch (Exception e) {

                return false;
            }
        }
        return false;
    }

    public static boolean isLoaded() {
        initialize();
        return jecharactersLoaded;
    }
}
