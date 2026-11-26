package com.landrcy.witherscullmegumi.registry;

import com.landrcy.witherscullmegumi.WitherScullMegumiMod;
import com.landrcy.witherscullmegumi.entity.MegumiSummonedWitherEntity;
import com.landrcy.witherscullmegumi.entity.MegumiWitherSkeletonEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, WitherScullMegumiMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<MegumiWitherSkeletonEntity>> MEGUMI_WITHER_SKELETON =
            ENTITY_TYPES.register("megumi_wither_skeleton", () ->
                    EntityType.Builder.of(MegumiWitherSkeletonEntity::new, MobCategory.MONSTER)
                            .sized(0.7F, 2.4F)
                            .eyeHeight(2.1F)
                            .fireImmune()
                            .clientTrackingRange(10)
                            .build("megumi_wither_skeleton"));

    public static final DeferredHolder<EntityType<?>, EntityType<MegumiSummonedWitherEntity>> MEGUMI_WITHER =
            ENTITY_TYPES.register("megumi_wither", () ->
                    EntityType.Builder.of(MegumiSummonedWitherEntity::new, MobCategory.MONSTER)
                            .sized(0.9F, 3.5F)
                            .fireImmune()
                            .clientTrackingRange(10)
                            .build("megumi_wither"));
}
