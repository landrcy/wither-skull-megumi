package com.landrcy.witherscullmegumi.network;

import com.landrcy.witherscullmegumi.WitherScullMegumiMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RitualSyncPayload(
        boolean active,
        int skeletonEntityId,
        int ritualTick,
        double anchorX,
        double anchorY,
        double anchorZ,
        float anchorYaw
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RitualSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(WitherScullMegumiMod.MODID, "ritual_sync"));

    public static final StreamCodec<ByteBuf, RitualSyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RitualSyncPayload decode(ByteBuf buf) {
            boolean active = ByteBufCodecs.BOOL.decode(buf);
            int entityId = ByteBufCodecs.VAR_INT.decode(buf);
            int tick = ByteBufCodecs.VAR_INT.decode(buf);
            double x = ByteBufCodecs.DOUBLE.decode(buf);
            double y = ByteBufCodecs.DOUBLE.decode(buf);
            double z = ByteBufCodecs.DOUBLE.decode(buf);
            float yaw = ByteBufCodecs.FLOAT.decode(buf);
            return new RitualSyncPayload(active, entityId, tick, x, y, z, yaw);
        }

        @Override
        public void encode(ByteBuf buf, RitualSyncPayload payload) {
            ByteBufCodecs.BOOL.encode(buf, payload.active());
            ByteBufCodecs.VAR_INT.encode(buf, payload.skeletonEntityId());
            ByteBufCodecs.VAR_INT.encode(buf, payload.ritualTick());
            ByteBufCodecs.DOUBLE.encode(buf, payload.anchorX());
            ByteBufCodecs.DOUBLE.encode(buf, payload.anchorY());
            ByteBufCodecs.DOUBLE.encode(buf, payload.anchorZ());
            ByteBufCodecs.FLOAT.encode(buf, payload.anchorYaw());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
