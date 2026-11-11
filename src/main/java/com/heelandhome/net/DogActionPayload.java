package com.heelandhome.net;

import com.heelandhome.HeelAndHome;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> server. action: see constants. name = home name, dog = dog uuid ("" = all dogs). */
public record DogActionPayload(int action, String name, String dog) implements CustomPacketPayload {
    public static final int CALL = 0, HOME = 1, SET_HOME = 2, DELETE_HOME = 3, LINK = 4, REQUEST = 5;

    public static final Type<DogActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HeelAndHome.MODID, "dog_action"));

    public static final StreamCodec<ByteBuf, DogActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DogActionPayload::action,
            ByteBufCodecs.stringUtf8(64), DogActionPayload::name,
            ByteBufCodecs.stringUtf8(64), DogActionPayload::dog,
            DogActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
