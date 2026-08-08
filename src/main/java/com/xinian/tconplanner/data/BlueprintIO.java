package com.xinian.tconplanner.data;

import com.xinian.tconplanner.TConPlanner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.*;
import java.util.*;

public class BlueprintIO {

    private static final int CODE_LENGTH = 16;
    /** A real blueprint encodes to a few hundred bytes; anything far past that is not one */
    private static final int MAX_CODE_BYTES = 64 * 1024;
    
    /** Legacy machine-local short codes; read-only now that exports carry the full payload */
    private static final Map<String, String> codeCache = new HashMap<>();
    private static File codeStoreFile;

    public static void init(File folder) {
        codeStoreFile = new File(folder, "codes.dat");
        loadCodeStore();
    }
    
    private static void loadCodeStore() {
        if (codeStoreFile == null || !codeStoreFile.exists()) {
            return;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(codeStoreFile))) {
            @SuppressWarnings("unchecked")
            Map<String, String> loaded = (Map<String, String>) ois.readObject();
            codeCache.clear();
            codeCache.putAll(loaded);
        } catch (Exception e) {
            TConPlanner.LOGGER.error("Failed to load code store", e);
        }
    }
    
    /**
     * The portable code for a blueprint - the thing you can actually hand to someone else.
     * <p>
     * This used to mint a random 16-character handle and keep the real payload in a local
     * {@code codes.dat}, so the exported "code" resolved on the machine that made it and nowhere else.
     * Sharing one was the entire point of the feature. The short-code table is still read on import so
     * codes minted by older builds keep working.
     */
    public static String exportToCode(BaseBlueprint<?> blueprint) {
        return blueprintToCode(blueprint);
    }

    public static BaseBlueprint<?> importFromCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        //Short codes come from an upper-case alphabet, so only normalise when testing for one. Doing it
        //unconditionally mangled every full Base64 code, which is mixed case - meaning the direct-decode
        //path below could never once have succeeded.
        String shortCode = trimmed.toUpperCase(Locale.ROOT);
        if (shortCode.length() == CODE_LENGTH && codeCache.containsKey(shortCode)) {
            return codeToBlueprint(codeCache.get(shortCode));
        }
        return codeToBlueprint(trimmed);
    }

    public static String blueprintToCode(BaseBlueprint<?> blueprint) {
        if (blueprint == null || !blueprint.isComplete()) {
            return null;
        }
        try {
            CompoundTag data = new CompoundTag();
            data.putInt("version", 1);
            data.put("blueprint", blueprint.toNBT());
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            NbtIo.writeCompressed(data, baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            TConPlanner.LOGGER.error("Failed to encode blueprint", e);
            return null;
        }
    }

    public static BaseBlueprint<?> codeToBlueprint(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(code);
            //Pasted from the clipboard, so it is arbitrary input; a real blueprint is a few hundred bytes
            if (bytes.length > MAX_CODE_BYTES) {
                TConPlanner.LOGGER.warn("Ignoring blueprint code: {} bytes exceeds the {} byte limit", bytes.length, MAX_CODE_BYTES);
                return null;
            }
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            CompoundTag data = NbtIo.readCompressed(bais);

            if (data.contains("blueprint")) {
                CompoundTag bpTag = data.getCompound("blueprint");
                return deserializeBlueprint(bpTag);
            }
        } catch (Exception e) {
            TConPlanner.LOGGER.error("Failed to decode blueprint", e);
        }
        return null;
    }

    private static BaseBlueprint<?> deserializeBlueprint(CompoundTag tag) {
        if (tag.contains("tool")) {
            return Blueprint.fromNBT(tag);
        } else if (tag.contains("armor")) {
            return ArmorBlueprint.fromNBT(tag);
        }
        return null;
    }

}
