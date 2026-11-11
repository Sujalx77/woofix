package com.heelandhome;

import com.heelandhome.net.Network;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

@Mod(HeelAndHome.MODID)
public class HeelAndHome {
    public static final String MODID = "heelandhome";

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, MODID);

    public static final Supplier<SoundEvent> WHISTLE = SOUNDS.register("whistle",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MODID, "whistle")));

    public static final Supplier<AttachmentType<PlayerData>> PLAYER_DATA = ATTACHMENTS.register(
            "player_data",
            () -> AttachmentType.builder(() -> new PlayerData())
                    .serialize(PlayerData.CODEC)
                    .copyOnDeath()
                    .build());

    public HeelAndHome(IEventBus modBus, ModContainer container) {
        ATTACHMENTS.register(modBus);
        SOUNDS.register(modBus);
        modBus.addListener(Network::register);
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC, "woofix-common.toml");
    }
}
