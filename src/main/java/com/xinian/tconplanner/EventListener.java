package com.xinian.tconplanner;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.xinian.tconplanner.data.Blueprint;
import com.xinian.tconplanner.data.BaseBlueprint;
import com.xinian.tconplanner.data.PlannerData;
import com.xinian.tconplanner.screen.PlannerScreen;
import com.xinian.tconplanner.screen.buttons.BookmarkedButton;
import com.xinian.tconplanner.screen.ext.ExtIconButton;
import com.xinian.tconplanner.screen.ext.ExtItemStackButton;
import com.xinian.tconplanner.util.Icon;
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
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class EventListener {
    private static final Icon plannerIcon = new Icon(0, 0);
    private static final Icon importIcon = new Icon(8, 0);

    public static final Queue<Runnable> postRenderQueue = new LinkedBlockingQueue<>();

    private static StationSlotLayout layout = null;
    private static boolean starredLayout = false;
    private static final Field currentLayoutField;
    private static SlotButtonItem starredButton = null;
    private static boolean forceNextUpdate = false;
    private static TinkerStationButtonsWidget buttonScreen;
    private static final Field buttonsScreenField;

    static {
        try {
            currentLayoutField = TinkerStationScreen.class.getDeclaredField("currentLayout");
            currentLayoutField.setAccessible(true);

            buttonsScreenField = TinkerStationScreen.class.getDeclaredField("buttonsScreen");
            buttonsScreenField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post e) {
        postRenderQueue.clear();
        if (e.getScreen() instanceof TinkerStationScreen screen) {
            Minecraft mc = screen.getMinecraft();
            PlannerData data = TConPlanner.DATA;
            try {
                data.firstLoad();
                buttonScreen = (TinkerStationButtonsWidget)buttonsScreenField.get(screen);
            } catch (Exception ex) {
                TConPlanner.LOGGER.error("Failed to load planner data or access Tinkers' screen fields", ex);
            }
            updateLayout(screen, true);
            forceNextUpdate = true;
            int x = screen.cornerX + Config.CONFIG.buttonX.get(), y = screen.cornerY + Config.CONFIG.buttonY.get();

            e.addListener(new ExtIconButton(x, y, plannerIcon, TranslationUtil.createComponent("plannerbutton"), action -> mc.setScreen(new PlannerScreen(screen)), screen));

            Block stationBlock = Objects.requireNonNull(screen.getTileEntity()).getBlockState().getBlock();
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
            if (data.starred != null) {
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
                            throw new RuntimeException(ex);
                        }
                    }else{
                        if (data.starred instanceof Blueprint toolBlueprint) {
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
            PoseStack ms = e.getPoseStack();
            if (starredLayout) {
                Blueprint starred = (Blueprint)TConPlanner.DATA.starred;
                ItemStack carried = screen.getMenu().getCarried();
                for (int i = 0; i < layout.getInputSlots().size(); i++) {
                    LayoutSlot slot = layout.getInputSlots().get(i);
                    int slotX = slot.getX() + screen.cornerX, slotY = slot.getY() + screen.cornerY;
                    IToolPart part = starred.toolParts[i];
                    boolean hovered = e.getMouseX() > slotX && e.getMouseY() > slotY && e.getMouseX() < slotX + 16 && e.getMouseY() < slotY + 16; // 正确
                    ItemStack stack = screen.getMenu().getSlot(i + 1).getItem();
                    MaterialId material = starred.materials[i].getIdentifier();
                    if (stack.isEmpty()) {
                        ms.pushPose();
                        ms.translate(0, 0, 101);
                        int color = carried.isEmpty() ? 0x5a000050 : isValidToolPart(carried, part, material) ? 0x5ae8b641 : 0x5aff0000;
                        Screen.fill(ms, slotX, slotY, slotX + 16, slotY + 16, color);
                        ms.popPose();
                        if (hovered) {
                            screen.renderComponentTooltip(ms, Lists.newArrayList(TranslationUtil.createComponent("star.slot.missing").withStyle(ChatFormatting.DARK_RED), part.withMaterialForDisplay(material).getDisplayName()), e.getMouseX(), e.getMouseY());
                        }
                    } else if (!material.equals(part.getMaterial(stack).getId())) {
                        ms.pushPose();
                        ms.translate(0, 0, 101);
                        Screen.fill(ms, slotX, slotY, slotX + 16, slotY + 16, 0x7aff0000);
                        ms.popPose();
                        if (hovered) {
                            screen.renderComponentTooltip(ms, Lists.newArrayList(TranslationUtil.createComponent("star.slot.incorrect").withStyle(ChatFormatting.DARK_RED), part.withMaterialForDisplay(material).getDisplayName()), e.getMouseX(), e.getMouseY() - 30);
                        }
                    }
                }
            }
            if(starredButton != null){
                ms.pushPose();
                ms.translate(starredButton.x + 10, starredButton.y + 10, 105);
                ms.scale(0.5f, 0.5f, 1);
                BookmarkedButton.STAR_ICON.render(screen, ms, 0, 0);
                ms.popPose();
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

    private static void updateLayout(TinkerStationScreen screen, boolean force) {
        try {
            StationSlotLayout newLayout = (StationSlotLayout) currentLayoutField.get(screen);
            if(!force && newLayout == layout)return;
            forceNextUpdate = false;
            layout = newLayout;
            PlannerData data = TConPlanner.DATA;
            boolean foundButton = false;
            if(data.starred instanceof Blueprint toolBlueprint){
                StationSlotLayout starredSlotLayout = toolBlueprint.plannable.getLayout();
                starredLayout = layout == starredSlotLayout;
                for (SlotButtonItem button : buttonScreen.getButtons()) {
                    if(starredSlotLayout == button.getLayout()){
                        starredButton = button;
                        foundButton = true;
                    }
                }
            }
            if(!foundButton){
                starredLayout = false;
                starredButton = null;
            }
        } catch (Exception ex) {
            // 建议使用 Logger
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
        for (int i = 0; i < layout.getInputSlots().size(); i++) {
            MaterialId material = starred.materials[i].getIdentifier();
            AbstractContainerMenu container = screen.getMenu();
            Slot tconSlot = container.getSlot(i + 1);
            if(tconSlot.getItem().isEmpty() && mc.player != null){
                for(int j = 0; j < container.slots.size(); j++){
                    Slot loopSlot = container.slots.get(j);
                    if(!(loopSlot.container instanceof Inventory))continue;
                    ItemStack stackInInv = loopSlot.getItem();
                    if(isValidToolPart(stackInInv, (IToolPart) starred.materials[i], material)){
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
