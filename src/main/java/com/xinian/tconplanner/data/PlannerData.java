package com.xinian.tconplanner.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlannerData {

    public final List<BaseBlueprint<?>> saved = new ArrayList<>();
    public BaseBlueprint<?> starred;

    private final File bookmarkFile;
    private boolean hasLoaded;

    public PlannerData(File folder){
        bookmarkFile = new File(folder, "bookmark.dat");
        try {
            //noinspection ResultOfMethodCallIgnored
            folder.mkdir();
            if (!bookmarkFile.exists()) {
                assert bookmarkFile.createNewFile();
                save();
            }
        }catch (Exception ex){
            ex.printStackTrace();
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
        List<CompoundTag> others = new ArrayList<>();
        for (BaseBlueprint<?> bp : saved) {
            CompoundTag cnbt = bp.toNBT();
            if(bp.isComplete() && !others.contains(cnbt)){
                nbt.add(cnbt);
                others.add(cnbt);
            }
        }
        CompoundTag data = new CompoundTag();
        data.put("list", nbt);
        if(starred != null){
            if(starred.isComplete()){
                CompoundTag cnbt = starred.toNBT();
                data.put("starred", cnbt);
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
        CompoundTag data = NbtIo.readCompressed(bookmarkFile);
        ListTag nbt = data.getList("list", 10);
        for (int i = 0; i < nbt.size(); i++) {
            CompoundTag tag = nbt.getCompound(i);
            BaseBlueprint<?> bp = null;
            if (tag.contains("tool")) {
                bp = Blueprint.fromNBT(tag);
            } else if (tag.contains("armor")) {
                bp = ArmorBlueprint.fromNBT(tag);
            }
            if (bp != null) {
                saved.add(bp);
            }
        }
        if (data.contains("starred")) {
            CompoundTag starredTag = data.getCompound("starred");
            if (starredTag.contains("tool")) {
                starred = Blueprint.fromNBT(starredTag);
            } else if (starredTag.contains("armor")) {
                starred = ArmorBlueprint.fromNBT(starredTag);
            }
        }
    }

}
