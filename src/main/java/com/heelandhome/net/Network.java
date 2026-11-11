package com.heelandhome.net;

import com.heelandhome.DogManager;
import com.heelandhome.client.ClientHooks;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class Network {
    private Network() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        r.playToServer(DogActionPayload.TYPE, DogActionPayload.STREAM_CODEC, Network::handleAction);
        r.playToClient(StatePayload.TYPE, StatePayload.STREAM_CODEC, ClientHooks::onState);
    }

    private static void handleAction(DogActionPayload pl, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer p)) return;
        switch (pl.action()) {
            case DogActionPayload.CALL -> DogManager.command(p, DogManager.Mode.CALL, pl.dog(), "");
            case DogActionPayload.HOME -> DogManager.command(p, DogManager.Mode.HOME, pl.dog(), pl.name());
            case DogActionPayload.SET_HOME -> { DogManager.setHome(p, pl.name()); DogManager.sendState(p); }
            case DogActionPayload.DELETE_HOME -> { DogManager.deleteHome(p, pl.name()); DogManager.sendState(p); }
            case DogActionPayload.LINK -> { DogManager.linkDog(p, pl.dog(), pl.name()); DogManager.sendState(p); }
            case DogActionPayload.REQUEST -> DogManager.sendState(p);
            default -> { }
        }
    }
}
