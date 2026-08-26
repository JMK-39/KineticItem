package dev.xyat.kineticitem.item.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import dev.xyat.kineticitem.KineticItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
public class ItemProtectionConfig {
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("kineticcore/protection.toml");
    private static CommentedFileConfig configData;

    public static class ProtectionRule {
        public final String identifier;
        public final boolean fireImmune;
        public final boolean explosionImmune;
        public final boolean glowing;
        public final boolean noGravity;
        public CompoundTag nbt = null;
        public String baseId = null;
        public String modId = null;
        public TagKey<Item> tagKey = null;

        public ProtectionRule(String identifier, boolean fireImmune, boolean explosionImmune, boolean glowing, boolean noGravity) {
            String tempId = identifier;
            if (identifier.contains("{")) {
                int idx = identifier.indexOf("{");
                try {
                    this.nbt = TagParser.parseTag(identifier.substring(idx));
                    tempId = identifier.substring(0, idx).trim();
                } catch (Exception e) {
                    KineticItem.LOGGER.error("Failed to parse NBT in rule: {}", identifier);
                }
            }
            this.identifier = tempId;
            this.fireImmune = fireImmune;
            this.explosionImmune = explosionImmune;
            this.glowing = glowing;
            this.noGravity = noGravity;

            if (tempId.startsWith("@")) {
                this.modId = tempId.substring(1);
            } else if (tempId.startsWith("#")) {
                this.tagKey = ItemTags.create(new ResourceLocation(tempId.substring(1)));
            } else {
                this.baseId = tempId;
            }
        }
    }

    private static final Map<String, List<ProtectionRule>> PROTECTION_ID_MAP = new HashMap<>();
    private static final List<ProtectionRule> PROTECTION_PATTERN_LIST = new ArrayList<>();
    private static final Set<ResourceKey<DamageType>> GLOBAL_DAMAGE_IMMUNE_IDS = new HashSet<>();
    private static final Set<TagKey<DamageType>> GLOBAL_DAMAGE_IMMUNE_TAGS = new HashSet<>();
    private static final Set<ResourceLocation> GLOBAL_DIRECT_ENTITY_IMMUNE_IDS = new HashSet<>();
    private static final Set<TagKey<EntityType<?>>> GLOBAL_DIRECT_ENTITY_IMMUNE_TAGS = new HashSet<>();

    public static boolean enableItemProtection = true;
    public static boolean enableVoidSalvage = true;
    public static boolean enableGlobalItemDamageImmunity = true;
    public static boolean enableGlobalDirectEntityImmunity = true;
    public static List<String> indestructibleItemsRaw = new ArrayList<>();
    public static List<String> globalItemDamageImmunityRaw = new ArrayList<>();
    public static List<String> globalDirectEntityImmunityRaw = new ArrayList<>();

    public static void load() {
        try {
            configData = CommentedFileConfig.builder(CONFIG_PATH)
                    .sync().preserveInsertionOrder().writingMode(WritingMode.REPLACE).build();
            configData.load();
            setupConfig();
            configData.save();
            readValues();
        } catch (Exception e) {
            KineticItem.LOGGER.error("ItemProtectionConfig Load Failed", e);
        }
    }

