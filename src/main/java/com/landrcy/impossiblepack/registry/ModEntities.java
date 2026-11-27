package com.landrcy.impossiblepack.registry;

import com.landrcy.impossiblepack.TheImpossiblePackMod;
import com.landrcy.impossiblepack.entity.MahoragaWitherEntity;
import com.landrcy.impossiblepack.entity.MegumiWitherSkeletonEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister.Entities ENTITY_TYPES =
            DeferredRegister.createEntities(TheImpossiblePackMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<MegumiWitherSkeletonEntity>> MEGUMI_WITHER_SKELETON =
            ENTITY_TYPES.registerEntityType(
                    "megumi_wither_skeleton",
                    MegumiWitherSkeletonEntity::new,
                    MobCategory.MONSTER,
                    builder -> builder.sized(0.7F, 2.4F)
                            .eyeHeight(2.1F)
                            .fireImmune()
                            .clientTrackingRange(10)
            );

    public static final DeferredHolder<EntityType<?>, EntityType<MahoragaWitherEntity>> MAHORAGA_WITHER =
            ENTITY_TYPES.registerEntityType(
                    "mahoraga_wither",
                    MahoragaWitherEntity::new,
                    MobCategory.MONSTER,
                    builder -> builder.sized(0.9F, 3.5F)
                            .fireImmune()
                            .clientTrackingRange(10)
            );
}
