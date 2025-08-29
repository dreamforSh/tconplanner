package com.xinian.tconplanner.screen.buttons.modifiers;

import com.xinian.tconplanner.TConPlanner;
import com.xinian.tconplanner.data.ModifierInfo;
import com.xinian.tconplanner.screen.PlannerScreen;
import com.xinian.tconplanner.util.TranslationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.tools.SlotType;

import java.util.List;

public class ModifierStackButton extends Button {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(TConPlanner.MODID, "textures/gui/planner.png");

    private final IDisplayModifierRecipe recipe;
    private final PlannerScreen parent;
    private final ItemStack display;
    private final int index;

    private final Modifier modifier;
    private final ModifierInfo modifierInfo;
    private final Component displayName;

    public ModifierStackButton(ModifierInfo modifierInfo, int index, int level, ItemStack display, PlannerScreen parent) {
        super(0, 0, 100, 18, Component.literal(""), (button) -> {
            parent.selectedModifierStackIndex = index;
            parent.refresh();
        }, DEFAULT_NARRATION);
        this.modifier = modifierInfo.modifier;
        this.parent = parent;
        this.modifierInfo = modifierInfo;
        this.recipe = modifierInfo.recipe;
        this.display = display;
        this.index = index;
        displayName = modifier.getDisplayName(level);


        List<Component> tooltipLines = modifier.getDescriptionList();
        if (!tooltipLines.isEmpty()) {

            this.setTooltip(Tooltip.create(Component.translatable(String.join("\n", tooltipLines.stream().map(Component::getString).toList()))));
        }
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (parent.selectedModifierStackIndex == index) {
            guiGraphics.setColor(255 / 255f, 200 / 255f, 0f, 1f);
        } else {
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        guiGraphics.blit(TEXTURE, this.getX(), this.getY(), 0, 224, this.width, this.height, 256, 256);
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        guiGraphics.renderFakeItem(this.display, this.getX() + 1, this.getY() + 1);

        Font font = Minecraft.getInstance().font;


        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(this.getX() + 20, this.getY() + 2, 0);
        float nameWidth = font.width(this.getMessage());
        int maxWidth = this.width - 22;
        if (nameWidth > maxWidth) {
            float scale = maxWidth / nameWidth;
            guiGraphics.pose().scale(scale, scale, 1);
        }
        guiGraphics.drawString(font, this.getMessage(), 0, 0, 0xffffffff);
        guiGraphics.pose().popPose();


        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(this.getX() + 20, this.getY() + 11, 0);
        guiGraphics.pose().scale(0.5f, 0.5f, 1);
        if (recipe.getSlots() != null) {
            SlotType.SlotCount count = recipe.getSlots();
            MutableComponent text = count.count() == 1 ? TranslationUtil.createComponent("modifiers.usedslot", count.type().getDisplayName()) :
                    TranslationUtil.createComponent("modifiers.usedslots", count.count(), count.type().getDisplayName());
            guiGraphics.drawString(font, text, 0, 0, 0xffffffff);
        }
        guiGraphics.pose().popPose();
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
