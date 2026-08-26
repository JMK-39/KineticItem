package dev.xyat.kineticitem.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VoidPlaceholderItem extends Item {
    public VoidPlaceholderItem() {
        super(new Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC));
    }

    /**
     * 添加物品说明 (Tips)
     */
    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        // 第一行：警告这是禁用物品 (红色)
        tooltip.add(Component.translatable("tip.kineticitem.void_placeholder.warning"));
        // 第二行：说明无法获取和使用 (灰色)
        tooltip.add(Component.translatable("tip.kineticitem.void_placeholder.usage"));
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && entity instanceof Player player) {
            if (!player.isCreative()) {
                stack.setCount(0);
            }
        }
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        return player.isCreative();
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (entity.getOwner() == null || (entity.getOwner() instanceof Player p && !p.isCreative())) {
            entity.discard();
            return true;
        }
        return false;
    }
}