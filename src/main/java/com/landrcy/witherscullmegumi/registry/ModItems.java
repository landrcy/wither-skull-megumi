package com.landrcy.witherscullmegumi.registry;

import com.landrcy.witherscullmegumi.WitherScullMegumiMod;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(WitherScullMegumiMod.MODID);

    public static final DeferredItem<SpawnEggItem> MEGUMI_WITHER_SKELETON_SPAWN_EGG =
            ITEMS.register("megumi_wither_skeleton_spawn_egg", () ->
                    new DeferredSpawnEggItem(
                            ModEntities.MEGUMI_WITHER_SKELETON,
                            0x141620,
                            0x6E90C8,
                            new Item.Properties()));
}
