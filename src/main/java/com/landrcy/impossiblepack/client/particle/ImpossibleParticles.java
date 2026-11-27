package com.landrcy.impossiblepack.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Custom Minecraft 26.3 particle implementations with tailored movement physics:
 * - BoogieWoogieRingParticle: Rapid expanding JJK cursed-energy shockwave ring (cyan -> violet-magenta)
 * - BoogieWoogieSparkParticle: High-speed directional cursed-energy star spark with helical curl along swap lines
 * - SilkyWebStrandParticle: Real-world spider silk physics (strong air drag, near-weightless float, gentle breeze sway, sticks on blocks)
 * - SiegeDebrisParticle: Realistic stone/earth shatter chip with gravity, tumble spin, and floor bounce
 */
public final class ImpossibleParticles {
    private ImpossibleParticles() {}

    private static final int FULL_BRIGHT = LightCoordsUtil.pack(15, 15);

    // -------------------------------------------------------------------------
    // 1. Boogie Woogie Expanding Shockwave Ring
    // -------------------------------------------------------------------------
    public static final class BoogieWoogieRingParticle extends SingleQuadParticle {
        private final float startSize;
        private final float maxSize;
        private final float spinSpeed;

        public BoogieWoogieRingParticle(
                ClientLevel level,
                double x, double y, double z,
                double xd, double yd, double zd,
                SpriteSet sprites,
                RandomSource random
        ) {
            super(level, x, y, z, 0.0D, yd, 0.0D, sprites.get(random));
            this.hasPhysics = false;
            this.lifetime = 14 + random.nextInt(5);
            this.startSize = 0.22F;
            // xd encodes optional scale multiplier when > 0.05
            float scaleMult = (float) (Math.abs(xd) > 0.05D ? Math.abs(xd) : 1.0D);
            this.maxSize = (1.55F + random.nextFloat() * 0.45F) * scaleMult;
            this.quadSize = this.startSize;
            this.spinSpeed = (random.nextBoolean() ? 1.0F : -1.0F) * (0.14F + random.nextFloat() * 0.12F);
            this.roll = random.nextFloat() * Mth.TWO_PI;
            this.oRoll = this.roll;
            // Start electric cyan-white
            this.setColor(0.55F, 0.92F, 1.0F);
            this.setAlpha(0.95F);
        }

        @Override
        public void tick() {
            this.xo = this.x;
            this.yo = this.y;
            this.zo = this.z;
            this.oRoll = this.roll;

            if (this.age++ >= this.lifetime) {
                this.remove();
                return;
            }

            this.y += this.yd;
            this.yd *= 0.88D;
            this.roll += this.spinSpeed;

            float frac = (float) this.age / (float) this.lifetime;
            // Snappy ease-out expansion
            float eased = 1.0F - (1.0F - frac) * (1.0F - frac) * (1.0F - frac);
            this.quadSize = Mth.lerp(eased, this.startSize, this.maxSize);

            // Transition from electric cyan-white -> cursed violet-magenta
            this.rCol = Mth.lerp(frac, 0.55F, 0.88F);
            this.gCol = Mth.lerp(frac, 0.92F, 0.22F);
            this.bCol = Mth.lerp(frac, 1.0F, 1.0F);
            this.alpha = Mth.clamp(1.0F - frac * frac, 0.0F, 1.0F);
        }

        @Override
        protected int getLightCoords(float partialTick) {
            return FULL_BRIGHT;
        }

        @Override
        protected Layer getLayer() {
            return Layer.TRANSLUCENT;
        }

        public static ParticleProvider<SimpleParticleType> provider(SpriteSet sprites) {
            return (type, level, x, y, z, xd, yd, zd, rand) ->
                    new BoogieWoogieRingParticle(level, x, y, z, xd, yd, zd, sprites, rand);
        }
    }

    // -------------------------------------------------------------------------
    // 2. Boogie Woogie Directional Swap Stream Spark
    // -------------------------------------------------------------------------
    public static final class BoogieWoogieSparkParticle extends SingleQuadParticle {
        private final float spinSpeed;
        private final float swirlPhase;
        private final float baseSize;

