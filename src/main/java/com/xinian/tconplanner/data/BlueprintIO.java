package com.xinian.tconplanner.data;

import com.xinian.tconplanner.TConPlanner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;

import java.io.*;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;

public class BlueprintIO {

    private static final String EXPORT_EXTENSION = ".tconbp";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 16;
    
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
    
    private static void saveCodeStore() {
        if (codeStoreFile == null) {
            return;
        }
        try {
            codeStoreFile.getParentFile().mkdirs();
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(codeStoreFile))) {
                oos.writeObject(new HashMap<>(codeCache));
            }
        } catch (Exception e) {
            TConPlanner.LOGGER.error("Failed to save code store", e);
        }
    }

    public static String generateCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return code.toString();
    }

    public static String exportToShortCode(BaseBlueprint<?> blueprint) {
        if (blueprint == null || !blueprint.isComplete()) {
            return null;
        }
        String fullCode = blueprintToCode(blueprint);
        if (fullCode == null) {
            return null;
        }
        String shortCode = generateCode();
        codeCache.put(shortCode, fullCode);
        saveCodeStore();
        return shortCode;
    }

    public static BaseBlueprint<?> importFromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        code = code.trim().toUpperCase();
        
        // 如果是16位短码，从缓存查找
        if (code.length() == CODE_LENGTH && codeCache.containsKey(code)) {
            String fullCode = codeCache.get(code);
            return codeToBlueprint(fullCode);
        }
        
        // 否则尝试直接解码
        return codeToBlueprint(code);
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

    public static List<BaseBlueprint<?>> codeToBlueprints(String code) {
        List<BaseBlueprint<?>> result = new ArrayList<>();
        if (code == null || code.isEmpty()) {
            return result;
        }
        
        // 先尝试作为短码查找
        code = code.trim().toUpperCase();
        if (code.length() == CODE_LENGTH && codeCache.containsKey(code)) {
            BaseBlueprint<?> bp = codeToBlueprint(codeCache.get(code));
            if (bp != null) {
                result.add(bp);
            }
            return result;
        }
        
        // 尝试作为完整代码解码
        try {
            byte[] bytes = Base64.getDecoder().decode(code);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            CompoundTag data = NbtIo.readCompressed(bais);
            
            if (data.contains("blueprint")) {
                CompoundTag bpTag = data.getCompound("blueprint");
                BaseBlueprint<?> bp = deserializeBlueprint(bpTag);
                if (bp != null) {
                    result.add(bp);
                }
            } else if (data.contains("blueprints")) {
                ListTag list = data.getList("blueprints", 10);
                for (int i = 0; i < list.size(); i++) {
                    try {
                        CompoundTag bpTag = list.getCompound(i);
                        BaseBlueprint<?> bp = deserializeBlueprint(bpTag);
                        if (bp != null) {
                            result.add(bp);
                        }
                    } catch (Exception e) {
                        TConPlanner.LOGGER.warn("Failed to decode blueprint at index {}", i, e);
                    }
                }
            }
        } catch (Exception e) {
            TConPlanner.LOGGER.error("Failed to decode blueprints", e);
        }
        return result;
    }

    private static BaseBlueprint<?> deserializeBlueprint(CompoundTag tag) {
        if (tag.contains("tool")) {
            return Blueprint.fromNBT(tag);
        } else if (tag.contains("armor")) {
            return ArmorBlueprint.fromNBT(tag);
        }
        return null;
    }

    public static String getExportExtension() {
        return EXPORT_EXTENSION;
    }
}