    private static void setupConfig() {
        configData.setComment("indestructible",
                """
                         掉落物保护设置
                         Item Protection Settings
                         自定义掉落物的物理属性和无敌属性
                         Customize physics and invulnerability of dropped items.""");
        define("indestructible.enable", true,
                "是否启用掉落物保护功能\n" +
                        "Whether to enable item protection functionality");
        define("indestructible.void_salvage", true,
                """
                         是否开启虚空救援功能
                         Whether to enable void salvage functionality
                         开启后，受保护的物品掉进虚空会尝试传送到最近的地面
                         When enabled, protected items falling into void will teleport to nearest ground.""");
        define("indestructible.items", new ArrayList<>(),
                """
                         受保护的物品列表
                         List of protected items
                         格式："标识符;防火;防爆;发光;无重力"
                         Format: "Identifier;FireImmune;ExplosionImmune;Glowing;NoGravity"
                         标识符支持：ID (minecraft:apple), 标签 (#minecraft:logs), 模组 (@create)
                         Identifier supports: ID, Tags, or ModIDs.""");
        define("indestructible.global.damage.immunity_enable", true,
                """
                         是否启用所有掉落物通用伤害类型免疫
                         Whether to enable global damage type immunity for all dropped items
                         开启后，下面列表里的伤害类型会对任何掉落物无效
                         When enabled, damage types listed below will not hurt any dropped item.""");
        define("indestructible.global.damage.immunity_sources", new ArrayList<>(),
                """
                         所有掉落物通用免疫伤害类型列表
                         Global damage type immunity list for all dropped items
                         支持单个伤害ID，例如：minecraft:lava、minecraft:player_attack
                         Supports single damage IDs, such as minecraft:lava, minecraft:player_attack
                         支持伤害标签，前面加 #，例如：#minecraft:is_fire、#minecraft:is_explosion
                         Supports damage tags, add # before the tag, such as #minecraft:is_fire, #minecraft:is_explosion.""");
        define("indestructible.global.direct.entity.immunity_enable", true,
                """
                         是否启用所有掉落物通用直接伤害实体免疫
                         Whether to enable global direct damage entity immunity for all dropped items
                         开启后，下面列表里的实体直接造成伤害时，任何掉落物都会免疫
                         When enabled, dropped items will ignore damage directly caused by listed entities.""");
        define("indestructible.global.direct.entity.immunity_sources", new ArrayList<>(),
                """
                         所有掉落物通用免疫直接伤害实体列表
                         Global direct damage entity immunity list for all dropped items
                         支持单个实体ID，例如：tacz:bullet
                         Supports single entity IDs, such as tacz:bullet
                         支持实体标签，前面加 #，例如：#forge:bullets
                         Supports entity tags, add # before the tag, such as #forge:bullets.""");
    }

    private static void define(String path, Object def, String comment) {
        if (!configData.contains(path)) configData.set(path, def);
        configData.setComment(path, " " + comment.trim());
    }

    private static void readValues() {
        enableItemProtection = configData.getOrElse("indestructible.enable", true);
        enableVoidSalvage = configData.getOrElse("indestructible.void_salvage", true);
        enableGlobalItemDamageImmunity = configData.getOrElse("indestructible.global.damage.immunity_enable", true);
        enableGlobalDirectEntityImmunity = configData.getOrElse("indestructible.global.direct.entity.immunity_enable", true);
        indestructibleItemsRaw = configData.getOrElse("indestructible.items", new ArrayList<>());
        globalItemDamageImmunityRaw = configData.getOrElse("indestructible.global.damage.immunity_sources", new ArrayList<>());
        globalDirectEntityImmunityRaw = configData.getOrElse("indestructible.global.direct.entity.immunity_sources", new ArrayList<>());

        PROTECTION_ID_MAP.clear();
        PROTECTION_PATTERN_LIST.clear();
        GLOBAL_DAMAGE_IMMUNE_IDS.clear();
        GLOBAL_DAMAGE_IMMUNE_TAGS.clear();
        GLOBAL_DIRECT_ENTITY_IMMUNE_IDS.clear();
        GLOBAL_DIRECT_ENTITY_IMMUNE_TAGS.clear();

        for (String entry : indestructibleItemsRaw) {
            String[] parts = entry.split(";");
            if (parts.length >= 5) {
                boolean fire = Boolean.parseBoolean(parts[1].trim());
                boolean expl = Boolean.parseBoolean(parts[2].trim());
                boolean glow = Boolean.parseBoolean(parts[3].trim());
                boolean grav = Boolean.parseBoolean(parts[4].trim());

                ProtectionRule rule = new ProtectionRule(parts[0].trim(), fire, expl, glow, grav);

                if (rule.baseId != null) {
                    PROTECTION_ID_MAP.computeIfAbsent(rule.baseId, k -> new ArrayList<>()).add(rule);
                } else {
                    PROTECTION_PATTERN_LIST.add(rule);
                }
            }
        }

        for (String entry : globalItemDamageImmunityRaw) {
            registerGlobalDamageImmunity(entry);
        }

        for (String entry : globalDirectEntityImmunityRaw) {
            registerGlobalDirectEntityImmunity(entry);
        }
    }

