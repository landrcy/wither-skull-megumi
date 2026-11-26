package com.landrcy.witherscullmegumi.client.renderer;

import com.landrcy.witherscullmegumi.client.ClientRitualManager;
import com.landrcy.witherscullmegumi.client.ClientRitualManager.BonePartType;
import com.landrcy.witherscullmegumi.client.ClientRitualManager.FlyingBonePiece;
import com.landrcy.witherscullmegumi.entity.MegumiWitherSkeletonEntity;
import com.landrcy.witherscullmegumi.ritual.RitualConstants;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.SkullModel;
import net.minecraft.client.model.WitherBossModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * Renders the pitch-black Mahoraga Wither illusion:
 * 1. Everything fades/cuts to 100% pitch black EXCEPT the player and Wither Skull Megumi.
 * 2. Ceremonial Wither Skulls + an 8-skull Mahoraga Dharmachakra wheel surround both of them.
 * 3. A towering Wither rises behind Megumi, powers up with swirling invulnerable energy, and EXPLODES.
 * 4. Megumi's 6 skeletal body parts go launching & tumbling violently through the void.
 * 5. Reality snaps back, revealing the real Wither locked exclusively onto the player.
 */
public final class RitualIllusionRenderer {
    private RitualIllusionRenderer() {}

    private static final ResourceLocation WITHER_LOCATION =
            ResourceLocation.withDefaultNamespace("textures/entity/wither/wither.png");
    private static final ResourceLocation WITHER_INVULNERABLE_LOCATION =
            ResourceLocation.withDefaultNamespace("textures/entity/wither/wither_invulnerable.png");
    private static final ResourceLocation WITHER_ARMOR_LOCATION =
            ResourceLocation.withDefaultNamespace("textures/entity/wither/wither_armor.png");

    private static EntityModelSet cachedModelSet;
    private static SkullModel skullModel;
    private static WitherBossModel<WitherBoss> witherModel;
    private static WitherBossModel<WitherBoss> witherArmorModel;
    private static SkeletonModel<MegumiWitherSkeletonEntity> bonePartsModel;

    private static boolean renderingFullBrightHand = false;

