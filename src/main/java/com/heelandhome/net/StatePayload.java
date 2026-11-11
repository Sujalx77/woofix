package com.heelandhome.net;

import com.heelandhome.HeelAndHome;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Server -> client. homes = names; dogs = "uuid\tname\tlinkedHome". */
public record StatePayload(List<String> homes, List<String> dogs) implements CustomPacketPayload {
    public static final Type<StatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HeelAndHome.MODID, "state"));

    public static final StreamCodec<ByteBuf, StatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(256).apply(ByteBufCodecs.list(64)), StatePayload::homes,
            ByteBufCodecs.stringUtf8(256).apply(ByteBufCodecs.list(256)), StatePayload::dogs,
            StatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
