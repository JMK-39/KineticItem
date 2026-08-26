package dev.xyat.kineticitem.item.client.gui;

import dev.xyat.kineticitem.KineticItem;
import dev.xyat.kineticcore.api.client.ItemCache;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = KineticItem.MODID, value = Dist.CLIENT)
public class ItemCacheHudRenderer {

    @SubscribeEvent
    public static void onPlayerJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide && event.getEntity() == Minecraft.getInstance().player) {
            ItemCache.clear();
            ItemSearchCache.clear();
        }
    }

    public static Component getDisplayNameCustom(ItemStack stack) {
        if (stack.getItem() == net.minecraft.world.item.Items.ENCHANTED_BOOK) {
            try {
                List<Component> lines = stack.getTooltipLines(Minecraft.getInstance().player, TooltipFlag.Default.NORMAL);
                if (lines.size() > 1) return Component.translatable("gui.kineticitem.common.tooltip_pair", lines.get(0), lines.get(1));
            } catch (Exception ignored) {}
        }
        return stack.getHoverName();
    }
}
