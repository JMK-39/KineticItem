package dev.xyat.kineticitem.kubejs;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.fml.ModList;

public final class KineticItemKubeJSCompat {
    private KineticItemKubeJSCompat() {
    }

    public static boolean postItemRemoved(ItemEntity entity) {
        if (!ModList.get().isLoaded("kubejs")) {
            return false;
        }
        return Proxy.postItemRemoved(entity);
    }

    private static final class Proxy {
        private Proxy() {
        }

        private static boolean postItemRemoved(ItemEntity entity) {
            return KineticItemKubeJSPlugin.postItemRemoved(entity);
        }
    }
}
