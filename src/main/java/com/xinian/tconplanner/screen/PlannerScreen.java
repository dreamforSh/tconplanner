package com.xinian.tconplanner.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import com.xinian.tconplanner.TConPlanner;
import com.xinian.tconplanner.api.TCTool;
import com.xinian.tconplanner.data.Blueprint;
import com.xinian.tconplanner.data.ModifierInfo;
import com.xinian.tconplanner.data.PlannerData;
import com.xinian.tconplanner.util.MaterialSort;
import com.xinian.tconplanner.util.ModifierStack;
import com.xinian.tconplanner.util.TranslationUtil;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import slimeknights.mantle.recipe.helper.RecipeHelper;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.material.PartsModule;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolPartsHook;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.part.IToolPart;
import slimeknights.tconstruct.library.tools.part.ToolPartItem;
import slimeknights.tconstruct.tables.client.inventory.TinkerStationScreen;

import java.io.IOException;
import java.util.*;
import com.xinian.tconplanner.api.TCArmor;
import com.xinian.tconplanner.data.BaseBlueprint;
import com.xinian.tconplanner.screen.buttons.TextButton;
import com.xinian.tconplanner.data.ArmorBlueprint;

public class PlannerScreen extends Screen {

    public static final ResourceLocation TEXTURE = new ResourceLocation(TConPlanner.MODID, "textures/gui/planner.png");

    public enum PlannerMode {
        TOOLS, ARMORS
    }
    private PlannerMode currentMode = PlannerMode.TOOLS;

    private final HashMap<String, Object> cache = new HashMap<>();
    public final Deque<Runnable> postRenderTasks = new ArrayDeque<>();
    private final TinkerStationScreen child;
    private final List<TCTool> tools = TCTool.getTools();
    private final List<TCArmor> armors = TCArmor.getArmors();
    private final List<IDisplayModifierRecipe> modifiers;
    private final PlannerData data;

    public BaseBlueprint<?> blueprint;

    public int selectedPart = 0;
    public int materialPage = 0;
    public MaterialSort<?> sorter;

    public ModifierInfo selectedModifier;

    public int selectedModifierStackIndex = -1;
    public ModifierStack modifierStack;

    public int left, top, guiWidth, guiHeight;
    private Component titleText;

    public PlannerScreen(TinkerStationScreen child) {
        super(TranslationUtil.createComponent("name"));
        this.child = child;
        data = TConPlanner.DATA;
        try {
            data.load();
        } catch (Exception ex) {

            TConPlanner.LOGGER.error("Failed to load planner data", ex);
        }

        modifiers = getModifierRecipes();
    }

    public PlannerScreen(TinkerStationScreen child, ToolStack stack) {
        this(child);
        this.currentMode = PlannerMode.TOOLS;
        Optional<TCTool> optionalTCTool = TCTool.getTools().stream().filter(tool -> tool.getModifiable().getToolDefinition().getId().equals(stack.getDefinition().getId())).findAny();
        if (optionalTCTool.isPresent()) {
            blueprint = new Blueprint(optionalTCTool.get());
            for (int i = 0; i < blueprint.materials.length; i++) {
                blueprint.materials[i] = stack.getMaterial(i).get();
            }
            selectedPart = -1;
        }
    }

    @Override
    protected void init() {
        guiWidth = 175;
        guiHeight = 204;
        left = width / 2 - guiWidth / 2;
        top = height / 2 - guiHeight / 2;
        refresh();
    }

