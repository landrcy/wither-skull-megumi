package com.landrcy.impossiblepack.client;

import com.landrcy.impossiblepack.entity.MegumiWitherSkeletonEntity;
import com.landrcy.impossiblepack.network.RitualSyncPayload;
import com.landrcy.impossiblepack.registry.ModSounds;
import com.landrcy.impossiblepack.ritual.RitualConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ClientRitualManager {
    private ClientRitualManager() {}

    public enum BonePartType {
        HEAD,
        BODY,
        LEFT_ARM,
        RIGHT_ARM,
        LEFT_LEG,
        RIGHT_LEG
    }

    public static final class FlyingBonePiece {
        public final BonePartType type;
        public Vec3 prevPos;
        public Vec3 pos;
        public Vec3 vel;
        public float prevRotX, prevRotY, prevRotZ;
        public float rotX, rotY, rotZ;
        public final float rotVelX, rotVelY, rotVelZ;

        public FlyingBonePiece(BonePartType type, Vec3 startPos, Vec3 vel, float rotVelX, float rotVelY, float rotVelZ) {
            this.type = type;
            this.prevPos = startPos;
            this.pos = startPos;
            this.vel = vel;
            this.rotVelX = rotVelX;
            this.rotVelY = rotVelY;
            this.rotVelZ = rotVelZ;
        }

        public void tick() {
            this.prevPos = this.pos;
            this.prevRotX = this.rotX;
            this.prevRotY = this.rotY;
            this.prevRotZ = this.rotZ;

            this.pos = this.pos.add(this.vel);
            this.vel = this.vel.scale(0.96D).subtract(0.0D, 0.045D, 0.0D);

            this.rotX += this.rotVelX;
            this.rotY += this.rotVelY;
            this.rotZ += this.rotVelZ;
        }
    }

    private static boolean active = false;
    private static int skeletonEntityId = -1;
    private static int ritualTick = 0;
    private static Vec3 anchorPos = Vec3.ZERO;
    private static float anchorYaw = 0.0F;
    private static boolean spawnedExplosionParts = false;
    private static int postIllusionTicks = 0;
    private static final List<FlyingBonePiece> flyingPieces = new ArrayList<>();
    @Nullable
    private static SoundInstance activeSummonSound = null;

    public static void handleSyncPayload(RitualSyncPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (payload.active()) {
            boolean newlyStarted = !active || skeletonEntityId != payload.skeletonEntityId();
            active = true;
            skeletonEntityId = payload.skeletonEntityId();
            anchorPos = new Vec3(payload.anchorX(), payload.anchorY(), payload.anchorZ());
            anchorYaw = payload.anchorYaw();

            if (newlyStarted) {
                ritualTick = payload.ritualTick();
                spawnedExplosionParts = false;
                flyingPieces.clear();
                postIllusionTicks = 0;

                // Stop any prior summon audio and play the full 16-second mahoraga_summon.ogg right from the start!
                stopSummonSound(mc);
                SimpleSoundInstance summonInstance = SimpleSoundInstance.forUI(ModSounds.MAHORAGA_SUMMON.get(), 1.0F, 1.0F);
                mc.getSoundManager().play(summonInstance);
                activeSummonSound = summonInstance;
            } else {
                ritualTick = Math.max(ritualTick, payload.ritualTick());
            }

            if (ritualTick >= RitualConstants.WITHER_EXPLODE_TICK && !spawnedExplosionParts) {
                triggerBoneLaunch();
            }
        } else {
            if (active) {
                active = false;
                postIllusionTicks = 14;
                stopSummonSound(mc);
            }
        }
    }

    private static void stopSummonSound(Minecraft mc) {
        if (activeSummonSound != null) {
            mc.getSoundManager().stop(activeSummonSound);
            activeSummonSound = null;
        }
    }

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            reset();
            return;
        }

        if (postIllusionTicks > 0) {
            postIllusionTicks--;
        }

        if (!flyingPieces.isEmpty()) {
            for (FlyingBonePiece piece : flyingPieces) {
                piece.tick();
            }
            if (!active && postIllusionTicks == 0) {
                flyingPieces.clear();
            }
        }

        if (!active) {
            return;
        }

        // Keep player safely stationary on ledges/bridges while the world is blacked out
        mc.player.setDeltaMovement(0.0D, Math.min(0.0D, mc.player.getDeltaMovement().y), 0.0D);

        Entity entity = mc.level.getEntity(skeletonEntityId);
        if (entity instanceof MegumiWitherSkeletonEntity megumi && megumi.isRitualActive()) {
            anchorPos = megumi.position();
            anchorYaw = megumi.getRitualYaw();
            if (Math.abs(megumi.getRitualTicks() - ritualTick) > 3) {
                ritualTick = megumi.getRitualTicks();
            }
        }

        ritualTick++;

        if (ritualTick >= RitualConstants.WITHER_EXPLODE_TICK && !spawnedExplosionParts) {
            triggerBoneLaunch();
        }

        if (ritualTick >= RitualConstants.ILLUSION_END_TICK + 6) {
            active = false;
            postIllusionTicks = 14;
            stopSummonSound(mc);
        }
    }

    private static void triggerBoneLaunch() {
        spawnedExplosionParts = true;
        flyingPieces.clear();

        RandomSource rand = RandomSource.create(99412L + skeletonEntityId);
        float yawRad = anchorYaw * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yawRad), 0.0D, Mth.cos(yawRad)).normalize();
        Vec3 right = new Vec3(forward.z, 0.0D, -forward.x).normalize();

        // Skull hurtles past the player's upper-right shoulder
        flyingPieces.add(new FlyingBonePiece(
                BonePartType.HEAD,
                anchorPos.add(0.0D, 2.05D, 0.0D),
                forward.scale(0.64D).add(right.scale(0.20D)).add(0.0D, 0.34D, 0.0D),
                26.0F, 33.0F, -19.0F
        ));

        // Coated Jujutsu torso blasts forward-left
        flyingPieces.add(new FlyingBonePiece(
                BonePartType.BODY,
                anchorPos.add(0.0D, 1.45D, 0.0D),
                forward.scale(0.56D).add(right.scale(-0.28D)).add(0.0D, 0.22D, 0.0D),
                -22.0F, 19.0F, 27.0F
        ));

        // Left & right arms spin wide past the camera
        flyingPieces.add(new FlyingBonePiece(
                BonePartType.LEFT_ARM,
                anchorPos.add(right.scale(-0.35D)).add(forward.scale(0.35D)).add(0.0D, 1.65D, 0.0D),
                forward.scale(0.70D).add(right.scale(-0.44D)).add(0.0D, 0.28D, 0.0D),
                35.0F, -28.0F, 32.0F
        ));

        flyingPieces.add(new FlyingBonePiece(
                BonePartType.RIGHT_ARM,
                anchorPos.add(right.scale(0.35D)).add(forward.scale(0.28D)).add(0.0D, 1.52D, 0.0D),
                forward.scale(0.66D).add(right.scale(0.46D)).add(0.0D, 0.25D, 0.0D),
                -30.0F, 36.0F, -25.0F
        ));

        // Left & right legs tumble outward
        flyingPieces.add(new FlyingBonePiece(
                BonePartType.LEFT_LEG,
                anchorPos.add(right.scale(-0.18D)).add(0.0D, 0.72D, 0.0D),
                forward.scale(0.50D).add(right.scale(-0.35D)).add(0.0D, 0.16D + rand.nextDouble() * 0.06D, 0.0D),
                29.0F, 21.0F, -33.0F
        ));

        flyingPieces.add(new FlyingBonePiece(
                BonePartType.RIGHT_LEG,
                anchorPos.add(right.scale(0.18D)).add(0.0D, 0.72D, 0.0D),
                forward.scale(0.52D).add(right.scale(0.37D)).add(0.0D, 0.18D + rand.nextDouble() * 0.06D, 0.0D),
                -32.0F, -23.0F, 30.0F
        ));
    }

    public static void reset() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            stopSummonSound(mc);
        }
        active = false;
        skeletonEntityId = -1;
        ritualTick = 0;
        spawnedExplosionParts = false;
        postIllusionTicks = 0;
        flyingPieces.clear();
    }

    public static boolean isIllusionActive() {
        return active;
    }

    public static int getRitualTick() {
        return ritualTick;
    }

    public static int getSkeletonEntityId() {
        return skeletonEntityId;
    }

    public static Vec3 getAnchorPos() {
        return anchorPos;
    }

    public static float getAnchorYaw() {
        return anchorYaw;
    }

    public static int getPostIllusionTicks() {
        return postIllusionTicks;
    }

    public static List<FlyingBonePiece> getFlyingPieces() {
        return flyingPieces;
    }
}
