package com.xinian.tconplanner.screen.buttons;

import com.xinian.tconplanner.util.ContingameApiHelper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class ImeCompatibleEditBox extends EditBox {
    public ImeCompatibleEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
    }

    @Override
    public void setFocused(boolean isFocused) {
        super.setFocused(isFocused);
        if (ContingameApiHelper.isLoaded()) {
            if (isFocused) {
                ContingameApiHelper.setEditBox(this);
            }
        }
    }
}
