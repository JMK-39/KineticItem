package dev.xyat.kineticitem.item.mixin;

import com.google.gson.JsonElement;
import dev.xyat.kineticitem.item.InitItems;
import dev.xyat.kineticitem.item.config.BanItemConfig;
import dev.xyat.kineticitem.item.util.ItemBanControl;
import dev.xyat.kineticitem.item.util.JsonTraverser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class ItemManagementMixins {
    private static final Logger LOGGER = LogManager.getLogger("kineticitem/BanItem");

    @Mixin(Slot.class)
    public static class SlotMixin {
        @Inject(method = "set", at = @At("HEAD"))
        private void kineticitem$onItemSet(ItemStack stack, CallbackInfo ci) {
            kineticitem$sanitizeSlotStack(stack, "Slot#set");
        }

        @Inject(method = "setByPlayer", at = @At("HEAD"))
        private void kineticitem$onSetByPlayer(ItemStack stack, CallbackInfo ci) {
            kineticitem$sanitizeSlotStack(stack, "Slot#setByPlayer");
        }

        @Unique
        private void kineticitem$sanitizeSlotStack(ItemStack stack, String phase) {
            if (stack == null || stack.isEmpty()) return;
            try {
                if (stack.is(InitItems.VOID_PLACEHOLDER.get())) {
                    kineticitem$clearSlotStack(stack);
                    return;
                }
                if (ItemBanControl.shouldSkip()) return;
                String replacement = BanItemConfig.getReplacement(stack);
                if (BanItemConfig.VOID_ID.equals(replacement)) {
                    kineticitem$clearSlotStack(stack);
                }
            } catch (Throwable e) {
                LOGGER.error("槽位封禁检查异常，已跳过本次检查。phase={} stack={}", phase, stack, e);
            }
        }

        @Unique
        private void kineticitem$clearSlotStack(ItemStack stack) {
            stack.setCount(0);
        }
    }

    @Mixin(ItemStack.class)
    public static abstract class ItemStackMixin {
        @Inject(method = "forgeInit", at = @At("RETURN"), remap = false)
        private void kineticitem$interceptInit(CallbackInfo ci) {
        }

        @Inject(method = "setTag", at = @At("RETURN"))
        private void kineticitem$interceptSetTag(CompoundTag pTag, CallbackInfo ci) {
        }

        @Inject(method = "is(Lnet/minecraft/world/item/Item;)Z", at = @At("HEAD"), cancellable = true)
        private void kineticitem$isItem(Item pItem, CallbackInfoReturnable<Boolean> cir) {
            ItemStack self = (ItemStack) (Object) this;
            if (self.isEmpty() || pItem == null || pItem == Items.AIR) return;
            if (self.getItem() != pItem) {
                ResourceLocation inId = ForgeRegistries.ITEMS.getKey(pItem);
                if (inId != null) {
                    String replacementStr = BanItemConfig.getReplacement(inId.toString());
                    if (replacementStr != null && !BanItemConfig.VOID_ID.equals(replacementStr)) {
                        int bracket = replacementStr.indexOf('{');
                        String targetPureId = bracket == -1 ? replacementStr : replacementStr.substring(0, bracket);
                        ResourceLocation thisId = ForgeRegistries.ITEMS.getKey(self.getItem());
                        if (thisId != null && targetPureId.equals(thisId.toString())) {
                            cir.setReturnValue(true);
                        }
                    }
                }
            }
        }

        @Inject(method = "is(Lnet/minecraft/tags/TagKey;)Z", at = @At("HEAD"), cancellable = true)
        private void kineticitem$isMergedTag(TagKey<Item> tagKey, CallbackInfoReturnable<Boolean> cir) {
            ItemStack self = (ItemStack) (Object) this;
            if (!self.isEmpty() && BanItemConfig.hasMergedTag(self, tagKey)) {
                cir.setReturnValue(true);
            }
        }

        @Inject(method = "getTags", at = @At("RETURN"), cancellable = true)
        private void kineticitem$getMergedTags(CallbackInfoReturnable<Stream<TagKey<Item>>> cir) {
            ItemStack self = (ItemStack) (Object) this;
            if (self.isEmpty()) return;
            Set<TagKey<Item>> mergedTags = BanItemConfig.getMergedTagKeys(self);
            if (!mergedTags.isEmpty()) {
                Stream<TagKey<Item>> original = cir.getReturnValue();
                if (original == null) original = Stream.empty();
                cir.setReturnValue(Stream.concat(original, mergedTags.stream()).distinct());
            }
        }
    }

    @Mixin(SimpleJsonResourceReloadListener.class)
    public static class SimpleJsonResourceReloadListenerMixin {
        @Inject(method = "prepare*", at = @At("RETURN"))
        private void kineticitem$replaceJsonData(ResourceManager rm, ProfilerFiller pf, CallbackInfoReturnable<Map<ResourceLocation, JsonElement>> cir) {
            Map<ResourceLocation, JsonElement> map = cir.getReturnValue();
            if (map != null && !BanItemConfig.ruleReplacementMap.isEmpty()) {
                Map<String, String> stringRules = new java.util.HashMap<>();
                BanItemConfig.ruleReplacementMap.forEach((rule, target) -> {
                    if (!rule.hasNbt && !BanItemConfig.VOID_ID.equals(target)) {
                        int bracket = target.indexOf('{');
                        String pureTarget = bracket == -1 ? target : target.substring(0, bracket);
                        stringRules.put(rule.baseId, pureTarget);
                    }
                });
                if (!stringRules.isEmpty()) {
                    for (JsonElement element : map.values()) {
                        try {
                            JsonTraverser.replaceIds(element, stringRules);
                        } catch (Throwable e) {
                            LOGGER.error("数据包 JSON ID 替换失败，已跳过单个 JSON", e);
                        }
                    }
                }
            }
        }
    }
}
