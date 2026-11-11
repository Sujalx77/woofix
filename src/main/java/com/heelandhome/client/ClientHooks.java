package com.heelandhome.client;

import com.heelandhome.net.StatePayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public final class ClientHooks {
    private ClientHooks() {}

    public static void onState(StatePayload pl, IPayloadContext ctx) {
        ClientState.homes = new ArrayList<>(pl.homes());
        List<ClientState.Dog> dogs = new ArrayList<>();
        for (String s : pl.dogs()) {
            String[] parts = s.split("\t", -1);
            if (parts.length >= 3) dogs.add(new ClientState.Dog(parts[0], parts[1], parts[2]));
        }
        ClientState.dogs = dogs;
        ClientState.validate();
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof DogMenuScreen s) s.refresh();
    }
}