        public BoogieWoogieSparkParticle(
                ClientLevel level,
                double x, double y, double z,
                double xd, double yd, double zd,
                SpriteSet sprites,
                RandomSource random
        ) {
            super(level, x, y, z, 0.0D, 0.0D, 0.0D, sprites.get(random));
            this.xd = xd;
            this.yd = yd;
            this.zd = zd;
            this.hasPhysics = false;
            this.lifetime = 12 + random.nextInt(10);
            this.baseSize = 0.14F + random.nextFloat() * 0.14F;
            this.quadSize = this.baseSize;
            this.spinSpeed = (random.nextBoolean() ? 1.0F : -1.0F) * (0.25F + random.nextFloat() * 0.25F);
            this.swirlPhase = random.nextFloat() * Mth.TWO_PI;
            this.roll = random.nextFloat() * Mth.TWO_PI;
            this.oRoll = this.roll;

            // Alternate between JJK cursed magenta, electric cyan, and pure white glints
            int tint = random.nextInt(3);
            if (tint == 0) {
                this.setColor(0.92F, 0.28F, 1.0F);
            } else if (tint == 1) {
                this.setColor(0.38F, 0.94F, 1.0F);
            } else {
                this.setColor(1.0F, 0.92F, 1.0F);
            }
            this.setAlpha(0.95F);
        }

        @Override
        public void tick() {
            this.xo = this.x;
            this.yo = this.y;
            this.zo = this.z;
            this.oRoll = this.roll;

            if (this.age++ >= this.lifetime) {
                this.remove();
                return;
            }

            // Crisp directional velocity with subtle cursed-energy helical ripple
            float ripple = 0.014F;
            double sx = Math.cos(this.age * 0.55F + this.swirlPhase) * ripple;
            double sy = Math.sin(this.age * 0.55F + this.swirlPhase) * ripple;
            this.move(this.xd + sx, this.yd + sy, this.zd - sx);

            this.xd *= 0.94D;
            this.yd *= 0.94D;
            this.zd *= 0.94D;
            this.roll += this.spinSpeed;

            float frac = (float) this.age / (float) this.lifetime;
            this.quadSize = this.baseSize * (1.0F - frac * 0.72F);
            this.alpha = Mth.clamp(1.0F - frac * frac, 0.0F, 1.0F);
        }

        @Override
        protected int getLightCoords(float partialTick) {
            return FULL_BRIGHT;
        }

        @Override
        protected Layer getLayer() {
            return Layer.TRANSLUCENT;
        }

        public static ParticleProvider<SimpleParticleType> provider(SpriteSet sprites) {
            return (type, level, x, y, z, xd, yd, zd, rand) ->
                    new BoogieWoogieSparkParticle(level, x, y, z, xd, yd, zd, sprites, rand);
        }
    }

    // -------------------------------------------------------------------------
    // 3. Spider Real-World Silky Web Strand Particle
    // -------------------------------------------------------------------------
    public static final class SilkyWebStrandParticle extends SingleQuadParticle {
        private final float swayPhase;
        private final float swayFreq;
        private final float spinSpeed;
        private final float targetSize;
        private boolean stuck = false;

        public SilkyWebStrandParticle(
                ClientLevel level,
                double x, double y, double z,
                double xd, double yd, double zd,
                SpriteSet sprites,
                RandomSource random
        ) {
            super(level, x, y, z, 0.0D, 0.0D, 0.0D, sprites.get(random));
            this.xd = xd;
            this.yd = yd;
            this.zd = zd;
            this.hasPhysics = true;
            this.lifetime = 36 + random.nextInt(26);
            this.targetSize = 0.22F + random.nextFloat() * 0.20F;
            this.quadSize = this.targetSize * 0.45F;
            this.swayPhase = random.nextFloat() * Mth.TWO_PI;
            this.swayFreq = 0.18F + random.nextFloat() * 0.14F;
            this.spinSpeed = (random.nextFloat() - 0.5F) * 0.09F;
            this.roll = random.nextFloat() * Mth.TWO_PI;
            this.oRoll = this.roll;

            // Delicate pearlescent spider-silk white with faint cool sheen
            float shade = 0.93F + random.nextFloat() * 0.07F;
            this.setColor(shade * 0.98F, shade * 0.99F, shade);
            this.setAlpha(0.86F);
        }

