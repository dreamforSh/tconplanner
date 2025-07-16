package com.xinian.tconplanner.screen.buttons.modifiers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import com.xinian.tconplanner.data.ModifierInfo;
import com.xinian.tconplanner.screen.PlannerScreen;
import com.xinian.tconplanner.util.TranslationUtil;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.tools.SlotType;

import java.util.ArrayList;
import java.util.List;

public class ModifierStackButton extends Button {

    private final Modifier modifier;
    private final IDisplayModifierRecipe recipe;
    private final ModifierInfo modifierInfo;
    private final PlannerScreen parent;
    private final Component displayName;
    private final ItemStack display;
    private final int index;

    public ModifierStackButton(ModifierInfo modifierInfo, int index, int level, ItemStack display, PlannerScreen parent) {
        super(0, 0, 100, 18, Component.literal(""), e -> {
        });
        this.modifierInfo = modifierInfo;
        this.parent = parent;
        this.modifier = modifierInfo.modifier;
        this.recipe = modifierInfo.recipe;
        this.display = display;
        this.index = index;
        displayName = modifier.getDisplayName(level);
    }

    @Override
    public void renderButton(@NotNull PoseStack stack, int mouseX, int mouseY, float p_230431_4_) {
        PlannerScreen.bindTexture();
        RenderSystem.enableBlend();
        if(parent.selectedModifierStackIndex == index){
            RenderSystem.setShaderColor(255/255f, 200/255f, 0f, 1f);
        }
        parent.blit(stack, x, y, 0, 224, 100, 18);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        Minecraft.getInstance().getItemRenderer().renderGuiItem(display, x + 1, y + 1);
        Font font = Minecraft.getInstance().font;
        stack.pushPose();
        stack.translate(x + 20, y + 2, 0);
        float nameWidth = font.width(displayName);
        int maxWidth = width - 22;
        if (nameWidth > maxWidth) {
            float scale = maxWidth / nameWidth;
            stack.scale(scale, scale, 1);
        }
        Screen.drawString(stack, font, displayName, 0, 0, 0xff_ff_ff_ff);
        stack.popPose();

        stack.pushPose();
        stack.translate(x + 20, y + 11, 0);
        stack.scale(0.5f, 0.5f, 1);
        if (recipe.getSlots() != null) {
            SlotType.SlotCount count = recipe.getSlots();
            MutableComponent text = count.count() == 1 ? TranslationUtil.createComponent("modifiers.usedslot", count.type().getDisplayName()) :
                    TranslationUtil.createComponent("modifiers.usedslots", count.count(), count.type().getDisplayName());
            Screen.drawString(stack, font, text, 0, 0, 0xff_ff_ff_ff);
        }
        stack.popPose();
        if (isHovered) {
            renderToolTip(stack, mouseX, mouseY);
        }
    }

    @Override
    public void renderToolTip(@NotNull PoseStack stack, int mouseX, int mouseY) {
        parent.postRenderTasks.add(() -> {
            List<Component> tooltips = new ArrayList<>(modifier.getDescriptionList());
            parent.renderComponentTooltip(stack, tooltips, mouseX, mouseY);
        });
    }

    @Override
    public void onPress() {
        parent.selectedModifierStackIndex = index;
        parent.refresh();
    }

    public ModifierInfo getModifierInfo() {
        return modifierInfo;
    }
}

