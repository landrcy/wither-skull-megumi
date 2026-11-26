package com.landrcy.witherscullmegumi.entity;

import com.landrcy.witherscullmegumi.network.RitualSyncPayload;
import com.landrcy.witherscullmegumi.registry.ModEntities;
import com.landrcy.witherscullmegumi.ritual.RitualConstants;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Special Wither Skeleton ("Wither Skull Megumi").
 * When attacked by a player, enters the Mahoraga summoning hand-sign pose,
 * traps the attacker in a pitch-black illusion surrounded by Wither Skulls,
 * summons a Wither behind itself that powers up and blasts its bones flying,
 * then snaps back to reality leaving a Wither that ONLY targets the attacker.
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

    @Nullable
    public UUID getTargetPlayerUuid() {
        return this.targetPlayerUuid;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
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

        if (attacker != null) {
            if (!this.level().isClientSide() && attacker instanceof ServerPlayer serverPlayer) {
                startRitual(serverPlayer);
            }
            return false;
        }

        return super.hurt(source, amount);
    }

    private void startRitual(ServerPlayer attacker) {
        if (isRitualActive()) {
            return;
        }

        this.targetPlayerUuid = attacker.getUUID();
        this.getNavigation().stop();
        this.setTarget(null);
        this.setAggressive(false);
        this.clearFire();
        this.setDeltaMovement(Vec3.ZERO);

        // Stow weapon so both skeletal fists cleanly form Megumi's Mahoraga summoning mudra
        this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);

        // Ensure comfortable framing distance (~2.6 blocks) if the player attacked at point-blank melee range
        Vec3 toSkeleton = this.position().subtract(attacker.position());
        Vec3 horiz = new Vec3(toSkeleton.x, 0.0D, toSkeleton.z);
        double dist = horiz.length();
        if (dist > 0.001D && dist < 2.6D) {
            Vec3 dir = horiz.normalize();
            double pushNeeded = 2.6D - dist;
            for (int step = 4; step >= 1; step--) {
                Vec3 offset = dir.scale(pushNeeded * (step / 4.0D));
                if (this.level().noCollision(this, this.getBoundingBox().move(offset))) {
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

        // Initial dramatic cursed chime + skeleton impact sound
        this.level().playSound(null, this.blockPosition(), SoundEvents.WITHER_SKELETON_HURT, SoundSource.HOSTILE, 1.0F, 0.75F);
        this.level().playSound(null, attacker.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 1.4F, 0.55F);
        this.level().playSound(null, attacker.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 1.5F, 0.65F);

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
            if (this.level().isClientSide() && this.random.nextInt(5) == 0) {
                this.level().addParticle(
                        ParticleTypes.SMOKE,
                        this.getRandomX(0.45D),
                        this.getY() + 0.4D + this.random.nextDouble() * 1.6D,
                        this.getRandomZ(0.45D),
                        0.0D, 0.02D, 0.0D
                );
            }
            return;
        }

        // Ritual mode: freeze body physics and lock exact facing direction
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

        // Shield the player from outside interference during the 6.4s pitch-black domain illusion
        targetPlayer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 10, 4, false, false, false));

        int tick = getRitualTicks() + 1;
        this.entityData.set(DATA_RITUAL_TICKS, tick);

        // Sound cues for Mahoraga Wither Skull manifestation & Dharmachakra wheel clicks
        if (tick >= RitualConstants.SKULLS_START_TICK && tick <= RitualConstants.SKULLS_FULL_TICK && (tick - RitualConstants.SKULLS_START_TICK) % 6 == 0) {
            float progress = (float) (tick - RitualConstants.SKULLS_START_TICK) / (RitualConstants.SKULLS_FULL_TICK - RitualConstants.SKULLS_START_TICK);
            serverLevel.playSound(null, targetPlayer.blockPosition(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.HOSTILE, 0.9F, 0.55F + progress * 0.25F);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.WITHER_SKELETON_STEP, SoundSource.HOSTILE, 1.1F, 0.55F);
        }

        // Wither materializes behind Megumi and begins charging up
        if (tick == RitualConstants.WITHER_APPEAR_TICK) {
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.6F, 0.5F);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 1.8F, 0.55F);
            syncRitualToTarget(targetPlayer, true, tick);
        } else if (tick > RitualConstants.WITHER_APPEAR_TICK && tick < RitualConstants.WITHER_EXPLODE_TICK && (tick - RitualConstants.WITHER_APPEAR_TICK) % 10 == 0) {
            float chargeFrac = (float) (tick - RitualConstants.WITHER_APPEAR_TICK) / (RitualConstants.WITHER_EXPLODE_TICK - RitualConstants.WITHER_APPEAR_TICK);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE, 1.3F, 0.55F + chargeFrac * 0.9F);
        }

        // THE EXPLOSION: Wither finishes powering up behind Megumi, detonates, and sends his body parts launching!
        if (tick == RitualConstants.WITHER_EXPLODE_TICK) {
            this.entityData.set(DATA_EXPLODED, true);
            syncRitualToTarget(targetPlayer, true, tick);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.2F, 0.75F);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.8F, 0.95F);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.WITHER_SKELETON_DEATH, SoundSource.HOSTILE, 1.8F, 0.7F);
        }

        // Exit the illusion and spawn the real Wither that ONLY targets the player!
        if (tick >= RitualConstants.ILLUSION_END_TICK) {
            syncRitualToTarget(targetPlayer, false, tick);
            spawnLockedWither(serverLevel, targetPlayer);
            this.discard();
        }
    }

    private void spawnLockedWither(ServerLevel serverLevel, ServerPlayer targetPlayer) {
        Vec3 anchor = this.ritualAnchorPos != null ? this.ritualAnchorPos : this.position();
        float yawRad = getRitualYaw() * Mth.DEG_TO_RAD;
        // Forward direction from Megumi toward the player: (-sin(yaw), 0, cos(yaw))
        Vec3 forwardToPlayer = new Vec3(-Mth.sin(yawRad), 0.0D, Mth.cos(yawRad));
        Vec3 desiredWitherPos = anchor.subtract(forwardToPlayer.scale(2.2D)).add(0.0D, 0.35D, 0.0D);

        MegumiSummonedWitherEntity wither = ModEntities.MEGUMI_WITHER.get().create(serverLevel);
        if (wither == null) {
            return;
        }

        wither.moveTo(desiredWitherPos.x, desiredWitherPos.y, desiredWitherPos.z, getRitualYaw(), 0.0F);
        if (!serverLevel.noCollision(wither)) {
            // Fall back to Megumi's position if blocks obstruct behind him in a tight corridor
            wither.moveTo(anchor.x, anchor.y + 0.2D, anchor.z, getRitualYaw(), 0.0F);
        }

        wither.setInvulnerableTicks(0);
        wither.setLockedTargetUuid(targetPlayer.getUUID());
        wither.setTarget(targetPlayer);
        wither.setPersistenceRequired();

        serverLevel.addFreshEntity(wither);

        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, wither.getX(), wither.getY() + 1.5D, wither.getZ(), 2, 0.3D, 0.3D, 0.3D, 0.0D);
        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, anchor.x, anchor.y + 1.2D, anchor.z, 45, 0.8D, 0.8D, 0.8D, 0.12D);
        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, anchor.x, anchor.y + 1.0D, anchor.z, 35, 0.7D, 0.7D, 0.7D, 0.08D);
        serverLevel.playSound(null, wither.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 1.5F, 0.8F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("RitualActive", isRitualActive());
        compound.putInt("RitualTicks", getRitualTicks());
        compound.putFloat("RitualYaw", getRitualYaw());
        compound.putBoolean("Exploded", hasExploded());
        if (this.targetPlayerUuid != null) {
            compound.putUUID("TargetPlayerUUID", this.targetPlayerUuid);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("RitualActive")) {
            this.entityData.set(DATA_RITUAL_ACTIVE, compound.getBoolean("RitualActive"));
            this.entityData.set(DATA_RITUAL_TICKS, compound.getInt("RitualTicks"));
            this.entityData.set(DATA_RITUAL_YAW, compound.getFloat("RitualYaw"));
            this.entityData.set(DATA_EXPLODED, compound.getBoolean("Exploded"));
        }
        if (compound.hasUUID("TargetPlayerUUID")) {
            this.targetPlayerUuid = compound.getUUID("TargetPlayerUUID");
        }
    }
}