        @Override
        public void tick() {
            this.xo = this.x;
            this.yo = this.y;
            this.zo = this.z;
            this.oRoll = this.roll;

            if (this.age++ >= this.lifetime) {
                this.remove();
                return;
            }

            float frac = (float) this.age / (float) this.lifetime;

            // Silk unfurls smoothly right after firing, then stays delicate & translucent
            float unfurl = Mth.clamp(this.age / 5.0F, 0.0F, 1.0F);
            this.quadSize = Mth.lerp(unfurl, this.targetSize * 0.45F, this.targetSize * (1.0F + frac * 0.35F));

            if (this.onGround) {
                // Sticky web clings to surfaces once it lands!
                this.stuck = true;
            }

            if (this.stuck) {
                this.xd *= 0.4D;
                this.yd = 0.0D;
                this.zd *= 0.4D;
            } else {
                // Real-world spider silk physics: strong air drag slows fast launch into floating gossamer threads
                this.xd *= 0.87D;
                this.yd = this.yd * 0.87D - 0.0042D; // Ultra-light real-web settling gravity
                this.zd *= 0.87D;

                // Gentle air-current wind billowing
                double breezeX = Math.sin(this.age * this.swayFreq + this.swayPhase) * 0.0085D;
                double breezeZ = Math.cos(this.age * this.swayFreq * 0.85F + this.swayPhase) * 0.0085D;
                double breezeY = Math.sin(this.age * this.swayFreq * 0.6F + this.swayPhase) * 0.0025D;

                this.move(this.xd + breezeX, this.yd + breezeY, this.zd + breezeZ);
                this.roll += this.spinSpeed * (1.0F - frac * 0.5F);
            }

            // Stay crisp & visible for most of lifetime, then gently dissolve
            if (frac > 0.68F) {
                float fade = (frac - 0.68F) / 0.32F;
                this.alpha = Mth.clamp(0.86F * (1.0F - fade), 0.0F, 0.86F);
            }
        }

        @Override
        protected Layer getLayer() {
            return Layer.TRANSLUCENT;
        }

        public static ParticleProvider<SimpleParticleType> provider(SpriteSet sprites) {
            return (type, level, x, y, z, xd, yd, zd, rand) ->
                    new SilkyWebStrandParticle(level, x, y, z, xd, yd, zd, sprites, rand);
        }
    }

    // -------------------------------------------------------------------------
    // 4. Zombie Siege Block-Break & Bridging Debris Chip Particle
    // -------------------------------------------------------------------------
    public static final class SiegeDebrisParticle extends SingleQuadParticle {
        private float spinSpeed;
        private final float startSize;
        private boolean bounced = false;

        public SiegeDebrisParticle(
                ClientLevel level,
                double x, double y, double z,
                double xd, double yd, double zd,
                SpriteSet sprites,
                RandomSource random
        ) {
            super(level, x, y, z, 0.0D, 0.0D, 0.0D, sprites.get(random));
            this.xd = xd;
            this.yd = yd;
            this.zd = zd;
            this.hasPhysics = true;
            this.lifetime = 18 + random.nextInt(14);
            this.startSize = 0.09F + random.nextFloat() * 0.09F;
            this.quadSize = this.startSize;
            this.spinSpeed = (random.nextBoolean() ? 1.0F : -1.0F) * (0.22F + random.nextFloat() * 0.28F);
            this.roll = random.nextFloat() * Mth.TWO_PI;
            this.oRoll = this.roll;

            float shade = 0.72F + random.nextFloat() * 0.28F;
            this.setColor(shade, shade * 0.98F, shade * 0.95F);
            this.setAlpha(1.0F);
        }

        @Override
        public void tick() {
            this.xo = this.x;
            this.yo = this.y;
            this.zo = this.z;
            this.oRoll = this.roll;

            if (this.age++ >= this.lifetime) {
                this.remove();
                return;
            }

            this.yd -= 0.046D; // Crisp stone chip gravity
            double prevYd = this.yd;
            this.move(this.xd, this.yd, this.zd);

            if (this.onGround && !this.bounced && prevYd < -0.06D) {
                // Realistic small stone bounce!
                this.bounced = true;
                this.yd = -prevYd * 0.36D;
                this.xd *= 0.65D;
                this.zd *= 0.65D;
                this.spinSpeed *= 0.55F;
            } else if (this.onGround) {
                this.xd *= 0.72D;
                this.zd *= 0.72D;
                this.spinSpeed *= 0.7F;
            } else {
                this.xd *= 0.96D;
                this.zd *= 0.96D;
            }

            this.roll += this.spinSpeed;

            float frac = (float) this.age / (float) this.lifetime;
            if (frac > 0.65F) {
                float shrink = (frac - 0.65F) / 0.35F;
                this.quadSize = this.startSize * (1.0F - shrink * 0.85F);
            }
        }

        @Override
        protected Layer getLayer() {
            return Layer.OPAQUE;
        }

        public static ParticleProvider<SimpleParticleType> provider(SpriteSet sprites) {
            return (type, level, x, y, z, xd, yd, zd, rand) ->
                    new SiegeDebrisParticle(level, x, y, z, xd, yd, zd, sprites, rand);
        }
    }
}
