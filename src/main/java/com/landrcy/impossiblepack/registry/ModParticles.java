package com.landrcy.impossiblepack.registry;

import com.landrcy.impossiblepack.TheImpossiblePackMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    private ModParticles() {}

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, TheImpossiblePackMod.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BOOGIE_WOOGIE_RING =
            PARTICLE_TYPES.register("boogie_woogie_ring", () -> new ModSimpleParticleType(true));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BOOGIE_WOOGIE_SPARK =
            PARTICLE_TYPES.register("boogie_woogie_spark", () -> new ModSimpleParticleType(true));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SILKY_WEB_STRAND =
            PARTICLE_TYPES.register("silky_web_strand", () -> new ModSimpleParticleType(true));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SIEGE_DEBRIS =
            PARTICLE_TYPES.register("siege_debris", () -> new ModSimpleParticleType(false));

    private static final class ModSimpleParticleType extends SimpleParticleType {
        private ModSimpleParticleType(boolean overrideLimiter) {
            super(overrideLimiter);
        }
    }
}
