package com.heelandhome.client;

import com.heelandhome.HeelAndHome;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = HeelAndHome.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private static final String CATEGORY = "key.categories.heelandhome";

    public static final KeyMapping MENU_KEY = new KeyMapping(
            "key.heelandhome.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);
    // Quick keys are unbound by default: bind them in Controls only if you want them.
    public static final KeyMapping CALL_KEY = new KeyMapping(
            "key.heelandhome.call", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY);
    public static final KeyMapping HOME_KEY = new KeyMapping(
            "key.heelandhome.home", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY);

    private ClientModEvents() {}

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent e) {
        e.register(MENU_KEY);
        e.register(CALL_KEY);
        e.register(HOME_KEY);
    }
}
