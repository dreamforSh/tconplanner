package com.xinian.tconplanner.screen.buttons;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.CreateNarration;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import com.xinian.tconplanner.data.BaseBlueprint;
import com.xinian.tconplanner.screen.PlannerScreen;
import com.xinian.tconplanner.util.TranslationUtil;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.recipe.RecipeResult;
import slimeknights.tconstruct.library.tools.part.ToolPartItem;

import java.util.ArrayList;
import java.util.List;

public class MaterialButton extends Button {

    public final IMaterial material;
    public final ItemStack stack;
    private final PlannerScreen parent;
    public boolean selected = false;
    public Component errorText;

    public MaterialButton(IMaterial material, ItemStack stack, int x, int y, PlannerScreen parent) {
        super(x, y, 16, 16, stack.getHoverName(), button -> parent.setPart(material), DEFAULT_NARRATION);
        this.material = material;
        this.stack = stack;
        this.parent = parent;
        if (parent.blueprint.isComplete()) {
            //In-place swap instead of clone(): cloning round-trips through NBT, once per material tile
            RecipeResult<?> result = parent.blueprint.validateWithMaterial(parent.selectedPart, material);
            if (result.hasError()) {
                errorText = result.getMessage().copy().withStyle(ChatFormatting.DARK_RED);
            }
        }
    }

    private static final CreateNarration DEFAULT_NARRATION = (p_253298_) -> p_253298_.get();

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.renderItem(this.stack, x, y);
        int right = x + width;
        int bottom = y + height;
        //guiOverlay, not the plain fill: renderItem draws at z=150 and writes depth, so a default fill
        //at z=0 is depth-rejected exactly over the icon - the tint only survived in the sprite's
        //transparent margin, which is why selection and error highlights looked punched out
        if (selected) {
            graphics.fill(RenderType.guiOverlay(), x, y, right, bottom, 0x55_00_ff_00);
        }
        if (errorText != null) {
            graphics.fill(RenderType.guiOverlay(), x, y, right, bottom, 0x55_ff_00_00);
        }
        if (isHovered) {
            graphics.fill(RenderType.guiOverlay(), x, y, right, bottom, 0x80_ffea00);
            renderToolTip(graphics, mouseX, mouseY);
        }
    }

    public void renderToolTip(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        if (errorText == null) {
            parent.postRenderTasks.add(() -> {
                List<Component> tooltip = new ArrayList<>();
                if (Screen.hasControlDown() && stack.getItem() instanceof ToolPartItem part) {
                    tooltip.add(part.getName(stack));
                    List<ModifierEntry> entries = MaterialRegistry.getInstance().getTraits(material.getIdentifier(), part.getStatType());
                    for (ModifierEntry entry : entries) {
                        Modifier modifier = entry.getModifier();
                        tooltip.add(Component.literal("").append(modifier.getDisplayName(entry.getLevel())).withStyle(ChatFormatting.UNDERLINE));
                        TextColor c = TextColor.fromRgb(modifier.getColor());
                        for (Component comp : modifier.getDescriptionList(entry.getLevel())) {
                            tooltip.add(Component.literal("").append(comp).withStyle(Style.EMPTY.withColor(c)));
                        }
                    }
                } else {
                    tooltip.addAll(stack.getTooltipLines(parent.getMinecraft().player, TooltipFlag.Default.NORMAL));
                    if (!Screen.hasControlDown()) {
                        tooltip.add(TranslationUtil.createComponent("parts.modifier_descriptions", TConstruct.makeTranslation("key", "ctrl").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC)));
                    }
                }
                parent.renderComponentTooltip(graphics, tooltip, mouseX, mouseY);
            });
        } else {
            parent.postRenderTasks.add(() -> graphics.renderTooltip(parent.getFont(), errorText, mouseX, mouseY));
        }
    }

    @Override
    public void onPress() {
        if (errorText == null) {
            parent.setPart(material);
        }
    }

    @Override
    public void playDownSound(@NotNull SoundManager sound) {
        if (errorText != null) {
            sound.play(SimpleSoundInstance.forUI(SoundEvents.ANVIL_HIT, 1.0F));
        } else {
            super.playDownSound(sound);
        }
    }
}
