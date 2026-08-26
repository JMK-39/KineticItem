package dev.xyat.kineticitem.item.client.gui;

import dev.xyat.kineticitem.item.config.BanItemConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public final class ItemUnificationHelper {
    private static final List<String> TYPE_SUFFIXES = List.of(
            "raw_materials", "storage_blocks", "ingots", "nuggets", "ores", "gems",
            "dusts", "plates", "gears", "rods", "wires", "coins", "clusters",
            "shards", "crystals", "sheets", "powders", "pearls"
    );
    private static final Set<String> VALID_TYPES = Set.of(
            "raw_material", "storage_block", "ingot", "nugget", "ore", "gem",
            "dust", "plate", "gear", "rod", "wire", "coin", "cluster",
            "shard", "crystal", "sheet", "powder", "pearl"
    );
    private static final List<String> STRATA_PRIORITY = List.of(
            "deepslate", "blackstone", "netherrack", "end_stone", "basalt", "andesite", "diorite", "granite", "stone"
    );

    private ItemUnificationHelper() {
    }

    public record MergeGroup(String type, String material, String strata, String tagId) {
        public String key() {
            return type + "|" + material + "|" + strata;
        }

        public boolean sameGroup(MergeGroup other) {
            return other != null && type.equals(other.type) && material.equals(other.material) && strata.equals(other.strata);
        }
    }

    public static Set<MergeGroup> getGroupsForIdentifier(String idStr) {
        if (idStr == null || idStr.isBlank()) return Collections.emptySet();
        ItemStack stack = BanItemConfig.parseItemStack(idStr);
        Set<String> tags = new TreeSet<>(ItemSearchCache.getRegistryTagIdsForId(idStr));
        if (stack != null && !stack.isEmpty()) tags.addAll(BanItemConfig.getMergedTagIds(stack));
        return getGroupsFromTags(tags, idStr);
    }

    public static Set<MergeGroup> getGroupsForStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Collections.emptySet();
        String idStr = BanItemConfig.getItemIdentifier(stack);
        Set<String> tags = new TreeSet<>(ItemSearchCache.getRegistryTagIdsForStack(stack));
        tags.addAll(BanItemConfig.getMergedTagIds(stack));
        return getGroupsFromTags(tags, idStr);
    }

    public static Set<String> getTagIdsForIdentifier(String idStr) {
        Set<MergeGroup> groups = getGroupsForIdentifier(idStr);
        if (groups.isEmpty()) return Collections.emptySet();
        Set<String> result = new TreeSet<>();
        for (MergeGroup group : groups) result.add(group.tagId);
        return Collections.unmodifiableSet(result);
    }

    public static boolean matchesAnyGroup(ItemStack stack, Set<MergeGroup> targetGroups) {
        if (stack == null || stack.isEmpty() || targetGroups == null || targetGroups.isEmpty()) return false;
        Set<MergeGroup> itemGroups = getGroupsForStack(stack);
        if (itemGroups.isEmpty()) return false;
        for (MergeGroup itemGroup : itemGroups) {
            for (MergeGroup targetGroup : targetGroups) {
                if (itemGroup.sameGroup(targetGroup)) return true;
            }
        }
        return false;
    }

    private static Set<MergeGroup> getGroupsFromTags(Set<String> tags, String idStr) {
        if (tags == null || tags.isEmpty()) return Collections.emptySet();
        String itemStrata = detectStrata(idStr, tags);
        Set<MergeGroup> result = new LinkedHashSet<>();
        for (String tagId : tags) {
            String clean = normalizeTagId(tagId);
            if (clean.isEmpty()) continue;
            ParsedTag parsed = parseUnifiedTag(clean);
            if (parsed == null) continue;
            String strata = "ore".equals(parsed.type) ? itemStrata : "";
            result.add(new MergeGroup(parsed.type, parsed.material, strata, clean));
        }
        if (result.isEmpty()) return Collections.emptySet();
        return Collections.unmodifiableSet(result);
    }

    private static ParsedTag parseUnifiedTag(String tagId) {
        ResourceLocation location;
        try {
            location = new ResourceLocation(tagId);
        } catch (Exception ignored) {
            return null;
        }

        String path = location.getPath().toLowerCase(Locale.ROOT);
        String[] parts = path.split("/");
        if (parts.length >= 2) {
            String type = canonicalType(parts[0]);
            String material = cleanMaterial(parts[1]);
            if (isValid(type, material)) return new ParsedTag(type, material);
        }

        for (String suffix : TYPE_SUFFIXES) {
            String end = "_" + suffix;
            if (path.endsWith(end)) {
                String material = cleanMaterial(path.substring(0, path.length() - end.length()));
                String type = canonicalType(suffix);
                if (isValid(type, material)) return new ParsedTag(type, material);
            }
        }

        if (path.startsWith("raw_") && path.endsWith("_blocks")) {
            String material = cleanMaterial(path.substring(0, path.length() - "_blocks".length()));
            if (!material.isEmpty()) return new ParsedTag("storage_block", material);
        }

        return null;
    }

    private static String canonicalType(String type) {
        if (type == null) return "";
        String clean = type.toLowerCase(Locale.ROOT).trim();
        return switch (clean) {
            case "ingots" -> "ingot";
            case "nuggets" -> "nugget";
            case "ores" -> "ore";
            case "gems" -> "gem";
            case "dusts" -> "dust";
            case "plates" -> "plate";
            case "gears" -> "gear";
            case "rods" -> "rod";
            case "wires" -> "wire";
            case "coins" -> "coin";
            case "clusters" -> "cluster";
            case "shards" -> "shard";
            case "crystals" -> "crystal";
            case "sheets" -> "sheet";
            case "powders" -> "powder";
            case "pearls" -> "pearl";
            case "raw_materials", "raw_material" -> "raw_material";
            case "storage_blocks", "storage_block", "blocks", "block" -> "storage_block";
            default -> clean;
        };
    }

    private static boolean isValid(String type, String material) {
        return VALID_TYPES.contains(type) && material != null && !material.isBlank() && !"ground".equals(material);
    }

    private static String cleanMaterial(String material) {
        if (material == null) return "";
        String clean = material.toLowerCase(Locale.ROOT).trim();
        if (clean.startsWith("raw_")) clean = clean.substring(4);
        return clean;
    }

    private static String detectStrata(String idStr, Set<String> tags) {
        for (String tag : tags) {
            String clean = normalizeTagId(tag);
            int idx = clean.indexOf("ores_in_ground/");
            if (idx >= 0) {
                String strata = clean.substring(idx + "ores_in_ground/".length());
                if (!strata.isBlank()) return cleanMaterial(strata);
            }
        }

        String base = baseId(idStr);
        String path;
        try {
            path = new ResourceLocation(base).getPath().toLowerCase(Locale.ROOT);
        } catch (Exception ignored) {
            path = base.toLowerCase(Locale.ROOT);
        }
        for (String strata : STRATA_PRIORITY) {
            if ("stone".equals(strata)) continue;
            if (path.contains(strata + "_") || path.contains("_" + strata + "_") || path.contains("_" + strata)) return strata;
        }
        return "stone";
    }

    private static String normalizeTagId(String tagId) {
        if (tagId == null) return "";
        String clean = tagId.trim().toLowerCase(Locale.ROOT);
        if (clean.startsWith("#")) clean = clean.substring(1);
        try {
            return new ResourceLocation(clean).toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String baseId(String idStr) {
        if (idStr == null) return "";
        int bracket = idStr.indexOf('{');
        return bracket == -1 ? idStr : idStr.substring(0, bracket);
    }

    private record ParsedTag(String type, String material) {
    }

}
