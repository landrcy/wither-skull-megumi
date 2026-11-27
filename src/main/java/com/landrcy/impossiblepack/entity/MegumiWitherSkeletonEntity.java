package com.landrcy.impossiblepack.entity;

import com.landrcy.impossiblepack.network.RitualSyncPayload;
import com.landrcy.impossiblepack.registry.ModEntities;
import com.landrcy.impossiblepack.ritual.RitualConstants;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Coated Wither Skeleton (5% natural replacement of Wither Skeletons).
 * Attacking it starts the 16-second Mahoraga summon audio & pose, blacks out the world,
 * surrounds you and him with Wither Skulls, charges a Mahoraga-Wheeled Wither behind him,
 * explodes and kills him launching his body parts at 16.0s, and then snaps out of the illusion
 * playing divine_wheel_adapting as the Mahoraga Wither locks solely onto you!
 */
public class MegumiWitherSkeletonEntity extends WitherSkeleton {

    private static final EntityDataAccessor<Boolean> DATA_RITUAL_ACTIVE =
            SynchedEntityData.defineId(MegumiWitherSkeletonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_RITUAL_TICKS =
            SynchedEntityData.defineId(MegumiWitherSkeletonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_RITUAL_YAW =
            SynchedEntityData.defineId(MegumiWitherSkeletonEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_TARGET_ENTITY_ID =
            SynchedEntityData.defineId(MegumiWitherSkeletonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_EXPLODED =
            SynchedEntityData.defineId(MegumiWitherSkeletonEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID targetPlayerUuid;
    @Nullable
    private Vec3 ritualAnchorPos;

    public MegumiWitherSkeletonEntity(EntityType<? extends WitherSkeleton> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_RITUAL_ACTIVE, false);
        builder.define(DATA_RITUAL_TICKS, 0);
        builder.define(DATA_RITUAL_YAW, 0.0F);
        builder.define(DATA_TARGET_ENTITY_ID, -1);
        builder.define(DATA_EXPLODED, false);
    }

    public boolean isRitualActive() {
        return this.entityData.get(DATA_RITUAL_ACTIVE);
    }

    public int getRitualTicks() {
        return this.entityData.get(DATA_RITUAL_TICKS);
    }

    public float getRitualYaw() {
        return this.entityData.get(DATA_RITUAL_YAW);
    }

    public int getTargetEntityId() {
        return this.entityData.get(DATA_TARGET_ENTITY_ID);
    }

    public boolean hasExploded() {
        return this.entityData.get(DATA_EXPLODED);
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(serverLevel, source, amount);
        }
        if (isRitualActive()) {
            return false;
        }

        Player attacker = null;
        if (source.getEntity() instanceof Player p) {
            attacker = p;
        } else if (source.getDirectEntity() instanceof Player p) {
            attacker = p;
        }

        if (attacker instanceof ServerPlayer serverPlayer) {
            startRitual(serverLevel, serverPlayer);
            return false;
        }

        return super.hurtServer(serverLevel, source, amount);
    }

    private void startRitual(ServerLevel serverLevel, ServerPlayer attacker) {
        if (isRitualActive()) {
            return;
        }

        this.targetPlayerUuid = attacker.getUUID();
        this.getNavigation().stop();
        this.setTarget(null);
        this.setAggressive(false);
        this.clearFire();
        this.setDeltaMovement(Vec3.ZERO);

        // Stow weapon so both hands form Megumi's Mahoraga summoning hand-sign pose
        this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);

        // Ensure comfortable viewing framing (~2.8 blocks) if struck at point-blank melee range
        Vec3 toSkeleton = this.position().subtract(attacker.position());
        Vec3 horiz = new Vec3(toSkeleton.x, 0.0D, toSkeleton.z);
        double dist = horiz.length();
        if (dist > 0.001D && dist < 2.8D) {
            Vec3 dir = horiz.normalize();
            double pushNeeded = 2.8D - dist;
            for (int step = 4; step >= 1; step--) {
                Vec3 offset = dir.scale(pushNeeded * (step / 4.0D));
                if (serverLevel.noCollision(this, this.getBoundingBox().move(offset))) {
                    this.setPos(this.getX() + offset.x, this.getY(), this.getZ() + offset.z);
                    break;
                }
            }
        }

        double dx = attacker.getX() - this.getX();
        double dz = attacker.getZ() - this.getZ();
        float yawToPlayer = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;

        this.ritualAnchorPos = this.position();
        this.setYRot(yawToPlayer);
        this.setYBodyRot(yawToPlayer);
        this.setYHeadRot(yawToPlayer);
        this.yRotO = yawToPlayer;
        this.yBodyRotO = yawToPlayer;
        this.yHeadRotO = yawToPlayer;
        this.setXRot(0.0F);
        this.setNoGravity(true);

        this.entityData.set(DATA_RITUAL_YAW, yawToPlayer);
        this.entityData.set(DATA_TARGET_ENTITY_ID, attacker.getId());
        this.entityData.set(DATA_RITUAL_TICKS, 0);
        this.entityData.set(DATA_EXPLODED, false);
        this.entityData.set(DATA_RITUAL_ACTIVE, true);

        // Note: ClientRitualManager immediately plays the full 16-second mahoraga_summon.ogg on the client upon receiving sync(tick=0)
        syncRitualToTarget(attacker, true, 0);
    }

    private void syncRitualToTarget(ServerPlayer target, boolean active, int tick) {
        Vec3 anchor = this.ritualAnchorPos != null ? this.ritualAnchorPos : this.position();
        PacketDistributor.sendToPlayer(target, new RitualSyncPayload(
                active,
                this.getId(),
                tick,
                anchor.x,
                anchor.y,
                anchor.z,
                getRitualYaw()
        ));
    }

    @Override
    public void tick() {
        if (!isRitualActive()) {
            super.tick();
            return;
        }

        this.baseTick();
        float yaw = getRitualYaw();
        this.setDeltaMovement(Vec3.ZERO);
        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);
        this.yRotO = yaw;
        this.yBodyRotO = yaw;
        this.yHeadRotO = yaw;

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            tickRitualServer(serverLevel);
        }
    }

