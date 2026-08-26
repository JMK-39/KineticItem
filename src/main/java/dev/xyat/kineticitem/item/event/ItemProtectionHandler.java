package dev.xyat.kineticitem.item.event;

import dev.xyat.kineticitem.KineticItem;
import dev.xyat.kineticitem.item.config.ItemProtectionConfig;
import dev.xyat.kineticitem.item.util.ItemProtectionList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KineticItem.MODID)
public class ItemProtectionHandler {

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (!ItemProtectionConfig.enableItemProtection || event.getLevel().isClientSide) return;

        if (event.getEntity() instanceof ItemEntity itemEntity) {
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) return;

            ItemProtectionConfig.ProtectionRule rule = ItemProtectionConfig.getProtectionRule(stack);
            if (rule != null) {
                itemEntity.setUnlimitedLifetime();

                CompoundTag entityData = new CompoundTag();
                itemEntity.saveWithoutId(entityData);
                boolean hasOwner = entityData.hasUUID("Owner") || entityData.hasUUID("Thrower");

                if (!hasOwner) {
                    if (rule.noGravity) {
                        itemEntity.setNoGravity(true);
                        itemEntity.setDeltaMovement(Vec3.ZERO);
                    }
                    if (rule.glowing) {
                        itemEntity.setGlowingTag(true);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onItemDamage(ItemEntityDamageEvent event) {
        ItemEntity itemEntity = event.getEntity();
        ItemStack stack = itemEntity.getItem();
        if (stack.isEmpty()) return;

        DamageSource source = event.getSource();

        if (ItemProtectionConfig.isGlobalDirectEntityImmune(source)) {
            event.setCanceled(true);
            return;
        }

        if (ItemProtectionConfig.isGlobalItemDamageImmune(source)) {
            event.setCanceled(true);
            return;
        }

        Level level = itemEntity.level();
        String damageId = ItemProtectionConfig.getDamageSourceId(source);

        ItemProtectionConfig.ProtectionRule rule = ItemProtectionConfig.getProtectionRule(stack);
        boolean isProtected = (rule != null) || ItemProtectionList.isFireImmune(stack);

        if (!isProtected) return;

        if (damageId.equals("minecraft:out_of_world") || source.getMsgId().equals("outOfWorld")) {
            if (ItemProtectionConfig.enableVoidSalvage) {
                event.setCanceled(true);

                int safeY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, itemEntity.getBlockX(), itemEntity.getBlockZ());

                if (safeY > level.getMinBuildHeight()) {
                    itemEntity.teleportTo(itemEntity.getX(), safeY + 1.5, itemEntity.getZ());
                } else {
                    BlockPos spawn = level.getSharedSpawnPos();
                    itemEntity.teleportTo(spawn.getX(), spawn.getY() + 1.0, spawn.getZ());
                }

                itemEntity.setDeltaMovement(Vec3.ZERO);
                KineticItem.LOGGER.debug("Void Salvage: Teleported protected item {} to safety", stack.getHoverName().getString());
                return;
            }
        }

        if (ItemProtectionList.isFireImmune(stack) && source.is(DamageTypeTags.IS_FIRE)) {
            event.setCanceled(true);
            return;
        }

        if (rule != null) {
            if (rule.fireImmune && source.is(DamageTypeTags.IS_FIRE)) {
                event.setCanceled(true);
            } else if (rule.explosionImmune && source.is(DamageTypeTags.IS_EXPLOSION)) {
                event.setCanceled(true);
            }
        }
    }
}