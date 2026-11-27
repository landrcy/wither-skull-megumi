package com.landrcy.impossiblepack.event;

import com.landrcy.impossiblepack.TheImpossiblePackMod;
import com.landrcy.impossiblepack.entity.MegumiWitherSkeletonEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core "The Impossible Pack" combat modifiers:
 * 1. Creepers get struck by lightning 0.1s (2 ticks) after starting their fuse and turn into Charged Creepers.
 * 2. Skeleton arrows curve mid-flight toward the player so they almost always hit.
 * 3. Zombies and Skeletons always spawn with at least 1 piece of armor, weighted toward more pieces,
 *    with progressively lower chances for stronger tiers (Leather -> Copper -> Gold -> Chainmail -> Iron -> Diamond -> Netherite).
 */
@EventBusSubscriber(modid = TheImpossiblePackMod.MODID)
public final class ImpossibleMobEvents {
    private ImpossibleMobEvents() {}

    private static final String ARMORED_TAG = "the_impossible_pack_armored";
    private static final int CREEPER_LIGHTNING_DELAY_TICKS = 2; // 0.1 seconds at 20 TPS

    private static final Map<UUID, Integer> CREEPER_FUSE_TICKS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> CREEPER_BOLT_SHIELD_UNTIL = new ConcurrentHashMap<>();

    private enum ArmorTier {
        LEATHER(Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS, 0.085F),
        COPPER(Items.COPPER_HELMET, Items.COPPER_CHESTPLATE, Items.COPPER_LEGGINGS, Items.COPPER_BOOTS, 0.085F),
        GOLD(Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS, 0.085F),
        CHAINMAIL(Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS, 0.085F),
        IRON(Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS, 0.085F),
        DIAMOND(Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS, 0.04F),
        NETHERITE(Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS, 0.03F);

        final Item helmet;
        final Item chestplate;
        final Item leggings;
        final Item boots;
        final float dropChance;

        ArmorTier(Item helmet, Item chestplate, Item leggings, Item boots, float dropChance) {
            this.helmet = helmet;
            this.chestplate = chestplate;
            this.leggings = leggings;
            this.boots = boots;
            this.dropChance = dropChance;
        }

        Item itemForSlot(EquipmentSlot slot) {
            return switch (slot) {
                case HEAD -> helmet;
                case CHEST -> chestplate;
                case LEGS -> leggings;
                case FEET -> boots;
                default -> helmet;
            };
        }
    }

