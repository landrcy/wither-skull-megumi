package com.landrcy.witherscullmegumi.client;

import com.landrcy.witherscullmegumi.entity.MegumiWitherSkeletonEntity;
import com.landrcy.witherscullmegumi.network.RitualSyncPayload;
import com.landrcy.witherscullmegumi.ritual.RitualConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks client-side illusion state, camera shake, reality-snap flash,
 * and Megumi's launched 3D skeletal body parts when the Wither explodes behind him.
 */
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
    private static int postIllusionFlashTicks = 0;
    private static final List<FlyingBonePiece> flyingPieces = new ArrayList<>();

    public static void handleSyncPayload(RitualSyncPayload payload) {
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
                postIllusionFlashTicks = 0;
            } else {
                ritualTick = Math.max(ritualTick, payload.ritualTick());
            }

            if (ritualTick >= RitualConstants.WITHER_EXPLODE_TICK && !spawnedExplosionParts) {
                triggerBoneLaunch();
            }
        } else {
            if (active) {
                active = false;
                postIllusionFlashTicks = 10;
            }
        }
    }

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            reset();
            return;
        }

        if (postIllusionFlashTicks > 0) {
            postIllusionFlashTicks--;
        }

        // Tick flying skeletal body parts both at the end of the illusion and briefly after snapping back
        if (!flyingPieces.isEmpty()) {
            for (FlyingBonePiece piece : flyingPieces) {
                piece.tick();
            }
            if (!active && postIllusionFlashTicks == 0) {
                flyingPieces.clear();
            }
        }

        if (!active) {
            return;
        }

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

        if (ritualTick >= RitualConstants.ILLUSION_END_TICK + 4) {
            // Safety fallback in case final packet was delayed
            active = false;
            postIllusionFlashTicks = 10;
        }
    }

    private static void triggerBoneLaunch() {
        spawnedExplosionParts = true;
        flyingPieces.clear();

        RandomSource rand = RandomSource.create(44209L + skeletonEntityId);
        float yawRad = anchorYaw * Mth.DEG_TO_RAD;
        // Forward vector from Megumi toward the player
        Vec3 forward = new Vec3(-Mth.sin(yawRad), 0.0D, Mth.cos(yawRad)).normalize();
        Vec3 right = new Vec3(forward.z, 0.0D, -forward.x).normalize();

        // Head / Skull launches fast up & past the player's shoulder!
        flyingPieces.add(new FlyingBonePiece(
                BonePartType.HEAD,
                anchorPos.add(0.0D, 2.05D, 0.0D),
                forward.scale(0.62D).add(right.scale(0.18D)).add(0.0D, 0.34D, 0.0D),
                24.0F, 32.0F, -19.0F
        ));

        // Ribcage / Torso blasts forward & slightly left
        flyingPieces.add(new FlyingBonePiece(
                BonePartType.BODY,
                anchorPos.add(0.0D, 1.45D, 0.0D),
                forward.scale(0.54D).add(right.scale(-0.26D)).add(0.0D, 0.22D, 0.0D),
                -21.0F, 18.0F, 26.0F
        ));

        // Left Arm (posed forward) rockets outward to the left-front
        flyingPieces.add(new FlyingBonePiece(
                BonePartType.LEFT_ARM,
                anchorPos.add(right.scale(-0.35D)).add(forward.scale(0.35D)).add(0.0D, 1.65D, 0.0D),
                forward.scale(0.68D).add(right.scale(-0.42D)).add(0.0D, 0.28D, 0.0D),
                34.0F, -27.0F, 31.0F
        ));

        // Right Arm rockets outward to the right-front
        flyingPieces.add(new FlyingBonePiece(
                BonePartType.RIGHT_ARM,
                anchorPos.add(right.scale(0.35D)).add(forward.scale(0.28D)).add(0.0D, 1.52D, 0.0D),
                forward.scale(0.64D).add(right.scale(0.45D)).add(0.0D, 0.25D, 0.0D),
                -29.0F, 35.0F, -24.0F
        ));

        // Left Leg tumbles low & wide
        flyingPieces.add(new FlyingBonePiece(
                BonePartType.LEFT_LEG,
                anchorPos.add(right.scale(-0.18D)).add(0.0D, 0.72D, 0.0D),
                forward.scale(0.48D).add(right.scale(-0.34D)).add(0.0D, 0.16D + rand.nextDouble() * 0.06D, 0.0D),
                28.0F, 20.0F, -32.0F
        ));

        // Right Leg tumbles low & right
        flyingPieces.add(new FlyingBonePiece(
                BonePartType.RIGHT_LEG,
                anchorPos.add(right.scale(0.18D)).add(0.0D, 0.72D, 0.0D),
                forward.scale(0.50D).add(right.scale(0.36D)).add(0.0D, 0.18D + rand.nextDouble() * 0.06D, 0.0D),
                -31.0F, -22.0F, 29.0F
        ));
    }

    public static void reset() {
        active = false;
        skeletonEntityId = -1;
        ritualTick = 0;
        spawnedExplosionParts = false;
        postIllusionFlashTicks = 0;
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

    public static int getPostIllusionFlashTicks() {
        return postIllusionFlashTicks;
    }

    public static List<FlyingBonePiece> getFlyingPieces() {
        return flyingPieces;
    }

    @Nullable
    public static MegumiWitherSkeletonEntity getSkeletonEntity() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || skeletonEntityId < 0) {
            return null;
        }
        Entity e = mc.level.getEntity(skeletonEntityId);
        return e instanceof MegumiWitherSkeletonEntity m ? m : null;
    }
}
