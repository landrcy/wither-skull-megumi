package com.landrcy.witherscullmegumi.client.model;

import com.landrcy.witherscullmegumi.entity.MegumiWitherSkeletonEntity;
import com.landrcy.witherscullmegumi.ritual.RitualConstants;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Custom SkeletonModel for Wither Skull Megumi.
 * Animates smoothly into Megumi Fushiguro's iconic Mahoraga summoning hand-sign pose:
 * - Left arm raised higher & extended forward in front of face/chin, fist angled across centerline
 * - Right arm bent slightly lower & closer in front of chest, angled upward/inward beneath left fist
 * - Head pitched slightly down staring coldly straight ahead over the extended fist
 * - Forward-leaning grounded Jujutsu stance
 */
public class MegumiWitherSkeletonModel extends SkeletonModel<MegumiWitherSkeletonEntity> {

    public MegumiWitherSkeletonModel(ModelPart root) {
        super(root);
    }

    @Override
    public void prepareMobModel(MegumiWitherSkeletonEntity entity, float limbSwing, float limbSwingAmount, float partialTick) {
        if (entity.isRitualActive()) {
            this.rightArmPose = ArmPose.EMPTY;
            this.leftArmPose = ArmPose.EMPTY;
            return;
        }
        super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
    }

    @Override
    public void setupAnim(
            MegumiWitherSkeletonEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        if (!entity.isRitualActive()) {
            return;
        }

        float partialTick = ageInTicks - entity.tickCount;
        float ritualTime = entity.getRitualTicks() + Mth.clamp(partialTick, 0.0F, 1.0F);
        float blend = Mth.clamp(ritualTime / (float) RitualConstants.POSE_SNAP_TICKS, 0.0F, 1.0F);
        // Smooth ease-out snap into the Megumi summoning pose
        float t = 1.0F - (1.0F - blend) * (1.0F - blend) * (1.0F - blend);

        // Subtle cursed energy tremor right before the Wither behind him detonates
        float tremor = 0.0F;
        if (ritualTime > RitualConstants.WITHER_APPEAR_TICK) {
            float charge = Mth.clamp(
                    (ritualTime - RitualConstants.WITHER_APPEAR_TICK) / (RitualConstants.WITHER_EXPLODE_TICK - RitualConstants.WITHER_APPEAR_TICK),
                    0.0F,
                    1.0F
            );
            tremor = Mth.sin(ritualTime * 2.4F) * 0.025F * charge;
        }

        // Head: chin slightly down (+0.22 rad), eyes locked dead ahead over his extended left fist
        this.head.xRot = Mth.lerp(t, this.head.xRot, 0.22F + tremor * 0.4F);
        this.head.yRot = Mth.lerp(t, this.head.yRot, 0.0F);
        this.head.zRot = Mth.lerp(t, this.head.zRot, 0.0F);
        this.hat.copyFrom(this.head);

        // Body: subtle forward lean matching the cover pose
        this.body.xRot = Mth.lerp(t, this.body.xRot, 0.08F);
        this.body.yRot = Mth.lerp(t, this.body.yRot, -0.05F);
        this.body.zRot = 0.0F;

        // Left Arm (viewer's right): raised higher & extended forward in front of upper chest/chin
        this.leftArm.xRot = Mth.lerp(t, this.leftArm.xRot, -1.42F + tremor);
        this.leftArm.yRot = Mth.lerp(t, this.leftArm.yRot, 0.34F);
        this.leftArm.zRot = Mth.lerp(t, this.leftArm.zRot, 0.18F);

        // Right Arm (viewer's left): bent slightly lower & closer to chest, fist angled inward below left fist
        this.rightArm.xRot = Mth.lerp(t, this.rightArm.xRot, -1.10F - tremor);
        this.rightArm.yRot = Mth.lerp(t, this.rightArm.yRot, -0.46F);
        this.rightArm.zRot = Mth.lerp(t, this.rightArm.zRot, -0.22F);

        // Grounded wide stance
        this.leftLeg.xRot = Mth.lerp(t, this.leftLeg.xRot, -0.14F);
        this.leftLeg.yRot = Mth.lerp(t, this.leftLeg.yRot, -0.08F);
        this.leftLeg.zRot = Mth.lerp(t, this.leftLeg.zRot, -0.07F);

        this.rightLeg.xRot = Mth.lerp(t, this.rightLeg.xRot, 0.14F);
        this.rightLeg.yRot = Mth.lerp(t, this.rightLeg.yRot, 0.08F);
        this.rightLeg.zRot = Mth.lerp(t, this.rightLeg.zRot, 0.07F);
    }
}
