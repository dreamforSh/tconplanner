package com.xinian.tconplanner.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
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
import com.xinian.tconplanner.data.PlannerData;
import com.xinian.tconplanner.util.MaterialSort;
import com.xinian.tconplanner.util.ModifierEvaluator;
import com.xinian.tconplanner.util.TranslationUtil;
import net.minecraft.util.Mth;
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

    /** Survives {@link #refresh()} - only widgets are cleared, so the validation cache stays warm */
    public final ModifierEvaluator modifierEvaluator = new ModifierEvaluator();
    public String modifierSearch = "";
    public int modifierTab = ModifierPanel.TAB_ALL;
    public boolean modifierSearchFocused = false;
    /** Outlives the panel rebuilds so the caret and selection are not reset on every keystroke */
    public EditBox modifierSearchBox;

    public int left, top, guiWidth, guiHeight;
    /** Shrinks on narrow screens so the panel never costs more room than the old fixed 115 */
    public int modPanelWidth;
    private Component titleText;

    //
    public String materialSearch = "";
    public boolean materialSearchFocused = false;

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
        ResourceLocation definition = stack.getDefinition().getId();

        Optional<TCTool> tool = TCTool.getTools().stream()
                .filter(t -> t.getToolDefinition().getId().equals(definition)).findAny();
        if (tool.isPresent()) {
            currentMode = PlannerMode.TOOLS;
            importInto(new Blueprint(tool.get()), stack);
            return;
        }
        //Armour is tinkered at the same station, so importing a piece used to search only the tool list
        //and silently open an empty planner
        Optional<TCArmor> armor = TCArmor.getArmors().stream()
                .filter(a -> a.getToolDefinition().getId().equals(definition)).findAny();
        if (armor.isPresent()) {
            currentMode = PlannerMode.ARMORS;
            importInto(new ArmorBlueprint(armor.get()), stack);
        }
    }

    private void importInto(BaseBlueprint<?> imported, ToolStack stack) {
        blueprint = imported;
        for (int i = 0; i < imported.materials.length; i++) {
            //getMaterial answers MaterialVariant.UNKNOWN past the end of the stack's material list;
            //null leaves the part empty instead of pinning a placeholder material onto it
            IMaterial material = stack.getMaterial(i).get();
            imported.materials[i] = material == IMaterial.UNKNOWN ? null : material;
        }
        selectedPart = -1;
    }

    /** Width the tool/bookmark column occupies to the left of the main window, including its gap */
    private static final int LEFT_COLUMN = 104;

    @Override
    public void init() {
        guiWidth = 175;
        guiHeight = 204;
        //Centre the whole cluster - left column, main window, modifier panel - instead of centring the
        //main window alone. Centring the window meant the side panels were positioned by unclamped
        //arithmetic and simply fell off whichever edge ran out of room first.
        modPanelWidth = Mth.clamp(width - LEFT_COLUMN - guiWidth - 8, ModifierPanel.NARROW_WIDTH, ModifierPanel.WIDTH);
        int cluster = LEFT_COLUMN + guiWidth + modPanelWidth;
        left = Math.max(LEFT_COLUMN, (width - cluster) / 2 + LEFT_COLUMN);
        //Below the ~394px the cluster needs, something has to be clipped. Clip the left tool column,
        //which is a scrollable list, rather than the modifier panel - its +/-, reorder and remove hit
        //boxes are all anchored to the right edge and would be entirely off-screen and unclickable.
        left = Math.min(left, Math.max(0, width - guiWidth - modPanelWidth));
        top = Math.max(0, height / 2 - guiHeight / 2);
        refresh();
    }

    public void refresh() {
        clearWidgets();
        //clearWidgets leaves Screen.focused pointing at a detached widget, which would keep routing
        //key events to the old panel's search box instead of the one actually on screen
        setFocused(null);
        int toolSpace = 20;
        int panelWidth = 100;
        int panelX = left - LEFT_COLUMN;

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


        //Shown even with no bookmarks: the Export/Import buttons live in this panel, and gating it on a
        //non-empty list meant a player who had never bookmarked anything had no way to import a code -
        //which is exactly the player someone would share a code with. It also keeps the layout stable.
        addRenderableWidget(new BookmarkSelectPanel(panelX, top + 22 + toolSpace * 3 + 23 + 4 + 4, panelWidth,
                BookmarkSelectPanel.HEIGHT, data, this));

        if (blueprint != null) {
            int topPanelSize = 115;
            ItemStack result = blueprint.createOutput();
            ToolStack resultStack = result.isEmpty() ? null : ToolStack.from(result);
            addRenderableWidget(new BlueprintTopPanel(left, top, guiWidth, topPanelSize, result, resultStack, data, this));
            if (selectedPart != -1) {
                addRenderableWidget(new MaterialSelectPanel(left, top + topPanelSize, guiWidth, guiHeight - topPanelSize, this));
            }
            if (resultStack != null) {
                ModifierPanel modifierPanel = new ModifierPanel(left + guiWidth, top, modPanelWidth, guiHeight,
                        result, resultStack, modifiers, this);
                addRenderableWidget(modifierPanel);
                if (modifierSearchFocused) setFocused(modifierPanel);
            }
        }
    }

    /**
     * Swaps only the modifier panel, so the search box keeps its text, caret and focus while typing.
     */
    public void refreshModifierPanel() {
        List<GuiEventListener> toRemove = new ArrayList<>();
        for (GuiEventListener g : new ArrayList<>(this.children)) {
            if (g instanceof ModifierPanel) toRemove.add(g);
        }
        for (GuiEventListener w : toRemove) {
            this.removeWidget(w);
        }
        if (blueprint == null) return;
        ItemStack result = blueprint.createOutput();
        if (result.isEmpty()) return;
        ModifierPanel panel = new ModifierPanel(left + guiWidth, top, modPanelWidth, guiHeight,
                result, ToolStack.from(result), modifiers, this);
        addRenderableWidget(panel);
        //removeWidget leaves Screen.focused dangling on the detached panel; key events need the new one
        if (modifierSearchFocused) setFocused(panel);
    }

    /** The material-panel counterpart of {@link #refreshModifierPanel()} */
    public void refreshMaterialList() {

        List<GuiEventListener> toRemove = new ArrayList<>();
        for (GuiEventListener g : new ArrayList<>(this.children)) {
            if (g instanceof MaterialSelectPanel) toRemove.add(g);
        }
        for (GuiEventListener w : toRemove) {
            this.removeWidget(w);
        }

        if (selectedPart != -1) {
            MaterialSelectPanel panel = new MaterialSelectPanel(left, top + 115, guiWidth, guiHeight - 115, this);
            addRenderableWidget(panel);
            //removeWidget leaves Screen.focused dangling on the detached panel; key events need the new one
            if (materialSearchFocused) setFocused(panel);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        bindTexture();
        graphics.blit(TEXTURE, left, top, 0, 0, guiWidth, guiHeight);
        graphics.drawCenteredString(font, titleText, left + guiWidth / 2, top + 7, 0xffffffff);

        super.render(graphics, mouseX, mouseY, partialTick);
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
        //Follow the blueprint. Loading an armour bookmark while the Tools tab was active left the mode
        //buttons contradicting what was on screen, and clicking the highlighted one wiped the blueprint.
        if (bp instanceof ArmorBlueprint) currentMode = PlannerMode.ARMORS;
        else if (bp instanceof Blueprint) currentMode = PlannerMode.TOOLS;
        this.materialPage = 0;
        sorter = null;
        //A different tool has a different modifier list, so neither the query nor the cache carries over
        modifierSearch = "";
        modifierTab = ModifierPanel.TAB_ALL;
        modifierSearchFocused = false;
        modifierEvaluator.invalidate();
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
        refresh();
    }

    /**
     * At most one search box owns the keyboard. Both flags are cleared before dispatch; whichever box
     * the click lands in re-sets its own, and the panels' {@code setFocused} overrides follow the flag.
     * Doing the clearing here rather than in each panel matters because the dispatcher stops at the
     * first panel that consumes the click, so a panel that is never reached cannot clear itself.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        materialSearchFocused = false;
        modifierSearchFocused = false;
        return super.mouseClicked(mouseX, mouseY, button);
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

    public void renderItemTooltip(GuiGraphics graphics, ItemStack stack, int x, int y) {
        graphics.renderTooltip(font, stack, x, y);
    }

    public void renderComponentTooltip(GuiGraphics graphics, List<Component> tooltip, int x, int y) {
        graphics.renderTooltip(font, tooltip.stream().map(Component::getVisualOrderText).toList(), x, y);
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

    /**
     * Rolls a new material for every part, and nothing else.
     * <p>
     * This used to build a brand new blueprint from the same tool and hand it to {@link #setBlueprint},
     * which meant a button labelled "Randomize Materials" also threw away every modifier the player had
     * applied and every creative slot they had granted, with no confirmation and no undo.
     */
    public void randomize() {
        if (blueprint == null) {
            return;
        }
        Random random = new Random();
        List<IToolPart> parts = ToolPartsHook.parts(blueprint.toolDefinition);
        List<IMaterial> allMaterials = MaterialRegistry.getMaterials().stream()
                .filter(mat -> !mat.isHidden())
                .toList();

        int count = Math.min(parts.size(), blueprint.materials.length);
        for (int i = 0; i < count; i++) {
            IToolPart part = parts.get(i);
            List<IMaterial> usable = allMaterials.stream()
                    .filter(mat -> part.canUseMaterial(mat.getIdentifier()))
                    .toList();
            if (!usable.isEmpty()) {
                blueprint.materials[i] = usable.get(random.nextInt(usable.size()));
            }
        }

        //Materials changed, so every cached validation and preview is stale
        modifierEvaluator.invalidate();
        refresh();
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

    public void blit(GuiGraphics graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(TEXTURE, x, y, u, v, width, height);
    }

    public Font getFont() {
        return font;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

        private record ModifierSignature(slimeknights.tconstruct.library.modifiers.Modifier modifier, slimeknights.tconstruct.library.tools.SlotType.SlotCount slots, int level) {}

    private static RecipeManager cachedRecipeManager;
    private static List<IDisplayModifierRecipe> cachedRecipes = Collections.emptyList();
    private static Map<ResourceLocation, IDisplayModifierRecipe> cachedRecipeIndex = Collections.emptyMap();

    /**
     * Every modifier recipe that can be displayed, deduplicated by (modifier, slots, level).
     * <p>
     * Memoised against the {@link RecipeManager} instance, which a world change or datapack reload
     * replaces - so invalidation is free. This used to rescan on every call, and it is called from
     * {@code ModifierStack.fromNBT}, i.e. once per saved bookmark on load and once per blueprint clone.
     */
    public static List<IDisplayModifierRecipe> getModifierRecipes() {
        if (Minecraft.getInstance().level == null) {
            return Collections.emptyList();
        }
        RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();
        if (recipeManager == cachedRecipeManager) {
            return cachedRecipes;
        }
        List<IDisplayModifierRecipe> jeiRecipes = RecipeHelper.getJEIRecipes(Minecraft.getInstance().level.registryAccess(), recipeManager, TinkerRecipeTypes.TINKER_STATION.get(), IDisplayModifierRecipe.class);

        List<IDisplayModifierRecipe> cleanedList = new ArrayList<>();
        Map<ResourceLocation, IDisplayModifierRecipe> index = new HashMap<>();
        Set<ModifierSignature> seen = new HashSet<>();

        for (IDisplayModifierRecipe recipe : jeiRecipes) {
            if (recipe instanceof ITinkerStationRecipe stationRecipe) {
                //Index every real recipe id, not just the ones that survive the display dedup below.
                //Saved blueprints reference recipe ids, so indexing only the survivors meant a bookmark
                //whose recipe lost the dedup could not be resolved and silently lost that modifier.
                index.putIfAbsent(stationRecipe.getId(), recipe);
                ModifierEntry result = recipe.getDisplayResult();
                ModifierSignature signature = new ModifierSignature(result.getModifier(), recipe.getSlots(), result.getLevel());
                if (seen.add(signature)) {
                    cleanedList.add(recipe);
                }
            }
        }
        cachedRecipeManager = recipeManager;
        cachedRecipes = cleanedList;
        cachedRecipeIndex = index;
        return cachedRecipes;
    }

    /** Recipe-id lookup over the same deduplicated list, for deserialising a saved modifier stack */
    public static Map<ResourceLocation, IDisplayModifierRecipe> getModifierRecipeIndex() {
        getModifierRecipes();
        return cachedRecipeIndex;
    }

    /**
     * Drops the memoised recipe list. These are static, so without this the client would hold a
     * departed world's RecipeManager and every recipe object in it until the next world was joined.
     */
    public static void clearRecipeCache() {
        cachedRecipeManager = null;
        cachedRecipes = Collections.emptyList();
        cachedRecipeIndex = Collections.emptyMap();
    }
}
