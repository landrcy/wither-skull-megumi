package com.landrcy.impossiblepack.event;

import com.landrcy.impossiblepack.TheImpossiblePackMod;
import com.landrcy.impossiblepack.network.MobVfxPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Three signature mob gimmicks with custom particles & animations:
 * 1. Enderman — Boogie Woogie Position Swap (cursed-energy shockwave rings + directional swap streams + crisp clap)
 * 2. Spider / Cave Spider — Rear-Spring Silky Web Shot (abdomen springs upward, fires trailing silky web strands that trap & slow)
 * 3. Zombie — Look-Down Bridge Up, Look-Down Bridge Over, Block Breaking, & Baby-Zombie Fastball
 */
@EventBusSubscriber(modid = TheImpossiblePackMod.MODID)
public final class MobGimmickEvents {
    private MobGimmickEvents() {}

    // --- 1. Enderman Boogie Woogie state ---
    private static final Map<UUID, Long> ENDERMAN_SWAP_COOLDOWN = new ConcurrentHashMap<>();

    // --- 2. Spider Rear-Spring Silky Web state ---
    private static final int SPIDER_WINDUP_TICKS = 10;
    private static final long TEMP_COBWEB_DURATION_TICKS = 100L; // 5 seconds
    private static final Map<UUID, Long> SPIDER_WEB_COOLDOWN = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> SPIDER_WINDUP = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, List<WebShot>> ACTIVE_WEB_SHOTS = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, Map<BlockPos, Long>> TEMP_COBWEBS = new ConcurrentHashMap<>();

    // --- 3. Zombie Bridge / Break / Fastball state ---
    private static final Map<UUID, Long> ZOMBIE_ACTION_COOLDOWN = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> ZOMBIE_THROW_COOLDOWN = new ConcurrentHashMap<>();
    private static final Map<UUID, LookDownPose> ZOMBIE_LOOK_DOWN = new ConcurrentHashMap<>();
    private static final Map<UUID, BlockBreakProgress> ZOMBIE_BREAK_STATE = new ConcurrentHashMap<>();

    private static final class WebShot {
        final UUID shooterId;
        Vec3 pos;
        Vec3 vel;
        int age;
        final int maxAge;

        WebShot(UUID shooterId, Vec3 pos, Vec3 vel, int maxAge) {
            this.shooterId = shooterId;
            this.pos = pos;
            this.vel = vel;
            this.age = 0;
            this.maxAge = maxAge;
        }
    }

    private record LookDownPose(double x, double y, double z, float pitch, long untilTick) {}

    private static final class BlockBreakProgress {
        final BlockPos pos;
        int ticks;
        long lastTick;

        BlockBreakProgress(BlockPos pos, long lastTick) {
            this.pos = pos;
            this.ticks = 0;
            this.lastTick = lastTick;
        }
    }

    // =========================================================================
    // TICK & COMBAT EVENT HOOKS
    // =========================================================================

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide() || !(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (entity instanceof Enderman enderman) {
            tickEndermanBoogieWoogie(enderman, serverLevel);
        } else if (entity instanceof Spider spider) {
            tickSpiderWebShot(spider, serverLevel);
        } else if (entity instanceof Zombie zombie) {
            tickZombieSiege(zombie, serverLevel);
        }
    }

