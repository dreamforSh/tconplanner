package com.xinian.tconplanner.screen.buttons;

import com.xinian.tconplanner.data.BaseBlueprint;
import com.xinian.tconplanner.data.Blueprint;
import com.xinian.tconplanner.screen.PlannerScreen;
import com.xinian.tconplanner.util.TranslationUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
        super(x, y, 16, 16, stack.getHoverName(), button -> {}, DEFAULT_NARRATION);
        this.material = material;
        this.stack = stack;
        this.parent = parent;
        if (parent.blueprint.isComplete()) {
            BaseBlueprint<?> cloned = parent.blueprint.clone();
            cloned.materials[parent.selectedPart] = material;
            RecipeResult<?> result = cloned.validate();
            if (result.hasError()) {
                errorText = result.getMessage().copy().withStyle(ChatFormatting.DARK_RED);
            }
        }
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.renderFakeItem(this.stack, this.getX(), this.getY());

        int right = this.getX() + this.width;
        int bottom = this.getY() + this.height;

        if (this.selected) {
            guiGraphics.fill(this.getX(), this.getY(), right, bottom, 0x55_00_ff_00);
        }
        if (this.errorText != null) {
            guiGraphics.fill(this.getX(), this.getY(), right, bottom, 0x55_ff_00_00);
        }
        if (this.isHoveredOrFocused()) {
            guiGraphics.fill(this.getX(), this.getY(), right, bottom, 0x80_ffea00);
            this.renderToolTip(guiGraphics, mouseX, mouseY);
        }
    }

    // This is no longer an override, but a helper method called from renderWidget
    private void renderToolTip(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (errorText != null) {
            guiGraphics.renderTooltip(minecraft.font, errorText, mouseX, mouseY);
        } else {
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
                tooltip.addAll(stack.getTooltipLines(minecraft.player, TooltipFlag.Default.NORMAL));
                if (!Screen.hasControlDown()) {
                    tooltip.add(TranslationUtil.createComponent("parts.modifier_descriptions", TConstruct.makeTranslation("key", "ctrl").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC)));
                }
            }
            guiGraphics.renderComponentTooltip(minecraft.font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public void onPress() {
        if (this.errorText == null) {
            parent.setPart(material);
        }
    }

    @Override
    public void playDownSound(@NotNull SoundManager sound) {
        if (this.errorText != null) {
            sound.play(SimpleSoundInstance.forUI(SoundEvents.ANVIL_HIT, 1.0F));
        } else {
            super.playDownSound(sound);
        }
    }
}
