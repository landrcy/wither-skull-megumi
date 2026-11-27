package com.landrcy.impossiblepack.client;

import com.landrcy.impossiblepack.network.MobVfxPayload;
import com.landrcy.impossiblepack.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles incoming S2C MobVfxPayload packets on the client:
 * - Spawns directional custom particles with exact movement velocities
 * - Tracks Spider abdomen spring-up pose timing for SpringSpiderRenderer
 */
public final class ClientMobVfxManager {
    private ClientMobVfxManager() {}

    public static final int SPIDER_SPRING_DURATION_TICKS = 22;

    private static final Map<Integer, Long> SPIDER_SPRING_START_TICK = new ConcurrentHashMap<>();

    public static void handlePayload(MobVfxPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }

        RandomSource rand = level.getRandom();
        switch (payload.effectType()) {
            case MobVfxPayload.BOOGIE_WOOGIE_SWAP -> spawnBoogieWoogieVfx(
                    level, rand,
                    payload.x1(), payload.y1(), payload.z1(),
                    payload.x2(), payload.y2(), payload.z2()
            );
            case MobVfxPayload.SPIDER_SPRING_POSE -> {
                SPIDER_SPRING_START_TICK.put(payload.entityId(), level.getGameTime());
                // Subtle silk-gland glint right at the raised spider abdomen
                for (int i = 0; i < 6; i++) {
                    double vx = (rand.nextDouble() - 0.5D) * 0.08D;
                    double vy = 0.04D + rand.nextDouble() * 0.08D;
                    double vz = (rand.nextDouble() - 0.5D) * 0.08D;
                    level.addParticle(ModParticles.SILKY_WEB_STRAND.get(), payload.x1(), payload.y1(), payload.z1(), vx, vy, vz);
                }
            }
            case MobVfxPayload.SPIDER_WEB_STREAM -> spawnSpiderWebStream(
                    level, rand,
                    payload.x1(), payload.y1(), payload.z1(),
                    payload.x2(), payload.y2(), payload.z2()
            );
            case MobVfxPayload.SPIDER_WEB_SPLAT -> spawnSpiderWebSplat(
                    level, rand,
                    payload.x1(), payload.y1(), payload.z1()
            );
            case MobVfxPayload.ZOMBIE_BLOCK_DEBRIS -> spawnZombieBlockDebris(
                    level, rand,
                    payload.x1(), payload.y1(), payload.z1(),
                    payload.x2(), payload.y2(), payload.z2()
            );
            default -> {}
        }
    }

    /**
     * Returns normalized spider rear-spring animation age in ticks [0..SPIDER_SPRING_DURATION_TICKS], or -1 if idle.
     */
    public static float getSpiderSpringAge(int entityId, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return -1.0F;
        }
        Long start = SPIDER_SPRING_START_TICK.get(entityId);
        if (start == null) {
            return -1.0F;
        }
        float age = (mc.level.getGameTime() - start) + partialTick;
        if (age < 0.0F || age > SPIDER_SPRING_DURATION_TICKS) {
            if (age > SPIDER_SPRING_DURATION_TICKS + 2.0F) {
                SPIDER_SPRING_START_TICK.remove(entityId);
            }
            return -1.0F;
        }
        return age;
    }

    private static void spawnBoogieWoogieVfx(
            ClientLevel level, RandomSource rand,
            double x1, double y1, double z1,
            double x2, double y2, double z2
    ) {
        // 1. Concentric expanding Boogie Woogie shockwave rings at BOTH swap locations
        for (int i = 0; i < 3; i++) {
            double scale = 0.85D + i * 0.35D;
            double yOff = i * 0.35D;
            level.addParticle(ModParticles.BOOGIE_WOOGIE_RING.get(), x1, y1 + yOff, z1, scale, 0.02D, 0.0D);
            level.addParticle(ModParticles.BOOGIE_WOOGIE_RING.get(), x2, y2 + yOff, z2, scale, 0.02D, 0.0D);
        }

        // 2. High-speed cursed-energy sparks streaming accurately along A -> B and B -> A!
        Vec3 a = new Vec3(x1, y1 + 0.5D, z1);
        Vec3 b = new Vec3(x2, y2 + 0.5D, z2);
        Vec3 delta = b.subtract(a);
        double dist = Math.max(0.1D, delta.length());
        Vec3 dirAB = delta.scale(1.0D / dist);

        int streamCount = Mth.clamp((int) (dist * 3.0D), 14, 34);
        for (int i = 0; i < streamCount; i++) {
            double t = (double) i / (double) streamCount;
            double spread = 0.25D;
            double ox = (rand.nextDouble() - 0.5D) * spread;
            double oy = (rand.nextDouble() - 0.5D) * spread;
            double oz = (rand.nextDouble() - 0.5D) * spread;
            double speed = 0.22D + rand.nextDouble() * 0.22D;
            boolean forward = (i % 2 == 0);
            Vec3 origin = forward ? a.lerp(b, t * 0.85D) : b.lerp(a, t * 0.85D);
            Vec3 vel = dirAB.scale(forward ? speed : -speed);
            level.addParticle(
                    ModParticles.BOOGIE_WOOGIE_SPARK.get(),
                    origin.x + ox, origin.y + oy, origin.z + oz,
                    vel.x, vel.y, vel.z
            );
        }

        // 3. Crisp radial clap-burst sparks around both swap endpoints
        for (int end = 0; end < 2; end++) {
            double cx = end == 0 ? x1 : x2;
            double cy = (end == 0 ? y1 : y2) + 0.45D;
            double cz = end == 0 ? z1 : z2;
            for (int i = 0; i < 16; i++) {
                double ang = (i / 16.0D) * Mth.TWO_PI + rand.nextDouble() * 0.2D;
                double spd = 0.14D + rand.nextDouble() * 0.16D;
                double vy = (rand.nextDouble() - 0.35D) * 0.22D;
                level.addParticle(
                        ModParticles.BOOGIE_WOOGIE_SPARK.get(),
                        cx, cy, cz,
                        Math.cos(ang) * spd, vy, Math.sin(ang) * spd
                );
            }
        }
    }

    private static void spawnSpiderWebStream(
            ClientLevel level, RandomSource rand,
            double x, double y, double z,
            double vx, double vy, double vz
    ) {
        // Silky gossamer strands streaming right behind the flying web projectile with realistic silk drag!
        for (int i = 0; i < 5; i++) {
            double back = i * 0.18D;
            double px = x - vx * back + (rand.nextDouble() - 0.5D) * 0.12D;
            double py = y - vy * back + (rand.nextDouble() - 0.5D) * 0.12D;
            double pz = z - vz * back + (rand.nextDouble() - 0.5D) * 0.12D;
            // Inherit a fraction of shot velocity so strands stream smoothly backward and billow in the breeze
            double svx = vx * 0.22D + (rand.nextDouble() - 0.5D) * 0.05D;
            double svy = vy * 0.22D + 0.015D + (rand.nextDouble() - 0.5D) * 0.04D;
            double svz = vz * 0.22D + (rand.nextDouble() - 0.5D) * 0.05D;
            level.addParticle(ModParticles.SILKY_WEB_STRAND.get(), px, py, pz, svx, svy, svz);
        }
    }

    private static void spawnSpiderWebSplat(ClientLevel level, RandomSource rand, double x, double y, double z) {
        // Radial sticky web burst that shoots outward, slows in mid-air like real spider silk, and clings gently!
        for (int i = 0; i < 24; i++) {
            double theta = rand.nextDouble() * Mth.TWO_PI;
            double phi = (rand.nextDouble() - 0.25D) * Mth.HALF_PI;
            double spd = 0.12D + rand.nextDouble() * 0.22D;
            double vx = Math.cos(theta) * Math.cos(phi) * spd;
            double vy = Math.sin(phi) * spd + 0.05D;
            double vz = Math.sin(theta) * Math.cos(phi) * spd;
            level.addParticle(ModParticles.SILKY_WEB_STRAND.get(), x, y, z, vx, vy, vz);
        }
    }

    private static void spawnZombieBlockDebris(
            ClientLevel level, RandomSource rand,
            double x, double y, double z,
            double nx, double ny, double nz
    ) {
        // Directional stone/earth shatter chips spraying cleanly outward along (nx, ny, nz) with real gravity & bounce
        for (int i = 0; i < 14; i++) {
            double ox = (rand.nextDouble() - 0.5D) * 0.55D;
            double oy = (rand.nextDouble() - 0.5D) * 0.55D;
            double oz = (rand.nextDouble() - 0.5D) * 0.55D;
            double spd = 0.10D + rand.nextDouble() * 0.16D;
            double vx = nx * spd + (rand.nextDouble() - 0.5D) * 0.14D;
            double vy = ny * spd + 0.10D + rand.nextDouble() * 0.16D;
            double vz = nz * spd + (rand.nextDouble() - 0.5D) * 0.14D;
            level.addParticle(ModParticles.SIEGE_DEBRIS.get(), x + ox, y + oy, z + oz, vx, vy, vz);
        }
    }
}
