package com.landrcy.witherscullmegumi.client;

import com.landrcy.witherscullmegumi.WitherScullMegumiMod;
import com.landrcy.witherscullmegumi.client.renderer.MegumiWitherSkeletonRenderer;
import com.landrcy.witherscullmegumi.client.renderer.RitualIllusionRenderer;
import com.landrcy.witherscullmegumi.registry.ModEntities;
import net.minecraft.client.renderer.entity.WitherBossRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

public final class ClientEvents {
    private ClientEvents() {}

    @EventBusSubscriber(modid = WitherScullMegumiMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBusEvents {
        private ModBusEvents() {}

        @SubscribeEvent
        public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.MEGUMI_WITHER_SKELETON.get(), MegumiWitherSkeletonRenderer::new);
            event.registerEntityRenderer(ModEntities.MEGUMI_WITHER.get(), WitherBossRenderer::new);
        }
    }

    @EventBusSubscriber(modid = WitherScullMegumiMod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static final class GameBusEvents {
        private GameBusEvents() {}

        @SubscribeEvent
        public static void onClientTickPost(ClientTickEvent.Post event) {
            ClientRitualManager.clientTick();
        }

        @SubscribeEvent
        public static void onRenderLevelStage(RenderLevelStageEvent event) {
            RitualIllusionRenderer.onRenderLevelStage(event);
        }

        @SubscribeEvent
        public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
            RitualIllusionRenderer.onComputeFogColor(event);
        }

        @SubscribeEvent
        public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
            RitualIllusionRenderer.onComputeCameraAngles(event);
        }

        @SubscribeEvent
        public static void onMovementInputUpdate(MovementInputUpdateEvent event) {
            RitualIllusionRenderer.onMovementInputUpdate(event);
        }

        @SubscribeEvent
        public static void onRenderHand(RenderHandEvent event) {
            RitualIllusionRenderer.onRenderHand(event);
        }
    }
}
