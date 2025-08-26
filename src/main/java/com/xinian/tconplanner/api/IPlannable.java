package com.xinian.tconplanner.api;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.item.IModifiable;

import java.util.List;

public interface IPlannable {
    Component getName();

    Component getDescription();

    IModifiable getModifiable();

    Item getItem();

    ToolDefinition getToolDefinition();

    ItemStack getRenderStack();

    List<TCSlotPos> getSlotPos();
}