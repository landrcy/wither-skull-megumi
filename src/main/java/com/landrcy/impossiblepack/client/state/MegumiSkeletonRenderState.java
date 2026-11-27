package com.landrcy.impossiblepack.client.state;

import net.minecraft.client.renderer.entity.state.SkeletonRenderState;

public class MegumiSkeletonRenderState extends SkeletonRenderState {
    public int entityId = -1;
    public boolean ritualActive = false;
    public float ritualTime = 0.0F;
    public boolean hasExploded = false;
}
