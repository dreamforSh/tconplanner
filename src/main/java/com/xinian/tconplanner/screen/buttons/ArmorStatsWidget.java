package com.xinian.tconplanner.screen.buttons;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xinian.tconplanner.screen.PlannerScreen;
import com.xinian.tconplanner.util.TranslationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.tools.item.armor.ModifiableArmorItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;


public class ArmorStatsWidget extends PlannerPanelWidget {

    private final ItemStack stack;

    public ArmorStatsWidget(int x, int y, ItemStack stack, PlannerScreen parent) {
        super(x, y, 18, 18, parent);
        this.stack = stack;
    }

    @Override
    public void renderButton(@NotNull PoseStack matrices, int mouseX, int mouseY, float partialTicks) {
        if (!this.stack.isEmpty()) {

            Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(stack, this.x, this.y);
        } else {
            GuiComponent.fill(matrices, this.x, this.y, this.x + 16, this.y + 16, 0x5a000050);
        }
    }

    @Override
    protected void renderBg(@NotNull PoseStack pPoseStack, @NotNull Minecraft pMinecraft, int pMouseX, int pMouseY) {

    }

    @Override
    public void updateNarration(net.minecraft.client.gui.narration.@NotNull NarrationElementOutput pNarrationElementOutput) {

    }

    @Override
    public void renderToolTip(@NotNull PoseStack matrices, int mouseX, int mouseY) {
        if (isHoveredOrFocused() && !this.stack.isEmpty()) {
            List<Component> tooltips = new ArrayList<>();


            if (this.stack.getItem() instanceof ModifiableArmorItem armorItem) {
                EquipmentSlot slot = armorItem.getSlot();


                double defense = getAttributeValue(stack, slot, Attributes.ARMOR).orElse(0.0);
                double toughness = getAttributeValue(stack, slot, Attributes.ARMOR_TOUGHNESS).orElse(0.0);
                double knockbackResistance = getAttributeValue(stack, slot, Attributes.KNOCKBACK_RESISTANCE).orElse(0.0);

                if (defense > 0) {
                    tooltips.add(TranslationUtil.createComponent("armor.stats.defense", String.format("%.1f", defense)));
                }
                if (toughness > 0) {
                    tooltips.add(TranslationUtil.createComponent("armor.stats.toughness", String.format("%.1f", toughness)));
                }
                if (knockbackResistance > 0) {
                    tooltips.add(TranslationUtil.createComponent("armor.stats.knockback_resistance", String.format("%.1f", knockbackResistance * 100)));
                }
            }


            tooltips.addAll(parent.getTooltipFromItem(this.stack));
            parent.renderComponentTooltip(matrices, tooltips, mouseX, mouseY);
        }
    }


    private Optional<Double> getAttributeValue(ItemStack stack, EquipmentSlot slot, net.minecraft.world.entity.ai.attributes.Attribute attribute) {
        Collection<AttributeModifier> modifiers = stack.getAttributeModifiers(slot).get(attribute);
        if (modifiers.isEmpty()) {
            return Optional.empty();
        }
        return modifiers.stream().findFirst().map(AttributeModifier::getAmount);
    }
}
