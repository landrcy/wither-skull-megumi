package com.landrcy.witherscullmegumi.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

/**
 * The Wither manifested at the end of Wither Skull Megumi's summoning ritual.
 * Strictly and permanently locked onto ONLY the player who attacked Megumi.
 */
public class MegumiSummonedWitherEntity extends WitherBoss {

    private static final EntityDataAccessor<Optional<UUID>> DATA_LOCKED_TARGET_UUID =
            SynchedEntityData.defineId(MegumiSummonedWitherEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    public MegumiSummonedWitherEntity(EntityType<? extends WitherBoss> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_LOCKED_TARGET_UUID, Optional.empty());
    }

    public void setLockedTargetUuid(@Nullable UUID uuid) {
        this.entityData.set(DATA_LOCKED_TARGET_UUID, Optional.ofNullable(uuid));
    }

    @Nullable
    public UUID getLockedTargetUuid() {
        return this.entityData.get(DATA_LOCKED_TARGET_UUID).orElse(null);
    }

    public boolean isLockedTarget(@Nullable LivingEntity candidate) {
        if (candidate == null || !candidate.isAlive() || candidate.isSpectator()) {
            return false;
        }
        UUID locked = getLockedTargetUuid();
        return locked != null && locked.equals(candidate.getUUID());
    }

    @Nullable
    public Player findLockedPlayer() {
        UUID locked = getLockedTargetUuid();
        if (locked == null) {
            return null;
        }
        Player player = this.level().getPlayerByUUID(locked);
        if (player != null && player.isAlive() && !player.isSpectator() && player.distanceToSqr(this) <= 128.0D * 128.0D) {
            return player;
        }
        return null;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new WitherInvulnerableHoldGoal());
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0D, 40, 20.0F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        // Exclusive target goal: ONLY ever targets the player bound by the ritual.
        this.targetSelector.addGoal(1, new LockedPlayerOnlyTargetGoal(this));
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return isLockedTarget(target);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null && !isLockedTarget(target)) {
            return;
        }
        super.setTarget(target);
    }

    @Override
    public void setLastHurtByMob(@Nullable LivingEntity livingEntity) {
        // Ignore aggro provocation from any entity other than our sole bound target.
        if (livingEntity != null && !isLockedTarget(livingEntity)) {
            return;
        }
        super.setLastHurtByMob(livingEntity);
    }

    @Override
    protected void customServerAiStep() {
        Player lockedPlayer = findLockedPlayer();
        if (lockedPlayer != null) {
            super.setTarget(lockedPlayer);
            int targetId = lockedPlayer.getId();
            this.setAlternativeTarget(0, targetId);
            this.setAlternativeTarget(1, targetId);
            this.setAlternativeTarget(2, targetId);
        } else {
            super.setTarget(null);
            this.setAlternativeTarget(0, 0);
            this.setAlternativeTarget(1, 0);
            this.setAlternativeTarget(2, 0);
        }

        super.customServerAiStep();

        // Enforce strict target lock on all 3 Wither heads after vanilla AI step as well.
        if (lockedPlayer != null) {
            int targetId = lockedPlayer.getId();
            this.setAlternativeTarget(0, targetId);
            this.setAlternativeTarget(1, targetId);
            this.setAlternativeTarget(2, targetId);
        } else {
            this.setAlternativeTarget(0, 0);
            this.setAlternativeTarget(1, 0);
            this.setAlternativeTarget(2, 0);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, amount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        UUID locked = getLockedTargetUuid();
        if (locked != null) {
            compound.putUUID("LockedTargetUUID", locked);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.hasUUID("LockedTargetUUID")) {
            setLockedTargetUuid(compound.getUUID("LockedTargetUUID"));
        }
    }

    private class WitherInvulnerableHoldGoal extends Goal {
        public WitherInvulnerableHoldGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return MegumiSummonedWitherEntity.this.getInvulnerableTicks() > 0;
        }
    }

    private static class LockedPlayerOnlyTargetGoal extends TargetGoal {
        private final MegumiSummonedWitherEntity wither;
        @Nullable
        private Player lockedTarget;

        public LockedPlayerOnlyTargetGoal(MegumiSummonedWitherEntity wither) {
            super(wither, false, false);
            this.wither = wither;
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            this.lockedTarget = this.wither.findLockedPlayer();
            return this.lockedTarget != null;
        }

        @Override
        public boolean canContinueToUse() {
            Player current = this.wither.findLockedPlayer();
            if (current == null) {
                return false;
            }
            this.lockedTarget = current;
            return true;
        }

        @Override
        public void start() {
            this.wither.setTarget(this.lockedTarget);
            super.start();
        }
    }
}
