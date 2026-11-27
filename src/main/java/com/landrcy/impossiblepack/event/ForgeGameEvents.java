package com.landrcy.impossiblepack.event;

import com.landrcy.impossiblepack.TheImpossiblePackMod;
import com.landrcy.impossiblepack.entity.MegumiWitherSkeletonEntity;
import com.landrcy.impossiblepack.registry.ModEntities;
import com.landrcy.impossiblepack.ritual.RitualConstants;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

@EventBusSubscriber(modid = TheImpossiblePackMod.MODID)
public final class ForgeGameEvents {
    private ForgeGameEvents() {}

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.isSpawnCancelled()) {
            return;
        }
        if (!(event.getEntity() instanceof WitherSkeleton) || event.getEntity().getType() != EntityTypes.WITHER_SKELETON) {
            return;
        }

        EntitySpawnReason reason = event.getSpawnType();
        boolean isNaturalLike = reason == EntitySpawnReason.NATURAL
                || reason == EntitySpawnReason.STRUCTURE
                || reason == EntitySpawnReason.CHUNK_GENERATION
                || reason == EntitySpawnReason.SPAWNER
                || reason == EntitySpawnReason.PATROL
                || reason == EntitySpawnReason.REINFORCEMENT
                || reason == EntitySpawnReason.TRIAL_SPAWNER;

        if (!isNaturalLike) {
            return;
        }

        ServerLevel serverLevel = event.getLevel().getLevel();
        // 5% of natural Wither Skeletons wear Megumi's coat and summon Mahoraga Wither when attacked; 95% remain normal
        if (serverLevel.getRandom().nextFloat() < RitualConstants.NATURAL_COATED_SPAWN_CHANCE) {
            event.setSpawnCancelled(true);

            MegumiWitherSkeletonEntity megumi = ModEntities.MEGUMI_WITHER_SKELETON.get().create(serverLevel, reason);
            if (megumi != null) {
                megumi.snapTo(
                        event.getX(),
                        event.getY(),
                        event.getZ(),
                        event.getEntity().getYRot(),
                        event.getEntity().getXRot()
                );
                EventHooks.finalizeMobSpawn(megumi, event.getLevel(), event.getDifficulty(), reason, null);
                serverLevel.addFreshEntity(megumi);
            }
        }
    }
}
