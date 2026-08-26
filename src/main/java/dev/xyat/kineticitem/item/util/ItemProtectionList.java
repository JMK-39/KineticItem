package dev.xyat.kineticitem.item.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Set;

public class ItemProtectionList {
    private static final Set<String> FIRE_IMMUNE_IDS = new HashSet<>();
    private static final Set<TagKey<Item>> FIRE_IMMUNE_TAGS = new HashSet<>();
    private static final Set<String> FIRE_IMMUNE_MODS = new HashSet<>();

    /**
     * KubeJS 调用的注册方法
     */
    public static void addFireImmune(String id) {
        if (id.startsWith("#")) {
            FIRE_IMMUNE_TAGS.add(ItemTags.create(new ResourceLocation(id.substring(1))));
        } else if (id.startsWith("@")) {
            FIRE_IMMUNE_MODS.add(id.substring(1));
        } else {
            FIRE_IMMUNE_IDS.add(id);
        }
    }

    /**
     * 清空列表 (用于重载脚本时)
     */
    public static void clear() {
        FIRE_IMMUNE_IDS.clear();
        FIRE_IMMUNE_TAGS.clear();
        FIRE_IMMUNE_MODS.clear();
    }

    /**
     * 判断物品是否在列表中
     */
    public static boolean isFireImmune(ItemStack stack) {
        if (stack.isEmpty()) return false;

        ResourceLocation registryName = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (registryName == null) return false;

        // 1. 检查精确 ID
        if (FIRE_IMMUNE_IDS.contains(registryName.toString())) return true;

        // 2. 检查 Mod ID
        if (FIRE_IMMUNE_MODS.contains(registryName.getNamespace())) return true;

        // 3. 检查 Tag
        for (TagKey<Item> tag : FIRE_IMMUNE_TAGS) {
            if (stack.is(tag)) return true;
        }

        return false;
    }
}