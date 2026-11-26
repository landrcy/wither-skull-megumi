package com.landrcy.witherscullmegumi.client.renderer;

import com.landrcy.witherscullmegumi.WitherScullMegumiMod;
import com.landrcy.witherscullmegumi.client.model.MegumiWitherSkeletonModel;
import com.landrcy.witherscullmegumi.entity.MegumiWitherSkeletonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

public class MegumiWitherSkeletonRenderer extends HumanoidMobRenderer<MegumiWitherSkeletonEntity, MegumiWitherSkeletonModel> {

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(WitherScullMegumiMod.MODID, "textures/entity/megumi_wither_skeleton.png");

    public MegumiWitherSkeletonRenderer(EntityRendererProvider.Context context) {
        super(context, new MegumiWitherSkeletonModel(context.bakeLayer(ModelLayers.WITHER_SKELETON)), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                new SkeletonModel<>(context.bakeLayer(ModelLayers.WITHER_SKELETON_INNER_ARMOR)),
                new SkeletonModel<>(context.bakeLayer(ModelLayers.WITHER_SKELETON_OUTER_ARMOR)),
                context.getModelManager()
        ));
    }

    @Override
    public ResourceLocation getTextureLocation(MegumiWitherSkeletonEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(MegumiWitherSkeletonEntity livingEntity, PoseStack poseStack, float partialTickTime) {
        // Match vanilla Wither Skeleton's towering 1.2x scale
        poseStack.scale(1.2F, 1.2F, 1.2F);
    }

    @Override
    public void render(
            MegumiWitherSkeletonEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        if (entity.hasExploded()) {
            // Once the Wither explodes behind him, his intact body disappears as his 6 individual bone parts launch away!
            return;
        }
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}
