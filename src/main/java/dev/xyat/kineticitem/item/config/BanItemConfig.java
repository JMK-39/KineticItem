package dev.xyat.kineticitem.item.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class BanItemConfig {
    private static final Logger LOGGER = LogManager.getLogger("kineticitem/Config");
    public static final String VOID_ID = "kineticitem:void_placeholder";
    public static final Path PATH = Paths.get("config", "kineticcore", "banitem.json");
    public static final Path BACKUP_PATH = Paths.get("config", "kineticcore", "banitem.old.json");
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static volatile Data data = new Data();
    public static volatile Map<ItemRule, String> ruleReplacementMap = new HashMap<>();

    private static final ConcurrentHashMap<Item, String> ITEM_ID_CACHE = new ConcurrentHashMap<>();
    private static final AtomicInteger INVALID_RULE_LOG_COUNT = new AtomicInteger();

    public static volatile Map<String, String> pureIdReplacements = new HashMap<>();
    public static volatile Map<String, List<Map.Entry<ItemRule, String>>> nbtReplacements = new HashMap<>();
    public static volatile List<Map.Entry<ItemRule, String>> macroReplacements = new ArrayList<>();
    public static volatile Map<String, Set<String>> targetMergedTags = new HashMap<>();

    public static volatile Set<String> pureIdBanned = new HashSet<>();
    public static volatile Map<String, List<ItemRule>> nbtBanned = new HashMap<>();
    public static volatile List<ItemRule> macroBanned = new ArrayList<>();

    public static String getItemIdFromCache(Item item) {
        return ITEM_ID_CACHE.computeIfAbsent(item, k -> {
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(k);
            return rl != null ? rl.toString() : "";
        });
    }

    public static class Data {
        public List<String> bannedItems = new ArrayList<>();
        public Map<String, List<String>> mergedItems = new HashMap<>();
    }

    public static class ItemRule {
        public final String originalString;
        public final String baseId;
        public final CompoundTag nbt;
        public final boolean hasNbt;

        public ItemRule(String str) {
            String clean = normalizeRuleIdentifier(str);
            this.originalString = clean;
            int bracket = clean.indexOf('{');
            if (bracket == -1) {
                this.baseId = clean;
                this.nbt = null;
                this.hasNbt = false;
            } else {
                this.baseId = clean.substring(0, bracket);
                this.nbt = safeParseTagForRule(clean, clean.substring(bracket));
                this.hasNbt = true;
            }
        }

        public boolean matchesWithId(ItemStack stack, String itemId) {
            if (stack == null || itemId == null || itemId.isEmpty() || this.originalString.isEmpty()) return false;
            if (this.originalString.startsWith("@")) {
                int colonIdx = itemId.indexOf(':');
                String namespace = colonIdx == -1 ? itemId : itemId.substring(0, colonIdx);
                return namespace.equals(this.originalString.substring(1));
            }
            if (this.originalString.startsWith("#")) {
                String targetTag = this.originalString.substring(1);
                try {
                    return stack.getTags().anyMatch(t -> t.location().toString().toLowerCase(Locale.ROOT).equals(targetTag));
                } catch (Throwable e) {
                    logLimitedWarn("匹配物品标签规则时出错，规则=" + this.originalString + ", 物品=" + itemId, e);
                    return false;
                }
            }

            if (!itemId.equals(this.baseId)) return false;
            if (!this.hasNbt) return true;
            if (this.nbt != null) {
                return net.minecraft.nbt.NbtUtils.compareNbt(this.nbt, stack.getTag(), true);
            }
            return false;
        }

        public boolean matches(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return false;
            String itemId = getItemIdFromCache(stack.getItem());
            if (itemId.isEmpty()) return false;
            return matchesWithId(stack, itemId);
        }

        @Override
        public int hashCode() { return originalString.hashCode(); }
        @Override
        public boolean equals(Object obj) {
            return obj instanceof ItemRule && ((ItemRule)obj).originalString.equals(this.originalString);
        }
    }

    public static boolean isProtected(String identifier) {
        if (identifier == null) return false;
        String clean = identifier.toLowerCase(Locale.ROOT);
        return clean.contains("kineticitem");
    }

    public static boolean isBanned(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String itemId = getItemIdFromCache(stack.getItem());
        if (itemId.isEmpty()) return false;

        if (pureIdBanned.contains(itemId)) return true;

        List<ItemRule> nbtRules = nbtBanned.get(itemId);
        if (nbtRules != null) {
            for (ItemRule rule : nbtRules) {
                if (rule.matchesWithId(stack, itemId)) return true;
            }
        }

        for (ItemRule rule : macroBanned) {
            if (rule.matchesWithId(stack, itemId)) return true;
        }

        return false;
    }

    public static boolean isBannedStr(String idStr) {
        String clean = normalizeRuleIdentifier(idStr);
        if (clean.isEmpty() || clean.startsWith("@") || clean.startsWith("#")) return false;
        if (data != null && data.bannedItems != null && data.bannedItems.contains(clean)) return true;

        int bracket = clean.indexOf('{');
        String baseId = bracket == -1 ? clean : clean.substring(0, bracket);
        if (pureIdBanned.contains(baseId)) return true;

        ItemStack stack = parseItemStack(clean);
        return !stack.isEmpty() && isBanned(stack);
    }

    public static Set<String> getMergedTagIds(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Collections.emptySet();
        String itemId = getItemIdFromCache(stack.getItem());
        Set<String> configured = targetMergedTags.get(itemId);
        if (configured == null || configured.isEmpty()) return Collections.emptySet();
        return configured;
    }

    public static Set<TagKey<Item>> getMergedTagKeys(ItemStack stack) {
        Set<String> ids = getMergedTagIds(stack);
        if (ids.isEmpty()) return Collections.emptySet();
        Set<TagKey<Item>> result = new HashSet<>();
        for (String id : ids) {
            try {
                result.add(ItemTags.create(new ResourceLocation(id)));
            } catch (Exception ignored) {}
        }
        if (result.isEmpty()) return Collections.emptySet();
        return Collections.unmodifiableSet(result);
    }

    public static boolean hasMergedTag(ItemStack stack, TagKey<Item> tagKey) {
        if (tagKey == null) return false;
        String tagId = tagKey.location().toString().toLowerCase(Locale.ROOT);
        return getMergedTagIds(stack).contains(tagId);
    }

    private static String normalizeMergedTagId(String tagId) {
        if (tagId == null) return "";
        String clean = tagId.trim().toLowerCase(Locale.ROOT);
        if (clean.startsWith("#")) clean = clean.substring(1);
        try {
            return new ResourceLocation(clean).toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static void normalizeData() {
        if (data == null) data = new Data();
        if (data.bannedItems == null) data.bannedItems = new ArrayList<>();
        if (data.mergedItems == null) data.mergedItems = new LinkedHashMap<>();

        data.bannedItems = normalizeRuleList(data.bannedItems, "bannedItems");
        data.mergedItems = normalizeRuleMap(data.mergedItems);
    }

    private static List<String> normalizeRuleList(List<String> source, String name) {
        if (source == null || source.isEmpty()) return new ArrayList<>();
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String raw : source) {
            String clean = normalizeRuleIdentifier(raw);
            if (clean.isEmpty()) {
                logLimitedWarn("已跳过无效规则，位置=" + name + ", 原始值=" + raw, null);
                continue;
            }
            result.add(clean);
        }
        return new ArrayList<>(result);
    }

    private static String normalizePrefixedRuleBody(String raw) {
        return raw.substring(1).trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeRuleIdentifier(String identifier) {
        if (identifier == null) return "";
        String raw = identifier.trim();
        if (raw.isEmpty()) return "";
        if (raw.startsWith("@")) {
            String namespace = normalizePrefixedRuleBody(raw);
            if (namespace.isEmpty() || namespace.contains(" ") || namespace.contains("{")) return "";
            return "@" + namespace;
        }
        if (raw.startsWith("#")) {
            String tag = normalizePrefixedRuleBody(raw);
            try {
                return "#" + new ResourceLocation(tag);
            } catch (Exception e) {
                logLimitedWarn("无效标签规则=" + raw, e);
                return "";
            }
        }

        int bracket = raw.indexOf('{');
        String idPart = bracket == -1 ? raw : raw.substring(0, bracket);
        String nbtPart = bracket == -1 ? "" : raw.substring(bracket).trim();
        String cleanId = idPart.trim().toLowerCase(Locale.ROOT);
        if (cleanId.isEmpty()) return "";
        try {
            cleanId = new ResourceLocation(cleanId).toString();
        } catch (Exception e) {
            logLimitedWarn("无效物品 ID=" + raw, e);
            return "";
        }
        if (!nbtPart.isEmpty()) {
            CompoundTag parsed = safeParseTagForRule(raw, nbtPart);
            if (parsed == null) return "";
            String compact = compactKnownNbtIdentifier(cleanId, parsed);
            if (!compact.isEmpty()) return compact;
            return cleanId + nbtPart;
        }
        return cleanId;
    }

    private static String compactKnownNbtIdentifier(String baseId, CompoundTag tag) {
        if (baseId == null || baseId.isBlank() || tag == null || tag.isEmpty()) return "";
        if (tag.contains("GunId", 8)) {
            String gunId = tag.getString("GunId").trim();
            if (!gunId.isEmpty()) {
                CompoundTag stable = new CompoundTag();
                stable.putString("GunId", gunId);
                return baseId + stable;
            }
        }
        return "";
    }

    private static CompoundTag safeParseTagForRule(String rule, String nbtText) {
        if (nbtText == null || nbtText.isBlank()) return null;
        try {
            return TagParser.parseTag(nbtText);
        } catch (Exception e) {
            logLimitedWarn("无效 NBT 规则，已跳过，规则=" + rule + ", NBT=" + nbtText, e);
            return null;
        }
    }

    private static void logLimitedWarn(String message, Throwable throwable) {
        if (INVALID_RULE_LOG_COUNT.incrementAndGet() > 200) return;
        if (throwable == null) LOGGER.warn(message);
        else LOGGER.warn(message, throwable);
    }
    private static Map<String, List<String>> normalizeRuleMap(Map<String, List<String>> source) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        if (source == null || source.isEmpty()) return result;
        for (Map.Entry<String, List<String>> entry : source.entrySet()) {
            String target = cleanIdentifier(entry.getKey());
            if (target.isEmpty()) continue;
            List<String> values = entry.getValue() == null ? Collections.emptyList() : entry.getValue();
            List<String> cleanSources = values.stream()
                    .map(BanItemConfig::cleanIdentifier)
                    .filter(s -> !s.isEmpty())
                    .filter(s -> !getBaseIdentifier(s).equals(getBaseIdentifier(target)))
                    .distinct()
                    .collect(Collectors.toCollection(ArrayList::new));
            if (!cleanSources.isEmpty()) result.put(target, cleanSources);
        }
        return result;
    }
    private static String cleanIdentifier(String identifier) {
        return normalizeRuleIdentifier(identifier);
    }

    public static String getBaseIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) return "";
        String raw = identifier.trim();
        int bracket = raw.indexOf('{');
        String base = bracket == -1 ? raw : raw.substring(0, bracket);
        if (base.startsWith("@") || base.startsWith("#")) return base.toLowerCase(Locale.ROOT);
        try {
            return new ResourceLocation(base.trim().toLowerCase(Locale.ROOT)).toString();
        } catch (Exception ignored) {
            return "";
        }
    }
    static { load(); }

    public static void load() {
        try {
            if (Files.exists(PATH)) {
                String json = Files.readString(PATH);
                data = readDataFromJson(json, "load");
            } else {
                data = new Data();
            }
            normalizeData();
            if (!Files.exists(PATH)) writeConfigOnly();
        } catch (Throwable e) {
            LOGGER.error("配置加载失败，已备份异常文件并使用空白安全配置", e);
            backupBrokenConfig();
            data = new Data();
            try {
                normalizeData();
                writeConfigOnly();
            } catch (Throwable writeError) {
                LOGGER.error("写入空白安全配置失败", writeError);
            }
        }
        rebuildCache();
    }

    public static void save() {
        try {
            normalizeData();
            writeConfigOnly();
            rebuildCache();
        } catch (Throwable e) {
            LOGGER.error("保存物品配置失败", e);
        }
    }

    public static boolean applyJson(String json, String source, boolean saveFile) {
        try {
            if (json == null || json.isBlank()) {
                LOGGER.warn("收到空白配置，来源={}", source);
                return false;
            }
            data = readDataFromJson(json, source);
            normalizeData();
            if (saveFile) writeConfigOnly();
            rebuildCache();
            return true;
        } catch (Throwable e) {
            LOGGER.error("应用配置失败，已忽略本次错误数据，来源={}", source, e);
            return false;
        }
    }

    public static String getNetworkJson() {
        try {
            normalizeData();
        } catch (Throwable e) {
            LOGGER.error("生成同步配置前清洗失败，将使用当前内存数据", e);
        }
        return GSON.toJson(data == null ? new Data() : data);
    }

    private static Data readDataFromJson(String json, String source) {
        try {
            Data loaded = GSON.fromJson(json, Data.class);
            if (loaded == null) {
                LOGGER.warn("配置内容为空，来源={}", source);
                return new Data();
            }
            return loaded;
        } catch (Throwable e) {
            throw new IllegalArgumentException("无法解析 " + PATH.getFileName() + "，来源=" + source, e);
        }
    }

    private static void writeConfigOnly() {
        try {
            if (PATH.getParent() != null) Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(data == null ? new Data() : data));
        } catch (Throwable e) {
            throw new IllegalStateException("写入 " + PATH.getFileName() + " 失败", e);
        }
    }

    private static void backupBrokenConfig() {
        try {
            if (!Files.exists(PATH)) return;
            if (BACKUP_PATH.getParent() != null) Files.createDirectories(BACKUP_PATH.getParent());
            Files.copy(PATH, BACKUP_PATH, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.warn("已备份异常配置到 {}", BACKUP_PATH);
        } catch (Throwable e) {
            LOGGER.error("备份异常配置失败", e);
        }
    }

    private static void collectMergeRules(Map<String, List<String>> rules, Map<ItemRule, String> replacements, Map<String, Set<String>> mergedTagsByTarget) {
        if (rules == null || rules.isEmpty()) return;
        for (Map.Entry<String, List<String>> entry : rules.entrySet()) {
            String target = cleanIdentifier(entry.getKey());
            if (target.isEmpty()) continue;
            ItemRule targetRule = new ItemRule(target);
            collectVirtualMergedTags(targetRule.baseId, target, mergedTagsByTarget);

            List<String> sources = entry.getValue() == null ? Collections.emptyList() : entry.getValue();
            for (String sourceRaw : sources) {
                String source = cleanIdentifier(sourceRaw);
                if (source.isEmpty()) continue;
                replacements.put(new ItemRule(source), target);
                collectVirtualMergedTags(targetRule.baseId, source, mergedTagsByTarget);
            }
        }
    }

    private static void collectVirtualMergedTags(String targetBaseId, String identifier, Map<String, Set<String>> mergedTagsByTarget) {
        if (targetBaseId == null || targetBaseId.isEmpty() || identifier == null || identifier.isEmpty()) return;
        Set<String> tags = getRegistryTagIdsForIdentifier(identifier);
        if (tags.isEmpty()) return;
        mergedTagsByTarget.computeIfAbsent(targetBaseId, key -> new TreeSet<>()).addAll(tags);
    }

    private static Set<String> getRegistryTagIdsForIdentifier(String identifier) {
        ItemStack stack = parseItemStack(identifier);
        if (stack == null || stack.isEmpty()) return Collections.emptySet();
        Set<String> result = new TreeSet<>();
        try {
            stack.getTags().forEach(tag -> {
                String clean = normalizeMergedTagId(tag.location().toString());
                if (!clean.isEmpty()) result.add(clean);
            });
        } catch (Exception ignored) {
        }
        return result;
    }

    public static void rebuildCache() {
        try {
            rebuildCacheUnsafe();
        } catch (Throwable e) {
            clearRuntimeCache();
            LOGGER.error("重建物品封禁运行缓存失败，已清空运行缓存，避免影响进服", e);
        }
    }

    private static void rebuildCacheUnsafe() {
        normalizeData();
        List<ItemRule> newBannedRules = new ArrayList<>();
        Map<ItemRule, String> newReplacementMap = new HashMap<>();
        Map<String, Set<String>> newTargetMergedTags = new HashMap<>();

        for (String banned : data.bannedItems) {
            ItemRule rule = new ItemRule(banned);
            newBannedRules.add(rule);
            newReplacementMap.put(rule, VOID_ID);
        }
        collectMergeRules(data.mergedItems, newReplacementMap, newTargetMergedTags);

        ruleReplacementMap = Collections.unmodifiableMap(new HashMap<>(newReplacementMap));
        Map<String, Set<String>> frozenTargetTags = new HashMap<>();
        newTargetMergedTags.forEach((key, value) ->
                frozenTargetTags.put(key, Collections.unmodifiableSet(new TreeSet<>(value))));
        targetMergedTags = Collections.unmodifiableMap(frozenTargetTags);

        Map<String, String> newPureIdReplacements = new HashMap<>();
        Map<String, List<Map.Entry<ItemRule, String>>> newNbtReplacements = new HashMap<>();
        List<Map.Entry<ItemRule, String>> newMacroReplacements = new ArrayList<>();

        for (Map.Entry<ItemRule, String> entry : newReplacementMap.entrySet()) {
            ItemRule rule = entry.getKey();
            if (rule.originalString.startsWith("@") || rule.originalString.startsWith("#")) {
                newMacroReplacements.add(entry);
            } else if (rule.hasNbt) {
                newNbtReplacements.computeIfAbsent(rule.baseId, key -> new ArrayList<>()).add(entry);
            } else {
                newPureIdReplacements.put(rule.baseId, entry.getValue());
            }
        }

        pureIdReplacements = Collections.unmodifiableMap(newPureIdReplacements);
        Map<String, List<Map.Entry<ItemRule, String>>> frozenNbtReplacements = new HashMap<>();
        newNbtReplacements.forEach((key, value) ->
                frozenNbtReplacements.put(key, Collections.unmodifiableList(new ArrayList<>(value))));
        nbtReplacements = Collections.unmodifiableMap(frozenNbtReplacements);
        macroReplacements = Collections.unmodifiableList(new ArrayList<>(newMacroReplacements));

        Set<String> newPureIdBanned = new HashSet<>();
        Map<String, List<ItemRule>> newNbtBanned = new HashMap<>();
        List<ItemRule> newMacroBanned = new ArrayList<>();

        for (ItemRule rule : newBannedRules) {
            if (rule.originalString.startsWith("@") || rule.originalString.startsWith("#")) {
                newMacroBanned.add(rule);
            } else if (rule.hasNbt) {
                newNbtBanned.computeIfAbsent(rule.baseId, key -> new ArrayList<>()).add(rule);
            } else {
                newPureIdBanned.add(rule.baseId);
            }
        }

        pureIdBanned = Collections.unmodifiableSet(newPureIdBanned);
        Map<String, List<ItemRule>> frozenNbtBanned = new HashMap<>();
        newNbtBanned.forEach((key, value) ->
                frozenNbtBanned.put(key, Collections.unmodifiableList(new ArrayList<>(value))));
        nbtBanned = Collections.unmodifiableMap(frozenNbtBanned);
        macroBanned = Collections.unmodifiableList(new ArrayList<>(newMacroBanned));
    }

    private static void clearRuntimeCache() {
        ruleReplacementMap = Collections.emptyMap();
        pureIdReplacements = Collections.emptyMap();
        nbtReplacements = Collections.emptyMap();
        macroReplacements = Collections.emptyList();
        targetMergedTags = Collections.emptyMap();
        pureIdBanned = Collections.emptySet();
        nbtBanned = Collections.emptyMap();
        macroBanned = Collections.emptyList();
    }

    public static String getReplacement(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        String itemId = getItemIdFromCache(stack.getItem());
        if (itemId.isEmpty()) return null;

        List<Map.Entry<ItemRule, String>> nbtRules = nbtReplacements.get(itemId);
        if (nbtRules != null) {
            for (Map.Entry<ItemRule, String> entry : nbtRules) {
                if (entry.getKey().matchesWithId(stack, itemId)) return entry.getValue();
            }
        }

        String pureTarget = pureIdReplacements.get(itemId);
        if (pureTarget != null) {
            return pureTarget;
        }

        for (Map.Entry<ItemRule, String> entry : macroReplacements) {
            if (entry.getKey().matchesWithId(stack, itemId)) return entry.getValue();
        }

        return null;
    }

    public static String getReplacement(String id) {
        return pureIdReplacements.get(id);
    }

    public static String getItemIdentifier(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return "";
        String base = id.toString();
        CompoundTag tag = stack.getTag();
        if (tag == null || tag.isEmpty()) return base;
        String compact = compactKnownNbtIdentifier(base, tag);
        if (!compact.isEmpty()) return compact;
        return base + tag;
    }

    public static ItemStack parseItemStack(String identifier) {
        String clean = normalizeRuleIdentifier(identifier);
        if (clean.isEmpty() || clean.startsWith("@") || clean.startsWith("#")) return ItemStack.EMPTY;
        final ItemStack[] result = {ItemStack.EMPTY};

        dev.xyat.kineticitem.item.util.ItemBanControl.withSkip(() -> {
            try {
                int bracket = clean.indexOf('{');
                if (bracket == -1) {
                    Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(clean));
                    if (item != null) result[0] = new ItemStack(item);
                } else {
                    String id = clean.substring(0, bracket);
                    String nbt = clean.substring(bracket);
                    Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
                    if (item != null) {
                        CompoundTag tag = safeParseTagForRule(clean, nbt);
                        if (tag != null) {
                            ItemStack stack = new ItemStack(item);
                            stack.setTag(tag);
                            result[0] = stack;
                        }
                    }
                }
            } catch (Throwable e) {
                logLimitedWarn("解析物品失败，identifier=" + identifier + ", clean=" + clean, e);
            }
            return null;
        });

        return result[0];
    }
}