    private static void registerGlobalDamageImmunity(String raw) {
        if (raw == null) return;

        String value = raw.trim();
        if (value.isEmpty()) return;

        try {
            if (value.startsWith("#")) {
                ResourceLocation tagId = new ResourceLocation(value.substring(1));
                GLOBAL_DAMAGE_IMMUNE_TAGS.add(TagKey.create(Registries.DAMAGE_TYPE, tagId));
            } else {
                ResourceLocation damageId = new ResourceLocation(value);
                GLOBAL_DAMAGE_IMMUNE_IDS.add(ResourceKey.create(Registries.DAMAGE_TYPE, damageId));
            }
        } catch (Exception e) {
            KineticItem.LOGGER.warn("Invalid global item damage immunity entry: {}", raw);
        }
    }

    private static void registerGlobalDirectEntityImmunity(String raw) {
        if (raw == null) return;

        String value = raw.trim();
        if (value.isEmpty()) return;

        try {
            if (value.startsWith("#")) {
                ResourceLocation tagId = new ResourceLocation(value.substring(1));
                GLOBAL_DIRECT_ENTITY_IMMUNE_TAGS.add(TagKey.create(Registries.ENTITY_TYPE, tagId));
            } else {
                GLOBAL_DIRECT_ENTITY_IMMUNE_IDS.add(new ResourceLocation(value));
            }
        } catch (Exception e) {
            KineticItem.LOGGER.warn("Invalid global direct entity immunity entry: {}", raw);
        }
    }

    public static ProtectionRule getProtectionRule(ItemStack stack) {
        if (!enableItemProtection || stack.isEmpty()) return null;
        ResourceLocation itemIdRL = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemIdRL == null) return null;
        String itemId = itemIdRL.toString();

        List<ProtectionRule> idRules = PROTECTION_ID_MAP.get(itemId);
        if (idRules != null) {
            for (ProtectionRule rule : idRules) {
                if (rule.nbt != null) {
                    if (stack.hasTag() && NbtUtils.compareNbt(rule.nbt, stack.getTag(), true)) return rule;
                } else {
                    return rule;
                }
            }
        }

