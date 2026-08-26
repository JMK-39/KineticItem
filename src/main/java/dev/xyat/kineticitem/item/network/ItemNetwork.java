package dev.xyat.kineticitem.item.network;

import dev.xyat.kineticcore.api.NetworkCompressUtil;
import dev.xyat.kineticitem.KineticItem;
import dev.xyat.kineticitem.item.config.BanItemConfig;
import dev.xyat.kineticitem.item.config.ItemProtectionConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = KineticItem.MODID)
public class ItemNetwork {
    private static final Logger LOGGER = LogManager.getLogger("kineticitem/ItemNetwork");
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(KineticItem.MODID, "item_network"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static final int EDITOR_BAN_ITEM = 0;
    public static final int EDITOR_MERGE_ITEM = 1;

    private static final int MAX_PROTECTION_RULES = 4096;
    private static final int MAX_PROTECTION_RULE_LENGTH = 32767;

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, OpenBannedGuiPacket.class, OpenBannedGuiPacket::encode, OpenBannedGuiPacket::decode, OpenBannedGuiPacket::handle);
        CHANNEL.registerMessage(id++, OpenMergeGuiPacket.class, OpenMergeGuiPacket::encode, OpenMergeGuiPacket::decode, OpenMergeGuiPacket::handle);
        CHANNEL.registerMessage(id++, SaveBanConfigPacket.class, SaveBanConfigPacket::encode, SaveBanConfigPacket::decode, SaveBanConfigPacket::handle);
        CHANNEL.registerMessage(id++, SyncBanConfigPacket.class, SyncBanConfigPacket::encode, SyncBanConfigPacket::decode, SyncBanConfigPacket::handle);
        CHANNEL.registerMessage(id++, RequestOpenEditorPacket.class, RequestOpenEditorPacket::encode, RequestOpenEditorPacket::decode, RequestOpenEditorPacket::handle);
        CHANNEL.registerMessage(id, SaveProtectionRulesPacket.class, SaveProtectionRulesPacket::encode, SaveProtectionRulesPacket::decode, SaveProtectionRulesPacket::handle);
    }

    public static void requestOpenEditor(int editorType) {
        CHANNEL.sendToServer(new RequestOpenEditorPacket(editorType));
    }

    public static void saveProtectionRules(List<String> rules) {
        CHANNEL.sendToServer(new SaveProtectionRulesPacket(rules));
    }

    public record RequestOpenEditorPacket(int editorType) {
        public static void encode(RequestOpenEditorPacket msg, FriendlyByteBuf buf) {
            buf.writeVarInt(msg.editorType);
        }

        public static RequestOpenEditorPacket decode(FriendlyByteBuf buf) {
            return new RequestOpenEditorPacket(buf.readVarInt());
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null || !player.hasPermissions(2)) return;
                if (editorType == EDITOR_BAN_ITEM) {
                    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenBannedGuiPacket());
                } else if (editorType == EDITOR_MERGE_ITEM) {
                    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenMergeGuiPacket());
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendServerConfigToPlayer(player, true);
        }
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player != null) {
            sendServerConfigToPlayer(player, true);
            return;
        }
        BanItemConfig.load();
        String json = BanItemConfig.getNetworkJson();
        for (ServerPlayer target : event.getPlayerList().getPlayers()) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> target), new SyncBanConfigPacket(json));
        }
    }

    public static void sendServerConfigToPlayer(ServerPlayer player, boolean reloadFromFile) {
        if (player == null) return;
        try {
            if (reloadFromFile) BanItemConfig.load();
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncBanConfigPacket(BanItemConfig.getNetworkJson()));
        } catch (Throwable e) {
            LOGGER.error("Failed to sync item configuration to player {}", player.getGameProfile().getName(), e);
        }
    }

    public static void syncServerConfigToAllPlayers() {
        try {
            CHANNEL.send(PacketDistributor.ALL.noArg(), new SyncBanConfigPacket(BanItemConfig.getNetworkJson()));
        } catch (Throwable e) {
            LOGGER.error("Failed to sync item configuration to all players", e);
        }
    }

    public static class OpenBannedGuiPacket {
        public static void encode(OpenBannedGuiPacket msg, FriendlyByteBuf buf) {
        }

        public static OpenBannedGuiPacket decode(FriendlyByteBuf buf) {
            return new OpenBannedGuiPacket();
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> dev.xyat.kineticitem.item.client.ItemClientProxy::openBannedGui));
            ctx.get().setPacketHandled(true);
        }
    }

    public static class OpenMergeGuiPacket {
        public static void encode(OpenMergeGuiPacket msg, FriendlyByteBuf buf) {
        }

        public static OpenMergeGuiPacket decode(FriendlyByteBuf buf) {
            return new OpenMergeGuiPacket();
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> dev.xyat.kineticitem.item.client.ItemClientProxy::openMergeGui));
            ctx.get().setPacketHandled(true);
        }
    }

    public static class SaveBanConfigPacket {
        private final String jsonData;

        public SaveBanConfigPacket(String jsonData) {
            this.jsonData = jsonData;
        }

        public static void encode(SaveBanConfigPacket msg, FriendlyByteBuf buf) {
            buf.writeByteArray(NetworkCompressUtil.compress(msg.jsonData));
        }

        public static SaveBanConfigPacket decode(FriendlyByteBuf buf) {
            try {
                return new SaveBanConfigPacket(NetworkCompressUtil.decompress(buf.readByteArray()));
            } catch (Throwable e) {
                LOGGER.error("Failed to decode item configuration save packet", e);
                return new SaveBanConfigPacket("");
            }
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null || !player.hasPermissions(2)) return;
                if (BanItemConfig.applyJson(jsonData, "server packet from " + player.getGameProfile().getName(), true)) {
                    syncServerConfigToAllPlayers();
                } else {
                    sendServerConfigToPlayer(player, false);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static final class SaveProtectionRulesPacket {
        private final List<String> rules;

        public SaveProtectionRulesPacket(List<String> rules) {
            this.rules = rules == null ? List.of() : new ArrayList<>(rules);
        }

        public static void encode(SaveProtectionRulesPacket msg, FriendlyByteBuf buf) {
            if (msg.rules.size() > MAX_PROTECTION_RULES) throw new IllegalArgumentException("Too many protection rules");
            buf.writeVarInt(msg.rules.size());
            for (String rule : msg.rules) {
                String value = rule == null ? "" : rule;
                if (value.length() > MAX_PROTECTION_RULE_LENGTH) throw new IllegalArgumentException("Protection rule is too long");
                buf.writeUtf(value, MAX_PROTECTION_RULE_LENGTH);
            }
        }

        public static SaveProtectionRulesPacket decode(FriendlyByteBuf buf) {
            int size = buf.readVarInt();
            if (size < 0 || size > MAX_PROTECTION_RULES) throw new IllegalArgumentException("Invalid protection rule count");
            List<String> rules = new ArrayList<>(size);
            for (int i = 0; i < size; i++) rules.add(buf.readUtf(MAX_PROTECTION_RULE_LENGTH));
            return new SaveProtectionRulesPacket(rules);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null || !player.hasPermissions(2) || !ItemProtectionConfig.areValidProtectionRules(rules)) return;
                List<String> previous = new ArrayList<>(ItemProtectionConfig.indestructibleItemsRaw);
                try {
                    ItemProtectionConfig.indestructibleItemsRaw = new ArrayList<>(rules);
                    ItemProtectionConfig.save();
                } catch (Throwable e) {
                    ItemProtectionConfig.indestructibleItemsRaw = previous;
                    ItemProtectionConfig.load();
                    LOGGER.error("Failed to save item protection rules", e);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class SyncBanConfigPacket {
        private final String jsonData;

        public SyncBanConfigPacket(String jsonData) {
            this.jsonData = jsonData;
        }

        public static void encode(SyncBanConfigPacket msg, FriendlyByteBuf buf) {
            buf.writeByteArray(NetworkCompressUtil.compress(msg.jsonData));
        }

        public static SyncBanConfigPacket decode(FriendlyByteBuf buf) {
            try {
                return new SyncBanConfigPacket(NetworkCompressUtil.decompress(buf.readByteArray()));
            } catch (Throwable e) {
                LOGGER.error("Failed to decode item configuration sync packet", e);
                return new SyncBanConfigPacket("");
            }
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> dev.xyat.kineticitem.item.client.ItemClientProxy.handleSyncBanConfig(jsonData)));
            ctx.get().setPacketHandled(true);
        }
    }
}
