package com.landrcy.impossiblepack.client.renderer;

import com.landrcy.impossiblepack.client.model.MahoragaWheelModel;
import com.landrcy.impossiblepack.client.state.MahoragaWitherRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.monster.wither.WitherBossModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;

/**
 * Renders the 3D Golden Eight-Handled Mahoraga Divine Wheel floating & tilted above the Wither's center head.
 * Smoothly snaps 45 degrees and flashes bright gold whenever the Wither adapts!
 */
public class MahoragaWheelLayer extends RenderLayer<MahoragaWitherRenderState, WitherBossModel> {

    private final MahoragaWheelModel wheelModel;
    private static final int FULL_BRIGHT = LightCoordsUtil.pack(15, 15);

    public MahoragaWheelLayer(RenderLayerParent<MahoragaWitherRenderState, WitherBossModel> parent, EntityModelSet modelSet) {
        super(parent);
        this.wheelModel = new MahoragaWheelModel(modelSet.bakeLayer(MahoragaWheelModel.LAYER_LOCATION));
    }

    @Override
    public void submit(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            MahoragaWitherRenderState state,
            float yRot,
            float xRot
    ) {
        poseStack.pushPose();

        float hoverBob = Mth.sin(state.ageInTicks * 0.08F) * 0.04F;
        poseStack.translate(0.0F, -0.72F + hoverBob, 0.16F);
        poseStack.rotateDegrees(Axis.XP, -22.0F);

        float prevDeg = Math.max(0, state.wheelSteps - 1) * 45.0F;
        float targetDeg = state.wheelSteps * 45.0F;
        float snapProgress = Mth.clamp(state.ticksSinceWheelTurn / 8.0F, 0.0F, 1.0F);
        float easedSnap = 1.0F - (1.0F - snapProgress) * (1.0F - snapProgress);
        float currentWheelDeg = state.wheelSteps > 0 ? Mth.lerp(easedSnap, prevDeg, targetDeg) : 0.0F;

        float turnPulse = state.ticksSinceWheelTurn < 14.0F
                ? 1.0F + (1.0F - state.ticksSinceWheelTurn / 14.0F) * 0.16F
                : 1.0F;

        poseStack.rotateDegrees(Axis.YP, currentWheelDeg);
        float s = 0.85F * turnPulse;
        poseStack.scale(s, s, s);

        int tint = state.ticksSinceWheelTurn < 12.0F
                ? ARGB.color(255, 255, 250, 210)
                : -1;

        submitNodeCollector.submitModel(
                this.wheelModel,
                Unit.INSTANCE,
                poseStack,
                RenderTypes.entityCutout(MahoragaWheelModel.TEXTURE),
                FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                tint,
                null,
                state.outlineColor
        );

        poseStack.popPose();
    }
}
