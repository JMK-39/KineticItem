package dev.xyat.kineticitem.item.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.Cancelable;

/**
 * 物品实体受伤事件。
 * <p>
 * 触发时机：ItemEntity.hurt() 方法头部。
 * 作用：允许拦截或修改物品受到的伤害。
 * <p>
 * 如果取消此事件 (setCanceled(true))，物品将不会受到伤害。
 */
@Cancelable
public class ItemEntityDamageEvent extends EntityEvent {
    private final DamageSource source;
    private final float amount;

    public ItemEntityDamageEvent(ItemEntity entity, DamageSource source, float amount) {
        super(entity);
        this.source = source;
        this.amount = amount;
    }

    @Override
    public ItemEntity getEntity() {
        return (ItemEntity) super.getEntity();
    }

    public DamageSource getSource() {
        return source;
    }

    public float getAmount() {
        return amount;
    }
}