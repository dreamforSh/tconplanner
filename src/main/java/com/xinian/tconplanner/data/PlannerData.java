package com.xinian.tconplanner.data;

import com.xinian.tconplanner.TConPlanner;
import com.xinian.tconplanner.util.TranslationUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
    /** Set when a read failed outright; blocks save() so a bad read cannot erase the file */
    private boolean loadFailed;
    private boolean warnedReadOnly;
    /**
     * Raw NBT for entries that did not resolve this session - a bookmark for a tool from a mod that is
     * currently absent, or one whose material has gone. They are written back verbatim, so temporarily
     * removing a mod no longer deletes the bookmarks that referenced it.
     */
    private final List<CompoundTag> unresolved = new ArrayList<>();
    private CompoundTag unresolvedStar;

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
        if (loadFailed) {
            //NbtIo.writeCompressed truncates. Without this guard, one unreadable file plus any later
            //bookmark action would have written an empty list over the user's whole collection.
            //A transient failure clears itself, because load() runs again on every planner open.
            TConPlanner.LOGGER.warn("Not writing {}: the last read of it failed, so the in-memory list may be incomplete", bookmarkFile);
            warnReadOnlyOnce();
            return;
        }
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
        //Carry through anything this session could not interpret, so it survives for a session that can
        for (CompoundTag stale : unresolved) {
            if (!added.contains(stale)) {
                nbt.add(stale);
                added.add(stale);
            }
        }
        CompoundTag data = new CompoundTag();
        data.putInt(VERSION_KEY, DATA_VERSION);
        data.put(LIST_KEY, nbt);
        if(starred != null && starred.isComplete()){
            try {
                data.put(STARRED_KEY, starred.toNBT());
            } catch (Exception e) {
                TConPlanner.LOGGER.warn("Failed to save starred blueprint", e);
            }
        } else if (unresolvedStar != null) {
            data.put(STARRED_KEY, unresolvedStar);
        }
        writeAtomically(data);
    }

    /**
     * Refusing to save is the safe answer to an unreadable file, but doing it silently just looks like
     * bookmarking is broken. Say it once, with the path, so the player can move the file aside.
     */
    private void warnReadOnlyOnce() {
        if (warnedReadOnly) return;
        warnedReadOnly = true;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(
                    TranslationUtil.createComponent("bookmarks.readonly", bookmarkFile.getName()).withStyle(ChatFormatting.RED),
                    false);
        }
    }

    /**
     * Writes through a temp file and renames, so an exception or a crash part-way through leaves the
     * previous bookmark.dat intact instead of a truncated one.
     */
    private void writeAtomically(CompoundTag data) throws IOException {
        File temp = new File(bookmarkFile.getParentFile(), bookmarkFile.getName() + ".tmp");
        NbtIo.writeCompressed(data, temp);
        try {
            Files.move(temp.toPath(), bookmarkFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp.toPath(), bookmarkFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void firstLoad() throws IOException {
        if(!hasLoaded) {
            load();
        }
    }

    /**
     * Parses into locals and only commits on success, so a failed read leaves whatever is already in
     * memory alone rather than replacing it with nothing.
     */
    public void load() throws IOException {
        hasLoaded = true;
        if (!bookmarkFile.exists()) {
            saved.clear();
            starred = null;
            unresolved.clear();
            unresolvedStar = null;
            loadFailed = false;
            return;
        }

        List<BaseBlueprint<?>> loaded = new ArrayList<>();
        List<CompoundTag> stale = new ArrayList<>();
        BaseBlueprint<?> loadedStar = null;
        CompoundTag staleStar = null;
        try {
            CompoundTag data = NbtIo.readCompressed(bookmarkFile);

            ListTag nbt = data.getList(LIST_KEY, 10);
            for (int i = 0; i < nbt.size(); i++) {
                CompoundTag tag = nbt.getCompound(i);
                BaseBlueprint<?> bp = null;
                try {
                    bp = deserializeBlueprint(tag);
                } catch (Exception e) {
                    TConPlanner.LOGGER.warn("Failed to load blueprint at index {}", i, e);
                }
                if (bp != null && bp.isComplete()) {
                    loaded.add(bp);
                } else {
                    //Unreadable right now, but not necessarily junk - hold the raw tag for save()
                    stale.add(tag);
                }
            }

            if (data.contains(STARRED_KEY)) {
                CompoundTag starredTag = data.getCompound(STARRED_KEY);
                try {
                    loadedStar = deserializeBlueprint(starredTag);
                } catch (Exception e) {
                    TConPlanner.LOGGER.warn("Failed to load starred blueprint", e);
                }
                if (loadedStar == null || !loadedStar.isComplete()) {
                    loadedStar = null;
                    staleStar = starredTag;
                }
            }
        } catch (Exception e) {
            TConPlanner.LOGGER.error("Failed to read bookmark.dat; keeping the current bookmarks and blocking writes", e);
            loadFailed = true;
            return;
        }

        saved.clear();
        saved.addAll(loaded);
        starred = loadedStar;
        unresolved.clear();
        unresolved.addAll(stale);
        unresolvedStar = staleStar;
        loadFailed = false;
        if (!stale.isEmpty()) {
            TConPlanner.LOGGER.info("{} bookmark(s) could not be resolved in this world and will be preserved as-is", stale.size());
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
        unresolved.clear();
        unresolvedStar = null;
    }
}
