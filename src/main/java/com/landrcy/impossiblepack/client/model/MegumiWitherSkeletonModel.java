package com.landrcy.impossiblepack.client.model;

import com.landrcy.impossiblepack.client.state.MegumiSkeletonRenderState;
import com.landrcy.impossiblepack.ritual.RitualConstants;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.util.Mth;

public class MegumiWitherSkeletonModel extends SkeletonModel<MegumiSkeletonRenderState> {

    public MegumiWitherSkeletonModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(MegumiSkeletonRenderState state) {
        super.setupAnim(state);

        if (!state.ritualActive) {
            return;
        }

        float ritualTime = state.ritualTime;
        float blend = Mth.clamp(ritualTime / (float) RitualConstants.POSE_SNAP_TICKS, 0.0F, 1.0F);
        float t = 1.0F - (1.0F - blend) * (1.0F - blend) * (1.0F - blend);

        float tremor = 0.0F;
        if (ritualTime > RitualConstants.WITHER_APPEAR_TICK) {
            float charge = Mth.clamp(
                    (ritualTime - RitualConstants.WITHER_APPEAR_TICK)
                            / (float) (RitualConstants.WITHER_EXPLODE_TICK - RitualConstants.WITHER_APPEAR_TICK),
                    0.0F,
                    1.0F
            );
            tremor = Mth.sin(ritualTime * 2.4F) * 0.03F * charge * charge;
        }

        // Head: chin slightly down (+0.22 rad), eyes staring straight ahead over the extended left fist
        this.head.xRot = Mth.lerp(t, this.head.xRot, 0.22F + tremor * 0.4F);
        this.head.yRot = Mth.lerp(t, this.head.yRot, 0.0F);
        this.head.zRot = Mth.lerp(t, this.head.zRot, 0.0F);
        this.hat.xRot = this.head.xRot;
        this.hat.yRot = this.head.yRot;
        this.hat.zRot = this.head.zRot;

        // Body: subtle forward lean matching the Megumi pose
        this.body.xRot = Mth.lerp(t, this.body.xRot, 0.08F);
        this.body.yRot = Mth.lerp(t, this.body.yRot, -0.05F);
        this.body.zRot = 0.0F;

        // Left Arm (viewer's right): raised higher & extended forward in front of upper chest/chin
        this.leftArm.xRot = Mth.lerp(t, this.leftArm.xRot, -1.42F + tremor);
        this.leftArm.yRot = Mth.lerp(t, this.leftArm.yRot, 0.34F);
        this.leftArm.zRot = Mth.lerp(t, this.leftArm.zRot, 0.18F);

        // Right Arm (viewer's left): bent slightly lower & closer in front of chest beneath left fist
        this.rightArm.xRot = Mth.lerp(t, this.rightArm.xRot, -1.10F - tremor);
        this.rightArm.yRot = Mth.lerp(t, this.rightArm.yRot, -0.46F);
        this.rightArm.zRot = Mth.lerp(t, this.rightArm.zRot, -0.22F);

        // Grounded Jujutsu stance
        this.leftLeg.xRot = Mth.lerp(t, this.leftLeg.xRot, -0.14F);
        this.leftLeg.yRot = Mth.lerp(t, this.leftLeg.yRot, -0.08F);
        this.leftLeg.zRot = Mth.lerp(t, this.leftLeg.zRot, -0.07F);

        this.rightLeg.xRot = Mth.lerp(t, this.rightLeg.xRot, 0.14F);
        this.rightLeg.yRot = Mth.lerp(t, this.rightLeg.yRot, 0.08F);
        this.rightLeg.zRot = Mth.lerp(t, this.rightLeg.zRot, 0.07F);
    }
}
