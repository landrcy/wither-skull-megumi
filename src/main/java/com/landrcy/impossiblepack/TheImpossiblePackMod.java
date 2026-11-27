package com.landrcy.impossiblepack;

import com.landrcy.impossiblepack.entity.MahoragaWitherEntity;
import com.landrcy.impossiblepack.entity.MegumiWitherSkeletonEntity;
import com.landrcy.impossiblepack.network.MobVfxPayload;
import com.landrcy.impossiblepack.network.RitualSyncPayload;
import com.landrcy.impossiblepack.registry.ModEntities;
import com.landrcy.impossiblepack.registry.ModItems;
import com.landrcy.impossiblepack.registry.ModParticles;
import com.landrcy.impossiblepack.registry.ModSounds;
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

@Mod(TheImpossiblePackMod.MODID)
public class TheImpossiblePackMod {
    public static final String MODID = "the_impossible_pack";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TheImpossiblePackMod(IEventBus modEventBus, ModContainer modContainer) {
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModParticles.PARTICLE_TYPES.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);

        modEventBus.addListener(this::registerAttributes);
        modEventBus.addListener(this::addCreativeTabs);
        modEventBus.addListener(this::registerPayloads);

        LOGGER.info("Initialized The Impossible Pack ({}) for Minecraft 26.3", MODID);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.MEGUMI_WITHER_SKELETON.get(), MegumiWitherSkeletonEntity.createAttributes().build());
        event.put(ModEntities.MAHORAGA_WITHER.get(), MahoragaWitherEntity.createAttributes().build());
    }

    private void addCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.MEGUMI_WITHER_SKELETON_SPAWN_EGG.get());
            event.accept(ModItems.MAHORAGA_WITHER_SPAWN_EGG.get());
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("3");
        registrar.playToClient(RitualSyncPayload.TYPE, RitualSyncPayload.STREAM_CODEC);
        registrar.playToClient(MobVfxPayload.TYPE, MobVfxPayload.STREAM_CODEC);
    }
}
