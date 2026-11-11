package com.heelandhome;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ALLOW_TELEPORT;
    public static final ModConfigSpec.BooleanValue CROSS_DIMENSION;
    public static final ModConfigSpec.BooleanValue WHISTLE_SOUND;
    public static final ModConfigSpec.BooleanValue DISABLE_NEAR_CALL;
    public static final ModConfigSpec.IntValue NEAR_CALL_DISTANCE;
    public static final ModConfigSpec.IntValue WALK_RANGE;
    public static final ModConfigSpec.IntValue COOLDOWN_TICKS;
    public static final ModConfigSpec.IntValue WALK_TIMEOUT_TICKS;
    public static final ModConfigSpec.IntValue WALK_ONLY_TIMEOUT_TICKS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        ALLOW_TELEPORT = b.comment("false = dogs NEVER teleport. They always walk to you / home (only loaded dogs in the same dimension can be reached).")
                .define("allowTeleport", true);
        CROSS_DIMENSION = b.comment("Allow calling / sending dogs between dimensions (needs allowTeleport = true).")
                .define("crossDimension", true);
        WHISTLE_SOUND = b.comment("Play a whistle sound when you call your dog.")
                .define("whistleSound", true);
        DISABLE_NEAR_CALL = b.comment("true = calling does nothing for dogs that are already closer than nearCallDistance. Sending home still works.")
                .define("disableNearCall", false);
        NEAR_CALL_DISTANCE = b.comment("Distance in blocks used by disableNearCall.")
                .defineInRange("nearCallDistance", 32, 1, 256);
        WALK_RANGE = b.comment("With teleporting on: dogs closer than this (blocks) walk, farther dogs teleport.")
                .defineInRange("walkRange", 32, 0, 256);
        COOLDOWN_TICKS = b.comment("Cooldown between call / send-home uses, in ticks (20 = 1 second).")
                .defineInRange("cooldownTicks", 20, 0, 1200);
        WALK_TIMEOUT_TICKS = b.comment("With teleporting on: a walking dog that has not arrived after this many ticks teleports.")
                .defineInRange("walkTimeoutTicks", 400, 40, 6000);
        WALK_ONLY_TIMEOUT_TICKS = b.comment("With teleporting off: a dog gives up walking after this many ticks.")
                .defineInRange("walkOnlyTimeoutTicks", 1200, 100, 12000);
        SPEC = b.build();
    }

    private Config() {}
}
