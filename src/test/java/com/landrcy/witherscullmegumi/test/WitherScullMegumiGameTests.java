package com.landrcy.witherscullmegumi.test;

import com.landrcy.witherscullmegumi.WitherScullMegumiMod;
import com.landrcy.witherscullmegumi.entity.MegumiSummonedWitherEntity;
import com.landrcy.witherscullmegumi.entity.MegumiWitherSkeletonEntity;
import com.landrcy.witherscullmegumi.registry.ModEntities;
import com.landrcy.witherscullmegumi.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(WitherScullMegumiMod.MODID)
@PrefixGameTestTemplate(false)
@EventBusSubscriber(modid = WitherScullMegumiMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class WitherScullMegumiGameTests {

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        event.register(WitherScullMegumiGameTests.class);
    }

    @GameTest(template = "arena")
    public static void megumiSkeletonAndExclusiveWitherTest(GameTestHelper helper) {
        // 1. Verify custom entity and spawn egg registration
        MegumiWitherSkeletonEntity megumi = helper.spawn(ModEntities.MEGUMI_WITHER_SKELETON.get(), new BlockPos(2, 2, 2));
        helper.assertTrue(megumi != null && megumi.isAlive(), "Megumi Wither Skeleton should spawn alive");
        helper.assertFalse(megumi.isRitualActive(), "Ritual should not be active before player attacks");
        helper.assertTrue(ModItems.MEGUMI_WITHER_SKELETON_SPAWN_EGG.get() != null, "Spawn egg item should be registered");

        // 2. Verify MegumiSummonedWitherEntity strictly targets ONLY the bound player and ignores all other mobs & players
        Player attacker = helper.makeMockPlayer(GameType.SURVIVAL);
        Player bystander = helper.makeMockPlayer(GameType.SURVIVAL);
        Cow innocentCow = helper.spawn(EntityType.COW, new BlockPos(3, 2, 3));

        MegumiSummonedWitherEntity wither = helper.spawn(ModEntities.MEGUMI_WITHER.get(), new BlockPos(4, 3, 4));
        wither.setLockedTargetUuid(attacker.getUUID());

        helper.assertTrue(wither.canAttack(attacker), "Summoned Wither MUST target the locked attacker");
        helper.assertFalse(wither.canAttack(bystander), "Summoned Wither MUST NOT target other players");
        helper.assertFalse(wither.canAttack(innocentCow), "Summoned Wither MUST NOT target other mobs");

        // Attempt to force target to bystander or cow; verify Wither rejects them
        wither.setTarget(bystander);
        helper.assertTrue(wither.getTarget() == null, "Summoned Wither must reject setTarget for bystander player");
        wither.setTarget(innocentCow);
        helper.assertTrue(wither.getTarget() == null, "Summoned Wither must reject setTarget for other mobs");

        wither.setTarget(attacker);
        helper.assertTrue(wither.getTarget() == attacker, "Summoned Wither must accept locked attacker as target");

        helper.succeed();
    }
}
