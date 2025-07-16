package com.xinian.tconplanner.screen.buttons;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import com.xinian.tconplanner.screen.PlannerScreen;

public class MatPageButton extends Button
        {
private final boolean right;
private final PlannerScreen parent;
public MatPageButton(int x, int y, int change, PlannerScreen parent) {
    super(x, y, 38, 20, Component.literal(""), button -> {parent.materialPage += change; parent.refresh();});  // 变更：new TextComponent("") -> Component.literal("")
    right = change > 0;
    this.parent = parent;
}

@Override
public void renderButton(PoseStack stack, int mouseX, int mouseY, float p_230431_4_) {
    PlannerScreen.bindTexture();
    parent.blit(stack, x, y, right ? 176 : 214, active ? 20 : 0, width, height);
}

}
