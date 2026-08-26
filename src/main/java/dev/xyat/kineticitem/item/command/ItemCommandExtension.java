package dev.xyat.kineticitem.item.command;

import dev.xyat.kineticcore.command.KTCommandApi;
import dev.xyat.kineticcore.command.KTCommandExtension;
import dev.xyat.kineticitem.KineticItem;
import dev.xyat.kineticitem.item.config.BanItemConfig;
import dev.xyat.kineticitem.item.config.ItemProtectionConfig;
import dev.xyat.kineticitem.item.network.ItemNetwork;
import net.minecraft.commands.CommandSourceStack;

public final class ItemCommandExtension implements KTCommandExtension {
    private ItemCommandExtension() {
    }

    public static void install() {
        KTCommandApi.register(KineticItem.MODID, new ItemCommandExtension());
    }

    @Override
    public void reload(CommandSourceStack source) {
        BanItemConfig.load();
        ItemProtectionConfig.load();
        ItemNetwork.syncServerConfigToAllPlayers();
    }
}
