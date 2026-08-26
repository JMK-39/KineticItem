package dev.xyat.kineticitem.item.client;

import com.mojang.logging.LogUtils;
import dev.xyat.kineticitem.KineticItem;
import dev.xyat.kineticitem.item.client.gui.BannedItemScreen;
import dev.xyat.kineticitem.item.client.gui.ItemSearchCache;
import dev.xyat.kineticitem.item.client.gui.MergeItemScreen;
import dev.xyat.kineticitem.item.config.BanItemConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;

@Mod.EventBusSubscriber(modid = KineticItem.MODID, value = Dist.CLIENT)
public class ItemClientProxy {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean pendingTabRefresh = false;

    public static void openBannedGui() {
        ItemSearchCache.prepareCache(() -> Minecraft.getInstance().setScreen(new BannedItemScreen()));
    }

    public static void openMergeGui() {
        ItemSearchCache.prepareCache(() -> Minecraft.getInstance().setScreen(new MergeItemScreen()));
    }

    public static void handleSyncBanConfig(String jsonData) {
        try {
            boolean ok = BanItemConfig.applyJson(jsonData, "client sync packet", false);
            if (!ok) {
                LOGGER.warn("客户端同步配置被拒绝，保留当前内存配置");
                return;
            }
            ItemSearchCache.clear();

            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null) {
                rebuildCreativeTabs();
            } else {
                pendingTabRefresh = true;
            }
        } catch (Throwable e) {
            LOGGER.error("同步物品封禁数据时发生异常", e);
        }
    }

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        if (pendingTabRefresh) {
            pendingTabRefresh = false;
            rebuildCreativeTabs();
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        try {
            BanItemConfig.load();
            ItemSearchCache.clear();
            pendingTabRefresh = false;
        } catch (Throwable e) {
            LOGGER.error("离开服务器后重载本地物品封禁配置失败", e);
        }
    }

    private static void rebuildCreativeTabs() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        try {
            CreativeModeTab.ItemDisplayParameters params = new CreativeModeTab.ItemDisplayParameters(
                    mc.player.connection.enabledFeatures(),
                    mc.options.operatorItemsTab().get(),
                    mc.level.registryAccess()
            );

            for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
                tab.buildContents(params);
            }

            if (mc.screen instanceof CreativeModeInventoryScreen) {
                mc.setScreen(new CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), mc.options.operatorItemsTab().get()));
            }
        } catch (Throwable e) {
            LOGGER.error("重载创造模式标签页时发生异常", e);
        }
    }
}
