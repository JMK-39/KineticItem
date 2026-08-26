package dev.xyat.kineticitem.kubejs;

import dev.latvian.mods.kubejs.entity.EntityEventJS;
import dev.latvian.mods.kubejs.event.EventExit;
import dev.xyat.kineticitem.item.event.ItemEntityDamageEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Optional;

public class ItemEntityDamageEventJS extends EntityEventJS {
    private final ItemEntity itemEntity;
    @Nullable
    private final ItemEntityDamageEvent damageEvent;

    public ItemEntityDamageEventJS(ItemEntityDamageEvent event) {
        this.itemEntity = event.getEntity();
        this.damageEvent = event;
    }

    public ItemEntityDamageEventJS(ItemEntity entity) {
        this.itemEntity = entity;
        this.damageEvent = null;
    }

    @Override
    public ItemEntity getEntity() {
        return itemEntity;
    }

    public ItemStack getItem() {
        return itemEntity.getItem();
    }

    public Level getLevel() {
        return itemEntity.level();
    }

    public double getX() {
        return itemEntity.getX();
    }

    public double getY() {
        return itemEntity.getY();
    }

    public double getZ() {
        return itemEntity.getZ();
    }

    public float getAmount() {
        return damageEvent != null ? damageEvent.getAmount() : 0f;
    }

    @Nullable
    public DamageSource getDamageSource() {
        return damageEvent != null ? damageEvent.getSource() : null;
    }
    public String getDamageType() {
        DamageSource source = getDamageSource();
        if (source == null) return "none";

        Optional<ResourceKey<DamageType>> key = source.typeHolder().unwrapKey();
        return key.map(resourceKey -> resourceKey.location().toString()).orElse(source.getMsgId());
    }
    public String getDamageMsgId() {
        DamageSource source = getDamageSource();
        return source != null ? source.getMsgId() : "none";
    }

    @Nullable
    public Entity getDirectEntity() {
        DamageSource source = getDamageSource();
        return source != null ? source.getDirectEntity() : null;
    }

    @Nullable
    public Entity getSourceEntity() {
        DamageSource source = getDamageSource();
        return source != null ? source.getEntity() : null;
    }
    public String getDirectEntityType() {
        Entity entity = getDirectEntity();
        if (entity == null) return "none";

        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return id != null ? id.toString() : "none";
    }
    public String getSourceEntityType() {
        Entity entity = getSourceEntity();
        if (entity == null) return "none";

        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return id != null ? id.toString() : "none";
    }
    public boolean isDamageType(String id) {
        return getDamageType().equals(id);
    }
    public boolean isDirectEntity(String id) {
        return getDirectEntityType().equals(id);
    }
    public boolean isSourceEntity(String id) {
        return getSourceEntityType().equals(id);
    }
    public boolean isDirectEntityFromMod(String modId) {
        Entity entity = getDirectEntity();
        if (entity == null) return false;

        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return id != null && id.getNamespace().equals(modId);
    }
    public boolean isSourceEntityFromMod(String modId) {
        Entity entity = getSourceEntity();
        if (entity == null) return false;

        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return id != null && id.getNamespace().equals(modId);
    }
    public boolean isFire() {
        return damageEvent != null && damageEvent.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE);
    }
    public boolean isExplosion() {
        return damageEvent != null && damageEvent.getSource().is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION);
    }
    public boolean isCactus() {
        return damageEvent != null && damageEvent.getSource().is(net.minecraft.world.damagesource.DamageTypes.CACTUS);
    }

    @Override
    public Object cancel() throws EventExit {
        if (damageEvent != null && damageEvent.isCancelable()) {
            damageEvent.setCanceled(true);
        }
        return super.cancel();
    }
}