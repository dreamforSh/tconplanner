package com.xinian.tconplanner.screen.buttons;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.CreateNarration;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import com.xinian.tconplanner.screen.PlannerScreen;
import com.xinian.tconplanner.util.ToolPartLookup;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.tools.part.IToolPart;

import javax.annotation.Nullable;

public class ToolPartButton extends Button {

    private final ItemStack stack;
    private final IMaterial material;
    /** Null for a tool with no parts; the icon then comes from {@link ToolPartLookup} */
    @Nullable
    public final IToolPart part;
    private final PlannerScreen parent;
    public final int index;

    public ToolPartButton(int index, int x, int y, @Nullable IToolPart part, MaterialStatsId statType,
                          IMaterial material, PlannerScreen parent){
        super(x, y, 16, 16, Component.literal(""), button -> parent.setSelectedPart(index), DEFAULT_NARRATION);
        this.index = index;
        this.part = part;
        this.parent = parent;
        this.material = material;
        this.stack = ToolPartLookup.display(statType, part, material);
    }

    private static final CreateNarration DEFAULT_NARRATION = (p_253298_) -> p_253298_.get();

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean selected = parent.selectedPart == index;
        PlannerScreen.bindTexture();
        RenderSystem.setShaderColor(1f, 1f, 1f, 0.7f);
        RenderSystem.enableBlend();
        
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(0, 0, 200);
        parent.blit(graphics, x - 1, y - 1, 176 + (material == null ? 18 : 0), 41 + (selected ? 18 : 0), 18, 18);
        poseStack.popPose();
        //The plate is blitted at alpha 0.7; the part icon below must not inherit that
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        poseStack.pushPose();
        poseStack.translate(0, 0, 201);
        graphics.renderItem(this.stack, x, y);
        poseStack.popPose();
        
        if(isHovered){
            renderToolTip(graphics, mouseX, mouseY);
        }
    }

    public void renderToolTip(GuiGraphics graphics, int mouseX, int mouseY) {
        parent.postRenderTasks.add(() -> parent.renderItemTooltip(graphics, this.stack, mouseX, mouseY));
    }


}