    @SubscribeEvent
    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        tickWebShots(serverLevel);
        tickTempCobwebs(serverLevel);
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide() || !(event.getEntity().level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Enderman reactive Boogie Woogie when attacked by a player
        if (event.getEntity() instanceof Enderman enderman
                && event.getSource().getEntity() instanceof ServerPlayer player
                && isValidTargetPlayer(player, serverLevel)) {
            long now = serverLevel.getGameTime();
            long readyAt = ENDERMAN_SWAP_COOLDOWN.getOrDefault(enderman.getUUID(), 0L);
            boolean playerCheesing = player.isInWater() || (player.getY() - enderman.getY() >= 1.75D);
            if (now >= readyAt && enderman.distanceToSqr(player) <= 22.0D * 22.0D) {
                if (playerCheesing || enderman.getRandom().nextFloat() < 0.45F) {
                    performBoogieWoogieSwap(enderman, player, serverLevel, now);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide() || !(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        UUID id = entity.getUUID();
        ENDERMAN_SWAP_COOLDOWN.remove(id);
        SPIDER_WEB_COOLDOWN.remove(id);
        SPIDER_WINDUP.remove(id);
        ZOMBIE_ACTION_COOLDOWN.remove(id);
        ZOMBIE_THROW_COOLDOWN.remove(id);
        ZOMBIE_LOOK_DOWN.remove(id);

        BlockBreakProgress breakState = ZOMBIE_BREAK_STATE.remove(id);
        if (breakState != null) {
            serverLevel.destroyBlockProgress(entity.getId(), breakState.pos, -1);
        }
    }

    private static boolean isValidTargetPlayer(Player player, ServerLevel serverLevel) {
        return player != null
                && player.isAlive()
                && !player.isSpectator()
                && !player.isCreative()
                && player.level() == serverLevel;
    }

    // =========================================================================
    // 1. ENDERMAN — BOOGIE WOOGIE POSITION SWAP
    // =========================================================================

    private static void tickEndermanBoogieWoogie(Enderman enderman, ServerLevel serverLevel) {
        if (!enderman.isAlive()) {
            return;
        }
        if (!(enderman.getTarget() instanceof ServerPlayer player) || !isValidTargetPlayer(player, serverLevel)) {
            return;
        }

        double distSqr = enderman.distanceToSqr(player);
        if (distSqr < 2.2D * 2.2D || distSqr > 24.0D * 24.0D) {
            return;
        }

        long now = serverLevel.getGameTime();
        long readyAt = ENDERMAN_SWAP_COOLDOWN.getOrDefault(enderman.getUUID(), 0L);
        if (readyAt == 0L) {
            // Brief initial grace period after aggro before first clap
            ENDERMAN_SWAP_COOLDOWN.put(enderman.getUUID(), now + 35L + enderman.getRandom().nextInt(25));
            return;
        }

        boolean playerCheesing = player.isInWater() || (player.getY() - enderman.getY() >= 2.0D);
        // If player tries water/pillar cheese, shave cooldown so Enderman immediately claps them down
        if (playerCheesing && now + 35L < readyAt) {
            readyAt = now + 15L;
            ENDERMAN_SWAP_COOLDOWN.put(enderman.getUUID(), readyAt);
        }

        if (now < readyAt) {
            return;
        }

        performBoogieWoogieSwap(enderman, player, serverLevel, now);
    }

    private static void performBoogieWoogieSwap(Enderman enderman, ServerPlayer player, ServerLevel serverLevel, long now) {
        Vec3 enderPos = enderman.position();
        Vec3 playerPos = player.position();

        // Find clean spot near player's old position so Enderman doesn't suffocate inside a 2-high ceiling trap
        Vec3 enderDest = findSafeEndermanSpot(serverLevel, playerPos);
        Vec3 playerDest = enderPos;

        // Play Boogie Woogie CLAP at BOTH original positions before moving
        playBoogieClapSound(serverLevel, enderPos);
        playBoogieClapSound(serverLevel, playerPos);

        // Execute instant position swap
        player.resetFallDistance();
        enderman.resetFallDistance();
        player.teleportTo(
                serverLevel,
                playerDest.x, playerDest.y, playerDest.z,
                Set.of(),
                player.getYRot(),
                player.getXRot(),
                false
        );
        enderman.snapTo(enderDest.x, enderDest.y, enderDest.z, enderman.getYRot(), enderman.getXRot());
        enderman.getNavigation().stop();
        enderman.setTarget(player);
        enderman.swingForAttack(InteractionHand.MAIN_HAND);
        enderman.swingForAttack(InteractionHand.OFF_HAND);

        // Broadcast custom Boogie Woogie rings + directional A<->B cursed-energy streams
        Vec3 mid = enderPos.lerp(playerPos, 0.5D);
        MobVfxPayload payload = new MobVfxPayload(
                MobVfxPayload.BOOGIE_WOOGIE_SWAP,
                enderman.getId(),
                enderPos.x, enderPos.y + 0.55D, enderPos.z,
                playerPos.x, playerPos.y + 0.55D, playerPos.z
        );
        PacketDistributor.sendToPlayersNear(serverLevel, null, mid.x, mid.y, mid.z, 64.0D, payload);

        // Next swap cooldown: 4.5s - 6.5s (90 - 130 ticks)
        ENDERMAN_SWAP_COOLDOWN.put(enderman.getUUID(), now + 90L + enderman.getRandom().nextInt(41));
    }

    private static Vec3 findSafeEndermanSpot(ServerLevel level, Vec3 desired) {
        BlockPos base = BlockPos.containing(desired);
        if (hasClearance(level, base, 3)) {
            return desired;
        }
        int[][] offsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}};
        for (int[] off : offsets) {
            BlockPos candidate = base.offset(off[0], 0, off[1]);
            if (hasClearance(level, candidate, 3)) {
                return new Vec3(candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D);
            }
        }
        return desired;
    }

    private static boolean hasClearance(ServerLevel level, BlockPos base, int height) {
        for (int i = 0; i < height; i++) {
            BlockPos p = base.above(i);
            if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static void playBoogieClapSound(ServerLevel level, Vec3 pos) {
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, 0.90F, 1.85F);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.00F, 1.45F);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.80F, 1.25F);
    }

    // =========================================================================
    // 2. SPIDER / CAVE SPIDER — REAR-SPRING SILKY WEB SHOT
    // =========================================================================

    private static void tickSpiderWebShot(Spider spider, ServerLevel serverLevel) {
        if (!spider.isAlive()) {
            SPIDER_WINDUP.remove(spider.getUUID());
            return;
        }

        UUID id = spider.getUUID();
        long now = serverLevel.getGameTime();

        // Check active rear-spring windup
        Integer windup = SPIDER_WINDUP.get(id);
        if (windup != null) {
            // Brace briefly so the spider's springing abdomen pose is super readable
            Vec3 vel = spider.getDeltaMovement();
            spider.setDeltaMovement(vel.x * 0.65D, vel.y, vel.z * 0.65D);

            if (spider.getTarget() instanceof Player target && isValidTargetPlayer(target, serverLevel)) {
                spider.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }

            if (windup <= 1) {
                SPIDER_WINDUP.remove(id);
                if (spider.getTarget() instanceof Player target && isValidTargetPlayer(target, serverLevel)) {
                    fireSpiderSilkyWeb(spider, target, serverLevel);
                }
            } else {
                SPIDER_WINDUP.put(id, windup - 1);
            }
            return;
        }

        // Start a new rear-spring web shot when aggroed at mid-range
        if (!(spider.getTarget() instanceof Player target) || !isValidTargetPlayer(target, serverLevel)) {
            return;
        }

        long readyAt = SPIDER_WEB_COOLDOWN.getOrDefault(id, 0L);
        if (readyAt == 0L) {
            SPIDER_WEB_COOLDOWN.put(id, now + 25L + spider.getRandom().nextInt(25));
            return;
        }
        if (now < readyAt) {
            return;
        }

        double distSqr = spider.distanceToSqr(target);
        if (distSqr < 3.2D * 3.2D || distSqr > 17.5D * 17.5D) {
            return;
        }
        if (!spider.hasLineOfSight(target)) {
            return;
        }

        // Trigger the Spider's rear-spring animation on clients + schedule launch at peak of spring!
        SPIDER_WINDUP.put(id, SPIDER_WINDUP_TICKS);
        SPIDER_WEB_COOLDOWN.put(id, now + 85L + spider.getRandom().nextInt(40));

        Vec3 rearPos = computeSpiderRearOrigin(spider);
        MobVfxPayload springPayload = new MobVfxPayload(
                MobVfxPayload.SPIDER_SPRING_POSE,
                spider.getId(),
                rearPos.x, rearPos.y, rearPos.z,
                0.0D, 0.0D, 0.0D
        );
        PacketDistributor.sendToPlayersNear(serverLevel, null, spider.getX(), spider.getY(), spider.getZ(), 48.0D, springPayload);
        serverLevel.playSound(null, spider.getX(), spider.getY(), spider.getZ(), SoundEvents.SPIDER_STEP, SoundSource.HOSTILE, 0.8F, 1.7F);
    }

    private static Vec3 computeSpiderRearOrigin(Spider spider) {
        float yawRad = spider.yBodyRot * Mth.DEG_TO_RAD;
        // Opposite of facing direction = behind the spider's thorax where its raised abdomen/butt sits
        double backX = Math.sin(yawRad);
        double backZ = -Math.cos(yawRad);
        return new Vec3(
                spider.getX() + backX * 0.65D,
                spider.getY() + spider.getBbHeight() * 0.85D + 0.28D,
                spider.getZ() + backZ * 0.65D
        );
    }

    private static void fireSpiderSilkyWeb(Spider spider, Player target, ServerLevel serverLevel) {
        Vec3 origin = computeSpiderRearOrigin(spider);
        double dist = origin.distanceTo(target.position());
        // Lead target slightly so sprinting players can still get caught by the flying silk
        double travelTicks = Mth.clamp(dist / 0.88D, 2.0D, 18.0D);
        Vec3 aimPoint = new Vec3(target.getX(), target.getY() + 0.45D, target.getZ())
                .add(target.getDeltaMovement().scale(Math.min(travelTicks * 0.45D, 4.5D)));

        Vec3 delta = aimPoint.subtract(origin);
        double len = Math.max(0.1D, delta.length());
        double speed = 0.88D;
        // Ballistic loft to compensate for gentle silk gravity
        double arcLift = 0.014D * travelTicks;
        Vec3 vel = delta.scale(speed / len).add(0.0D, arcLift, 0.0D);

        ACTIVE_WEB_SHOTS
                .computeIfAbsent(serverLevel.dimension(), k -> new ArrayList<>())
                .add(new WebShot(spider.getUUID(), origin, vel, 36));

        serverLevel.playSound(null, origin.x, origin.y, origin.z, SoundEvents.WIND_CHARGE_THROW, SoundSource.HOSTILE, 0.75F, 1.75F);
        serverLevel.playSound(null, origin.x, origin.y, origin.z, SoundEvents.COBWEB_PLACE, SoundSource.HOSTILE, 0.85F, 1.35F);

        // Initial silky launch burst right off the raised spider abdomen
        MobVfxPayload streamPayload = new MobVfxPayload(
                MobVfxPayload.SPIDER_WEB_STREAM,
                spider.getId(),
                origin.x, origin.y, origin.z,
                vel.x, vel.y, vel.z
        );
        PacketDistributor.sendToPlayersNear(serverLevel, null, origin.x, origin.y, origin.z, 52.0D, streamPayload);
    }

    private static void tickWebShots(ServerLevel serverLevel) {
        List<WebShot> shots = ACTIVE_WEB_SHOTS.get(serverLevel.dimension());
        if (shots == null || shots.isEmpty()) {
            return;
        }

        Iterator<WebShot> it = shots.iterator();
        while (it.hasNext()) {
            WebShot shot = it.next();
            shot.age++;
            if (shot.age > shot.maxAge) {
                it.remove();
                continue;
            }

            Vec3 prevPos = shot.pos;
            shot.vel = shot.vel.scale(0.985D).add(0.0D, -0.025D, 0.0D);
            Vec3 nextPos = prevPos.add(shot.vel);

            // Emit continuous silky web trail particles on clients
            MobVfxPayload trail = new MobVfxPayload(
                    MobVfxPayload.SPIDER_WEB_STREAM,
                    0,
                    nextPos.x, nextPos.y, nextPos.z,
                    shot.vel.x, shot.vel.y, shot.vel.z
            );
            PacketDistributor.sendToPlayersNear(serverLevel, null, nextPos.x, nextPos.y, nextPos.z, 52.0D, trail);

            // Check entity hit along flight segment
            AABB sweep = new AABB(prevPos, nextPos).inflate(0.45D);
            List<LivingEntity> hits = serverLevel.getEntitiesOfClass(
                    LivingEntity.class,
                    sweep,
                    e -> e.isAlive()
                            && !(e instanceof Spider)
                            && (!(e instanceof Player p) || (!p.isSpectator() && !p.isCreative()))
            );

            if (!hits.isEmpty()) {
                LivingEntity victim = hits.get(0);
                Entity shooterEntity = serverLevel.getEntity(shot.shooterId);
                if (shooterEntity instanceof LivingEntity shooter) {
                    victim.hurtServer(serverLevel, serverLevel.damageSources().mobAttack(shooter), 2.0F);
                } else {
                    victim.hurtServer(serverLevel, serverLevel.damageSources().generic(), 2.0F);
                }
                victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0));

                BlockPos trapPos = BlockPos.containing(victim.getX(), victim.getY() + 0.1D, victim.getZ());
                placeTemporaryCobweb(serverLevel, trapPos);
                sendWebSplat(serverLevel, victim.position().add(0.0D, 0.55D, 0.0D));
                it.remove();
                continue;
            }

            // Check solid block collision
            BlockPos nextBlock = BlockPos.containing(nextPos);
            BlockState hitState = serverLevel.getBlockState(nextBlock);
            if (!hitState.getCollisionShape(serverLevel, nextBlock).isEmpty()) {
                BlockPos placePos = BlockPos.containing(prevPos);
                placeTemporaryCobweb(serverLevel, placePos);
                sendWebSplat(serverLevel, nextPos);
                it.remove();
                continue;
            }

            shot.pos = nextPos;
        }
    }

    private static void placeTemporaryCobweb(ServerLevel level, BlockPos pos) {
        BlockState existing = level.getBlockState(pos);
        if (existing.isAir() || existing.canBeReplaced()) {
            level.setBlock(pos, Blocks.COBWEB.defaultBlockState(), 3);
            TEMP_COBWEBS
                    .computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>())
                    .put(pos.immutable(), level.getGameTime() + TEMP_COBWEB_DURATION_TICKS);
        }
    }

    private static void sendWebSplat(ServerLevel level, Vec3 pos) {
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.COBWEB_PLACE, SoundSource.HOSTILE, 0.95F, 1.05F);
        MobVfxPayload splat = new MobVfxPayload(
                MobVfxPayload.SPIDER_WEB_SPLAT,
                0,
                pos.x, pos.y, pos.z,
                0.0D, 0.0D, 0.0D
        );
        PacketDistributor.sendToPlayersNear(level, null, pos.x, pos.y, pos.z, 52.0D, splat);
    }

    private static void tickTempCobwebs(ServerLevel level) {
        Map<BlockPos, Long> webs = TEMP_COBWEBS.get(level.dimension());
        if (webs == null || webs.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Iterator<Map.Entry<BlockPos, Long>> it = webs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Long> entry = it.next();
            if (now >= entry.getValue()) {
                BlockPos pos = entry.getKey();
                if (level.getBlockState(pos).is(Blocks.COBWEB)) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                            SoundEvents.COBWEB_BREAK, SoundSource.BLOCKS, 0.45F, 1.35F);
                }
                it.remove();
            }
        }
    }

    // =========================================================================
    // 3. ZOMBIE — LOOK DOWN & BRIDGE UP / OVER + BREAK BLOCKS + BABY FASTBALL
    // =========================================================================

    private static void tickZombieSiege(Zombie zombie, ServerLevel serverLevel) {
        if (!zombie.isAlive() || zombie.isBaby()) {
            return;
        }

        UUID id = zombie.getUUID();
        long now = serverLevel.getGameTime();

        // Maintain active "looking down" pose while bridging up or over
        LookDownPose lookPose = ZOMBIE_LOOK_DOWN.get(id);
        if (lookPose != null) {
            if (now <= lookPose.untilTick()) {
                zombie.getLookControl().setLookAt(lookPose.x(), lookPose.y(), lookPose.z(), 45.0F, 65.0F);
                zombie.setXRot(lookPose.pitch());
            } else {
                ZOMBIE_LOOK_DOWN.remove(id);
            }
        }

        if (!(zombie.getTarget() instanceof Player player) || !isValidTargetPlayer(player, serverLevel)) {
            clearZombieBreakProgress(zombie, serverLevel);
            return;
        }

        double dx = player.getX() - zombie.getX();
        double dy = player.getY() - zombie.getY();
        double dz = player.getZ() - zombie.getZ();
        double horizDist = Math.sqrt(dx * dx + dz * dz);

        if (horizDist > 20.0D || Math.abs(dy) > 18.0D) {
            clearZombieBreakProgress(zombie, serverLevel);
            return;
        }

        // 3A. Baby-Zombie Fastball toss onto elevated players
        if (dy >= 2.0D && horizDist <= 13.0D && now >= ZOMBIE_THROW_COOLDOWN.getOrDefault(id, 0L)) {
            if (tryTossNearbyBabyZombie(zombie, player, serverLevel, now)) {
                return;
            }
        }

        // Direction unit vector toward player on XZ plane
        double invHoriz = horizDist > 0.05D ? (1.0D / horizDist) : 0.0D;
        double dirX = dx * invHoriz;
        double dirZ = dz * invHoriz;

        int stepX = Math.abs(dirX) >= 0.35D ? (dirX > 0.0D ? 1 : -1) : 0;
        int stepZ = Math.abs(dirZ) >= 0.35D ? (dirZ > 0.0D ? 1 : -1) : 0;
        if (stepX == 0 && stepZ == 0) {
            stepX = 1;
        }

        BlockPos feetPos = zombie.blockPosition();
        BlockPos frontPos = feetPos.offset(stepX, 0, stepZ);
        BlockPos frontHeadPos = frontPos.above();
        BlockPos frontBelowPos = frontPos.below();
        BlockPos ceilingPos = feetPos.above(2);

        long actionReadyAt = ZOMBIE_ACTION_COOLDOWN.getOrDefault(id, 0L);

        // 3B. Look Down & Bridge UP (Pillar toward elevated player + break ceiling overhead if blocked)
        boolean shouldBridgeUp = dy >= 1.65D && horizDist <= 14.0D
                && (horizDist <= 5.5D || isSolidBlock(serverLevel, frontPos) || isSolidBlock(serverLevel, frontHeadPos)
                || zombie.getDeltaMovement().horizontalDistanceSqr() < 0.004D);

        if (shouldBridgeUp) {
            // If a ceiling is blocking upward pillaring, break through the ceiling first!
            if (isSolidBlock(serverLevel, ceilingPos)) {
                if (canZombieBreak(serverLevel, ceilingPos)) {
                    tickZombieBlockBreak(zombie, serverLevel, ceilingPos, now);
                }
                return;
            }

            // Also if standing right next to a wall at eye level while rising, clear it if needed
            clearZombieBreakProgress(zombie, serverLevel);

            if (now >= actionReadyAt && (zombie.onGround() || zombie.isInWater())) {
                BlockState feetState = serverLevel.getBlockState(feetPos);
                if (feetState.isAir() || feetState.canBeReplaced()) {
                    ensureCobblestoneOffhand(zombie);

                    // Physically look straight down at feet while pillaring!
                    ZOMBIE_LOOK_DOWN.put(id, new LookDownPose(
                            zombie.getX(), zombie.getY() - 2.2D, zombie.getZ(), 82.0F, now + 10L
                    ));
                    zombie.getLookControl().setLookAt(zombie.getX(), zombie.getY() - 2.2D, zombie.getZ(), 60.0F, 80.0F);
                    zombie.setXRot(82.0F);

                    // Lift zombie smoothly onto the new pillar block and jump
                    double newY = feetPos.getY() + 1.02D;
                    zombie.setPos(zombie.getX(), Math.max(zombie.getY() + 0.35D, newY), zombie.getZ());
                    zombie.setDeltaMovement(dirX * 0.12D, 0.42D, dirZ * 0.12D);
                    zombie.needsSync = true;

                    serverLevel.setBlock(feetPos, Blocks.COBBLESTONE.defaultBlockState(), 3);
                    zombie.swingForAttack(InteractionHand.OFF_HAND);
                    serverLevel.playSound(null, feetPos.getX() + 0.5D, feetPos.getY() + 0.5D, feetPos.getZ() + 0.5D,
                            SoundEvents.STONE_PLACE, SoundSource.HOSTILE, 0.85F, 0.95F);
                    sendZombieDebris(serverLevel, zombie.getId(),
                            feetPos.getX() + 0.5D, feetPos.getY() + 0.85D, feetPos.getZ() + 0.5D,
                            0.0D, 0.5D, 0.0D);

                    ZOMBIE_ACTION_COOLDOWN.put(id, now + 13L);
                }
            }
            return;
        }

        // 3C. Look Down & Bridge OVER Gaps / Ravines / Moats / Between Pillars
        if (horizDist >= 1.8D && Math.abs(dy) <= 3.2D) {
            boolean frontOpen = !isSolidBlock(serverLevel, frontPos) && !isSolidBlock(serverLevel, frontHeadPos);
            BlockState belowState = serverLevel.getBlockState(frontBelowPos);
            boolean belowPlaceable = belowState.isAir() || belowState.canBeReplaced();
            boolean isRealGap = !belowState.getFluidState().isEmpty()
                    || (!isSolidBlock(serverLevel, frontBelowPos.below()) && belowPlaceable);

            if (frontOpen && belowPlaceable && isRealGap) {
                clearZombieBreakProgress(zombie, serverLevel);
                if (now >= actionReadyAt) {
                    ensureCobblestoneOffhand(zombie);

                    double bx = frontBelowPos.getX() + 0.5D;
                    double by = frontBelowPos.getY() + 0.85D;
                    double bz = frontBelowPos.getZ() + 0.5D;

                    // Look down at the gap edge as the zombie bridges across!
                    ZOMBIE_LOOK_DOWN.put(id, new LookDownPose(bx, by, bz, 68.0F, now + 9L));
                    zombie.getLookControl().setLookAt(bx, by, bz, 60.0F, 75.0F);
                    zombie.setXRot(68.0F);

                    serverLevel.setBlock(frontBelowPos, Blocks.COBBLESTONE.defaultBlockState(), 3);
                    zombie.swingForAttack(InteractionHand.OFF_HAND);
                    serverLevel.playSound(null, bx, by, bz, SoundEvents.STONE_PLACE, SoundSource.HOSTILE, 0.85F, 1.05F);
                    sendZombieDebris(serverLevel, zombie.getId(), bx, by, bz, dirX, 0.45D, dirZ);

                    // Step confidently onto the newly placed bridge block
                    zombie.setDeltaMovement(dirX * 0.20D, Math.max(zombie.getDeltaMovement().y, 0.06D), dirZ * 0.20D);
                    zombie.needsSync = true;

                    ZOMBIE_ACTION_COOLDOWN.put(id, now + 11L);
                }
                return;
            }
        }

        // 3D. Break Blocking Walls / Obstacles in front of the Zombie
        if (horizDist >= 1.35D) {
            BlockPos wallTarget = null;
            if (isSolidBlock(serverLevel, frontHeadPos) && canZombieBreak(serverLevel, frontHeadPos)) {
                wallTarget = frontHeadPos;
            } else if (isSolidBlock(serverLevel, frontPos) && canZombieBreak(serverLevel, frontPos)) {
                // Only break foot-level block if we can't just step up onto it (i.e. head above it is blocked or it's taller)
                if (isSolidBlock(serverLevel, frontHeadPos) || isSolidBlock(serverLevel, frontPos.above(2))
                        || zombie.getDeltaMovement().horizontalDistanceSqr() < 0.005D) {
                    wallTarget = frontPos;
                }
            }

            if (wallTarget != null) {
                tickZombieBlockBreak(zombie, serverLevel, wallTarget, now);
                return;
            }
        }

        clearZombieBreakProgress(zombie, serverLevel);
    }

    private static boolean tryTossNearbyBabyZombie(Zombie thrower, Player player, ServerLevel level, long now) {
        List<Zombie> babies = level.getEntitiesOfClass(
                Zombie.class,
                thrower.getBoundingBox().inflate(2.8D),
                z -> z != thrower && z.isAlive() && z.isBaby() && z.onGround()
        );
        if (babies.isEmpty()) {
            return false;
        }

        Zombie baby = babies.get(0);
        Vec3 from = baby.position();
        Vec3 to = player.position().add(0.0D, 0.8D, 0.0D);
        Vec3 delta = to.subtract(from);
        double horiz = Math.max(1.0D, Math.sqrt(delta.x * delta.x + delta.z * delta.z));

        double vx = (delta.x / horiz) * Mth.clamp(horiz * 0.11D, 0.35D, 0.92D);
        double vz = (delta.z / horiz) * Mth.clamp(horiz * 0.11D, 0.35D, 0.92D);
        double vy = Mth.clamp(0.55D + Math.max(0.0D, delta.y) * 0.12D, 0.58D, 1.08D);

        baby.setDeltaMovement(vx, vy, vz);
        baby.needsSync = true;
        baby.setTarget(player);
        thrower.swingForAttack(InteractionHand.MAIN_HAND);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.SNOWBALL_THROW, SoundSource.HOSTILE, 1.0F, 0.72F);
        sendZombieDebris(level, thrower.getId(), from.x, from.y + 0.5D, from.z, delta.x / horiz, 0.7D, delta.z / horiz);

        ZOMBIE_THROW_COOLDOWN.put(thrower.getUUID(), now + 90L);
        return true;
    }

    private static void tickZombieBlockBreak(Zombie zombie, ServerLevel level, BlockPos targetPos, long now) {
        UUID id = zombie.getUUID();
        BlockBreakProgress prog = ZOMBIE_BREAK_STATE.get(id);
        if (prog == null || !prog.pos.equals(targetPos)) {
            if (prog != null) {
                level.destroyBlockProgress(zombie.getId(), prog.pos, -1);
            }
            prog = new BlockBreakProgress(targetPos.immutable(), now);
            ZOMBIE_BREAK_STATE.put(id, prog);
        }

        BlockState state = level.getBlockState(targetPos);
        if (!canZombieBreak(level, targetPos)) {
            clearZombieBreakProgress(zombie, level);
            return;
        }

        float hardness = state.getDestroySpeed(level, targetPos);
        int ticksNeeded = Mth.clamp((int) (14.0F + hardness * 10.0F), 14, 52);

        prog.ticks++;
        prog.lastTick = now;

        double bx = targetPos.getX() + 0.5D;
        double by = targetPos.getY() + 0.5D;
        double bz = targetPos.getZ() + 0.5D;
        zombie.getLookControl().setLookAt(bx, by, bz, 45.0F, 45.0F);

        int stage = Mth.clamp((int) ((prog.ticks / (float) ticksNeeded) * 9.0F), 0, 9);
        level.destroyBlockProgress(zombie.getId(), targetPos, stage);

        if (prog.ticks % 5 == 1) {
            zombie.swingForAttack(InteractionHand.MAIN_HAND);
            level.playSound(null, bx, by, bz, state.getSoundType().getHitSound(), SoundSource.HOSTILE, 0.75F, 0.9F);
            Vec3 away = zombie.position().add(0.0D, 0.9D, 0.0D).subtract(bx, by, bz).normalize();
            sendZombieDebris(level, zombie.getId(), bx, by, bz, away.x, Math.max(0.25D, away.y), away.z);
        }

        if (prog.ticks >= ticksNeeded) {
            level.destroyBlockProgress(zombie.getId(), targetPos, -1);
            ZOMBIE_BREAK_STATE.remove(id);

            Vec3 away = zombie.position().add(0.0D, 0.9D, 0.0D).subtract(bx, by, bz).normalize();
            level.destroyBlock(targetPos, true, zombie);
            sendZombieDebris(level, zombie.getId(), bx, by, bz, away.x, 0.55D, away.z);
        }
    }

    private static void clearZombieBreakProgress(Zombie zombie, ServerLevel level) {
        BlockBreakProgress prog = ZOMBIE_BREAK_STATE.remove(zombie.getUUID());
        if (prog != null) {
            level.destroyBlockProgress(zombie.getId(), prog.pos, -1);
        }
    }

    private static boolean isSolidBlock(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && !state.getCollisionShape(level, pos).isEmpty();
    }

    private static boolean canZombieBreak(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.canBeReplaced() || state.hasBlockEntity()) {
            return false;
        }
        float speed = state.getDestroySpeed(level, pos);
        return speed >= 0.0F && speed <= 5.5F;
    }

    private static void ensureCobblestoneOffhand(Zombie zombie) {
        if (zombie.getOffhandItem().isEmpty()) {
            zombie.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.COBBLESTONE));
        }
        // Always guarantee 0% drop chance so bridging cobblestone never drops
        zombie.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
    }

    private static void sendZombieDebris(
            ServerLevel level, int entityId,
            double x, double y, double z,
            double nx, double ny, double nz
    ) {
        MobVfxPayload payload = new MobVfxPayload(
                MobVfxPayload.ZOMBIE_BLOCK_DEBRIS,
                entityId,
                x, y, z,
                nx, ny, nz
        );
        PacketDistributor.sendToPlayersNear(level, null, x, y, z, 48.0D, payload);
    }
}
