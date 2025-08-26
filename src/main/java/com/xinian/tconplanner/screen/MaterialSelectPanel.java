package com.xinian.tconplanner.screen;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import com.xinian.tconplanner.data.BaseBlueprint;
import com.xinian.tconplanner.screen.buttons.IconButton;
import com.xinian.tconplanner.screen.buttons.MatPageButton;
import com.xinian.tconplanner.screen.buttons.MaterialButton;
import com.xinian.tconplanner.util.MaterialSort;
import com.xinian.tconplanner.util.TranslationUtil;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.stats.IMaterialStats;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.tools.part.IToolPart;

import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MaterialSelectPanel extends PlannerPanel{
    private static final int materialPageSize = 3*9;

    //搜索框
    private final EditBox searchBox;

    public MaterialSelectPanel(int x, int y, int width, int height, PlannerScreen parent) {
        super(x, y, width, height, parent);

        //搜索框位置
        int searchY = 90;
        this.searchBox = new EditBox(Minecraft.getInstance().font,
                8, searchY, width - 16, 14,
                Component.literal("Search"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setValue(parent.materialSearch);
        // 调用parent.refreshMaterialList()，然后在PlannerScreen中安全移除旧面板
        this.searchBox.setResponder(text -> {
            parent.materialSearch = text.toLowerCase();
            parent.materialPage = 0; // 重置到第一页
            parent.refreshMaterialList();
        });
        addChild(this.searchBox);


        BaseBlueprint<?> blueprint = parent.blueprint;
        //Add material list for the tool part
        IToolPart part = (IToolPart) blueprint.toolParts[parent.selectedPart];
        //替换
        //List<IMaterial> usable = MaterialRegistry.getMaterials().stream().filter(part::canUseMaterial).collect(Collectors.toList());
        List<IMaterial> usable = MaterialRegistry.getMaterials().stream()
                .filter(part::canUseMaterial)
                .filter(mat -> {
                    if (parent.materialSearch == null || parent.materialSearch.isEmpty()) return true;
                    String search = parent.materialSearch.toLowerCase();
                    String id = mat.getIdentifier().toString().toLowerCase();
                    String translationKey = "material." + mat.getIdentifier().toString().replace(':', '.');
                    String name = Component.translatable(translationKey).getString().toLowerCase();
                    // 先普通字符串搜索，再拼音搜索
                    //我不知道1.19怎么判断的，没有ModList吗
                    //if (ModList.get().isLoaded("jecharacters")) {
                        return id.contains(search) || name.contains(search);
                                //开发环境报错
                               // ||me.towdium.jecharacters.utils.Match.matches(name, search);
                    //}
                    //else {
                    //    return id.contains(search) || name.contains(search);
                    //}

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

    @Override
    public void render(@NotNull PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        //先渲染panel的子控件，再把搜索框渲染到最上层，保证不会被覆盖
        super.render(poseStack, mouseX, mouseY, partialTicks);
        searchBox.render(poseStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        //如果点击了搜索框，就把焦点给它，并返回true
        if (searchBox.mouseClicked(mouseX, mouseY, button)) {
            searchBox.setFocused(true);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
