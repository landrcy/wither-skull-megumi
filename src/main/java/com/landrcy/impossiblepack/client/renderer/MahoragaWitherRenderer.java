package com.landrcy.impossiblepack.client.renderer;

import com.landrcy.impossiblepack.client.state.MahoragaWitherRenderState;
import com.landrcy.impossiblepack.entity.MahoragaWitherEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.wither.WitherBossModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class MahoragaWitherRenderer extends MobRenderer<MahoragaWitherEntity, MahoragaWitherRenderState, WitherBossModel> {

    private static final Identifier WITHER_INVULNERABLE_LOCATION =
            Identifier.withDefaultNamespace("textures/entity/wither/wither_invulnerable.png");
    private static final Identifier WITHER_LOCATION =
            Identifier.withDefaultNamespace("textures/entity/wither/wither.png");
    private static final Identifier WITHER_ARMOR_LOCATION =
            Identifier.withDefaultNamespace("textures/entity/wither/wither_armor.png");

    public MahoragaWitherRenderer(EntityRendererProvider.Context context) {
        super(context, new WitherBossModel(context.bakeLayer(ModelLayers.WITHER)), 1.0F);
        this.addLayer(new MahoragaWitherArmorLayer(this, context.getModelSet()));
        this.addLayer(new MahoragaWheelLayer(this, context.getModelSet()));
    }

    @Override
    protected int getBlockLightLevel(MahoragaWitherEntity entity, BlockPos blockPos) {
        return 15;
    }

    @Override
    public MahoragaWitherRenderState createRenderState() {
        return new MahoragaWitherRenderState();
    }

    @Override
    public Identifier getTextureLocation(MahoragaWitherRenderState state) {
        int invulnerableTicks = Mth.floor(state.invulnerableTicks);
        return invulnerableTicks > 0 && (invulnerableTicks > 80 || invulnerableTicks / 5 % 2 != 1)
                ? WITHER_INVULNERABLE_LOCATION
                : WITHER_LOCATION;
    }

    @Override
    protected void scale(MahoragaWitherRenderState state, PoseStack poseStack) {
        float scale = 2.0F;
        if (state.invulnerableTicks > 0.0F) {
            scale -= state.invulnerableTicks / 220.0F * 0.5F;
        }
        poseStack.scale(scale, scale, scale);
    }

    @Override
    public void extractRenderState(MahoragaWitherEntity entity, MahoragaWitherRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        int invulnerableTicks = entity.getInvulnerableTicks();
        state.invulnerableTicks = invulnerableTicks > 0 ? invulnerableTicks - partialTicks : 0.0F;
        System.arraycopy(entity.getHeadXRots(), 0, state.xHeadRots, 0, state.xHeadRots.length);
        System.arraycopy(entity.getHeadYRots(), 0, state.yHeadRots, 0, state.yHeadRots.length);
        state.isPowered = entity.isPowered();
        state.wheelSteps = entity.getWheelSteps();
        state.ticksSinceWheelTurn = Math.max(0.0F, (entity.tickCount - entity.getLastWheelTurnTick()) + partialTicks);
    }

    private static final class MahoragaWitherArmorLayer extends RenderLayer<MahoragaWitherRenderState, WitherBossModel> {
        private final WitherBossModel armorModel;

        private MahoragaWitherArmorLayer(RenderLayerParent<MahoragaWitherRenderState, WitherBossModel> parent, EntityModelSet modelSet) {
            super(parent);
            this.armorModel = new WitherBossModel(modelSet.bakeLayer(ModelLayers.WITHER_ARMOR));
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
            if (!state.isPowered) {
                return;
            }
            float t = state.ageInTicks;
            float u = (Mth.cos(t * 0.02F) * 3.0F) % 1.0F;
            float v = (t * 0.01F) % 1.0F;
            submitNodeCollector.order(1).submitModel(
                    this.armorModel,
                    state,
                    poseStack,
                    RenderTypes.energySwirl(WITHER_ARMOR_LOCATION, u, v),
                    lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    -8355712,
                    null,
                    state.outlineColor
            );
        }
    }
}