        for (ProtectionRule rule : PROTECTION_PATTERN_LIST) {
            if (rule.modId != null) {
                if (itemIdRL.getNamespace().equals(rule.modId)) return rule;
            } else if (rule.tagKey != null) {
                if (stack.is(rule.tagKey)) return rule;
            }
        }
        return null;
    }

    public static boolean isGlobalItemDamageImmune(DamageSource source) {
        if (!enableGlobalItemDamageImmunity || source == null) return false;

        Optional<ResourceKey<DamageType>> key = source.typeHolder().unwrapKey();
        if (key.isPresent() && GLOBAL_DAMAGE_IMMUNE_IDS.contains(key.get())) {
            return true;
        }

        for (TagKey<DamageType> tag : GLOBAL_DAMAGE_IMMUNE_TAGS) {
            if (source.is(tag)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isGlobalDirectEntityImmune(DamageSource source) {
        if (!enableGlobalDirectEntityImmunity || source == null) return false;

        Entity directEntity = source.getDirectEntity();
        if (directEntity == null) return false;

        EntityType<?> entityType = directEntity.getType();
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entityType);

        if (entityId != null && GLOBAL_DIRECT_ENTITY_IMMUNE_IDS.contains(entityId)) {
            return true;
        }

        for (TagKey<EntityType<?>> tag : GLOBAL_DIRECT_ENTITY_IMMUNE_TAGS) {
            if (entityType.is(tag)) {
                return true;
            }
        }

        return false;
    }

    public static String getDamageSourceId(DamageSource source) {
        if (source == null) return "unknown";
        return source.typeHolder().unwrapKey()
                .map(key -> key.location().toString())
                .orElse(source.getMsgId());
    }

    public static void setProtectionRules(List<String> values) {
        if (!areValidProtectionRules(values)) {
            throw new IllegalArgumentException("invalid protection rule");
        }
        indestructibleItemsRaw = new ArrayList<>(values);
    }

    public static void setDamageSources(List<String> values) {
        if (!areValidResourceEntries(values)) {
            throw new IllegalArgumentException("invalid damage source id");
        }
        globalItemDamageImmunityRaw = new ArrayList<>(values);
    }

    public static void setDirectEntitySources(List<String> values) {
        if (!areValidResourceEntries(values)) {
            throw new IllegalArgumentException("invalid entity source id");
        }
        globalDirectEntityImmunityRaw = new ArrayList<>(values);
    }

    public static boolean areValidProtectionRules(List<String> values) {
        if (values == null) return false;
        for (String value : values) {
            if (!isValidProtectionRule(value)) return false;
        }
        return true;
    }

    public static boolean areValidResourceEntries(List<String> values) {
        if (values == null) return false;
        for (String raw : values) {
            if (raw == null) return false;
            String value = raw.trim();
            if (value.isEmpty()) return false;
            String id = value.startsWith("#") ? value.substring(1) : value;
            if (id.isEmpty()) return false;
            try {
                new ResourceLocation(id);
            } catch (Exception ignored) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidProtectionRule(String raw) {
        if (raw == null || raw.isBlank()) return false;
        String[] parts = raw.split(";", 5);
        if (parts.length != 5) return false;
        String identifier = parts[0].trim();
        if (identifier.isEmpty()) return false;
        if (!isBoolean(parts[1]) || !isBoolean(parts[2]) || !isBoolean(parts[3]) || !isBoolean(parts[4])) {
            return false;
        }

        String plainIdentifier = identifier;
        int nbtStart = identifier.indexOf('{');
        if (nbtStart >= 0) {
            plainIdentifier = identifier.substring(0, nbtStart).trim();
            try {
                TagParser.parseTag(identifier.substring(nbtStart));
            } catch (Exception ignored) {
                return false;
            }
        }

        if (plainIdentifier.startsWith("@")) {
            String modId = plainIdentifier.substring(1);
            if (modId.isEmpty()) return false;
            try {
                new ResourceLocation(modId, "validation");
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }

        String resource = plainIdentifier.startsWith("#") ? plainIdentifier.substring(1) : plainIdentifier;
        if (resource.isEmpty()) return false;
        try {
            new ResourceLocation(resource);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isBoolean(String value) {
        String normalized = value == null ? "" : value.trim();
        return "true".equalsIgnoreCase(normalized) || "false".equalsIgnoreCase(normalized);
    }

    public static void save() {
        if (configData == null) {
            throw new IllegalStateException("Item protection config is not loaded");
        }
        configData.set("indestructible.enable", enableItemProtection);
        configData.set("indestructible.void_salvage", enableVoidSalvage);
        configData.set("indestructible.items", indestructibleItemsRaw);
        configData.set("indestructible.global.damage.immunity_enable", enableGlobalItemDamageImmunity);
        configData.set("indestructible.global.damage.immunity_sources", globalItemDamageImmunityRaw);
        configData.set("indestructible.global.direct.entity.immunity_enable", enableGlobalDirectEntityImmunity);
        configData.set("indestructible.global.direct.entity.immunity_sources", globalDirectEntityImmunityRaw);
        configData.save();
        readValues();
    }
}