    private static void ensureModelsInitialized(Minecraft mc) {
        EntityModelSet currentSet = mc.getEntityModels();
        if (currentSet != cachedModelSet || skullModel == null) {
            cachedModelSet = currentSet;
            skullModel = new SkullModel(currentSet.bakeLayer(ModelLayers.WITHER_SKULL));
            witherModel = new WitherBossModel<>(currentSet.bakeLayer(ModelLayers.WITHER));
            witherArmorModel = new WitherBossModel<>(currentSet.bakeLayer(ModelLayers.WITHER_ARMOR));
            bonePartsModel = new SkeletonModel<>(currentSet.bakeLayer(ModelLayers.WITHER_SKELETON));
        }
    }

    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (ClientRitualManager.isIllusionActive()) {
            float t = ClientRitualManager.getRitualTick() + (float) event.getPartialTick();
            float alpha = Mth.clamp(t / (float) RitualConstants.BLACKOUT_FULL_TICK, 0.0F, 1.0F);
            event.setRed(Mth.lerp(alpha, event.getRed(), 0.0F));
            event.setGreen(Mth.lerp(alpha, event.getGreen(), 0.0F));
            event.setBlue(Mth.lerp(alpha, event.getBlue(), 0.0F));
        }
    }

    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!ClientRitualManager.isIllusionActive() && ClientRitualManager.getPostIllusionFlashTicks() <= 0) {
            return;
        }

        float partial = (float) event.getPartialTick();
        float t = ClientRitualManager.getRitualTick() + partial;
        float shakeIntensity = 0.0F;

        if (ClientRitualManager.isIllusionActive()) {
            if (t >= RitualConstants.WITHER_APPEAR_TICK && t < RitualConstants.WITHER_EXPLODE_TICK) {
                float charge = (t - RitualConstants.WITHER_APPEAR_TICK)
                        / (float) (RitualConstants.WITHER_EXPLODE_TICK - RitualConstants.WITHER_APPEAR_TICK);
                shakeIntensity = charge * charge * 0.9F;
            } else if (t >= RitualConstants.WITHER_EXPLODE_TICK) {
                float sinceExplode = t - RitualConstants.WITHER_EXPLODE_TICK;
                shakeIntensity = Math.max(0.0F, 3.2F * (1.0F - sinceExplode / 20.0F));
            }
        } else if (ClientRitualManager.getPostIllusionFlashTicks() > 0) {
            shakeIntensity = (ClientRitualManager.getPostIllusionFlashTicks() / 10.0F) * 1.1F;
        }

        if (shakeIntensity > 0.001F) {
            event.setPitch(event.getPitch() + Mth.sin(t * 2.9F) * shakeIntensity);
            event.setYaw(event.getYaw() + Mth.cos(t * 2.3F) * shakeIntensity * 0.85F);
            event.setRoll(event.getRoll() + Mth.sin(t * 3.7F) * shakeIntensity * 0.65F);
        }
    }

    public static void onMovementInputUpdate(MovementInputUpdateEvent event) {
        if (ClientRitualManager.isIllusionActive()) {
            // Keep the player safely anchored on their bridge/ledge while the world is pitch black,
            // while allowing full 360-degree mouse camera freedom to look around at the skulls & Wither!
            event.getInput().forwardImpulse = 0.0F;
            event.getInput().leftImpulse = 0.0F;
            event.getInput().up = false;
            event.getInput().down = false;
            event.getInput().left = false;
            event.getInput().right = false;
            event.getInput().jumping = false;
        }
    }

    public static void onRenderHand(RenderHandEvent event) {
        if (!ClientRitualManager.isIllusionActive() || renderingFullBrightHand) {
            return;
        }

        // Guarantee the player's first-person hand/item is crisp full-bright ("everything goes black but him and I")
        // even if triggered inside a dark cave or unlit Nether Fortress!
        if (event.getPackedLight() < LightTexture.FULL_BRIGHT) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                event.setCanceled(true);
                if (event.getHand() == InteractionHand.MAIN_HAND) {
                    renderingFullBrightHand = true;
                    try {
                        mc.getEntityRenderDispatcher().getItemInHandRenderer().renderHandsWithItems(
                                event.getPartialTick(),
                                event.getPoseStack(),
                                mc.renderBuffers().bufferSource(),
                                mc.player,
                                LightTexture.FULL_BRIGHT
                        );
                    } finally {
                        renderingFullBrightHand = false;
                    }
                }
            }
        }
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer localPlayer = mc.player;
        if (localPlayer == null || mc.level == null) {
            return;
        }

        boolean illusionActive = ClientRitualManager.isIllusionActive();
        int postFlashTicks = ClientRitualManager.getPostIllusionFlashTicks();
        List<FlyingBonePiece> flyingPieces = ClientRitualManager.getFlyingPieces();

        if (!illusionActive && postFlashTicks <= 0 && flyingPieces.isEmpty()) {
            return;
        }

        ensureModelsInitialized(mc);
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();

        // Post-illusion reality return: continue rendering tumbling bone pieces in the real world + brief snap flash
        if (!illusionActive) {
            if (!flyingPieces.isEmpty()) {
                Matrix4fStack mvStack = RenderSystem.getModelViewStack();
                mvStack.pushMatrix();
                mvStack.mul(event.getModelViewMatrix());
                RenderSystem.applyModelViewMatrix();
                Lighting.setupLevel();

                PoseStack poseStack = new PoseStack();
                MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
                renderFlyingBonePieces(poseStack, bufferSource, camPos, partialTick, flyingPieces);
                bufferSource.endBatch();

                mvStack.popMatrix();
                RenderSystem.applyModelViewMatrix();
            }
            if (postFlashTicks > 0) {
                float flashAlpha = Mth.clamp((postFlashTicks - partialTick) / 10.0F, 0.0F, 1.0F) * 0.45F;
                drawCameraEnclosingQuadCube(event.getModelViewMatrix(), 0.08F, 0.10F, 0.16F, flashAlpha);
            }
            return;
        }

        float ritualTime = ClientRitualManager.getRitualTick() + partialTick;
        float blackoutAlpha = Mth.clamp(ritualTime / (float) RitualConstants.BLACKOUT_FULL_TICK, 0.0F, 1.0F);

        // Brief blinding flash right on the tick the Wither detonates behind Megumi
        float explosionFlash = 0.0F;
        if (ritualTime >= RitualConstants.WITHER_EXPLODE_TICK && ritualTime <= RitualConstants.WITHER_EXPLODE_TICK + 7.0F) {
            explosionFlash = (1.0F - (ritualTime - RitualConstants.WITHER_EXPLODE_TICK) / 7.0F) * 0.72F;
        }

        // Step 1: Black out the entire world (sky, terrain, blocks, fluids, particles, unrelated mobs)
        mc.getMainRenderTarget().bindWrite(false);
        if (blackoutAlpha >= 0.995F) {
            RenderSystem.clearColor(explosionFlash * 0.85F, explosionFlash * 0.90F, explosionFlash, 1.0F);
            RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        } else {
            drawCameraEnclosingQuadCube(event.getModelViewMatrix(), 0.0F, 0.0F, 0.0F, blackoutAlpha);
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        }

        // Step 2: Set up world-space camera rotation matrix so ONLY us, Megumi, the Mahoraga skulls,
        // the powering-up Wither, and his launched body parts render inside the black void!
        Matrix4fStack mvStack = RenderSystem.getModelViewStack();
        mvStack.pushMatrix();
        mvStack.mul(event.getModelViewMatrix());
        RenderSystem.applyModelViewMatrix();
        Lighting.setupLevel();

        PoseStack poseStack = new PoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();

        // 2a. Render the Player in third-person ("everything goes black but him and I")
        if (camera.isDetached()) {
            double px = Mth.lerp(partialTick, localPlayer.xOld, localPlayer.getX()) - camPos.x;
            double py = Mth.lerp(partialTick, localPlayer.yOld, localPlayer.getY()) - camPos.y;
            double pz = Mth.lerp(partialTick, localPlayer.zOld, localPlayer.getZ()) - camPos.z;
            float pYaw = Mth.lerp(partialTick, localPlayer.yRotO, localPlayer.getYRot());
            dispatcher.setRenderShadow(false);
            dispatcher.render(localPlayer, px, py, pz, pYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
            dispatcher.setRenderShadow(true);
        }

        // 2b. Render Wither Skull Megumi holding the summoning pose (until the Wither explodes him at tick 106)
        Vec3 anchorPos = ClientRitualManager.getAnchorPos();
        float anchorYaw = ClientRitualManager.getAnchorYaw();
        MegumiWitherSkeletonEntity megumi = ClientRitualManager.getSkeletonEntity();
        if (megumi != null && !megumi.hasExploded() && ritualTime < RitualConstants.WITHER_EXPLODE_TICK) {
            double mx = Mth.lerp(partialTick, megumi.xOld, megumi.getX()) - camPos.x;
            double my = Mth.lerp(partialTick, megumi.yOld, megumi.getY()) - camPos.y;
            double mz = Mth.lerp(partialTick, megumi.zOld, megumi.getZ()) - camPos.z;
            dispatcher.setRenderShadow(false);
            dispatcher.render(megumi, mx, my, mz, anchorYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
            dispatcher.setRenderShadow(true);
        }

        // 2c. Render the Mahoraga surrounding Wither Skulls & 8-Handled Wheel halo
        if (ritualTime >= RitualConstants.SKULLS_START_TICK) {
            Vec3 playerLerpPos = localPlayer.getPosition(partialTick);
            renderMahoragaSkulls(poseStack, bufferSource, camPos, playerLerpPos, anchorPos, anchorYaw, ritualTime);
        }

        // 2d. Render the towering Wither powering up directly behind Megumi
        if (ritualTime >= RitualConstants.WITHER_APPEAR_TICK) {
            renderPoweringUpWither(poseStack, bufferSource, camPos, anchorPos, anchorYaw, ritualTime);
        }

        // 2e. Render Megumi's 6 launched skeletal body parts blasting outward after the explosion
        if (!flyingPieces.isEmpty()) {
            renderFlyingBonePieces(poseStack, bufferSource, camPos, partialTick, flyingPieces);
        }

        bufferSource.endBatch();

        mvStack.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    private static void renderMahoragaSkulls(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            Vec3 camPos,
            Vec3 playerPos,
            Vec3 anchorPos,
            float anchorYaw,
            float ritualTime
    ) {
        // Midpoint between player and Megumi so the perimeter ring surrounds BOTH of them
        Vec3 arenaCenter = playerPos.add(anchorPos).scale(0.5D);
        float baseRadius = (float) Math.max(4.4D, playerPos.distanceTo(anchorPos) * 0.55D + 2.6D);

        float postExplodeTime = Math.max(0.0F, ritualTime - RitualConstants.WITHER_EXPLODE_TICK);
        float blastExpansion = postExplodeTime > 0.0F ? postExplodeTime * 0.85F : 0.0F;

        // 1. Outer ceremonial perimeter ring of 12 Wither Skulls surrounding player & Megumi
        int count = RitualConstants.PERIMETER_SKULL_COUNT;
        float spanTicks = (float) (RitualConstants.SKULLS_FULL_TICK - RitualConstants.SKULLS_START_TICK);

        for (int i = 0; i < count; i++) {
            // Stagger appearance smoothly around the perimeter
            float appearTick = RitualConstants.SKULLS_START_TICK + (i / (float) count) * (spanTicks * 0.82F);
            if (ritualTime < appearTick) {
                continue;
            }

            float popScale = Mth.clamp((ritualTime - appearTick) / 5.0F, 0.0F, 1.0F);
            float easedScale = 1.0F - (1.0F - popScale) * (1.0F - popScale);

            float angle = (anchorYaw * Mth.DEG_TO_RAD) + (i * Mth.TWO_PI / count) + ritualTime * 0.012F;
            float radius = baseRadius + blastExpansion;
            double sx = arenaCenter.x + Math.cos(angle) * radius;
            double sz = arenaCenter.z + Math.sin(angle) * radius;
            double hoverBob = Math.sin(ritualTime * 0.14F + i * 0.9F) * 0.18D;
            double sy = arenaCenter.y + 1.45D + (i % 2 == 0 ? 0.25D : -0.15D) + hoverBob + blastExpansion * 0.15D;

            // Face inward toward the center between player and Megumi
            double dx = arenaCenter.x - sx;
            double dz = arenaCenter.z - sz;
            float skullYaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
            float skullPitch = 8.0F;

            boolean blueCharged = (i % 3 == 0);
            renderSingleWitherSkull(
                    poseStack,
                    bufferSource,
                    sx - camPos.x,
                    sy - camPos.y,
                    sz - camPos.z,
                    skullYaw,
                    skullPitch,
                    1.15F * easedScale,
                    blueCharged
            );
        }

        // 2. Mahoraga's Eight-Handled Sacred Wheel (8 Wither Skulls floating in a tilted halo behind/above Megumi)
        if (ritualTime >= RitualConstants.SKULLS_START_TICK + 6 && postExplodeTime < 14.0F) {
            float wheelAppear = Mth.clamp((ritualTime - (RitualConstants.SKULLS_START_TICK + 6)) / 10.0F, 0.0F, 1.0F);
            float yawRad = anchorYaw * Mth.DEG_TO_RAD;
            Vec3 forward = new Vec3(-Mth.sin(yawRad), 0.0D, Mth.cos(yawRad));
            Vec3 wheelCenter = anchorPos.subtract(forward.scale(0.85D)).add(0.0D, 2.75D + blastExpansion * 0.35D, 0.0D);

            // Discrete 45-degree Mahoraga wheel clicks every 10 ticks!
            float stepIndex = (float) Math.floor(ritualTime / 10.0F);
            float stepFrac = Mth.clamp((ritualTime % 10.0F) / 2.5F, 0.0F, 1.0F);
            float wheelSpinDeg = (stepIndex + stepFrac) * 45.0F;
            float wheelRadius = (1.55F + blastExpansion * 0.65F) * wheelAppear;

            poseStack.pushPose();
            poseStack.translate(wheelCenter.x - camPos.x, wheelCenter.y - camPos.y, wheelCenter.z - camPos.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(-anchorYaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(-28.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(wheelSpinDeg));

            for (int w = 0; w < RitualConstants.WHEEL_SKULL_COUNT; w++) {
                float wAng = w * Mth.TWO_PI / RitualConstants.WHEEL_SKULL_COUNT;
                float wx = Mth.cos(wAng) * wheelRadius;
                float wy = Mth.sin(wAng) * wheelRadius;

                poseStack.pushPose();
                poseStack.translate(wx, wy, 0.0F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(-wheelSpinDeg));
                poseStack.scale(-0.78F * wheelAppear, -0.78F * wheelAppear, 0.78F * wheelAppear);

                ResourceLocation tex = (w % 2 == 0) ? WITHER_INVULNERABLE_LOCATION : WITHER_LOCATION;
                VertexConsumer vc = bufferSource.getBuffer(skullModel.renderType(tex));
                skullModel.setupAnim(0.0F, 0.0F, 0.0F);
                skullModel.renderToBuffer(poseStack, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                poseStack.popPose();
            }

            poseStack.popPose();
        }
    }

    private static void renderSingleWitherSkull(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            double relX,
            double relY,
            double relZ,
            float yawDeg,
            float pitchDeg,
            float scale,
            boolean invulnerableBlue
    ) {
        poseStack.pushPose();
        poseStack.translate(relX, relY, relZ);
        poseStack.scale(-scale, -scale, scale);
        ResourceLocation tex = invulnerableBlue ? WITHER_INVULNERABLE_LOCATION : WITHER_LOCATION;
        VertexConsumer vc = bufferSource.getBuffer(skullModel.renderType(tex));
        skullModel.setupAnim(0.0F, yawDeg, pitchDeg);
        skullModel.renderToBuffer(poseStack, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private static void renderPoweringUpWither(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
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

        // Looms 2.2 blocks directly behind Megumi, rising higher as it powers up
        double riseY = 0.05D + 0.55D * Mth.sin(charge * Mth.HALF_PI);
        Vec3 witherPos = anchorPos.subtract(forward.scale(2.2D)).add(0.0D, riseY, 0.0D);

        // Grows from 1.25x to full menacing 2.05x Wither Boss scale
        float witherScale = exploded
                ? 2.05F + Math.min(0.25F, (ritualTime - RitualConstants.WITHER_EXPLODE_TICK) * 0.02F)
                : Mth.lerp(charge, 1.25F, 2.05F);

        // Animate spine/tail breathing & heads glaring down over Megumi's shoulders at the player
        ModelPart root = witherModel.root();
        ModelPart ribcage = root.getChild("ribcage");
        ModelPart tail = root.getChild("tail");
        ModelPart centerHead = root.getChild("center_head");
        ModelPart rightHead = root.getChild("right_head");
        ModelPart leftHead = root.getChild("left_head");

        float animWave = Mth.cos(ritualTime * 0.22F);
        ribcage.xRot = (0.065F + 0.05F * animWave) * Mth.PI;
        tail.setPos(-2.0F, 6.9F + Mth.cos(ribcage.xRot) * 10.0F, -0.5F + Mth.sin(ribcage.xRot) * 10.0F);
        tail.xRot = (0.265F + 0.1F * animWave) * Mth.PI;
        centerHead.xRot = 0.18F;
        centerHead.yRot = 0.0F;
        rightHead.xRot = 0.22F;
        rightHead.yRot = -0.18F;
        leftHead.xRot = 0.22F;
        leftHead.yRot = 0.18F;

        // Copy pose to energy armor layer
        witherArmorModel.root().getChild("ribcage").copyFrom(ribcage);
        witherArmorModel.root().getChild("tail").copyFrom(tail);
        witherArmorModel.root().getChild("center_head").copyFrom(centerHead);
        witherArmorModel.root().getChild("right_head").copyFrom(rightHead);
        witherArmorModel.root().getChild("left_head").copyFrom(leftHead);

        // Flash rapidly between invulnerable blue/white and dark Wither texture during charge peak
        boolean useInvulnerableTex = !exploded && (charge < 0.35F || ((int) (ritualTime * (1.0F + charge * 2.0F)) % 2 == 0));
        ResourceLocation bodyTex = useInvulnerableTex ? WITHER_INVULNERABLE_LOCATION : WITHER_LOCATION;

        poseStack.pushPose();
        poseStack.translate(witherPos.x - camPos.x, witherPos.y - camPos.y, witherPos.z - camPos.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - anchorYaw));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.scale(witherScale, witherScale, witherScale);
        poseStack.translate(0.0F, -1.501F, 0.0F);

        VertexConsumer bodyVc = bufferSource.getBuffer(witherModel.renderType(bodyTex));
        witherModel.renderToBuffer(poseStack, bodyVc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);

        // Swirling Wither charge armor aura while powering up and right after detonation
        if (!exploded || ritualTime < RitualConstants.WITHER_EXPLODE_TICK + 12) {
            float u = Mth.cos(ritualTime * 0.05F) * 2.0F;
            float v = ritualTime * 0.03F;
            VertexConsumer swirlVc = bufferSource.getBuffer(RenderType.energySwirl(WITHER_ARMOR_LOCATION, u % 1.0F, v % 1.0F));
            int glowColor = FastColor.ARGB32.color(255, 160, 205, 255);
            witherArmorModel.renderToBuffer(poseStack, swirlVc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, glowColor);
        }

        poseStack.popPose();
    }

    private static void renderFlyingBonePieces(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            Vec3 camPos,
            float partialTick,
            List<FlyingBonePiece> pieces
    ) {
        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(MegumiWitherSkeletonRenderer.TEXTURE));

        for (FlyingBonePiece piece : pieces) {
            double x = Mth.lerp(partialTick, piece.prevPos.x, piece.pos.x) - camPos.x;
            double y = Mth.lerp(partialTick, piece.prevPos.y, piece.pos.y) - camPos.y;
            double z = Mth.lerp(partialTick, piece.prevPos.z, piece.pos.z) - camPos.z;

            float rx = Mth.lerp(partialTick, piece.prevRotX, piece.rotX);
            float ry = Mth.lerp(partialTick, piece.prevRotY, piece.rotY);
            float rz = Mth.lerp(partialTick, piece.prevRotZ, piece.rotZ);

            poseStack.pushPose();
            poseStack.translate(x, y, z);
            poseStack.mulPose(Axis.YP.rotationDegrees(ry));
            poseStack.mulPose(Axis.XP.rotationDegrees(rx));
            poseStack.mulPose(Axis.ZP.rotationDegrees(rz));
            poseStack.scale(-1.2F, -1.2F, 1.2F);

            renderIsolatedBonePart(piece.type, poseStack, vc);
            poseStack.popPose();
        }
    }

    private static void renderIsolatedBonePart(BonePartType type, PoseStack poseStack, VertexConsumer vc) {
        bonePartsModel.head.resetPose();
        bonePartsModel.hat.resetPose();
        bonePartsModel.body.resetPose();
        bonePartsModel.leftArm.resetPose();
        bonePartsModel.rightArm.resetPose();
        bonePartsModel.leftLeg.resetPose();
        bonePartsModel.rightLeg.resetPose();

        switch (type) {
            case HEAD -> {
                bonePartsModel.head.setPos(0.0F, 0.25F, 0.0F);
                bonePartsModel.hat.setPos(0.0F, 0.25F, 0.0F);
                bonePartsModel.head.render(poseStack, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                bonePartsModel.hat.render(poseStack, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            }
            case BODY -> {
                bonePartsModel.body.setPos(0.0F, -0.35F, 0.0F);
                bonePartsModel.body.render(poseStack, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            }
            case LEFT_ARM -> {
                bonePartsModel.leftArm.setPos(0.0F, -0.30F, 0.0F);
                bonePartsModel.leftArm.render(poseStack, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            }
            case RIGHT_ARM -> {
                bonePartsModel.rightArm.setPos(0.0F, -0.30F, 0.0F);
                bonePartsModel.rightArm.render(poseStack, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            }
            case LEFT_LEG -> {
                bonePartsModel.leftLeg.setPos(0.0F, -0.35F, 0.0F);
                bonePartsModel.leftLeg.render(poseStack, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            }
            case RIGHT_LEG -> {
                bonePartsModel.rightLeg.setPos(0.0F, -0.35F, 0.0F);
                bonePartsModel.rightLeg.render(poseStack, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            }
        }
    }

    /**
     * Draws a small inverted cube centered on the camera so any fade-in/out tint covers 100% of the screen
     * regardless of camera pitch/yaw/roll.
     */
    private static void drawCameraEnclosingQuadCube(Matrix4f modelView, float r, float g, float b, float a) {
        if (a <= 0.001F) {
            return;
        }

        Matrix4fStack mvStack = RenderSystem.getModelViewStack();
        mvStack.pushMatrix();
        mvStack.mul(modelView);
        RenderSystem.applyModelViewMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        float s = 1.5F;
        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        // 6 faces around (0,0,0)
        // -Z
        buf.addVertex(-s, -s, -s).setColor(r, g, b, a);
        buf.addVertex(s, -s, -s).setColor(r, g, b, a);
        buf.addVertex(s, s, -s).setColor(r, g, b, a);
        buf.addVertex(-s, s, -s).setColor(r, g, b, a);
        // +Z
        buf.addVertex(s, -s, s).setColor(r, g, b, a);
        buf.addVertex(-s, -s, s).setColor(r, g, b, a);
        buf.addVertex(-s, s, s).setColor(r, g, b, a);
        buf.addVertex(s, s, s).setColor(r, g, b, a);
        // -X
        buf.addVertex(-s, -s, s).setColor(r, g, b, a);
        buf.addVertex(-s, -s, -s).setColor(r, g, b, a);
        buf.addVertex(-s, s, -s).setColor(r, g, b, a);
        buf.addVertex(-s, s, s).setColor(r, g, b, a);
        // +X
        buf.addVertex(s, -s, -s).setColor(r, g, b, a);
        buf.addVertex(s, -s, s).setColor(r, g, b, a);
        buf.addVertex(s, s, s).setColor(r, g, b, a);
        buf.addVertex(s, s, -s).setColor(r, g, b, a);
        // +Y
        buf.addVertex(-s, s, -s).setColor(r, g, b, a);
        buf.addVertex(s, s, -s).setColor(r, g, b, a);
        buf.addVertex(s, s, s).setColor(r, g, b, a);
        buf.addVertex(-s, s, s).setColor(r, g, b, a);
        // -Y
        buf.addVertex(-s, -s, s).setColor(r, g, b, a);
        buf.addVertex(s, -s, s).setColor(r, g, b, a);
        buf.addVertex(s, -s, -s).setColor(r, g, b, a);
        buf.addVertex(-s, -s, -s).setColor(r, g, b, a);

        BufferUploader.drawWithShader(buf.buildOrThrow());

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        mvStack.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }
}
