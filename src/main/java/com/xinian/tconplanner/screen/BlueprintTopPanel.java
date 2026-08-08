package com.xinian.tconplanner.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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

import com.xinian.tconplanner.data.BaseBlueprint;
import java.util.List;

public class BlueprintTopPanel extends PlannerPanel{

    public BlueprintTopPanel(int x, int y, int width, int height, ItemStack result, ToolStack tool, PlannerData data, PlannerScreen parent) {
        super(x, y, width, height, parent);

        BaseBlueprint<?> blueprint = parent.blueprint;
        List<TCSlotPos> positions = blueprint.plannable.getSlotPos();
        for(int i = 0; i < blueprint.materials.length; i++){
            TCSlotPos pos = positions.get(i);
            //Null for a tool with no parts; ToolPartButton falls back to a representative part
            IToolPart part = blueprint.partAt(i);
            addChild(new ToolPartButton(i, pos.getX(), pos.getY(), part, blueprint.statTypes[i],
                    blueprint.materials[i], parent));
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float p_230430_4_) {
        PlannerScreen.bindTexture();
        int boxX = 13, boxY = 24, boxL = 81;
        if(mouseX > boxX + x && mouseY > boxY + y && mouseX < boxX + x + boxL && mouseY < boxY + y + boxL)
            RenderSystem.setShaderColor(1f, 1f, 1f, 0.75f);
        else RenderSystem.setShaderColor(1f, 1f, 1f, 0.5f);
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        graphics.blit(PlannerScreen.TEXTURE, x + boxX, y + boxY, boxX, boxY, boxL, boxL);
        //The blit above leaves the shader colour at alpha 0.5/0.75; without resetting it the 3.7x tool
        //preview and every widget drawn afterwards render washed out
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        int toolX = x + TCSlotPos.partsOffsetX + 7;
        int toolY = y + TCSlotPos.partsOffsetY + 22;
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(toolX, toolY, 0);
        poseStack.scale(3.7F, 3.7F, 1.0F);
        graphics.renderItem(parent.blueprint.plannable.getRenderStack(), 0, 0);
        poseStack.popPose();
        
        super.render(graphics, mouseX, mouseY, p_230430_4_);
    }
}
