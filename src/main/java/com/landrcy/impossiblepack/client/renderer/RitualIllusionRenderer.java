package com.landrcy.impossiblepack.client.renderer;

import com.landrcy.impossiblepack.client.ClientRitualManager;
import com.landrcy.impossiblepack.client.ClientRitualManager.BonePartType;
import com.landrcy.impossiblepack.client.ClientRitualManager.FlyingBonePiece;
import com.landrcy.impossiblepack.client.model.MahoragaWheelModel;
import com.landrcy.impossiblepack.client.state.MegumiSkeletonRenderState;
import com.landrcy.impossiblepack.ritual.RitualConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.model.monster.wither.WitherBossModel;
import net.minecraft.client.model.object.skull.SkullModel;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.client.renderer.entity.state.WitherRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.IRenderableSection;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.PrepareRenderBuffersEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class RitualIllusionRenderer {
    private RitualIllusionRenderer() {}

    private static final int FULL_BRIGHT = LightCoordsUtil.pack(15, 15);
    private static final Identifier WITHER_LOCATION =
            Identifier.withDefaultNamespace("textures/entity/wither/wither.png");
    private static final Identifier WITHER_INVULNERABLE_LOCATION =
            Identifier.withDefaultNamespace("textures/entity/wither/wither_invulnerable.png");
    private static final Identifier WITHER_ARMOR_LOCATION =
            Identifier.withDefaultNamespace("textures/entity/wither/wither_armor.png");
    private static final Vector3f BLACK_SKY = new Vector3f(0.0F, 0.0F, 0.0F);

    private static EntityModelSet cachedModelSet;
    private static SkullModel skullModel;
    private static WitherBossModel witherModel;
    private static WitherBossModel witherArmorModel;
    private static MahoragaWheelModel wheelModel;
    private static SkeletonModel<SkeletonRenderState> bonePartsModel;

    private static final List<IRenderableSection> stashedVisibleSections = new ArrayList<>();
    private static Collection<IRenderableSection> activeSectionCollection = null;

    private static void ensureModels(Minecraft mc) {
        EntityModelSet current = mc.getEntityModels();
        if (current != cachedModelSet || skullModel == null) {
            cachedModelSet = current;
            skullModel = new SkullModel(current.bakeLayer(ModelLayers.WITHER_SKULL));
            witherModel = new WitherBossModel(current.bakeLayer(ModelLayers.WITHER));
            witherArmorModel = new WitherBossModel(current.bakeLayer(ModelLayers.WITHER_ARMOR));
            wheelModel = new MahoragaWheelModel(current.bakeLayer(MahoragaWheelModel.LAYER_LOCATION));
            bonePartsModel = new SkeletonModel<>(current.bakeLayer(ModelLayers.WITHER_SKELETON));
        }
    }

    public static void onMovementInputUpdate(MovementInputUpdateEvent event) {
        if (ClientRitualManager.isIllusionActive()) {
            event.getInput().keyPresses = Input.EMPTY;
        }
    }

    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (!ClientRitualManager.isIllusionActive()) {
            return;
        }
        float t = ClientRitualManager.getRitualTick() + (float) event.getPartialTick();
        float alpha = Mth.clamp(t / (float) RitualConstants.BLACKOUT_FULL_TICK, 0.0F, 1.0F);

        float flash = 0.0F;
        if (t >= RitualConstants.WITHER_EXPLODE_TICK && t <= RitualConstants.WITHER_EXPLODE_TICK + 7.0F) {
            flash = (1.0F - (t - RitualConstants.WITHER_EXPLODE_TICK) / 7.0F) * 0.75F;
        }

        event.setRed(Mth.lerp(alpha, event.getRed(), flash * 0.90F));
        event.setGreen(Mth.lerp(alpha, event.getGreen(), flash * 0.92F));
        event.setBlue(Mth.lerp(alpha, event.getBlue(), flash));
    }

    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!ClientRitualManager.isIllusionActive()) {
            return;
        }
        float t = ClientRitualManager.getRitualTick() + (float) event.getPartialTick();
        if (t < RitualConstants.BLACKOUT_FULL_TICK) {
            float progress = Mth.clamp(t / (float) RitualConstants.BLACKOUT_FULL_TICK, 0.0F, 1.0F);
            float far = Mth.lerp(progress, event.getFarPlaneDistance(), 6.5F);
            event.setNearPlaneDistance(0.5F);
            event.setFarPlaneDistance(Math.max(4.0F, far));
        } else {
            event.setNearPlaneDistance(256.0F);
            event.setFarPlaneDistance(512.0F);
        }
    }

    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!ClientRitualManager.isIllusionActive() && ClientRitualManager.getPostIllusionTicks() <= 0) {
            return;
        }

        float partial = (float) event.getPartialTick();
        float t = ClientRitualManager.getRitualTick() + partial;
        float shake = 0.0F;

        if (ClientRitualManager.isIllusionActive()) {
            if (t >= RitualConstants.WITHER_APPEAR_TICK && t < RitualConstants.WITHER_EXPLODE_TICK) {
                float charge = (t - RitualConstants.WITHER_APPEAR_TICK)
                        / (float) (RitualConstants.WITHER_EXPLODE_TICK - RitualConstants.WITHER_APPEAR_TICK);
                shake = charge * charge * 0.95F;
            } else if (t >= RitualConstants.WITHER_EXPLODE_TICK) {
                float sinceExplode = t - RitualConstants.WITHER_EXPLODE_TICK;
                shake = Math.max(0.0F, 3.4F * (1.0F - sinceExplode / 18.0F));
            }
        } else if (ClientRitualManager.getPostIllusionTicks() > 0) {
            shake = (ClientRitualManager.getPostIllusionTicks() / 14.0F) * 1.1F;
        }

        if (shake > 0.001F) {
            event.setPitch(event.getPitch() + Mth.sin(t * 2.9F) * shake);
            event.setYaw(event.getYaw() + Mth.cos(t * 2.3F) * shake * 0.85F);
            event.setRoll(event.getRoll() + Mth.sin(t * 3.7F) * shake * 0.65F);
        }
    }

    public static void onExtractLevelRenderState(ExtractLevelRenderStateEvent event) {
        if (!ClientRitualManager.isIllusionActive()) {
            return;
        }

        float partialTick = event.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        float t = ClientRitualManager.getRitualTick() + partialTick;
        LevelRenderState state = event.getRenderState();

        state.skyRenderState.skybox = DimensionType.Skybox.NONE;
        state.skyRenderState.skyColor = BLACK_SKY;
        state.skyRenderState.starBrightness = 0.0F;
        state.skyRenderState.shouldRenderDarkDisc = false;
        state.cloudColor = 0;
        state.weatherRenderState.reset();
        state.particlesRenderState.reset();
        state.blockEntityRenderStates.clear();
        state.blockBreakingRenderStates.clear();
        state.blockOutlineRenderState = null;

        if (state.playerRenderState.avatarRenderState != null) {
            state.playerRenderState.avatarRenderState.lightCoords = FULL_BRIGHT;
        }

        if (t >= RitualConstants.BLACKOUT_FULL_TICK * 0.65F) {
            int activeMegumiId = ClientRitualManager.getSkeletonEntityId();
            state.entityRenderStates.removeIf(entityState -> {
                if (entityState instanceof AvatarRenderState) {
                    return false;
                }
                if (entityState instanceof MegumiSkeletonRenderState megumiState && megumiState.entityId == activeMegumiId) {
                    return false;
                }
                return true;
            });

            for (EntityRenderState kept : state.entityRenderStates) {
                kept.lightCoords = FULL_BRIGHT;
                kept.shadowRadius = 0.0F;
                kept.shadowPieces.clear();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer localPlayer = mc.player;
        if (localPlayer == null || mc.level == null) {
            return;
        }

        boolean illusionActive = ClientRitualManager.isIllusionActive();
        List<FlyingBonePiece> flyingPieces = ClientRitualManager.getFlyingPieces();
        if (!illusionActive && flyingPieces.isEmpty()) {
            return;
        }

        ensureModels(mc);
        LevelRenderState levelState = event.getLevelRenderState();
        float partialTick = levelState.worldPartialTicks;
        Vec3 camPos = levelState.cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();

        if (illusionActive) {
            float ritualTime = ClientRitualManager.getRitualTick() + partialTick;
            if (ritualTime >= RitualConstants.BLACKOUT_FULL_TICK && event.getRenderableSections() instanceof Collection<?> rawColl) {
                Collection<IRenderableSection> sectionColl = (Collection<IRenderableSection>) rawColl;
                stashedVisibleSections.clear();
                stashedVisibleSections.addAll(sectionColl);
                sectionColl.clear();
                activeSectionCollection = sectionColl;
            }

            Vec3 anchorPos = ClientRitualManager.getAnchorPos();
            float anchorYaw = ClientRitualManager.getAnchorYaw();

            if (ritualTime >= RitualConstants.SKULLS_START_TICK) {
                Vec3 playerPos = localPlayer.getPosition(partialTick);
                submitSurroundingSkulls(poseStack, collector, camPos, playerPos, anchorPos, anchorYaw, ritualTime);
            }

            if (ritualTime >= RitualConstants.WITHER_APPEAR_TICK) {
                submitChargingMahoragaWither(poseStack, collector, camPos, anchorPos, anchorYaw, ritualTime);
            }
        }

        if (!flyingPieces.isEmpty()) {
            submitFlyingBonePieces(poseStack, collector, camPos, partialTick, flyingPieces);
        }
    }

    public static void onPrepareRenderBuffers(PrepareRenderBuffersEvent event) {
        if (activeSectionCollection != null) {
            activeSectionCollection.addAll(stashedVisibleSections);
            stashedVisibleSections.clear();
            activeSectionCollection = null;
        }
    }

    private static void submitSurroundingSkulls(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            Vec3 camPos,
            Vec3 playerPos,
            Vec3 anchorPos,
            float anchorYaw,
            float ritualTime
    ) {
        Vec3 arenaCenter = playerPos.add(anchorPos).scale(0.5D);
        float baseRadius = (float) Math.max(4.4D, playerPos.distanceTo(anchorPos) * 0.55D + 2.6D);

        float postExplodeTime = Math.max(0.0F, ritualTime - RitualConstants.WITHER_EXPLODE_TICK);
        float blastExpansion = postExplodeTime > 0.0F ? postExplodeTime * 0.85F : 0.0F;

        int count = RitualConstants.PERIMETER_SKULL_COUNT;
        float spanTicks = (float) (RitualConstants.SKULLS_FULL_TICK - RitualConstants.SKULLS_START_TICK);

        for (int i = 0; i < count; i++) {
            float appearTick = RitualConstants.SKULLS_START_TICK + (i / (float) count) * (spanTicks * 0.85F);
            if (ritualTime < appearTick) {
                continue;
            }

            float popScale = Mth.clamp((ritualTime - appearTick) / 8.0F, 0.0F, 1.0F);
            float easedScale = 1.0F - (1.0F - popScale) * (1.0F - popScale);

            float angle = (anchorYaw * Mth.DEG_TO_RAD) + (i * Mth.TWO_PI / count) + ritualTime * 0.010F;
            float radius = baseRadius + blastExpansion;
            double sx = arenaCenter.x + Math.cos(angle) * radius;
            double sz = arenaCenter.z + Math.sin(angle) * radius;
            double hoverBob = Math.sin(ritualTime * 0.12F + i * 0.9F) * 0.18D;
            double sy = arenaCenter.y + 1.45D + (i % 2 == 0 ? 0.25D : -0.15D) + hoverBob + blastExpansion * 0.15D;

            double dx = arenaCenter.x - sx;
            double dz = arenaCenter.z - sz;
            float skullYaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;

            SkullModelBase.State skullState = new SkullModelBase.State();
            skullState.animationPos = 0.0F;
            skullState.yRot = skullYaw;
            skullState.xRot = 8.0F;

            Identifier tex = (i % 3 == 0) ? WITHER_INVULNERABLE_LOCATION : WITHER_LOCATION;
            float scale = 1.15F * easedScale;

            poseStack.pushPose();
            poseStack.translate(sx - camPos.x, sy - camPos.y, sz - camPos.z);
            poseStack.scale(-scale, -scale, scale);
            collector.submitModel(
                    skullModel,
                    skullState,
                    poseStack,
                    tex,
                    FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    EntityRenderState.NO_OUTLINE
            );
            poseStack.popPose();
        }
    }

    private static void submitChargingMahoragaWither(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            Vec3 camPos,
            Vec3 anchorPos,
            float anchorYaw,
            float ritualTime
    ) {
        float chargeDuration = (float) (RitualConstants.WITHER_EXPLODE_TICK - RitualConstants.WITHER_APPEAR_TICK);
        float charge = Mth.clamp((ritualTime - RitualConstants.WITHER_APPEAR_TICK) / chargeDuration, 0.0F, 1.0F);
        boolean exploded = ritualTime >= RitualConstants.WITHER_EXPLODE_TICK;

        float yawRad = anchorYaw * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yawRad), 0.0D, Mth.cos(yawRad));

        double riseY = 0.05D + 0.60D * Mth.sin(charge * Mth.HALF_PI);
        Vec3 witherPos = anchorPos.subtract(forward.scale(2.25D)).add(0.0D, riseY, 0.0D);

        float witherScale = exploded
                ? 2.08F + Math.min(0.25F, (ritualTime - RitualConstants.WITHER_EXPLODE_TICK) * 0.02F)
                : Mth.lerp(charge, 1.15F, 2.08F);

        WitherRenderState witherState = new WitherRenderState();
        witherState.ageInTicks = ritualTime;
        witherState.bodyRot = anchorYaw;
        witherState.yRot = 0.0F;
        witherState.xRot = 10.0F;
        witherState.xHeadRots[0] = 12.0F;
        witherState.xHeadRots[1] = 12.0F;
        witherState.yHeadRots[0] = anchorYaw - 12.0F;
        witherState.yHeadRots[1] = anchorYaw + 12.0F;
        witherState.isPowered = true;

        boolean useInvulnerableTex = !exploded && (charge < 0.30F || ((int) (ritualTime * (0.5F + charge * 2.2F)) % 2 == 0));
        Identifier bodyTex = useInvulnerableTex ? WITHER_INVULNERABLE_LOCATION : WITHER_LOCATION;

        poseStack.pushPose();
        poseStack.translate(witherPos.x - camPos.x, witherPos.y - camPos.y, witherPos.z - camPos.z);
        poseStack.rotateDegrees(Axis.YP, 180.0F - anchorYaw);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.scale(witherScale, witherScale, witherScale);
        poseStack.translate(0.0F, -1.501F, 0.0F);

        collector.submitModel(
                witherModel,
                witherState,
                poseStack,
                RenderTypes.entityCutout(bodyTex),
                FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                -1,
                null,
                EntityRenderState.NO_OUTLINE
        );

        float u = (Mth.cos(ritualTime * 0.04F) * 2.5F) % 1.0F;
        float v = (ritualTime * 0.025F) % 1.0F;
        collector.order(1).submitModel(
                witherArmorModel,
                witherState,
                poseStack,
                RenderTypes.energySwirl(WITHER_ARMOR_LOCATION, u, v),
                FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                ARGB.color(255, 165, 210, 255),
                null,
                EntityRenderState.NO_OUTLINE
        );

        // 3D Golden Eight-Handled Mahoraga Divine Wheel floating & rotating above the charging Wither's head
        poseStack.pushPose();
        float hoverBob = Mth.sin(ritualTime * 0.08F) * 0.04F;
        poseStack.translate(0.0F, -0.72F + hoverBob, 0.16F);
        poseStack.rotateDegrees(Axis.XP, -22.0F);
        float stepIndex = (float) Math.floor(ritualTime / 25.0F);
        float stepFrac = Mth.clamp((ritualTime % 25.0F) / 5.0F, 0.0F, 1.0F);
        poseStack.rotateDegrees(Axis.YP, (stepIndex + stepFrac) * 45.0F);
        poseStack.scale(0.85F, 0.85F, 0.85F);

        collector.submitModel(
                wheelModel,
                Unit.INSTANCE,
                poseStack,
                RenderTypes.entityCutout(MahoragaWheelModel.TEXTURE),
                FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                -1,
                null,
                EntityRenderState.NO_OUTLINE
        );
        poseStack.popPose();

        poseStack.popPose();
    }

    private static void submitFlyingBonePieces(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            Vec3 camPos,
            float partialTick,
            List<FlyingBonePiece> pieces
    ) {
        var renderType = RenderTypes.entityCutout(MegumiWitherSkeletonRenderer.TEXTURE);

        for (FlyingBonePiece piece : pieces) {
            double x = Mth.lerp(partialTick, piece.prevPos.x, piece.pos.x) - camPos.x;
            double y = Mth.lerp(partialTick, piece.prevPos.y, piece.pos.y) - camPos.y;
            double z = Mth.lerp(partialTick, piece.prevPos.z, piece.pos.z) - camPos.z;

            float rx = Mth.lerp(partialTick, piece.prevRotX, piece.rotX);
            float ry = Mth.lerp(partialTick, piece.prevRotY, piece.rotY);
            float rz = Mth.lerp(partialTick, piece.prevRotZ, piece.rotZ);

            poseStack.pushPose();
            poseStack.translate(x, y, z);
            poseStack.rotateDegrees(Axis.YP, ry);
            poseStack.rotateDegrees(Axis.XP, rx);
            poseStack.rotateDegrees(Axis.ZP, rz);
            poseStack.scale(-1.2F, -1.2F, 1.2F);

            bonePartsModel.resetPose();
            switch (piece.type) {
                case HEAD -> {
                    bonePartsModel.head.setPos(0.0F, 0.25F, 0.0F);
                    bonePartsModel.hat.setPos(0.0F, 0.25F, 0.0F);
                    collector.submitModelPart(bonePartsModel.head, poseStack, renderType, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, null);
                    collector.submitModelPart(bonePartsModel.hat, poseStack, renderType, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, null);
                }
                case BODY -> {
                    bonePartsModel.body.setPos(0.0F, -0.35F, 0.0F);
                    collector.submitModelPart(bonePartsModel.body, poseStack, renderType, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, null);
                }
                case LEFT_ARM -> {
                    bonePartsModel.leftArm.setPos(0.0F, -0.30F, 0.0F);
                    collector.submitModelPart(bonePartsModel.leftArm, poseStack, renderType, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, null);
                }
                case RIGHT_ARM -> {
                    bonePartsModel.rightArm.setPos(0.0F, -0.30F, 0.0F);
                    collector.submitModelPart(bonePartsModel.rightArm, poseStack, renderType, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, null);
                }
                case LEFT_LEG -> {
                    bonePartsModel.leftLeg.setPos(0.0F, -0.35F, 0.0F);
                    collector.submitModelPart(bonePartsModel.leftLeg, poseStack, renderType, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, null);
                }
                case RIGHT_LEG -> {
                    bonePartsModel.rightLeg.setPos(0.0F, -0.35F, 0.0F);
                    collector.submitModelPart(bonePartsModel.rightLeg, poseStack, renderType, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, null);
                }
            }

            poseStack.popPose();
        }
    }
}
