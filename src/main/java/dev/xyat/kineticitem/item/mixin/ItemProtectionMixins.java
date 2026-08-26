package dev.xyat.kineticitem.item.mixin;

import dev.xyat.kineticitem.kubejs.KineticItemKubeJSCompat;
import dev.xyat.kineticitem.item.config.ItemProtectionConfig;
import dev.xyat.kineticitem.item.event.ItemEntityDamageEvent;
import dev.xyat.kineticitem.item.util.ItemProtectionList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 掉落物保护系统 Mixin 汇总
 */
public class ItemProtectionMixins {

    // 1. ItemEntity Tweaks (掉落物实体核心逻辑)
    @Mixin(ItemEntity.class)
    public static abstract class ItemEntityTweaks extends Entity {

        public ItemEntityTweaks(EntityType<?> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
        }

        @Shadow public abstract ItemStack getItem();

        @Override
        protected void onBelowWorld() {
            // 尝试造成虚空伤害以触发下方的 hurt 逻辑，用于虚空救援
            if (this.hurt(this.damageSources().fellOutOfWorld(), 4.0F)) {
                super.onBelowWorld();
            }
        }

        /** 核心防火逻辑检查 */
        @Inject(method = "fireImmune", at = @At("HEAD"), cancellable = true)
        private void kineticitem$checkFireImmune(CallbackInfoReturnable<Boolean> cir) {
            ItemStack stack = this.getItem();
            if (ItemProtectionList.isFireImmune(stack)) {
                cir.setReturnValue(true);
                return;
            }
            if (ItemProtectionConfig.enableItemProtection) {
                ItemProtectionConfig.ProtectionRule rule = ItemProtectionConfig.getProtectionRule(stack);
                if (rule != null && rule.fireImmune) {
                    cir.setReturnValue(true);
                }
            }
        }

        /** 注入物品受伤逻辑 */
        @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
        private void kineticitem$onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
            ItemEntityDamageEvent event = new ItemEntityDamageEvent((ItemEntity) (Object) this, source, amount);
            if (MinecraftForge.EVENT_BUS.post(event)) {
                cir.setReturnValue(false);
            }
        }
    }

    // 2. Entity Tweaks (底层实体通用逻辑拦截)
    @Mixin(Entity.class)
    public static abstract class EntityTweaks {

        /** 阻止受保护物品产生视觉上的起火效果 */
        @Inject(method = "isOnFire", at = @At("HEAD"), cancellable = true)
        private void kineticitem$suppressVisualFire(CallbackInfoReturnable<Boolean> cir) {
            if ((Object) this instanceof ItemEntity itemEntity) {
                if (ItemProtectionList.isFireImmune(itemEntity.getItem())) {
                    cir.setReturnValue(false);
                    return;
                }

                if (ItemProtectionConfig.enableItemProtection) {
                    ItemProtectionConfig.ProtectionRule rule = ItemProtectionConfig.getProtectionRule(itemEntity.getItem());
                    if (rule != null && rule.fireImmune) {
                        cir.setReturnValue(false);
                    }
                }
            }
        }

        /** 注入物品消失/移除事件 */
        @Inject(method = "discard", at = @At("HEAD"), cancellable = true)
        private void kineticitem$onDiscard(CallbackInfo ci) {
            if ((Object) this instanceof ItemEntity itemEntity) {
                if (KineticItemKubeJSCompat.postItemRemoved(itemEntity)) {
                    ci.cancel();
                }
            }
        }
    }
}