    public void refresh() {
        clearWidgets();
        int toolSpace = 20;
        int panelWidth = 100;
        int panelX = left - panelWidth - 4;

        // Mode switch buttons
        addRenderableWidget(new TextButton(panelX, top, TranslationUtil.createComponent("mode.tools"), () -> {
            if (this.currentMode != PlannerMode.TOOLS) {
                this.currentMode = PlannerMode.TOOLS;
                this.setBlueprint(null);
            }
        }, this).withColor(currentMode == PlannerMode.TOOLS ? 0x50ff50 : 0xffffff).withWidth(48));
        addRenderableWidget(new TextButton(panelX + 52, top, TranslationUtil.createComponent("mode.armors"), () -> {
            if (this.currentMode != PlannerMode.ARMORS) {
                this.currentMode = PlannerMode.ARMORS;
                this.setBlueprint(null);
            }
        }, this).withColor(currentMode == PlannerMode.ARMORS ? 0x50ff50 : 0xffffff).withWidth(48));


        titleText = blueprint == null ? TranslationUtil.createComponent("notool") : blueprint.plannable.getName();

        if (currentMode == PlannerMode.TOOLS) {
            addRenderableWidget(new ToolSelectPanel(panelX, top + 22, panelWidth, toolSpace * 3 + 23 + 4, tools, this));
        } else {
            addRenderableWidget(new ArmorSelectPanel(panelX, top + 22, panelWidth, toolSpace * 3 + 23 + 4, armors, this));
        }


        if (!data.saved.isEmpty()) {
            addRenderableWidget(new BookmarkSelectPanel(panelX, top + 22 + toolSpace * 3 + 23 + 4 + 4, panelWidth, toolSpace * 5 + 23 + 4, data, this));
        }

        if (blueprint != null) {
            int topPanelSize = 115;
            ItemStack result = blueprint.createOutput();
            ToolStack resultStack = result.isEmpty() ? null : ToolStack.from(result);
            addRenderableWidget(new BlueprintTopPanel(left, top, guiWidth, topPanelSize, result, resultStack, data, this));
            if (selectedPart != -1) {
                addRenderableWidget(new MaterialSelectPanel(left, top + topPanelSize, guiWidth, guiHeight - topPanelSize, this));
            }
            if (resultStack != null) {
                addRenderableWidget(new ModifierPanel(left + guiWidth, top, 115, guiHeight, result, resultStack, modifiers, this));
            }
        }
    }

    @Override
    public void render(@NotNull PoseStack stack, int mouseX, int mouseY, float partialTick) {
        renderBackground(stack);
        bindTexture();
        this.blit(stack, left, top, 0, 0, guiWidth, guiHeight);
        drawCenteredString(stack, font, titleText, left + guiWidth / 2, top + 7, 0xffffffff);

        super.render(stack, mouseX, mouseY, partialTick);
        Runnable task;
        while ((task = postRenderTasks.poll()) != null) task.run();
    }

    public void setSelectedTool(int index) {
        setBlueprint(new Blueprint(tools.get(index)));
    }

    public void setSelectedArmor(int index) {
        setBlueprint(new ArmorBlueprint(armors.get(index)));
    }

    public void setBlueprint(BaseBlueprint<?> bp) {
        blueprint = bp;
        this.materialPage = 0;
        sorter = null;
        selectedModifier = null;
        modifierStack = null;
        selectedModifierStackIndex = -1;
        setSelectedPart(-1);
    }

    public void setSelectedPart(int index) {
        this.selectedPart = index;
        this.materialPage = 0;
        sorter = null;
        refresh();
    }

    public void setPart(IMaterial material) {
        blueprint.materials[selectedPart] = material;
        selectedModifier = null;
        selectedModifierStackIndex = -1;
        modifierStack = null;
        refresh();
    }

    @Override
    public boolean keyPressed(int key, int p_231046_2_, int p_231046_3_) {
        InputConstants.Key mouseKey = InputConstants.getKey(key, p_231046_2_);
        if (super.keyPressed(key, p_231046_2_, p_231046_3_)) {
            return true;
        } else if (this.minecraft != null && this.minecraft.options.keyInventory.isActiveAndMatches(mouseKey)) { // <<-- 变更点 5: 添加 null 检查
            this.onClose();
            return true;
        }
        if (key == GLFW.GLFW_KEY_B && blueprint != null && blueprint.isComplete()) {
            if (data.isBookmarked(blueprint)) unbookmarkCurrent();
            else bookmarkCurrent();
            return true;
        }
        return false;
    }

    public void renderItemTooltip(PoseStack mstack, ItemStack stack, int x, int y) {
        renderTooltip(mstack, stack, x, y);
    }


    @Override
    public void onClose() {
        // <<-- 变更点 5: 添加 null 检查
        if (this.minecraft != null) {
            this.minecraft.setScreen(child);
        }
    }

    public static void bindTexture() {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
    }

