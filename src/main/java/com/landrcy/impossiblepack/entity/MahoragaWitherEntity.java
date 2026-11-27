package com.landrcy.impossiblepack.entity;

import com.landrcy.impossiblepack.registry.ModSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Eight-Handled Mahoraga Wither:
 * - Wears the 3D Golden Eight-Handled Divine Wheel floating & rotating above its head.
 * - Exclusively locks onto ONLY the player who attacked the coated Wither Skeleton.
 * - Turns its Divine Wheel 45 degrees and plays divine_wheel_adapting right upon completing the summon
 *   AND whenever adapting to the player's damage types in combat!
 */
public class MahoragaWitherEntity extends WitherBoss {

    private static final EntityDataAccessor<String> DATA_LOCKED_TARGET_UUID =
            SynchedEntityData.defineId(MahoragaWitherEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_WHEEL_STEPS =
            SynchedEntityData.defineId(MahoragaWitherEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LAST_WHEEL_TURN_TICK =
            SynchedEntityData.defineId(MahoragaWitherEntity.class, EntityDataSerializers.INT);

    private float accumulatedDamageSinceTurn = 0.0F;
    private int meleeAdaptStacks = 0;
    private int projectileAdaptStacks = 0;
    private int explosionAdaptStacks = 0;
    private int magicAdaptStacks = 0;

    public MahoragaWitherEntity(EntityType<? extends WitherBoss> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_LOCKED_TARGET_UUID, "");
        builder.define(DATA_WHEEL_STEPS, 0);
        builder.define(DATA_LAST_WHEEL_TURN_TICK, -100);
    }

    public void setLockedTargetUuid(@Nullable UUID uuid) {
        this.entityData.set(DATA_LOCKED_TARGET_UUID, uuid == null ? "" : uuid.toString());
    }

    @Nullable
    public UUID getLockedTargetUuid() {
        String raw = this.entityData.get(DATA_LOCKED_TARGET_UUID);
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public int getWheelSteps() {
        return this.entityData.get(DATA_WHEEL_STEPS);
    }

    public int getLastWheelTurnTick() {
        return this.entityData.get(DATA_LAST_WHEEL_TURN_TICK);
    }

    public void triggerWheelAdaptationTurn(ServerLevel serverLevel) {
        this.entityData.set(DATA_WHEEL_STEPS, getWheelSteps() + 1);
        this.entityData.set(DATA_LAST_WHEEL_TURN_TICK, this.tickCount);
        serverLevel.playSound(
                null,
                this.getX(),
                this.getY() + 2.5D,
                this.getZ(),
                ModSounds.DIVINE_WHEEL_ADAPTING.get(),
                SoundSource.HOSTILE,
                2.2F,
                1.0F
        );
    }

    public boolean isLockedTarget(@Nullable LivingEntity candidate) {
        if (candidate == null || !candidate.isAlive() || candidate.isSpectator()) {
            return false;
        }
        UUID locked = getLockedTargetUuid();
        if (locked == null) {
            // If spawned directly via /summon or spawn egg without a ritual attacker, bind to the first player seen
            return candidate instanceof Player;
        }
        return locked.equals(candidate.getUUID());
    }

    @Nullable
    public Player findLockedPlayer() {
        UUID locked = getLockedTargetUuid();
        if (locked != null) {
            Player player = this.level().getPlayerByUUID(locked);
            if (player != null && player.isAlive() && !player.isSpectator() && player.distanceToSqr(this) <= 128.0D * 128.0D) {
                return player;
            }
            return null;
        }
        // Fallback when spawned directly via /summon mahoraga_wither: lock onto nearest non-spectator player
        Player nearest = this.level().getNearestPlayer(this, 64.0D);
        if (nearest != null && !nearest.isSpectator()) {
            setLockedTargetUuid(nearest.getUUID());
            return nearest;
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
        if (livingEntity != null && !isLockedTarget(livingEntity)) {
            return;
        }
        super.setLastHurtByMob(livingEntity);
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
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

        super.customServerAiStep(serverLevel);

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
    public boolean hurtServer(ServerLevel serverLevel, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(serverLevel, source, amount);
        }

        // Apply existing Mahoraga Divine Wheel adaptation damage reduction
        int stacks = getStacksForSource(source);
        float reduction = Math.min(0.65F, stacks * 0.16F);
        float adjustedAmount = amount * (1.0F - reduction);

        boolean hurt = super.hurtServer(serverLevel, source, adjustedAmount);
        if (hurt && this.isAlive()) {
            this.accumulatedDamageSinceTurn += amount;
            boolean readyByCooldown = (this.tickCount - getLastWheelTurnTick()) >= 60;
            if (this.accumulatedDamageSinceTurn >= 30.0F && readyByCooldown) {
                this.accumulatedDamageSinceTurn = 0.0F;
                incrementStacksForSource(source);
                this.heal(12.0F);
                triggerWheelAdaptationTurn(serverLevel);
            }
        }
        return hurt;
    }

    private int getStacksForSource(DamageSource source) {
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            return projectileAdaptStacks;
        } else if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            return explosionAdaptStacks;
        } else if (source.is(DamageTypeTags.WITCH_RESISTANT_TO)) {
            return magicAdaptStacks;
        }
        return meleeAdaptStacks;
    }

    private void incrementStacksForSource(DamageSource source) {
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            projectileAdaptStacks = Math.min(4, projectileAdaptStacks + 1);
        } else if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            explosionAdaptStacks = Math.min(4, explosionAdaptStacks + 1);
        } else if (source.is(DamageTypeTags.WITCH_RESISTANT_TO)) {
            magicAdaptStacks = Math.min(4, magicAdaptStacks + 1);
        } else {
            meleeAdaptStacks = Math.min(4, meleeAdaptStacks + 1);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        String raw = this.entityData.get(DATA_LOCKED_TARGET_UUID);
        if (raw != null && !raw.isEmpty()) {
            output.putString("LockedTargetUUID", raw);
        }
        output.putInt("WheelSteps", getWheelSteps());
        output.putInt("MeleeAdapt", meleeAdaptStacks);
        output.putInt("ProjectileAdapt", projectileAdaptStacks);
        output.putInt("ExplosionAdapt", explosionAdaptStacks);
        output.putInt("MagicAdapt", magicAdaptStacks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        String raw = input.getStringOr("LockedTargetUUID", "");
        this.entityData.set(DATA_LOCKED_TARGET_UUID, raw);
        this.entityData.set(DATA_WHEEL_STEPS, input.getIntOr("WheelSteps", 0));
        this.meleeAdaptStacks = input.getIntOr("MeleeAdapt", 0);
        this.projectileAdaptStacks = input.getIntOr("ProjectileAdapt", 0);
        this.explosionAdaptStacks = input.getIntOr("ExplosionAdapt", 0);
        this.magicAdaptStacks = input.getIntOr("MagicAdapt", 0);
    }

    private class WitherInvulnerableHoldGoal extends Goal {
        public WitherInvulnerableHoldGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return MahoragaWitherEntity.this.getInvulnerableTicks() > 0;
        }
    }

    private static class LockedPlayerOnlyTargetGoal extends TargetGoal {
        private final MahoragaWitherEntity wither;
        @Nullable
        private Player lockedTarget;

        public LockedPlayerOnlyTargetGoal(MahoragaWitherEntity wither) {
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
