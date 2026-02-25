package com.xinian.tconplanner.api;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.layout.LayoutSlot;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayout;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayoutLoader;

import java.util.List;
import java.util.stream.Collectors;

public class TCTool implements IPlannable {
    private static List<TCTool> ALL_TOOLS = null;
    private final StationSlotLayout layout;
    private final ItemStack renderTool;

    private TCTool(StationSlotLayout layout){
        this.layout = layout;
        this.renderTool = layout.getIcon().getValue(ItemStack.class);
    }

    @Override
    public Component getName(){
        return layout.getDisplayName();
    }

    @Override
    public Component getDescription(){
        return layout.getDescription();
    }

    @Override
    public ItemStack getRenderStack(){
        return renderTool;
    }

    @Override
    public IModifiable getModifiable(){
        return (IModifiable) getRenderStack().getItem();
    }

    @Override
    public Item getItem(){
        return getRenderStack().getItem();
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return getModifiable().getToolDefinition();
    }

    public List<TCSlotPos> getSlotPos(){
        return layout.getInputSlots().stream().map((LayoutSlot slot) -> new TCSlotPos(slot.getX(), slot.getY())).collect(Collectors.toList());
    }

    public StationSlotLayout getLayout(){
        return layout;
    }

    public static List<TCTool> getTools(){
        if(ALL_TOOLS == null){
            ALL_TOOLS = StationSlotLayoutLoader.getInstance().getSortedSlots().stream()
                    .filter(layout -> {
                        ItemStack stack = layout.getIcon().getValue(ItemStack.class);
                        return stack != null && !stack.isEmpty() && stack.is(TinkerTags.Items.MODIFIABLE) && stack.getItem() instanceof IModifiable;
                    }).map(TCTool::new).collect(Collectors.toList());
        }
        return ALL_TOOLS;
    }

}