package com.xinian.tconplanner.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class TranslationUtil {

    public static MutableComponent createComponent(String key, Object... inserts){
        return Component.translatable("gui.tconplanner." + key, inserts);
    }

}
