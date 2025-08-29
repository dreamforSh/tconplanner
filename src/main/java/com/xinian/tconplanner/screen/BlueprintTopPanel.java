package com.xinian.tconplanner.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.xinian.tconplanner.api.TCSlotPos;
import com.xinian.tconplanner.data.BaseBlueprint;
import com.xinian.tconplanner.data.PlannerData;
import com.xinian.tconplanner.screen.buttons.IconButton;
import com.xinian.tconplanner.screen.buttons.OutputToolWidget;
import com.xinian.tconplanner.screen.buttons.ToolPartButton;
import com.xinian.tconplanner.util.Icon;
import com.xinian.tconplanner.util.TranslationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.part.IToolPart;

import java.util.List;

public class BlueprintTopPanel extends PlannerPanel{

    public BlueprintTopPanel(int x, int y, int width, int height, ItemStack result, ToolStack tool, PlannerData data, PlannerScreen parent) {
        super(x, y, width, height, parent);

        BaseBlueprint<?> blueprint = parent.blueprint;
        List<TCSlotPos> positions = blueprint.plannable.getSlotPos();
        for(int i = 0; i < blueprint.materials.length; i++){
            TCSlotPos pos = positions.get(i);
            IToolPart part = (IToolPart) blueprint.toolParts[i];
            addChild(new ToolPartButton(i, pos.getX(), pos.getY(), part, blueprint.materials[i], parent));
        }


        addChild(new IconButton(parent.guiWidth - 70, 88, new Icon(3, 0),
                TranslationUtil.createComponent("randomize"), parent, e -> parent.randomize())
                .withSound(Holder.direct(SoundEvents.ENDERMAN_TELEPORT)));

        if(tool != null){
            addChild(new OutputToolWidget(parent.guiWidth - 34, 58, result, parent));
            boolean bookmarked = data.isBookmarked(blueprint);
            boolean starred = blueprint.equals(data.starred);
            addChild(new IconButton(parent.guiWidth - 33, 88, new Icon(bookmarked ? 2 : 1, 0),
                    TranslationUtil.createComponent(bookmarked ? "bookmark.remove" : "bookmark.add"), parent, e -> {if(bookmarked) parent.unbookmarkCurrent(); else parent.bookmarkCurrent();})
                    .withSound(bookmarked ? Holder.direct(SoundEvents.UI_STONECUTTER_TAKE_RESULT) : Holder.direct(SoundEvents.BOOK_PAGE_TURN)));
            if(bookmarked){
                addChild(new IconButton(parent.guiWidth - 18, 88, new Icon(starred ? 7 : 6, 0),
                        TranslationUtil.createComponent(starred ? "star.remove" : "star.add"), parent, e -> {if(starred) parent.unstarCurrent(); else parent.starCurrent();})
                        .withSound(starred ? Holder.direct(SoundEvents.UI_STONECUTTER_TAKE_RESULT) : Holder.direct(SoundEvents.BOOK_PAGE_TURN)));
            }
            assert Minecraft.getInstance().player != null;
            if(Minecraft.getInstance().player.isCreative()) {
                addChild(new IconButton(parent.guiWidth - 48, 88, new Icon(4, 0), TranslationUtil.createComponent("giveitem"), parent, e -> parent.giveItemstack(result))
                        .withSound(Holder.direct(SoundEvents.ITEM_PICKUP)));
            }
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float p_230430_4_) {
        PoseStack ms = guiGraphics.pose();
        ms.pushPose();
        ms.translate(this.getX() + TCSlotPos.partsOffsetX + 37, this.getY() + TCSlotPos.partsOffsetY + 52, 0);
        ms.scale(59.2F, -59.2F, 1.0F);
        //RenderSystem.disableDepthTest();
        int seed = parent.blueprint.plannable.getRenderStack().isEmpty() ? 0
                : parent.blueprint.plannable.getRenderStack().getPopTime();
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        Minecraft.getInstance().getItemRenderer().renderStatic(
                parent.blueprint.plannable.getRenderStack(),
                ItemDisplayContext.GUI,
                0xF000F0,
                OverlayTexture.NO_OVERLAY,
                ms,
                buffer,
                null,
                seed
        );
        buffer.endBatch();
        //RenderSystem.enableDepthTest();
        ms.popPose();
        int boxX = 13, boxY = 24, boxL = 81;
        float alpha = (mouseX > boxX + getX() && mouseY > boxY + getY() && mouseX < boxX + getX() + boxL && mouseY < boxY + getY() + boxL)
                ? 0.75f
                : 0.5f;
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        guiGraphics.blit(PlannerScreen.TEXTURE, getX() + boxX, getY() + boxY, boxX, boxY, boxL, boxL);

        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        super.render(guiGraphics, mouseX, mouseY, p_230430_4_);
    }

}
