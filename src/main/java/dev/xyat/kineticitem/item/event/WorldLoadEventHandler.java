package dev.xyat.kineticitem.item.event;

import dev.xyat.kineticitem.KineticItem;
import dev.xyat.kineticitem.item.config.BanItemConfig;
import dev.xyat.kineticitem.item.network.ItemNetwork;
import dev.xyat.kineticitem.item.util.ItemBanControl;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = KineticItem.MODID)
public class WorldLoadEventHandler {

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (!ItemBanControl.isReplacementEnabled()) {
            ItemBanControl.setReplacementEnabled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            // 当玩家登录时，服务端将配置强行打包同步给该玩家客户端
            String jsonData = BanItemConfig.GSON.toJson(BanItemConfig.data);
            ItemNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new ItemNetwork.SyncBanConfigPacket(jsonData));
        }
    }
}