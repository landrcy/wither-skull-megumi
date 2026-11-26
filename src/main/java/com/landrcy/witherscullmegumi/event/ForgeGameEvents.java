package com.landrcy.witherscullmegumi.event;

import com.landrcy.witherscullmegumi.WitherScullMegumiMod;
import com.landrcy.witherscullmegumi.entity.MegumiWitherSkeletonEntity;
import com.landrcy.witherscullmegumi.registry.ModEntities;
import com.landrcy.witherscullmegumi.ritual.RitualConstants;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

@EventBusSubscriber(modid = WitherScullMegumiMod.MODID)
public final class ForgeGameEvents {
    private ForgeGameEvents() {}

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.isSpawnCancelled()) {
            return;
        }
        if (!(event.getEntity() instanceof WitherSkeleton) || event.getEntity().getType() != EntityType.WITHER_SKELETON) {
            return;
        }

        MobSpawnType spawnType = event.getSpawnType();
        boolean isNaturalLike = spawnType == MobSpawnType.NATURAL
                || spawnType == MobSpawnType.STRUCTURE
                || spawnType == MobSpawnType.CHUNK_GENERATION
                || spawnType == MobSpawnType.SPAWNER
                || spawnType == MobSpawnType.PATROL
                || spawnType == MobSpawnType.REINFORCEMENT;

        if (!isNaturalLike) {
            return;
        }

        ServerLevel serverLevel = event.getLevel().getLevel();
        if (serverLevel.getRandom().nextFloat() < RitualConstants.NATURAL_REPLACE_CHANCE) {
            event.setSpawnCancelled(true);

            MegumiWitherSkeletonEntity megumi = ModEntities.MEGUMI_WITHER_SKELETON.get().create(serverLevel);
            if (megumi != null) {
                megumi.moveTo(
                        event.getX(),
                        event.getY(),
                        event.getZ(),
                        event.getEntity().getYRot(),
                        event.getEntity().getXRot()
                );
                EventHooks.finalizeMobSpawn(megumi, event.getLevel(), event.getDifficulty(), spawnType, null);
                serverLevel.addFreshEntity(megumi);
            }
        }
    }
}
