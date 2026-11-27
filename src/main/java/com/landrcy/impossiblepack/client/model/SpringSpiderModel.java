package com.landrcy.impossiblepack.client.model;

import com.landrcy.impossiblepack.client.ClientMobVfxManager;
import com.landrcy.impossiblepack.client.state.SpringSpiderRenderState;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.spider.SpiderModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/**
 * Extends vanilla SpiderModel to animate the spider's abdomen/butt ("body1")
 * coiling down and springing sharply UP into the air like a real spider shooting silk!
 */
public class SpringSpiderModel extends SpiderModel {
    private final ModelPart body1;
    private final ModelPart body0;
    private final ModelPart head;

    public SpringSpiderModel(ModelPart root) {
        super(root);
        this.body0 = root.getChild("body0");
        this.body1 = root.getChild("body1");
        this.head = root.getChild("head");
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);

        // Reset defaults before applying spring animation
        this.body1.xRot = 0.0F;
        this.body1.y = 15.0F;
        this.body1.z = 9.0F;
        this.body0.xRot = 0.0F;
        this.body0.y = 15.0F;
        this.head.y = 15.0F;

        if (!(state instanceof SpringSpiderRenderState springState) || springState.springAge < 0.0F) {
            return;
        }

        float age = springState.springAge;
        float lift = 0.0F;
        if (age < 7.0F) {
            // 0..7 ticks: crouch/coil down like a spring before snapping up!
            float p = age / 7.0F;
            lift = -0.22F * Mth.sin(p * Mth.PI);
        } else if (age < 13.0F) {
            // 7..13 ticks: explosive SPRING UP of the spider's rear abdomen (fires silky web at tick 10!)
            float p = (age - 7.0F) / 6.0F;
            // Elastic spring overshoot peaking at ~1.18
            lift = Mth.sin(p * Mth.HALF_PI) * 1.18F;
        } else if (age <= ClientMobVfxManager.SPIDER_SPRING_DURATION_TICKS) {
            // 13..22 ticks: bouncy spring settle back to resting stance
            float p = (age - 13.0F) / (ClientMobVfxManager.SPIDER_SPRING_DURATION_TICKS - 13.0F);
            float decay = (1.0F - p) * (1.0F - p);
            lift = decay * (1.0F + 0.22F * Mth.sin(p * Mth.TWO_PI * 1.5F));
        }

        // Pitch abdomen/butt HIGH UP (+xRot lifts the back end in Minecraft model coordinates where +Z is rear and -Y is up!)
        this.body1.xRot = lift * 0.78F;
        this.body1.y = 15.0F - lift * 4.4F;
        this.body1.z = 9.0F - lift * 1.8F;

        // Subtle counter-dip on the front head/thorax while bracing legs to fire
        this.body0.xRot = lift * 0.24F;
        this.body0.y = 15.0F + Math.max(0.0F, lift) * 0.8F;
        this.head.y = 15.0F + Math.max(0.0F, lift) * 1.4F;
    }
}
