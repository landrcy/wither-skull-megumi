package com.landrcy.impossiblepack.client.model;

import com.landrcy.impossiblepack.TheImpossiblePackMod;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;

/**
 * 3D Golden Eight-Handled Mahoraga Divine Wheel (Dharmachakra) model:
 * - Central sacred hub
 * - 8 radial spokes at 45-degree intervals
 * - 8 outer golden rim segments forming a full sacred ring
 * - 8 protruding Dharmachakra handles with pommel spheres at the tips
 */
public class MahoragaWheelModel extends Model<Unit> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(TheImpossiblePackMod.MODID, "mahoraga_wheel"), "main");

    public static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(TheImpossiblePackMod.MODID, "textures/entity/mahoraga_wheel.png");

    public MahoragaWheelModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Central sacred golden hub
        root.addOrReplaceChild(
                "hub",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.0F, -1.0F, -3.0F, 6.0F, 2.0F, 6.0F)
                        .texOffs(0, 8).addBox(-2.0F, -1.6F, -2.0F, 4.0F, 3.2F, 4.0F),
                PartPose.ZERO
        );

        // 8 radial spokes, 8 rim arcs, and 8 outer handles + pommel knobs
        for (int i = 0; i < 8; i++) {
            float angleY = i * (Mth.PI / 4.0F);
            root.addOrReplaceChild(
                    "arm_" + i,
                    CubeListBuilder.create()
                            // Spoke from hub (z=2.5) through rim to handle shaft (z=17.5)
                            .texOffs(0, 16).addBox(-0.75F, -0.75F, 2.5F, 1.5F, 1.5F, 15.0F)
                            // Outer ring segment at radius z=12.5 connecting adjacent spokes cleanly
                            .texOffs(0, 34).addBox(-5.4F, -1.1F, 11.8F, 10.8F, 2.2F, 2.0F)
                            // Sacred handle grip collar & pommel sphere at outer tip (z=17.5..20.0)
                            .texOffs(26, 0).addBox(-1.4F, -1.4F, 17.2F, 2.8F, 2.8F, 2.6F),
                    PartPose.rotation(0.0F, angleY, 0.0F)
            );
        }

        return LayerDefinition.create(mesh, 64, 64);
    }
}
