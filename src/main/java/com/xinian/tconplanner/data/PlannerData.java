package com.xinian.tconplanner.data;

import com.xinian.tconplanner.TConPlanner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PlannerData {

    private static final int DATA_VERSION = 1;
    private static final String VERSION_KEY = "version";
    private static final String LIST_KEY = "list";
    private static final String STARRED_KEY = "starred";

    public final List<BaseBlueprint<?>> saved = new ArrayList<>();
    public BaseBlueprint<?> starred;

    private final File bookmarkFile;
    private final File exportFolder;
    private boolean hasLoaded;

    public PlannerData(File folder){
        bookmarkFile = new File(folder, "bookmark.dat");
        exportFolder = new File(folder, "exports");
        try {
            //noinspection ResultOfMethodCallIgnored
            folder.mkdir();
            exportFolder.mkdir();
            if (!bookmarkFile.exists()) {
                if (bookmarkFile.createNewFile()) {
                    save();
                }
            }
        }catch (Exception ex){
            TConPlanner.LOGGER.error("Failed to initialize planner data", ex);
        }
    }

    public void refresh() throws IOException {
        save();
        load();
    }

    public boolean isBookmarked(BaseBlueprint<?> bp){
        return saved.stream().anyMatch(blueprint -> blueprint.equals(bp));
    }

    public void save() throws IOException {
        ListTag nbt = new ListTag();
        List<CompoundTag> added = new ArrayList<>();
        for (BaseBlueprint<?> bp : saved) {
            try {
                CompoundTag cnbt = bp.toNBT();
                if(bp.isComplete() && !added.contains(cnbt)){
                    nbt.add(cnbt);
                    added.add(cnbt);
                }
            } catch (Exception e) {
                TConPlanner.LOGGER.warn("Failed to save blueprint", e);
            }
        }
        CompoundTag data = new CompoundTag();
        data.putInt(VERSION_KEY, DATA_VERSION);
        data.put(LIST_KEY, nbt);
        if(starred != null && starred.isComplete()){
            try {
                CompoundTag cnbt = starred.toNBT();
                data.put(STARRED_KEY, cnbt);
            } catch (Exception e) {
                TConPlanner.LOGGER.warn("Failed to save starred blueprint", e);
            }
        }
        NbtIo.writeCompressed(data, bookmarkFile);
    }

    public void firstLoad() throws IOException {
        if(!hasLoaded) {
            load();
        }
    }

    public void load() throws IOException {
        hasLoaded = true;
        saved.clear();
        starred = null;
        if (!bookmarkFile.exists()) {
            return;
        }
        try {
            CompoundTag data = NbtIo.readCompressed(bookmarkFile);
            int version = data.getInt(VERSION_KEY);
            
            ListTag nbt = data.getList(LIST_KEY, 10);
            for (int i = 0; i < nbt.size(); i++) {
                CompoundTag tag = nbt.getCompound(i);
                try {
                    BaseBlueprint<?> bp = deserializeBlueprint(tag);
                    if (bp != null && bp.isComplete()) {
                        saved.add(bp);
                    }
                } catch (Exception e) {
                    TConPlanner.LOGGER.warn("Failed to load blueprint at index {}", i, e);
                }
            }
            
            if (data.contains(STARRED_KEY)) {
                CompoundTag starredTag = data.getCompound(STARRED_KEY);
                try {
                    starred = deserializeBlueprint(starredTag);
                } catch (Exception e) {
                    TConPlanner.LOGGER.warn("Failed to load starred blueprint", e);
                }
            }
        } catch (Exception e) {
            TConPlanner.LOGGER.error("Failed to load planner data", e);
        }
    }

    private BaseBlueprint<?> deserializeBlueprint(CompoundTag tag) {
        if (tag.contains("tool")) {
            return Blueprint.fromNBT(tag);
        } else if (tag.contains("armor")) {
            return ArmorBlueprint.fromNBT(tag);
        }
        return null;
    }

    public File getExportFolder() {
        return exportFolder;
    }

    public boolean addBlueprint(BaseBlueprint<?> blueprint) {
        if (blueprint == null || !blueprint.isComplete()) {
            return false;
        }
        if (!isBookmarked(blueprint)) {
            saved.add(blueprint);
            return true;
        }
        return false;
    }

    public boolean removeBlueprint(BaseBlueprint<?> blueprint) {
        return saved.removeIf(bp -> bp.equals(blueprint));
    }

    public void clearAll() {
        saved.clear();
        starred = null;
    }
}
