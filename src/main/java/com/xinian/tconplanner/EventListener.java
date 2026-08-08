package com.xinian.tconplanner;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import javax.annotation.Nullable;
import com.xinian.tconplanner.api.TCArmor;
import com.xinian.tconplanner.api.TCTool;
import com.xinian.tconplanner.data.Blueprint;
import com.xinian.tconplanner.data.BaseBlueprint;
import com.xinian.tconplanner.data.PlannerData;
import com.xinian.tconplanner.screen.PlannerScreen;
import com.xinian.tconplanner.screen.buttons.BookmarkedButton;
import com.xinian.tconplanner.screen.ext.ExtIconButton;
import com.xinian.tconplanner.screen.ext.ExtItemStackButton;
import com.xinian.tconplanner.util.Icon;
import com.xinian.tconplanner.util.ToolPartLookup;
import com.xinian.tconplanner.util.TranslationUtil;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.tools.layout.LayoutSlot;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayout;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.part.IToolPart;
import slimeknights.tconstruct.library.tools.part.ToolPartItem;
import slimeknights.tconstruct.tables.block.ScorchedAnvilBlock;
import slimeknights.tconstruct.tables.block.TinkersAnvilBlock;
import slimeknights.tconstruct.tables.client.inventory.TinkerStationScreen;
import slimeknights.tconstruct.tables.client.inventory.widget.SlotButtonItem;
import slimeknights.tconstruct.tables.client.inventory.widget.TinkerStationButtonsWidget;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class EventListener {
    private static final Icon plannerIcon = new Icon(0, 0);
    private static final Icon importIcon = new Icon(8, 0);

    public static final Queue<Runnable> postRenderQueue = new LinkedBlockingQueue<>();

    private static StationSlotLayout layout = null;
    private static boolean starredLayout = false;
    private static SlotButtonItem starredButton = null;
    private static boolean forceNextUpdate = false;
    private static TinkerStationButtonsWidget buttonScreen;
    private static Field buttonsScreenField;
    private static boolean buttonsScreenUnavailable;

    /**
     * Reads Tinkers' protected {@code buttonsScreen}, which is only needed to hang the star badge on the
     * layout selector.
     * <p>
     * Resolved lazily and degrading to null: this used to live in a static initialiser that rethrew as a
     * RuntimeException, so a Tinkers update that renamed the field would have taken the whole mod - and
     * with it the game - down at class-load time, over a cosmetic badge. A Forge access transformer
     * cannot reach it, since ATs only apply to the Minecraft artifact, so reflection stays.
     * <p>
     * The sibling {@code currentLayout} lookup is gone entirely: {@code getCurrentLayout()} is public.
     */
    @Nullable
    private static TinkerStationButtonsWidget readButtonsScreen(TinkerStationScreen screen) {
        if (buttonsScreenUnavailable) return null;
        try {
            if (buttonsScreenField == null) {
                buttonsScreenField = TinkerStationScreen.class.getDeclaredField("buttonsScreen");
                buttonsScreenField.setAccessible(true);
            }
            return (TinkerStationButtonsWidget) buttonsScreenField.get(screen);
        } catch (ReflectiveOperationException | RuntimeException e) {
            buttonsScreenUnavailable = true;
            TConPlanner.LOGGER.error("Could not read TinkerStationScreen.buttonsScreen; the starred-layout badge will not be shown", e);
            return null;
        }
    }

    /**
     * Everything this class and the planner memoise is static, so leaving a world would otherwise keep
     * that world's RecipeManager, every modifier recipe in it, and the station screen's widget graph
     * reachable until the next world was joined.
     */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut e) {
        PlannerScreen.clearRecipeCache();
        //Both lists are built from datapack-driven registries, so they belong to the world being left
        TCTool.invalidate();
        TCArmor.invalidate();
        ToolPartLookup.invalidate();
        layout = null;
        starredLayout = false;
        starredButton = null;
        buttonScreen = null;
        forceNextUpdate = false;
        postRenderQueue.clear();
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post e) {
        postRenderQueue.clear();
        if (e.getScreen() instanceof TinkerStationScreen screen) {
            Minecraft mc = screen.getMinecraft();
            PlannerData data = TConPlanner.DATA;
            try {
                data.firstLoad();
            } catch (Exception ex) {
                TConPlanner.LOGGER.error("Failed to load planner data", ex);
            }
            buttonScreen = readButtonsScreen(screen);
            updateLayout(screen, true);
            forceNextUpdate = true;
            int x = screen.cornerX + Config.CONFIG.buttonX.get(), y = screen.cornerY + Config.CONFIG.buttonY.get();

            e.addListener(new ExtIconButton(x, y, plannerIcon, TranslationUtil.createComponent("plannerbutton"), action -> mc.setScreen(new PlannerScreen(screen)), screen));

            //getTileEntity() is @Nullable in Tinkers and genuinely returns null - the block entity is not
            //resolved yet on the first init after joining, and is gone if the table is broken while open.
            //requireNonNull here crashed the client; instanceof on null is already false, so a null block
            //entity simply falls back to the tinker-station offsets.
            BlockEntity stationEntity = screen.getTileEntity();
            Block stationBlock = stationEntity == null ? null : stationEntity.getBlockState().getBlock();
            boolean isAnvil = stationBlock instanceof ScorchedAnvilBlock || stationBlock instanceof TinkersAnvilBlock;
            int importX = screen.cornerX, importY = screen.cornerY;
            importX += (isAnvil ? Config.CONFIG.importButtonXAnvil : Config.CONFIG.importButtonXStation).get();
            importY += (isAnvil ? Config.CONFIG.importButtonYAnvil : Config.CONFIG.importButtonYStation).get();

            e.addListener(new ExtIconButton(importX, importY, importIcon, TranslationUtil.createComponent("importtool"), action -> {
                Slot slot = screen.getMenu().getSlot(0);
                if (!slot.getItem().isEmpty()) {
                    mc.setScreen(new PlannerScreen(screen, ToolStack.from(slot.getItem())));
                }
            }, screen).withEnabledFunc(() -> {
                if (layout == null || !layout.isMain()) return false;
                Slot slot = screen.getMenu().getSlot(0);
                return !slot.getItem().isEmpty() && ToolStack.isInitialized(slot.getItem());
            }));
            //Only tool blueprints have a parts-to-slots path (see movePartsToSlots and updateLayout, both
            //of which already gate on Blueprint). A starred ARMOUR blueprint used to add a button that
            //rendered, swallowed clicks and did nothing at all.
            //...and only a tool that is actually assembled at a station. An ancient tool has no station
            //layout and no parts, so there is no layout to switch to and nothing to move into the slots
            if (data.starred instanceof Blueprint starred && canFillSlots(starred)) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.literal("---------").withStyle(ChatFormatting.GRAY)); // <<-- 变更点 3
                tooltip.add(TranslationUtil.createComponent("star.move").withStyle(ChatFormatting.GOLD));
                tooltip.add(TranslationUtil.createComponent("star.ext_remove").withStyle(ChatFormatting.RED));

                e.addListener(new ExtItemStackButton(screen.cornerX + 83, screen.cornerY + 58, data.starred.createOutput(), tooltip, btn -> {
                    if (Screen.hasShiftDown()) {
                        btn.visible = btn.active = false;
                        starredLayout = false;
                        data.starred = null;
                        try {
                            data.save();
                        } catch (IOException ex) {
                            //Rethrowing from a button handler took the whole client down over a failed write
                            TConPlanner.LOGGER.error("Failed to save planner data after un-starring", ex);
                        }
                    }else{
                        if (data.starred instanceof Blueprint toolBlueprint && canFillSlots(toolBlueprint)) {
                            movePartsToSlots(screen, mc, toolBlueprint);
                        }
                    }
                }, screen));
            }
        }
    }

    @SubscribeEvent
    public static void onScreenDraw(ScreenEvent.Render.Post e) {
        if (e.getScreen() instanceof TinkerStationScreen screen) {
            GuiGraphics ms = e.getGuiGraphics();
            PoseStack poseStack = ms.pose();
            if (starredLayout) {
                Blueprint starred = (Blueprint)TConPlanner.DATA.starred;
                ItemStack carried = screen.getMenu().getCarried();
                //A layout is only required to have at least as many input slots as the tool has
                //materials, not exactly as many, so the tool's own count is what bounds the walk
                int slots = Math.min(layout.getInputSlots().size(), starred.materials.length);
                for (int i = 0; i < slots; i++) {
                    LayoutSlot slot = layout.getInputSlots().get(i);
                    int slotX = slot.getX() + screen.cornerX, slotY = slot.getY() + screen.cornerY;
                    IToolPart part = starred.toolParts[i];
                    boolean hovered = e.getMouseX() > slotX && e.getMouseY() > slotY && e.getMouseX() < slotX + 16 && e.getMouseY() < slotY + 16;
                    ItemStack stack = screen.getMenu().getSlot(i + 1).getItem();
                    MaterialId material = starred.materials[i].getIdentifier();
                    if (stack.isEmpty()) {
                        poseStack.pushPose();
                        poseStack.translate(0, 0, 101);
                        int color = carried.isEmpty() ? 0x5a000050 : isValidToolPart(carried, part, material) ? 0x5ae8b641 : 0x5aff0000;
                        ms.fillGradient(slotX, slotY, slotX + 16, slotY + 16, color, color);
                        poseStack.popPose();
                    } else if (!material.equals(part.getMaterial(stack).getId())) {
                        //Unlike the empty-slot branch there IS an item in this slot, drawn at z=250, so a
                        //plain fillGradient at z=101 was depth-rejected and the "wrong part" warning only
                        //showed as a faint fringe. guiOverlay has no depth test, so it lands on top.
                        poseStack.pushPose();
                        ms.fillGradient(RenderType.guiOverlay(), slotX, slotY, slotX + 16, slotY + 16, 0x7aff0000, 0x7aff0000, 0);
                        poseStack.popPose();
                    }
                }
            }
            if(starredButton != null){
                poseStack.pushPose();
                poseStack.translate(starredButton.x + 10, starredButton.y + 10, 105);
                poseStack.scale(0.5f, 0.5f, 1);
                BookmarkedButton.STAR_ICON.render(screen, ms, 0, 0);
                poseStack.popPose();
            }

            while(!postRenderQueue.isEmpty()) {
                postRenderQueue.poll().run();
            }
        }
    }

    @SubscribeEvent
    public static void onScreenDraw(ScreenEvent.Render.Pre e) {
        if(e.getScreen() instanceof TinkerStationScreen){
            postRenderQueue.clear();
            updateLayout((TinkerStationScreen) e.getScreen(), forceNextUpdate);
        }
    }

    /**
     * Whether a starred blueprint can drive the station slots at all.
     * <p>
     * Both the slot overlay and the move-parts button read the tool's station layout and its parts.
     * A tool with no layout is not built at a station - Tinkers' ancient tools come from loot and
     * trades - so there is no layout to select and no part to put in a slot.
     */
    private static boolean canFillSlots(Blueprint blueprint) {
        return blueprint.plannable.getLayout() != null && blueprint.hasParts();
    }

    private static void updateLayout(TinkerStationScreen screen, boolean force) {
        try {
            StationSlotLayout newLayout = screen.getCurrentLayout();
            if(!force && newLayout == layout)return;
            forceNextUpdate = false;
            layout = newLayout;
            PlannerData data = TConPlanner.DATA;
            boolean foundButton = false;
            if(data.starred instanceof Blueprint toolBlueprint && canFillSlots(toolBlueprint)){
                StationSlotLayout starredSlotLayout = toolBlueprint.plannable.getLayout();
                //Both sides can be null - getCurrentLayout has no layout selected, canFillSlots has just
                //established the starred one is non-null - and null == null would light up the overlay
                //over a station whose slots the loop below would then read off a null layout
                starredLayout = layout != null && layout == starredSlotLayout;
                //null when the reflective lookup failed; the badge is cosmetic, so carry on without it
                if (buttonScreen != null) {
                    for (SlotButtonItem button : buttonScreen.getButtons()) {
                        if(starredSlotLayout == button.getLayout()){
                            starredButton = button;
                            foundButton = true;
                        }
                    }
                }
            }
            if(!foundButton){
                starredLayout = false;
                starredButton = null;
            }
        } catch (Exception ex) {
            TConPlanner.LOGGER.error("Failed to update layout", ex);
        }
    }

    private static void movePartsToSlots(TinkerStationScreen screen, Minecraft mc, Blueprint starred){
        if(layout == null || starred.plannable.getLayout() != layout){
            screen.onToolSelection(starred.plannable.getLayout());
            updateLayout(screen, true);
        }
        Player player = mc.player;
        MultiPlayerGameMode pc = mc.gameMode;
        assert player != null && pc != null;
        int slots = Math.min(layout.getInputSlots().size(), starred.materials.length);
        for (int i = 0; i < slots; i++) {
            MaterialId material = starred.materials[i].getIdentifier();
            AbstractContainerMenu container = screen.getMenu();
            Slot tconSlot = container.getSlot(i + 1);
            if(tconSlot.getItem().isEmpty() && mc.player != null){
                for(int j = 0; j < container.slots.size(); j++){
                    Slot loopSlot = container.slots.get(j);
                    if(!(loopSlot.container instanceof Inventory))continue;
                    ItemStack stackInInv = loopSlot.getItem();
                    if(isValidToolPart(stackInInv, starred.toolParts[i], material)){
                        handleMouseClick(pc, player, container, j, 0, ClickType.PICKUP);
                        handleMouseClick(pc, player, container, i + 1, 1, ClickType.PICKUP);
                        break;
                    }
                }
            }
        }
    }

    private static void handleMouseClick(MultiPlayerGameMode pc, Player player, AbstractContainerMenu container, int slot, int mouseButton, ClickType clickType){
        pc.handleInventoryMouseClick(container.containerId, slot, mouseButton, clickType, player);
    }

    private static boolean isValidToolPart(ItemStack stack, IToolPart part, MaterialId material){
        return stack.getItem() instanceof ToolPartItem toolPart
                && part.asItem() == toolPart
                && material.equals(toolPart.getMaterial(stack).getId());
    }
}
