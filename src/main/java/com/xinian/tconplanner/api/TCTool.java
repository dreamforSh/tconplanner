package com.xinian.tconplanner.api;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import com.xinian.tconplanner.data.BaseBlueprint;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolMaterialHook;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.layout.LayoutSlot;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayout;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayoutLoader;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TCTool implements IPlannable {
    private static List<TCTool> ALL_TOOLS = null;

    /** Null for a tool that cannot be built at a station, and so has no layout to be built from */
    @Nullable
    private final StationSlotLayout layout;
    private final ItemStack renderTool;
    private final IModifiable modifiable;

    private TCTool(StationSlotLayout layout){
        this.layout = layout;
        this.renderTool = layout.getIcon().getValue(ItemStack.class);
        this.modifiable = (IModifiable) this.renderTool.getItem();
    }

    /**
     * A tool with no station layout.
     * <p>
     * Everything the layout would have supplied - display name, icon, slot positions - is derived from
     * the item instead, the same way {@link TCArmor} already does it.
     */
    private TCTool(IModifiable modifiable){
        this.layout = null;
        this.modifiable = modifiable;
        this.renderTool = ToolBuildHandler.buildToolForRendering(modifiable.asItem(), modifiable.getToolDefinition());
    }

    @Override
    public Component getName(){
        return layout == null ? renderTool.getHoverName() : layout.getDisplayName();
    }

    @Override
    public Component getDescription(){
        return layout == null ? renderTool.getHoverName() : layout.getDescription();
    }

    @Override
    public ItemStack getRenderStack(){
        return renderTool;
    }

    @Override
    public IModifiable getModifiable(){
        return modifiable;
    }

    @Override
    public Item getItem(){
        return modifiable.asItem();
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return modifiable.getToolDefinition();
    }

    @Override
    public List<TCSlotPos> getSlotPos(){
        if (layout != null) {
            return layout.getInputSlots().stream()
                    .map((LayoutSlot slot) -> new TCSlotPos(slot.getX(), slot.getY()))
                    .collect(Collectors.toList());
        }
        //No layout to read positions from, so lay the material slots out in a centred column, matching
        //what TCArmor does for the pieces that have no layout either
        List<TCSlotPos> pos = new ArrayList<>();
        int slots = ToolMaterialHook.stats(getToolDefinition()).size();
        int startY = 49 - (slots * 9);
        for (int i = 0; i < slots; i++) {
            pos.add(new TCSlotPos(32, startY + i * 18));
        }
        return pos;
    }

    /** Null when this tool cannot be built at a station; callers must handle that */
    @Nullable
    public StationSlotLayout getLayout(){
        return layout;
    }

    /**
     * Drops the cached list. The layouts come from a datapack-driven loader, so they change on world
     * change and on /reload; without this the planner kept showing the previous world's tool set.
     */
    public static void invalidate(){
        ALL_TOOLS = null;
    }

    public static List<TCTool> getTools(){
        if(ALL_TOOLS == null){
            List<TCTool> tools = StationSlotLayoutLoader.getInstance().getSortedSlots().stream()
                    .filter(layout -> {
                        ItemStack stack = layout.getIcon().getValue(ItemStack.class);
                        if (stack == null || stack.isEmpty() || !stack.is(TinkerTags.Items.MODIFIABLE)) return false;
                        if (!(stack.getItem() instanceof IModifiable modifiable)) return false;
                        //Screens walk the material slots and the layout's input slots in lockstep, so a
                        //layout with fewer slots than the definition has materials would index past its end
                        return BaseBlueprint.isPlannable(modifiable.getToolDefinition())
                                && layout.getInputSlots().size() >= ToolMaterialHook.stats(modifiable.getToolDefinition()).size();
                    }).map(TCTool::new).collect(Collectors.toCollection(ArrayList::new));
            addLayoutlessTools(tools);
            ALL_TOOLS = tools;
        }
        return ALL_TOOLS;
    }

    /**
     * Appends the tools that have no station layout, so they are listed at all.
     * <p>
     * Tinkers gives a layout only to what can be assembled at a station. Its ancient tools cannot be -
     * they come from loot and villager trades - so none of the five ships a layout, and a list built
     * from {@link StationSlotLayoutLoader} alone can never contain them however permissive its filter
     * is. The tag is the same one Tinkers uses for them, so an addon that adds an uncraftable tool gets
     * picked up by adding it there, no code required.
     */
    private static void addLayoutlessTools(List<TCTool> tools) {
        Set<Item> known = new HashSet<>();
        for (TCTool tool : tools) {
            known.add(tool.getItem());
        }
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(TinkerTags.Items.ANCIENT_TOOLS)) {
            Item item = holder.value();
            if (known.contains(item)) continue;
            if (item instanceof IModifiable modifiable && BaseBlueprint.isPlannable(modifiable.getToolDefinition())) {
                tools.add(new TCTool(modifiable));
            }
        }
    }
}
