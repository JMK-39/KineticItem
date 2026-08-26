package dev.xyat.kineticitem.item.mixin.client;

import dev.xyat.kineticitem.item.config.BanItemConfig;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Set;

@Mixin(CreativeModeTab.class)
public abstract class CreativeModeTabMixin {
    private static final Logger LOGGER = LogManager.getLogger("kineticitem/BanItemCreativeTab");

    @Shadow
    private Collection<ItemStack> displayItems;

    @Shadow
    private Set<ItemStack> displayItemsSearchTab;

    @Inject(method = "buildContents", at = @At("RETURN"))
    private void kineticitem$filterBannedDisplayItems(CreativeModeTab.ItemDisplayParameters parameters, CallbackInfo ci) {
        try {
            this.displayItems = kineticitem$filterBannedItems(this.displayItems);
            this.displayItemsSearchTab = kineticitem$filterBannedItems(this.displayItemsSearchTab);
        } catch (Throwable e) {
            LOGGER.error("过滤创造模式标签页封禁物品失败，已保留原始标签页内容", e);
        }
    }

    private static Set<ItemStack> kineticitem$filterBannedItems(Collection<ItemStack> source) {
        Set<ItemStack> result = ItemStackLinkedSet.createTypeAndTagSet();
        if (source == null || source.isEmpty()) return result;
        for (ItemStack stack : source) {
            if (stack == null || stack.isEmpty()) continue;
            if (!BanItemConfig.isBanned(stack)) {
                result.add(stack);
            }
        }
        return result;
    }
}
