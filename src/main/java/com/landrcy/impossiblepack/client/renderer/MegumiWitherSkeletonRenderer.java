package com.landrcy.impossiblepack.client.renderer;

import com.landrcy.impossiblepack.TheImpossiblePackMod;
import com.landrcy.impossiblepack.client.model.MegumiWitherSkeletonModel;
import com.landrcy.impossiblepack.client.state.MegumiSkeletonRenderState;
import com.landrcy.impossiblepack.entity.MegumiWitherSkeletonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractSkeletonRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;

public class MegumiWitherSkeletonRenderer extends AbstractSkeletonRenderer<MegumiWitherSkeletonEntity, MegumiSkeletonRenderState> {

    public static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(TheImpossiblePackMod.MODID, "textures/entity/megumi_wither_skeleton.png");

    public MegumiWitherSkeletonRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                ModelLayers.WITHER_SKELETON_ARMOR,
                new MegumiWitherSkeletonModel(context.bakeLayer(ModelLayers.WITHER_SKELETON))
        );
    }

    @Override
    public MegumiSkeletonRenderState createRenderState() {
        return new MegumiSkeletonRenderState();
    }

    @Override
    public Identifier getTextureLocation(MegumiSkeletonRenderState state) {
        return TEXTURE;
    }

    @Override
    protected HumanoidModel.ArmPose getArmPose(MegumiWitherSkeletonEntity mob, HumanoidArm arm) {
        if (mob.isRitualActive()) {
            return HumanoidModel.ArmPose.EMPTY;
        }
        return super.getArmPose(mob, arm);
    }

    @Override
    public void extractRenderState(MegumiWitherSkeletonEntity entity, MegumiSkeletonRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.entityId = entity.getId();
        state.ritualActive = entity.isRitualActive();
        state.ritualTime = entity.getRitualTicks() + partialTicks;
        state.hasExploded = entity.hasExploded();
    }

    @Override
    public void submit(
            MegumiSkeletonRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera
    ) {
        if (state.hasExploded) {
            return;
        }
        super.submit(state, poseStack, submitNodeCollector, camera);
    }
}
