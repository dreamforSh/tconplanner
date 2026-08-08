package com.xinian.tconplanner.screen;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import com.xinian.tconplanner.data.BaseBlueprint;
import com.xinian.tconplanner.screen.buttons.IconButton;
import com.xinian.tconplanner.screen.buttons.MatPageButton;
import com.xinian.tconplanner.screen.buttons.MaterialButton;
import com.xinian.tconplanner.util.JECharactersIntegration;
import com.xinian.tconplanner.util.MaterialSort;
import com.xinian.tconplanner.util.TranslationUtil;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.stats.IMaterialStats;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.tools.part.IToolPart;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class MaterialSelectPanel extends PlannerPanel{
    private static final int materialPageSize = 3*9;

    //搜索框
    private final EditBox searchBox;

    public MaterialSelectPanel(int x, int y, int width, int height, PlannerScreen parent) {
        super(x, y, width, height, parent);

        //One instance re-mounted per rebuild, so text, caret, selection and focus survive the rebuild
        //every keystroke triggers - same reasoning as ModifierPanel.obtainSearchBox
        this.searchBox = obtainSearchBox(parent, width - 16);
        //addChild rebases coordinates, so they must be re-set each build rather than accumulated
        this.searchBox.x = 8;
        this.searchBox.y = 90;
        this.searchBox.width = width - 16;
        addChild(this.searchBox);


        BaseBlueprint<?> blueprint = parent.blueprint;
        //Add material list for the tool part
        IToolPart part = (IToolPart) blueprint.toolParts[parent.selectedPart];
        List<IMaterial> usable = MaterialRegistry.getMaterials().stream()
                .filter(part::canUseMaterial)
                .filter(mat -> {
                    if (parent.materialSearch == null || parent.materialSearch.isEmpty()) {
                        return true;
                    }
                    String search = parent.materialSearch.toLowerCase(java.util.Locale.ROOT);
                    String id = mat.getIdentifier().toString().toLowerCase(java.util.Locale.ROOT);
                    String translationKey = "material." + mat.getIdentifier().toString().replace(':', '.');
                    String name = Component.translatable(translationKey).getString().toLowerCase(java.util.Locale.ROOT);
                    if (id.contains(search) || name.contains(search)) {
                        return true;
                    }
                    if (JECharactersIntegration.isLoaded()) {
                        return JECharactersIntegration.matches(name, search);
                    }
                    return false;
                })
                .collect(Collectors.toList());

        MaterialStatsId statsId = part.getStatType();
        if(parent.sorter != null)usable.sort((o1, o2) -> parent.sorter.compare(o1, o2, statsId) * -1);
        int loopMin = parent.materialPage*materialPageSize;
        int loopMax = Math.min(usable.size(), (parent.materialPage+1)*materialPageSize);
        for (int i = loopMin; i < loopMax; i++) {
            int posIndex = i - loopMin;
            IMaterial mat = usable.get(i);
            MaterialButton data = new MaterialButton(mat, part.withMaterialForDisplay(mat.getIdentifier()), (posIndex % 9) * 18 + 8, 2 + (posIndex / 9) * 18, parent);
            if(blueprint.materials[parent.selectedPart] == mat)data.selected = true;
            addChild(data);
        }
        //Add material pagination buttons
        MatPageButton leftPage = new MatPageButton(6, height - 30, -1, parent);
        MatPageButton rightPage = new MatPageButton(width - 6 - 37, height - 30, 1, parent);
        leftPage.active = parent.materialPage > 0;
        rightPage.active = loopMax < usable.size();
        addChild(leftPage);
        addChild(rightPage);

        Class<? extends IMaterialStats> statClass = null;
        IMaterialStats defaultStats = MaterialRegistry.getInstance().getDefaultStats(part.getStatType());
        if (defaultStats != null) {
            statClass = defaultStats.getClass();
        }
        if(statClass != null){
            List<MaterialSort<?>> sorts = MaterialSort.MAP.getOrDefault(statClass, Lists.newArrayList());
            int startX = width/2 - 6*sorts.size();
            for (int i = 0; i < sorts.size(); i++) {
                MaterialSort<?> sort = sorts.get(i);
                addChild(new IconButton(startX + i*12, height - 30 + 3, sort.icon(), TranslationUtil.createComponent("sort", sort.text()), parent, e -> parent.sort(sort))
                        .withColor(sort == parent.sorter ? Color.WHITE : new Color(0.4f, 0.4f, 0.4f)).withSound(SoundEvents.PAINTING_PLACE));
            }
        }
    }

    private static EditBox obtainSearchBox(PlannerScreen parent, int width) {
        EditBox box = parent.materialSearchBox;
        if (box == null) {
            box = new EditBox(Minecraft.getInstance().font, 0, 0, width, 14,
                    TranslationUtil.createComponent("search"));
            box.setMaxLength(50);
            //The constructor's Component is narration only; this is the visible empty-state hint
            box.setHint(TranslationUtil.createComponent("search"));
            box.setValue(parent.materialSearch);
            box.setResponder(text -> {
                if (text.equals(parent.materialSearch)) return;
                //Stored as typed; case folding happens at comparison time so the box shows what you wrote
                parent.materialSearch = text;
                parent.materialPage = 0;
                parent.refreshMaterialList();
            });
            parent.materialSearchBox = box;
        } else if (!box.getValue().equals(parent.materialSearch)) {
            box.setValue(parent.materialSearch);
        }
        return box;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        //如果点击了搜索框，就把焦点给它，并返回true
        //Only ever SETS the flag; PlannerScreen.mouseClicked clears both before dispatch
        if (searchBox.mouseClicked(mouseX, mouseY, button)) {
            parent.materialSearchFocused = true;
            searchBox.setFocused(true);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Flag-driven on both edges, for the reason spelled out in {@code ModifierPanel.setFocused} */
    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        searchBox.setFocused(focused && parent.materialSearchFocused);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox.isFocused()) {
            //把键盘事件交给EditBox
            if (searchBox.keyPressed(keyCode, scanCode, modifiers)) return true;
            return searchBox.canConsumeInput();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchBox.isFocused()) {
            return searchBox.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }
}
