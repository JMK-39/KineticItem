package dev.xyat.kineticitem.item.config;

import dev.xyat.kineticcore.config.client.KTConfigApi;
import dev.xyat.kineticcore.config.client.KTConfigPage;
import dev.xyat.kineticcore.config.client.KTConfigScope;
import dev.xyat.kineticitem.item.network.ItemNetwork;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ItemProtectionConfigGui {
    public static final String PAGE_ID = "kineticitem:item_protection";

    private ItemProtectionConfigGui() {
    }

    public static void load() {
        KTConfigApi.register(KTConfigPage.builder(
                        PAGE_ID,
                        Component.translatable("cfg.kineticitem.protection")
                )
                .scope(KTConfigScope.SERVER_AUTHORITATIVE)
                .serverManaged()
                .applyTiming(KTConfigPage.ApplyTiming.MIXED)
                .applyNotice(Component.translatable("cfg.kineticitem.protection.apply_notice"))
                .section(Component.translatable("cfg.kineticitem.protection"))
                .description(Component.translatable("cfg.kineticitem.protection.description"))
                .booleanValue(
                        "enable_item_protection",
                        Component.translatable("cfg.kineticitem.prot.enable"),
                        () -> ItemProtectionConfig.enableItemProtection,
                        value -> ItemProtectionConfig.enableItemProtection = value,
                        true,
                        Component.translatable("cfg.kineticitem.prot.enable.tooltip")
                )
                .booleanValue(
                        "enable_void_salvage",
                        Component.translatable("cfg.kineticitem.prot.void_salvage"),
                        () -> ItemProtectionConfig.enableVoidSalvage,
                        value -> ItemProtectionConfig.enableVoidSalvage = value,
                        true,
                        Component.translatable("cfg.kineticitem.prot.void_salvage.tooltip")
                )
                .stringList(
                        "indestructible_items",
                        Component.translatable("cfg.kineticitem.prot.list"),
                        () -> new ArrayList<>(ItemProtectionConfig.indestructibleItemsRaw),
                        ItemProtectionConfig::setProtectionRules,
                        List.of(),
                        Component.translatable("cfg.kineticitem.prot.list.tooltip")
                )
                .booleanValue(
                        "global_damage_immunity_enabled",
                        Component.translatable("cfg.kineticitem.prot.global.damage.immunity.enable"),
                        () -> ItemProtectionConfig.enableGlobalItemDamageImmunity,
                        value -> ItemProtectionConfig.enableGlobalItemDamageImmunity = value,
                        true,
                        Component.translatable("cfg.kineticitem.prot.global.damage.immunity.enable.tip")
                )
                .stringList(
                        "global_damage_immunity",
                        Component.translatable("cfg.kineticitem.prot.global.damage.immunity.list"),
                        () -> new ArrayList<>(ItemProtectionConfig.globalItemDamageImmunityRaw),
                        ItemProtectionConfig::setDamageSources,
                        List.of(),
                        Component.translatable("cfg.kineticitem.prot.global.damage.immunity.list.tip")
                )
                .booleanValue(
                        "global_direct_entity_immunity_enabled",
                        Component.translatable("cfg.kineticitem.prot.global.direct.entity.immunity.enable"),
                        () -> ItemProtectionConfig.enableGlobalDirectEntityImmunity,
                        value -> ItemProtectionConfig.enableGlobalDirectEntityImmunity = value,
                        true,
                        Component.translatable("cfg.kineticitem.prot.global.direct.entity.immunity.enable.tip")
                )
                .stringList(
                        "global_direct_entity_immunity",
                        Component.translatable("cfg.kineticitem.prot.global.direct.entity.immunity.list"),
                        () -> new ArrayList<>(ItemProtectionConfig.globalDirectEntityImmunityRaw),
                        ItemProtectionConfig::setDirectEntitySources,
                        List.of(),
                        Component.translatable("cfg.kineticitem.prot.global.direct.entity.immunity.list.tip")
                )
                .section(Component.translatable("cfg.kineticitem.editors.title"))
                .description(Component.translatable("cfg.kineticitem.editors.description"))
                .action(
                        "open_banitem_editor",
                        Component.translatable("cfg.kineticitem.editor.banitem"),
                        () -> ItemNetwork.requestOpenEditor(ItemNetwork.EDITOR_BAN_ITEM),
                        Component.translatable("cfg.kineticitem.editor.banitem.tooltip")
                )
                .action(
                        "open_mergeitem_editor",
                        Component.translatable("cfg.kineticitem.editor.mergeitem"),
                        () -> ItemNetwork.requestOpenEditor(ItemNetwork.EDITOR_MERGE_ITEM),
                        Component.translatable("cfg.kineticitem.editor.mergeitem.tooltip")
                )
                .build());
    }

    public static Screen create(Screen parent) {
        return KTConfigApi.createScreen(parent, PAGE_ID);
    }

}
