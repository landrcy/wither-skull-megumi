package com.landrcy.witherscullmegumi;

import com.landrcy.witherscullmegumi.entity.MegumiSummonedWitherEntity;
import com.landrcy.witherscullmegumi.entity.MegumiWitherSkeletonEntity;
import com.landrcy.witherscullmegumi.network.ClientPayloadBridge;
import com.landrcy.witherscullmegumi.network.RitualSyncPayload;
import com.landrcy.witherscullmegumi.registry.ModEntities;
import com.landrcy.witherscullmegumi.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

@Mod(WitherScullMegumiMod.MODID)
public class WitherScullMegumiMod {
    public static final String MODID = "wither_scull_megumi";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WitherScullMegumiMod(IEventBus modEventBus, ModContainer modContainer) {
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);

        modEventBus.addListener(this::registerAttributes);
        modEventBus.addListener(this::addCreativeTabs);
        modEventBus.addListener(this::registerPayloads);

        LOGGER.info("Initialized Wither Scull Megumi ({})", MODID);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.MEGUMI_WITHER_SKELETON.get(), MegumiWitherSkeletonEntity.createAttributes().build());
        event.put(ModEntities.MEGUMI_WITHER.get(), MegumiSummonedWitherEntity.createAttributes().build());
    }

    private void addCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.MEGUMI_WITHER_SKELETON_SPAWN_EGG.get());
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                RitualSyncPayload.TYPE,
                RitualSyncPayload.STREAM_CODEC,
                ClientPayloadBridge::handleRitualSync
        );
    }
}