    public void bookmarkCurrent() {
        if (blueprint.isComplete()) {
            data.saved.add(blueprint);
            try {
                data.refresh();
            } catch (IOException e) {
                // <<-- 变更点 3: 使用 Logger
                TConPlanner.LOGGER.error("Failed to refresh planner data after bookmarking", e);
            }
        }
        refresh();
    }

    public void starCurrent() {
        if (blueprint.isComplete()) {
            data.starred = blueprint;
            try {
                data.refresh();
            } catch (IOException e) {

                TConPlanner.LOGGER.error("Failed to refresh planner data after starring", e);
            }
        }
        refresh();
    }

    public void unbookmarkCurrent() {
        if (blueprint.isComplete()) {
            data.saved.removeIf(blueprint1 -> blueprint1.equals(blueprint));
            if (blueprint.equals(data.starred)) data.starred = null;
            try {
                data.refresh();
            } catch (IOException e) {

                TConPlanner.LOGGER.error("Failed to refresh planner data after unbookmarking", e);
            }
        }
        refresh();
    }

    public void unstarCurrent() {
        if (blueprint.isComplete()) {
            data.starred = null;
            try {
                data.refresh();
            } catch (IOException e) {

                TConPlanner.LOGGER.error("Failed to refresh planner data after unstarring", e);
            }
        }
        refresh();
    }

    public void randomize() {
        if (blueprint instanceof Blueprint toolBlueprint) {
            setBlueprint(new Blueprint(toolBlueprint.plannable));
            Random random = new Random();
            List<IToolPart> parts = ToolPartsHook.parts(toolBlueprint.toolDefinition);
            List<IMaterial> allMaterials = new ArrayList<>(MaterialRegistry.getMaterials());
            for (int i = 0; i < parts.size(); i++) {
                IToolPart part = parts.get(i);

                List<IMaterial> usable = allMaterials.stream()
                        .filter(mat -> part.canUseMaterial(mat.getIdentifier()))
                        .toList();

                if (!usable.isEmpty()) {
                    this.blueprint.materials[i] = usable.get(random.nextInt(usable.size()));
                }
            }

            selectedModifier = null;
            refresh();
        }
    }

    public void giveItemstack(ItemStack stack) {

        if (this.minecraft == null || this.minecraft.player == null || this.minecraft.gameMode == null) {
            return;
        }
        Inventory inventory = this.minecraft.player.getInventory();
        for (int i = 0; i < inventory.items.size(); i++) {
            if (inventory.items.get(i).isEmpty()) {
                int slot = i;
                if (slot < 9) {
                    slot += 36;
                }
                this.minecraft.gameMode.handleCreativeModeItemAdd(stack, slot);
                return;
            }
        }
    }

    public void sort(MaterialSort<?> sort) {
        if (sorter == sort) sorter = null;
        else sorter = sort;
        refresh();
    }


    @SuppressWarnings("unchecked")
    public <T> T getCacheValue(String key, T defaultVal) {
        return (T) cache.getOrDefault(key, defaultVal);
    }

    public void setCacheValue(String key, Object value) {
        cache.put(key, value);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

        private record ModifierSignature(slimeknights.tconstruct.library.modifiers.Modifier modifier, slimeknights.tconstruct.library.tools.SlotType.SlotCount slots, int level) {}

    public static List<IDisplayModifierRecipe> getModifierRecipes() {

        if (Minecraft.getInstance().level == null) {
            return Collections.emptyList();
        }
        RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();
        List<IDisplayModifierRecipe> jeiRecipes = RecipeHelper.getJEIRecipes(recipeManager, TinkerRecipeTypes.TINKER_STATION.get(), IDisplayModifierRecipe.class);
        
        List<IDisplayModifierRecipe> cleanedList = new ArrayList<>();
        java.util.Set<ModifierSignature> seen = new java.util.HashSet<>();

        for (IDisplayModifierRecipe recipe : jeiRecipes) {
            if (recipe instanceof ITinkerStationRecipe) {
                ModifierEntry result = recipe.getDisplayResult();
                ModifierSignature signature = new ModifierSignature(result.getModifier(), recipe.getSlots(), result.getLevel());
                if (seen.add(signature)) {
                    cleanedList.add(recipe);
                }
            }
        }
        return cleanedList;
    }
}
