package com.landrcy.impossiblepack.network;

import com.landrcy.impossiblepack.TheImpossiblePackMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record MobVfxPayload(
        byte effectType,
        int entityId,
        double x1,
        double y1,
        double z1,
        double x2,
        double y2,
        double z2
) implements CustomPacketPayload {

    public static final byte BOOGIE_WOOGIE_SWAP = 0;
    public static final byte SPIDER_SPRING_POSE = 1;
    public static final byte SPIDER_WEB_STREAM = 2;
    public static final byte SPIDER_WEB_SPLAT = 3;
    public static final byte ZOMBIE_BLOCK_DEBRIS = 4;

    public static final CustomPacketPayload.Type<MobVfxPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TheImpossiblePackMod.MODID, "mob_vfx"));

    public static final StreamCodec<ByteBuf, MobVfxPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public MobVfxPayload decode(ByteBuf buf) {
            byte effectType = ByteBufCodecs.BYTE.decode(buf);
            int entityId = ByteBufCodecs.VAR_INT.decode(buf);
            double x1 = ByteBufCodecs.DOUBLE.decode(buf);
            double y1 = ByteBufCodecs.DOUBLE.decode(buf);
            double z1 = ByteBufCodecs.DOUBLE.decode(buf);
            double x2 = ByteBufCodecs.DOUBLE.decode(buf);
            double y2 = ByteBufCodecs.DOUBLE.decode(buf);
            double z2 = ByteBufCodecs.DOUBLE.decode(buf);
            return new MobVfxPayload(effectType, entityId, x1, y1, z1, x2, y2, z2);
        }

        @Override
        public void encode(ByteBuf buf, MobVfxPayload payload) {
            ByteBufCodecs.BYTE.encode(buf, payload.effectType());
            ByteBufCodecs.VAR_INT.encode(buf, payload.entityId());
            ByteBufCodecs.DOUBLE.encode(buf, payload.x1());
            ByteBufCodecs.DOUBLE.encode(buf, payload.y1());
            ByteBufCodecs.DOUBLE.encode(buf, payload.z1());
            ByteBufCodecs.DOUBLE.encode(buf, payload.x2());
            ByteBufCodecs.DOUBLE.encode(buf, payload.y2());
            ByteBufCodecs.DOUBLE.encode(buf, payload.z2());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