    private void tickRitualServer(ServerLevel serverLevel) {
        if (this.ritualAnchorPos != null) {
            this.setPos(this.ritualAnchorPos.x, this.ritualAnchorPos.y, this.ritualAnchorPos.z);
        } else {
            this.ritualAnchorPos = this.position();
        }

        ServerPlayer targetPlayer = this.targetPlayerUuid != null
                ? serverLevel.getServer().getPlayerList().getPlayer(this.targetPlayerUuid)
                : null;

        if (targetPlayer == null || !targetPlayer.isAlive() || targetPlayer.level() != serverLevel) {
            this.discard();
            return;
        }

        // Protect the player from outside mobs during the 16.9s pitch-black domain cutscene
        targetPlayer.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 12, 4, false, false, false));

        int tick = getRitualTicks() + 1;
        this.entityData.set(DATA_RITUAL_TICKS, tick);

        if (tick == RitualConstants.WITHER_APPEAR_TICK) {
            syncRitualToTarget(targetPlayer, true, tick);
        }

        // At 16.0s (tick 320), right when mahoraga_summon finishes:
        // The Wither behind him EXPLODES, killing the Wither Skeleton and sending his parts launching!
        if (tick == RitualConstants.WITHER_EXPLODE_TICK) {
            this.entityData.set(DATA_EXPLODED, true);
            syncRitualToTarget(targetPlayer, true, tick);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.2F, 0.8F);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.WITHER_SKELETON_DEATH, SoundSource.HOSTILE, 1.8F, 0.75F);
        }

        // At 16.9s (tick 338), snap out of the illusion, manifest the real Mahoraga Wither locked onto the player,
        // and immediately play divine_wheel_adapting as its Golden Eight-Handled Wheel turns!
        if (tick >= RitualConstants.ILLUSION_END_TICK) {
            syncRitualToTarget(targetPlayer, false, tick);
            spawnMahoragaWither(serverLevel, targetPlayer);
            this.discard();
        }
    }

    private void spawnMahoragaWither(ServerLevel serverLevel, ServerPlayer targetPlayer) {
        Vec3 anchor = this.ritualAnchorPos != null ? this.ritualAnchorPos : this.position();
        float yawRad = getRitualYaw() * Mth.DEG_TO_RAD;
        Vec3 forwardToPlayer = new Vec3(-Mth.sin(yawRad), 0.0D, Mth.cos(yawRad));
        Vec3 desiredWitherPos = anchor.subtract(forwardToPlayer.scale(2.2D)).add(0.0D, 0.35D, 0.0D);

        MahoragaWitherEntity wither = ModEntities.MAHORAGA_WITHER.get().create(serverLevel, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (wither == null) {
            return;
        }

        wither.snapTo(desiredWitherPos.x, desiredWitherPos.y, desiredWitherPos.z, getRitualYaw(), 0.0F);
        if (!serverLevel.noCollision(wither)) {
            wither.snapTo(anchor.x, anchor.y + 0.2D, anchor.z, getRitualYaw(), 0.0F);
        }

        wither.setInvulnerableTicks(0);
        wither.setLockedTargetUuid(targetPlayer.getUUID());
        wither.setTarget(targetPlayer);
        wither.setPersistenceRequired();

        serverLevel.addFreshEntity(wither);

        // Immediately turn the Divine Wheel and play divine_wheel_adapting!
        wither.triggerWheelAdaptationTurn(serverLevel);

        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, wither.getX(), wither.getY() + 1.5D, wither.getZ(), 2, 0.3D, 0.3D, 0.3D, 0.0D);
        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, anchor.x, anchor.y + 1.2D, anchor.z, 45, 0.8D, 0.8D, 0.8D, 0.12D);
        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, anchor.x, anchor.y + 1.0D, anchor.z, 35, 0.7D, 0.7D, 0.7D, 0.08D);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("RitualActive", isRitualActive());
        output.putInt("RitualTicks", getRitualTicks());
        output.putFloat("RitualYaw", getRitualYaw());
        output.putBoolean("Exploded", hasExploded());
        if (this.targetPlayerUuid != null) {
            output.putString("TargetPlayerUUID", this.targetPlayerUuid.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DATA_RITUAL_ACTIVE, input.getBooleanOr("RitualActive", false));
        this.entityData.set(DATA_RITUAL_TICKS, input.getIntOr("RitualTicks", 0));
        this.entityData.set(DATA_RITUAL_YAW, input.getFloatOr("RitualYaw", 0.0F));
        this.entityData.set(DATA_EXPLODED, input.getBooleanOr("Exploded", false));
        String uuidStr = input.getStringOr("TargetPlayerUUID", "");
        if (!uuidStr.isEmpty()) {
            try {
                this.targetPlayerUuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }
}
