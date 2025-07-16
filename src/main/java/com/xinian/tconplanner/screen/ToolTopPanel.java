package com.xinian.tconplanner.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import com.xinian.tconplanner.api.TCSlotPos;
import com.xinian.tconplanner.data.Blueprint;
import com.xinian.tconplanner.data.PlannerData;
import com.xinian.tconplanner.screen.buttons.IconButton;
import com.xinian.tconplanner.screen.buttons.OutputToolWidget;
import com.xinian.tconplanner.screen.buttons.ToolPartButton;
import com.xinian.tconplanner.util.Icon;
import com.xinian.tconplanner.util.TranslationUtil;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.part.IToolPart;

import java.util.List;

public class ToolTopPanel extends PlannerPanel {

    public ToolTopPanel(int x, int y, int width, int height, ItemStack result, ToolStack tool, PlannerData data, PlannerScreen parent) {
        super(x, y, width, height, parent);

        Blueprint blueprint = parent.blueprint;
        List<TCSlotPos> positions = blueprint.tool.getSlotPos();
        for (int i = 0; i < blueprint.materials.length; i++) {
            TCSlotPos pos = positions.get(i);

            IToolPart part = blueprint.toolParts[i];
            addChild(new ToolPartButton(i, pos.getX(), pos.getY(), part, blueprint.materials[i], parent));
        }

        addChild(new IconButton(parent.guiWidth - 70, 88, new Icon(3, 0),
                TranslationUtil.createComponent("randomize"), e -> parent.randomize())
                .withSound(Holder.direct(SoundEvents.ENDERMAN_TELEPORT)));

        if (tool != null) {
            addChild(new OutputToolWidget(parent.guiWidth - 34, 58, result));
            boolean bookmarked = data.isBookmarked(blueprint);
            boolean starred = blueprint.equals(data.starred);
            addChild(new IconButton(parent.guiWidth - 33, 88, new Icon(bookmarked ? 2 : 1, 0),
                    TranslationUtil.createComponent(bookmarked ? "bookmark.remove" : "bookmark.add"), e -> {
                if (bookmarked) parent.unbookmarkCurrent();
                else parent.bookmarkCurrent();
            })
                    .withSound(Holder.direct(bookmarked ? SoundEvents.UI_STONECUTTER_TAKE_RESULT : SoundEvents.BOOK_PAGE_TURN)));
            if (bookmarked) {
                addChild(new IconButton(parent.guiWidth - 18, 88, new Icon(starred ? 7 : 6, 0),
                        TranslationUtil.createComponent(starred ? "star.remove" : "star.add"), e -> {
                    if (starred) parent.unstarCurrent();
                    else parent.starCurrent();
                })
                        .withSound(Holder.direct(starred ? SoundEvents.UI_STONECUTTER_TAKE_RESULT : SoundEvents.BOOK_PAGE_TURN)));
            }
            assert Minecraft.getInstance().player != null;
            if (Minecraft.getInstance().player.isCreative()) {
                addChild(new IconButton(parent.guiWidth - 48, 88, new Icon(4, 0), TranslationUtil.createComponent("giveitem"), e -> parent.giveItemstack(result))
                        .withSound(Holder.direct(SoundEvents.ITEM_PICKUP)));
            }
        }
    }


    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        poseStack.translate(getX() + TCSlotPos.partsOffsetX + 7, getY() + TCSlotPos.partsOffsetY + 22, -200);
        poseStack.scale(3.7F, 3.7F, 1.0F);

        guiGraphics.renderItem(parent.blueprint.toolStack, 0, 0);
        poseStack.popPose();


        int boxX = 13, boxY = 24, boxL = 81;

        if (mouseX > boxX + getX() && mouseY > boxY + getY() && mouseX < boxX + getX() + boxL && mouseY < boxY + getY() + boxL)
            RenderSystem.setShaderColor(1f, 1f, 1f, 0.75f);
        else RenderSystem.setShaderColor(1f, 1f, 1f, 0.5f);

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();

        guiGraphics.blit(PlannerScreen.TEXTURE, getX() + boxX, getY() + boxY, boxX, boxY, boxL, boxL);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableDepthTest();


        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
    }
}
