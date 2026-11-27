package com.landrcy.impossiblepack.registry;

import com.landrcy.impossiblepack.TheImpossiblePackMod;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TheImpossiblePackMod.MODID);

    public static final DeferredItem<SpawnEggItem> MEGUMI_WITHER_SKELETON_SPAWN_EGG =
            ITEMS.registerItem(
                    "megumi_wither_skeleton_spawn_egg",
                    SpawnEggItem::new,
                    props -> props.spawnEgg(ModEntities.MEGUMI_WITHER_SKELETON.get())
            );

    public static final DeferredItem<SpawnEggItem> MAHORAGA_WITHER_SPAWN_EGG =
            ITEMS.registerItem(
                    "mahoraga_wither_spawn_egg",
                    SpawnEggItem::new,
                    props -> props.spawnEgg(ModEntities.MAHORAGA_WITHER.get())
            );
}
