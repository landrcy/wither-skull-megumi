package com.landrcy.impossiblepack.client.renderer;

import com.landrcy.impossiblepack.client.ClientMobVfxManager;
import com.landrcy.impossiblepack.client.model.SpringSpiderModel;
import com.landrcy.impossiblepack.client.state.SpringSpiderRenderState;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.spider.SpiderModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.SpiderEyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.monster.spider.Spider;

public class SpringSpiderRenderer<T extends Spider> extends MobRenderer<T, LivingEntityRenderState, SpiderModel> {

    private static final Identifier SPIDER_LOCATION =
            Identifier.withDefaultNamespace("textures/entity/spider/spider.png");
    private static final Identifier CAVE_SPIDER_LOCATION =
            Identifier.withDefaultNamespace("textures/entity/spider/cave_spider.png");

    private final Identifier texture;

    public SpringSpiderRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer, Identifier texture, float shadowRadius) {
        super(context, new SpringSpiderModel(context.bakeLayer(layer)), shadowRadius);
        this.texture = texture;
        this.addLayer(new SpiderEyesLayer<>(this));
    }

    public static SpringSpiderRenderer<Spider> spider(EntityRendererProvider.Context context) {
        return new SpringSpiderRenderer<>(context, ModelLayers.SPIDER, SPIDER_LOCATION, 0.8F);
    }

    public static SpringSpiderRenderer<CaveSpider> caveSpider(EntityRendererProvider.Context context) {
        return new SpringSpiderRenderer<>(context, ModelLayers.CAVE_SPIDER, CAVE_SPIDER_LOCATION, 0.56F);
    }

    @Override
    protected float getFlipDegrees() {
        return 180.0F;
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return this.texture;
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new SpringSpiderRenderState();
    }

    @Override
    public void extractRenderState(T entity, LivingEntityRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        if (state instanceof SpringSpiderRenderState springState) {
            springState.springAge = ClientMobVfxManager.getSpiderSpringAge(entity.getId(), partialTicks);
        }
    }
}