    // -------------------------------------------------------------------------
    // 1. Guaranteed + Scaled Armor for Zombies & Skeletons
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || event.getLevel().isClientSide()) {
            return;
        }

        Entity entity = event.getEntity();
        // Keep Megumi Wither Skeleton's Jujutsu coat clearly visible without covering it in random armor
        if (entity instanceof MegumiWitherSkeletonEntity) {
            return;
        }

        if (!(entity instanceof Zombie) && !(entity instanceof AbstractSkeleton)) {
            return;
        }

        Mob mob = (Mob) entity;
        if (mob.entityTags().contains(ARMORED_TAG)) {
            return;
        }
        mob.addTag(ARMORED_TAG);

        equipScaledArmor(mob, mob.getRandom());
    }

    private static void equipScaledArmor(Mob mob, RandomSource random) {
        // Piece count distribution: ALWAYS at least 1, weighted toward more pieces:
        // 1 piece: 12% | 2 pieces: 23% | 3 pieces: 30% | 4 pieces (full set): 35%
        float countRoll = random.nextFloat();
        int targetPieces;
        if (countRoll < 0.12F) {
            targetPieces = 1;
        } else if (countRoll < 0.35F) {
            targetPieces = 2;
        } else if (countRoll < 0.65F) {
            targetPieces = 3;
        } else {
            targetPieces = 4;
        }

        List<EquipmentSlot> emptySlots = new ArrayList<>(4);
        int equippedCount = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (mob.getItemBySlot(slot).isEmpty()) {
                emptySlots.add(slot);
            } else {
                equippedCount++;
            }
        }

        int piecesToAdd = Math.max(0, targetPieces - equippedCount);
        if (piecesToAdd == 0 || emptySlots.isEmpty()) {
            return;
        }

        Collections.shuffle(emptySlots, new java.util.Random(random.nextLong()));
        ArmorTier baseTier = rollArmorTier(random);

        for (int i = 0; i < Math.min(piecesToAdd, emptySlots.size()); i++) {
            EquipmentSlot slot = emptySlots.get(i);
            // 70% cohesive tier with the mob's base tier, 30% independently rolled piece for variety
            ArmorTier tier = random.nextFloat() < 0.70F ? baseTier : rollArmorTier(random);
            mob.setItemSlot(slot, new ItemStack(tier.itemForSlot(slot)));
            mob.setDropChance(slot, tier.dropChance);
        }
    }

    /**
     * Lower chance for stronger armor tiers:
     * Leather 34% | Copper 22% | Gold 16% | Chainmail 12% | Iron 10% | Diamond 4.5% | Netherite 1.5%
     */
    private static ArmorTier rollArmorTier(RandomSource random) {
        float roll = random.nextFloat();
        if (roll < 0.340F) {
            return ArmorTier.LEATHER;
        } else if (roll < 0.560F) {
            return ArmorTier.COPPER;
        } else if (roll < 0.720F) {
            return ArmorTier.GOLD;
        } else if (roll < 0.840F) {
            return ArmorTier.CHAINMAIL;
        } else if (roll < 0.940F) {
            return ArmorTier.IRON;
        } else if (roll < 0.985F) {
            return ArmorTier.DIAMOND;
        } else {
            return ArmorTier.NETHERITE;
        }
    }

    // -------------------------------------------------------------------------
    // 2. Creeper 0.1s Lightning Charge & 3. Skeleton Curving/Homing Arrows
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide() || !(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (entity instanceof Creeper creeper) {
            handleCreeperLightningCharge(creeper, serverLevel);
        } else if (entity instanceof AbstractArrow arrow) {
            handleSkeletonCurvingArrow(arrow, serverLevel);
        }
    }

    private static void handleCreeperLightningCharge(Creeper creeper, ServerLevel serverLevel) {
        UUID id = creeper.getUUID();
        if (!creeper.isAlive() || creeper.isPowered()) {
            CREEPER_FUSE_TICKS.remove(id);
            return;
        }

        if (creeper.getSwellDir() > 0 || creeper.isIgnited()) {
            int fuseTicks = CREEPER_FUSE_TICKS.getOrDefault(id, 0) + 1;
            if (fuseTicks >= CREEPER_LIGHTNING_DELAY_TICKS) {
                CREEPER_FUSE_TICKS.remove(id);

                // Shield this creeper from dying to its own charging lightning bolt / fire
                CREEPER_BOLT_SHIELD_UNTIL.put(id, serverLevel.getGameTime() + 12L);

                LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(serverLevel, EntitySpawnReason.EVENT);
                if (bolt != null) {
                    bolt.snapTo(creeper.getX(), creeper.getY(), creeper.getZ(), 0.0F, 0.0F);
                    // Charge immediately so even on the exact strike tick it becomes a Charged Creeper
                    creeper.thunderHit(serverLevel, bolt);
                    creeper.clearFire();
                    serverLevel.addFreshEntity(bolt);
                }
            } else {
                CREEPER_FUSE_TICKS.put(id, fuseTicks);
            }
        } else {
            CREEPER_FUSE_TICKS.remove(id);
        }
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Creeper creeper) || creeper.level().isClientSide()) {
            return;
        }

        Long shieldUntil = CREEPER_BOLT_SHIELD_UNTIL.get(creeper.getUUID());
        if (shieldUntil == null) {
            return;
        }

        long now = creeper.level().getGameTime();
        if (now > shieldUntil) {
            CREEPER_BOLT_SHIELD_UNTIL.remove(creeper.getUUID());
            return;
        }

        if (event.getSource().is(DamageTypeTags.IS_LIGHTNING) || event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setCanceled(true);
            creeper.clearFire();
        }
    }

    private static void handleSkeletonCurvingArrow(AbstractArrow arrow, ServerLevel serverLevel) {
        if (arrow.onGround() || arrow.tickCount < 1) {
            return;
        }

        Entity owner = arrow.getOwner();
        if (!(owner instanceof AbstractSkeleton skeleton)) {
            return;
        }

        Vec3 vel = arrow.getDeltaMovement();
        double speedSqr = vel.lengthSqr();
        if (speedSqr < 0.015D) {
            return;
        }

        // Prefer the skeleton's actual player target, or fall back to nearest survival/adventure player within 32 blocks
        Player target = null;
        if (skeleton.getTarget() instanceof Player p && p.isAlive() && !p.isSpectator() && p.level() == serverLevel && arrow.distanceToSqr(p) <= 48.0D * 48.0D) {
            target = p;
        } else {
            target = serverLevel.getNearestPlayer(
                    arrow.getX(), arrow.getY(), arrow.getZ(),
                    32.0D,
                    e -> e instanceof Player p && p.isAlive() && !p.isSpectator() && !p.isCreative()
            );
        }

        if (target == null) {
            return;
        }

        Vec3 aimPoint = new Vec3(target.getX(), target.getY(0.55D), target.getZ());
        Vec3 toTarget = aimPoint.subtract(arrow.position());
        double dist = toTarget.length();
        if (dist < 0.05D) {
            return;
        }

        Vec3 currentDir = vel.normalize();
        Vec3 desiredDir = toTarget.scale(1.0D / dist);

        // Only curve while target is generally ahead of the arrow (no unnatural 180-degree boomerangs after a miss)
        if (currentDir.dot(desiredDir) < -0.15D) {
            return;
        }

        // Smooth homing curve: subtly bends mid-air at long range and locks cleanly at close range
        double turnStrength = dist < 6.0D ? 0.34D : 0.22D;
        Vec3 steeredDir = currentDir.scale(1.0D - turnStrength).add(desiredDir.scale(turnStrength)).normalize();

        // Keep enough momentum so gravity never drops it short of the player
        double speed = Mth.clamp(Math.sqrt(speedSqr), 1.35D, 2.45D);
        double gravityLift = dist > 3.5D ? 0.042D : 0.025D;
        Vec3 newVel = steeredDir.scale(speed).add(0.0D, gravityLift, 0.0D);

        arrow.setDeltaMovement(newVel);
        float yRot = (float) (Mth.atan2(newVel.x, newVel.z) * (180.0D / Math.PI));
        float xRot = (float) (Mth.atan2(newVel.y, newVel.horizontalDistance()) * (180.0D / Math.PI));
        arrow.setYRot(yRot);
        arrow.setXRot(xRot);
        arrow.needsSync = true;
    }
}
