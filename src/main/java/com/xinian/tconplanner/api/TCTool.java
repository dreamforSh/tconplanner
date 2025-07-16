package com.xinian.tconplanner.api;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayout;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayoutLoader;

import java.util.List;
import java.util.stream.Collectors;

public class TCTool {
    private static List<TCTool> ALL_TOOLS = null;
    private final StationSlotLayout layout;
    private final ItemStack renderTool;

    private TCTool(StationSlotLayout layout){
        this.layout = layout;
        renderTool = layout.getIcon().getValue(ItemStack.class);
    }

    public Component getName(){
        return layout.getDisplayName();
    }

    public Component getDescription(){
        return layout.getDescription();
    }

    public ItemStack getRenderTool(){
        return renderTool;
    }

    public IModifiable getModifiable(){
        return (IModifiable) getRenderTool().getItem();
    }

    public Item getItem(){
        return getRenderTool().getItem();
    }

    public List<TCSlotPos> getSlotPos(){
        return layout.getInputSlots().stream().map(TCSlotPos::new).collect(Collectors.toList());
    }

    public StationSlotLayout getLayout(){
        return layout;
    }

    public static List<TCTool> getTools(){
        if(ALL_TOOLS == null){
            ALL_TOOLS = StationSlotLayoutLoader.getInstance().getSortedSlots().stream()
                    .filter(layout -> {
                        ItemStack stack = layout.getIcon().getValue(ItemStack.class);
                        return stack != null && stack.is(TinkerTags.Items.MODIFIABLE) && stack.getItem() instanceof IModifiable;
                    }).map(TCTool::new).collect(Collectors.toList());
        }
        return ALL_TOOLS;
    }

}
