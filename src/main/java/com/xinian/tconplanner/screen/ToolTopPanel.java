package com.xinian.tconplanner.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
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

public class ToolTopPanel extends PlannerPanel{

    public ToolTopPanel(int x, int y, int width, int height, ItemStack result, ToolStack tool, PlannerData data, PlannerScreen parent) {
        super(x, y, width, height, parent);

        Blueprint blueprint = parent.blueprint;
        List<TCSlotPos> positions = blueprint.tool.getSlotPos();
        for(int i = 0; i < blueprint.materials.length; i++){
            TCSlotPos pos = positions.get(i);
            IToolPart part = (IToolPart) blueprint.toolParts[i];
            addChild(new ToolPartButton(i, pos.getX(), pos.getY(), part, blueprint.materials[i], parent));
        }


        addChild(new IconButton(parent.guiWidth - 70, 88, new Icon(3, 0),
                TranslationUtil.createComponent("randomize"), parent, e -> parent.randomize())
                .withSound(SoundEvents.ENDERMAN_TELEPORT));

        if(tool != null){
            addChild(new OutputToolWidget(parent.guiWidth - 34, 58, result, parent));
            boolean bookmarked = data.isBookmarked(blueprint);
            boolean starred = blueprint.equals(data.starred);
            addChild(new IconButton(parent.guiWidth - 33, 88, new Icon(bookmarked ? 2 : 1, 0),
                    TranslationUtil.createComponent(bookmarked ? "bookmark.remove" : "bookmark.add"), parent, e -> {if(bookmarked) parent.unbookmarkCurrent(); else parent.bookmarkCurrent();})
                    .withSound(bookmarked ? SoundEvents.UI_STONECUTTER_TAKE_RESULT : SoundEvents.BOOK_PAGE_TURN));
            if(bookmarked){
                addChild(new IconButton(parent.guiWidth - 18, 88, new Icon(starred ? 7 : 6, 0),
                        TranslationUtil.createComponent(starred ? "star.remove" : "star.add"), parent, e -> {if(starred) parent.unstarCurrent(); else parent.starCurrent();})
                        .withSound(starred ? SoundEvents.UI_STONECUTTER_TAKE_RESULT : SoundEvents.BOOK_PAGE_TURN));
            }
            assert Minecraft.getInstance().player != null;
            if(Minecraft.getInstance().player.isCreative()) {
                addChild(new IconButton(parent.guiWidth - 48, 88, new Icon(4, 0), TranslationUtil.createComponent("giveitem"), parent, e -> parent.giveItemstack(result))
                        .withSound(SoundEvents.ITEM_PICKUP));
            }
        }
    }

    @Override
    public void render(@NotNull PoseStack stack, int mouseX, int mouseY, float p_230430_4_) {
        PoseStack itemModelStack = RenderSystem.getModelViewStack();
        itemModelStack.pushPose();
        itemModelStack.translate(x + TCSlotPos.partsOffsetX + 7, y + TCSlotPos.partsOffsetY + 22, -200);
        itemModelStack.scale(3.7F, 3.7F, 1.0F);
        Minecraft.getInstance().getItemRenderer().renderGuiItem(parent.blueprint.toolStack, 0, 0);
        itemModelStack.popPose();
        PlannerScreen.bindTexture();
        int boxX = 13, boxY = 24, boxL = 81;
        if(mouseX > boxX + x && mouseY > boxY + y && mouseX < boxX + x + boxL && mouseY < boxY + y + boxL)
            RenderSystem.setShaderColor(1f, 1f, 1f, 0.75f);
        else RenderSystem.setShaderColor(1f, 1f, 1f, 0.5f);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        this.blit(stack, x + boxX, y + boxY, boxX, boxY, boxL, boxL);
        super.render(stack, mouseX, mouseY, p_230430_4_);
    }
}
