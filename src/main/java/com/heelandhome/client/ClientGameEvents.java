package com.heelandhome.client;

import com.heelandhome.HeelAndHome;
import com.heelandhome.net.DogActionPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = HeelAndHome.MODID, value = Dist.CLIENT)
public final class ClientGameEvents {
    private ClientGameEvents() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        boolean menu = false, call = false, home = false;
        while (ClientModEvents.MENU_KEY.consumeClick()) menu = true;
        while (ClientModEvents.CALL_KEY.consumeClick()) call = true;
        while (ClientModEvents.HOME_KEY.consumeClick()) home = true;
        if (mc.screen != null) return;

        if (menu) {
            mc.setScreen(new DogMenuScreen());
            PacketDistributor.sendToServer(new DogActionPayload(DogActionPayload.REQUEST, "", ""));
        }
        if (call) {
            PacketDistributor.sendToServer(new DogActionPayload(DogActionPayload.CALL, "", ClientState.selectedDog));
        }
        if (home) {
            PacketDistributor.sendToServer(new DogActionPayload(DogActionPayload.HOME, ClientState.selectedHome, ClientState.selectedDog));
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        ClientState.reset();
    }
}
