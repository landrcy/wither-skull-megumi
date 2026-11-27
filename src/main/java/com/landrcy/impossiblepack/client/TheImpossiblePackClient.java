package com.landrcy.impossiblepack.client;

import com.landrcy.impossiblepack.TheImpossiblePackMod;
import com.landrcy.impossiblepack.client.model.MahoragaWheelModel;
import com.landrcy.impossiblepack.client.renderer.MahoragaWitherRenderer;
import com.landrcy.impossiblepack.client.renderer.MegumiWitherSkeletonRenderer;
import com.landrcy.impossiblepack.client.renderer.RitualIllusionRenderer;
import com.landrcy.impossiblepack.network.RitualSyncPayload;
import com.landrcy.impossiblepack.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.PrepareRenderBuffersEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@Mod(value = TheImpossiblePackMod.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = TheImpossiblePackMod.MODID, value = Dist.CLIENT)
public class TheImpossiblePackClient {

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MahoragaWheelModel.LAYER_LOCATION, MahoragaWheelModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MEGUMI_WITHER_SKELETON.get(), MegumiWitherSkeletonRenderer::new);
        event.registerEntityRenderer(ModEntities.MAHORAGA_WITHER.get(), MahoragaWitherRenderer::new);
    }

    @SubscribeEvent
    public static void registerClientPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(RitualSyncPayload.TYPE, (payload, ctx) -> ClientRitualManager.handleSyncPayload(payload));
    }

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        ClientRitualManager.clientTick();
    }

    @SubscribeEvent
    public static void onMovementInputUpdate(MovementInputUpdateEvent event) {
        RitualIllusionRenderer.onMovementInputUpdate(event);
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        RitualIllusionRenderer.onComputeFogColor(event);
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        RitualIllusionRenderer.onRenderFog(event);
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        RitualIllusionRenderer.onComputeCameraAngles(event);
    }

    @SubscribeEvent
    public static void onExtractLevelRenderState(ExtractLevelRenderStateEvent event) {
        RitualIllusionRenderer.onExtractLevelRenderState(event);
    }

    @SubscribeEvent
    public static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        RitualIllusionRenderer.onSubmitCustomGeometry(event);
    }

    @SubscribeEvent
    public static void onPrepareRenderBuffers(PrepareRenderBuffersEvent event) {
        RitualIllusionRenderer.onPrepareRenderBuffers(event);
    }
}
