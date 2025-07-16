package com.xinian.tconplanner.screen;

import com.google.common.collect.Lists;
import net.minecraft.sounds.SoundEvents;
import com.xinian.tconplanner.data.Blueprint;
import com.xinian.tconplanner.screen.buttons.IconButton;
import com.xinian.tconplanner.screen.buttons.MatPageButton;
import com.xinian.tconplanner.screen.buttons.MaterialButton;
import com.xinian.tconplanner.util.MaterialSort;
import com.xinian.tconplanner.util.TranslationUtil;
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

    public MaterialSelectPanel(int x, int y, int width, int height, PlannerScreen parent) {
        super(x, y, width, height, parent);
        Blueprint blueprint = parent.blueprint;
        //Add material list for the tool part
        IToolPart part = (IToolPart) blueprint.toolParts[parent.selectedPart];
        List<IMaterial> usable = MaterialRegistry.getMaterials().stream().filter(part::canUseMaterial).collect(Collectors.toList());
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
}
