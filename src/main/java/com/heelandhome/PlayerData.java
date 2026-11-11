package com.heelandhome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Saved on each player: homes, per-dog home links, and last known dog positions. */
public class PlayerData {

    public record Home(String name, String dim, BlockPos pos) {
        public static final Codec<Home> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("name").forGetter(Home::name),
                Codec.STRING.fieldOf("dim").forGetter(Home::dim),
                BlockPos.CODEC.fieldOf("pos").forGetter(Home::pos)
        ).apply(i, Home::new));
    }

    public record DogLoc(String dim, BlockPos pos, String name) {
        public static final Codec<DogLoc> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("dim").forGetter(DogLoc::dim),
                BlockPos.CODEC.fieldOf("pos").forGetter(DogLoc::pos),
                Codec.STRING.fieldOf("name").forGetter(DogLoc::name)
        ).apply(i, DogLoc::new));
    }

    public final List<Home> homes = new ArrayList<>();
    /** dog uuid -> home name */
    public final Map<String, String> dogHome = new HashMap<>();
    /** dog uuid -> last known location (also acts as the list of known dogs) */
    public final Map<String, DogLoc> lastSeen = new HashMap<>();

    public static final Codec<PlayerData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Home.CODEC.listOf().optionalFieldOf("homes", List.of()).forGetter(d -> d.homes),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("dogHome", Map.of()).forGetter(d -> d.dogHome),
            Codec.unboundedMap(Codec.STRING, DogLoc.CODEC).optionalFieldOf("lastSeen", Map.of()).forGetter(d -> d.lastSeen)
    ).apply(i, PlayerData::new));

    public PlayerData() {}

    public PlayerData(List<Home> h, Map<String, String> dh, Map<String, DogLoc> ls) {
        homes.addAll(h);
        dogHome.putAll(dh);
        lastSeen.putAll(ls);
    }
}
