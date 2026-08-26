package dev.xyat.kineticitem.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.xyat.kineticitem.item.event.ItemEntityDamageEvent;
import dev.xyat.kineticitem.item.util.ItemProtectionList;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class KineticItemKubeJSPlugin extends dev.latvian.mods.kubejs.KubeJSPlugin {
    public static final EventGroup GROUP = EventGroup.of("kineticitemEvents");

    private static EventHandler itemHurt;
    private static EventHandler itemSpawn;
    private static EventHandler itemRemoved;

    @Override
    public void init() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void registerEvents() {
        itemHurt = GROUP.server("itemHurt", () -> ItemEntityDamageEventJS.class).hasResult();
        itemSpawn = GROUP.server("itemSpawn", () -> ItemEntityDamageEventJS.class).hasResult();
        itemRemoved = GROUP.server("itemRemoved", () -> ItemEntityDamageEventJS.class).hasResult();
        GROUP.register();
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        event.add("ItemProtection", ItemProtectionList.class);
    }

    @SubscribeEvent
    public void onItemHurt(ItemEntityDamageEvent event) {
        if (itemHurt != null && itemHurt.hasListeners()) {
            if (itemHurt.post(new ItemEntityDamageEventJS(event)).override()) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onEntitySpawn(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ItemEntity item
                && itemSpawn != null
                && itemSpawn.hasListeners()
                && itemSpawn.post(new ItemEntityDamageEventJS(item)).override()) {
            event.setCanceled(true);
        }
    }

    public static boolean postItemRemoved(ItemEntity entity) {
        return itemRemoved != null
                && itemRemoved.hasListeners()
                && itemRemoved.post(new ItemEntityDamageEventJS(entity)).override();
    }
}
