package dev.xyat.kineticitem.item;

import dev.xyat.kineticitem.KineticItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class InitItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, KineticItem.MODID);

    // Register void placeholder
    public static final RegistryObject<Item> VOID_PLACEHOLDER = ITEMS.register("void_placeholder", VoidPlaceholderItem::new);
}