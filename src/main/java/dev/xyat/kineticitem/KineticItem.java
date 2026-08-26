package dev.xyat.kineticitem;

import com.mojang.logging.LogUtils;
import dev.xyat.kineticitem.item.InitItems;
import dev.xyat.kineticitem.item.command.ItemCommandExtension;
import dev.xyat.kineticitem.item.config.BanItemConfig;
import dev.xyat.kineticitem.item.config.ItemProtectionConfig;
import dev.xyat.kineticitem.item.config.ItemProtectionConfigGui;
import dev.xyat.kineticitem.item.network.ItemNetwork;
import dev.xyat.kineticcore.config.server.KTServerConfigApi;
import dev.xyat.kineticcore.config.server.KTServerConfigSpec;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(KineticItem.MODID)
public final class KineticItem {
    public static final String MODID = "kineticitem";
    public static final Logger LOGGER = LogUtils.getLogger();

    public KineticItem(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        InitItems.ITEMS.register(modEventBus);
        BanItemConfig.load();
        ItemProtectionConfig.load();
        KTServerConfigApi.register(KTServerConfigSpec.builder("kineticitem:item_protection")
                .booleanValue("enable_item_protection", () -> ItemProtectionConfig.enableItemProtection, value -> ItemProtectionConfig.enableItemProtection = value)
                .booleanValue("enable_void_salvage", () -> ItemProtectionConfig.enableVoidSalvage, value -> ItemProtectionConfig.enableVoidSalvage = value)
                .stringList("indestructible_items", () -> new java.util.ArrayList<>(ItemProtectionConfig.indestructibleItemsRaw), ItemProtectionConfig::setProtectionRules)
                .booleanValue("global_damage_immunity_enabled", () -> ItemProtectionConfig.enableGlobalItemDamageImmunity, value -> ItemProtectionConfig.enableGlobalItemDamageImmunity = value)
                .stringList("global_damage_immunity", () -> new java.util.ArrayList<>(ItemProtectionConfig.globalItemDamageImmunityRaw), ItemProtectionConfig::setDamageSources)
                .booleanValue("global_direct_entity_immunity_enabled", () -> ItemProtectionConfig.enableGlobalDirectEntityImmunity, value -> ItemProtectionConfig.enableGlobalDirectEntityImmunity = value)
                .stringList("global_direct_entity_immunity", () -> new java.util.ArrayList<>(ItemProtectionConfig.globalDirectEntityImmunityRaw), ItemProtectionConfig::setDirectEntitySources)
                .onSave(ItemProtectionConfig::save)
                .build());
        ItemNetwork.register();
        ItemCommandExtension.install();

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ItemProtectionConfigGui.load());
    }
}
