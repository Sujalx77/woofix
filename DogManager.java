package com.heelandhome;

import com.heelandhome.PlayerData.DogLoc;
import com.heelandhome.PlayerData.Home;
import com.heelandhome.net.StatePayload;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

/** All server-side logic: finding dogs, calling them, sending them home, homes, events. */
@EventBusSubscriber(modid = HeelAndHome.MODID)
public final class DogManager {
    public enum Mode { CALL, HOME }

    private record Dest(ServerLevel level, BlockPos pos) {}

    /** A chunk we force-loaded: either waiting for a dog to appear (dog != null) or just holding it open. */
    private static final class Pending {
        final UUID player, dog;
        final ResourceKey<Level> dim;
        final ChunkPos chunk;
        final Mode mode;
        final String home;
        final boolean forcedByUs;
        int age;

        Pending(UUID player, UUID dog, ResourceKey<Level> dim, ChunkPos chunk, Mode mode, String home, boolean forcedByUs) {
            this.player = player; this.dog = dog; this.dim = dim; this.chunk = chunk;
            this.mode = mode; this.home = home; this.forcedByUs = forcedByUs;
        }
    }

    private static final Map<UUID, Long> LAST_USE = new HashMap<>();
    private static final List<Pending> PENDING = new ArrayList<>();

    private DogManager() {}

    // ------------------------------------------------------------------ helpers

    private static PlayerData data(ServerPlayer p) {
        return p.getData(HeelAndHome.PLAYER_DATA);
    }

    private static void msg(ServerPlayer p, String key, Object... args) {
        p.displayClientMessage(Component.translatable("message.heelandhome." + key, args), true);
    }

    private static String dimId(Level l) {
        return l.dimension().location().toString();
    }

    private static ServerLevel levelOf(MinecraftServer s, String dim) {
        return s.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dim)));
    }

    private static Map<UUID, Wolf> loadedDogs(ServerPlayer p) {
        Map<UUID, Wolf> m = new LinkedHashMap<>();
        for (ServerLevel l : p.getServer().getAllLevels()) {
            for (Entity e : l.getAllEntities()) {
                if (e instanceof Wolf w && w.isTame() && w.isAlive() && p.getUUID().equals(w.getOwnerUUID())) {
                    m.put(w.getUUID(), w);
                }
            }
        }
        return m;
    }

    private static void remember(PlayerData d, Wolf w) {
        d.lastSeen.put(w.getUUID().toString(),
                new DogLoc(dimId(w.level()), w.blockPosition(), w.getName().getString()));
    }

    private static void forget(PlayerData d, String dogId) {
        d.lastSeen.remove(dogId);
        d.dogHome.remove(dogId);
    }

    private static Home findHome(PlayerData d, String name) {
        for (Home h : d.homes) if (h.name().equalsIgnoreCase(name)) return h;
        return null;
    }

    // ------------------------------------------------------------------ state sync

    public static void sendState(ServerPlayer p) {
        PlayerData d = data(p);
        loadedDogs(p).values().forEach(w -> remember(d, w));
        List<String> homes = new ArrayList<>();
        for (Home h : d.homes) homes.add(h.name());
        List<String> dogs = new ArrayList<>();
        for (Map.Entry<String, DogLoc> e : d.lastSeen.entrySet()) {
            dogs.add(e.getKey() + "\t" + e.getValue().name() + "\t" + d.dogHome.getOrDefault(e.getKey(), ""));
        }
        List<String> settings = List.of(
                "allowTeleport=" + Config.ALLOW_TELEPORT.get(),
                "crossDimension=" + Config.CROSS_DIMENSION.get(),
                "whistleSound=" + Config.WHISTLE_SOUND.get(),
                "disableNearCall=" + Config.DISABLE_NEAR_CALL.get(),
                "nearCallDistance=" + Config.NEAR_CALL_DISTANCE.get(),
                "canEdit=" + canEditConfig(p));
        PacketDistributor.sendToPlayer(p, new StatePayload(homes, dogs, settings));
    }

    // ------------------------------------------------------------------ homes

    // ------------------------------------------------------------------ in-game settings

    public static boolean canEditConfig(ServerPlayer p) {
        return p.hasPermissions(2) || p.getServer().isSingleplayerOwner(p.getGameProfile());
    }

    public static void setConfig(ServerPlayer p, String key, String value) {
        if (!canEditConfig(p)) { msg(p, "no_permission"); return; }
        switch (key) {
            case "allowTeleport" -> Config.ALLOW_TELEPORT.set(Boolean.parseBoolean(value));
            case "crossDimension" -> Config.CROSS_DIMENSION.set(Boolean.parseBoolean(value));
            case "whistleSound" -> Config.WHISTLE_SOUND.set(Boolean.parseBoolean(value));
            case "disableNearCall" -> Config.DISABLE_NEAR_CALL.set(Boolean.parseBoolean(value));
            case "nearCallDistance" -> {
                try {
                    Config.NEAR_CALL_DISTANCE.set(Math.max(1, Math.min(256, Integer.parseInt(value))));
                } catch (NumberFormatException ignored) { return; }
            }
            default -> { return; }
        }
        Config.SPEC.save();
    }

    public static void setHome(ServerPlayer p, String rawName) {
        PlayerData d = data(p);
        String name = rawName.replace("\t", "").trim();
        if (name.length() > 24) name = name.substring(0, 24);
        if (name.isEmpty()) name = "Home " + (d.homes.size() + 1);
        final String n = name;

        BlockPos pos;
        HitResult hr = p.pick(6.0, 1.0f, false);
        if (hr instanceof BlockHitResult bhr && hr.getType() == HitResult.Type.BLOCK) {
            pos = bhr.getBlockPos().relative(bhr.getDirection());
        } else {
            pos = p.blockPosition();
        }

        d.homes.removeIf(h -> h.name().equalsIgnoreCase(n));
        if (d.homes.size() >= 16) { msg(p, "too_many"); return; }
        d.homes.add(new Home(n, dimId(p.level()), pos));
        msg(p, "home_set", n, pos.toShortString());
    }

    public static void deleteHome(ServerPlayer p, String name) {
        PlayerData d = data(p);
        if (d.homes.removeIf(h -> h.name().equalsIgnoreCase(name))) {
            d.dogHome.values().removeIf(v -> v.equalsIgnoreCase(name));
            msg(p, "home_deleted", name);
        }
    }

    public static void linkDog(ServerPlayer p, String dogId, String homeName) {
        PlayerData d = data(p);
        List<String> ids = new ArrayList<>();
        if (dogId.isEmpty()) ids.addAll(d.lastSeen.keySet());
        else if (d.lastSeen.containsKey(dogId)) ids.add(dogId);
        Home h = homeName.isEmpty() ? null : findHome(d, homeName);
        if (!homeName.isEmpty() && h == null) return;
        for (String id : ids) {
            String dogName = d.lastSeen.get(id).name();
            if (h == null) {
                d.dogHome.remove(id);
                msg(p, "unlinked", dogName);
            } else {
                d.dogHome.put(id, h.name());
                msg(p, "linked", dogName, h.name());
            }
        }
    }

    // ------------------------------------------------------------------ call / send home

    public static void command(ServerPlayer p, Mode mode, String dogId, String homeName) {
        long now = p.level().getGameTime();
        Long last = LAST_USE.get(p.getUUID());
        if (last != null && now - last < Config.COOLDOWN_TICKS.get()) { msg(p, "cooldown"); return; }
        LAST_USE.put(p.getUUID(), now);

        PlayerData d = data(p);
        Map<UUID, Wolf> loaded = loadedDogs(p);
        loaded.values().forEach(w -> remember(d, w));

        List<UUID> ids = new ArrayList<>();
        if (dogId.isEmpty()) {
            for (String s : d.lastSeen.keySet()) {
                try { ids.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) { }
            }
        } else if (d.lastSeen.containsKey(dogId)) {
            try { ids.add(UUID.fromString(dogId)); } catch (IllegalArgumentException ignored) { }
        }
        if (ids.isEmpty()) { msg(p, "no_dogs"); return; }

        for (UUID id : ids) handle(p, d, id, mode, homeName, loaded.get(id));
    }

    private static Dest resolve(ServerPlayer p, PlayerData d, UUID id, Mode mode, String homeName) {
        if (mode == Mode.CALL) return new Dest(p.serverLevel(), p.blockPosition());
        String hn = homeName;
        if (hn.isEmpty()) hn = d.dogHome.getOrDefault(id.toString(), "");
        Home h = null;
        if (!hn.isEmpty()) h = findHome(d, hn);
        else if (!d.homes.isEmpty()) h = d.homes.get(0);
        if (h == null) return null;
        ServerLevel l = levelOf(p.getServer(), h.dim());
        return l == null ? null : new Dest(l, h.pos());
    }

    private static void handle(ServerPlayer p, PlayerData d, UUID id, Mode mode, String homeName, Wolf loadedWolf) {
        Dest dest = resolve(p, d, id, mode, homeName);
        if (dest == null) { msg(p, "no_home"); return; }

        if (loadedWolf != null) { dispatch(p, loadedWolf, mode, dest); return; }

        // Dog is in an unloaded area: load its last known chunk briefly, then act.
        DogLoc loc = d.lastSeen.get(id.toString());
        if (loc != null && !Config.ALLOW_TELEPORT.get()) { msg(p, "too_far", loc.name()); return; }
        ServerLevel lvl = loc == null ? null : levelOf(p.getServer(), loc.dim());
        if (lvl == null) { forget(d, id.toString()); msg(p, "not_found"); return; }
        ChunkPos cp = new ChunkPos(loc.pos());
        boolean ours = !lvl.getForcedChunks().contains(cp.toLong());
        if (ours) lvl.setChunkForced(cp.x, cp.z, true);
        PENDING.add(new Pending(p.getUUID(), id, lvl.dimension(), cp, mode, homeName, ours));
    }

    private static void dispatch(ServerPlayer p, Wolf wolf, Mode mode, Dest dest) {
        Component name = wolf.getName();
        if (wolf.isLeashed()) { msg(p, "busy", name); return; }
        boolean sameLevel = wolf.level() == dest.level();
        boolean tp = Config.ALLOW_TELEPORT.get();
        if (!sameLevel && !(tp && Config.CROSS_DIMENSION.get())) { msg(p, "no_cross", name); return; }

        double dist = sameLevel ? Math.sqrt(wolf.distanceToSqr(Vec3.atCenterOf(dest.pos()))) : Double.MAX_VALUE;
        if (mode == Mode.CALL && sameLevel && Config.DISABLE_NEAR_CALL.get()
                && dist < Config.NEAR_CALL_DISTANCE.get()) {
            msg(p, "too_close", name);
            return;
        }

        if (wolf.isPassenger()) wolf.stopRiding();
        wolf.setTarget(null);

        boolean walk = sameLevel && (!tp || dist <= Config.WALK_RANGE.get());
        if (walk) {
            CompoundTag t = wolf.getPersistentData();
            t.putInt(HeelGoal.MODE, mode == Mode.CALL ? 1 : 2);
            t.putInt(HeelGoal.X, dest.pos().getX());
            t.putInt(HeelGoal.Y, dest.pos().getY());
            t.putInt(HeelGoal.Z, dest.pos().getZ());
            wolf.setOrderedToSit(false);
            wolf.setInSittingPose(false);
        } else {
            teleportDog(wolf, dest.level(), dest.pos(), mode == Mode.HOME);
        }
        if (mode == Mode.CALL) whistle(p);
        msg(p, mode == Mode.CALL ? "coming" : "going", name);
    }

    private static final Map<UUID, Long> LAST_WHISTLE = new HashMap<>();

    private static void whistle(ServerPlayer p) {
        if (!Config.WHISTLE_SOUND.get()) return;
        long now = p.level().getGameTime();
        Long last = LAST_WHISTLE.get(p.getUUID());
        if (last != null && last == now) return; // one whistle per call, even with many dogs
        LAST_WHISTLE.put(p.getUUID(), now);
        p.level().playSound(null, p.blockPosition(), HeelAndHome.WHISTLE.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    public static void notifyArrived(Wolf w) {
        if (w.getOwner() instanceof ServerPlayer o) msg(o, "arrived", w.getName());
    }

    public static void notifyGaveUp(Wolf w) {
        if (w.getOwner() instanceof ServerPlayer o) msg(o, "gave_up", w.getName());
    }

    // ------------------------------------------------------------------ teleport

    public static Wolf teleportDog(Wolf wolf, ServerLevel dest, BlockPos near, boolean sit) {
        holdChunk(dest, near);
        BlockPos safe = findSafe(dest, near);
        Vec3 v = Vec3.atBottomCenterOf(safe);
        ServerLevel from = (ServerLevel) wolf.level();
        from.sendParticles(ParticleTypes.POOF, wolf.getX(), wolf.getY() + 0.5, wolf.getZ(), 12, 0.3, 0.3, 0.3, 0.02);

        wolf.getPersistentData().remove(HeelGoal.MODE);
        Wolf result = wolf;
        if (from != dest) {
            Entity moved = wolf.changeDimension(new DimensionTransition(
                    dest, v, Vec3.ZERO, wolf.getYRot(), wolf.getXRot(), DimensionTransition.DO_NOTHING));
            if (!(moved instanceof Wolf w2)) return null;
            result = w2;
        } else {
            wolf.getNavigation().stop();
            wolf.moveTo(v.x, v.y, v.z, wolf.getYRot(), wolf.getXRot());
        }
        result.getPersistentData().remove(HeelGoal.MODE);
        result.setTarget(null);
        result.setOrderedToSit(sit);
        result.setInSittingPose(sit);

        dest.sendParticles(ParticleTypes.HAPPY_VILLAGER, v.x, v.y + 0.5, v.z, 10, 0.3, 0.3, 0.3, 0.02);
        dest.playSound(null, safe, SoundEvents.WOLF_AMBIENT, SoundSource.NEUTRAL, 1.0f, 1.0f);
        if (sit) notifyArrived(result);
        return result;
    }

    private static void holdChunk(ServerLevel l, BlockPos p) {
        ChunkPos cp = new ChunkPos(p);
        if (!l.getForcedChunks().contains(cp.toLong())) {
            l.setChunkForced(cp.x, cp.z, true);
            PENDING.add(new Pending(null, null, l.dimension(), cp, null, null, true));
        }
    }

    private static BlockPos findSafe(ServerLevel l, BlockPos c) {
        int[] dys = {0, 1, -1, 2, -2};
        for (int r = 0; r <= 3; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    for (int dy : dys) {
                        BlockPos p = c.offset(dx, dy, dz);
                        if (isSafe(l, p)) return p;
                    }
                }
            }
        }
        return c;
    }

    private static boolean isSafe(ServerLevel l, BlockPos p) {
        if (p.getY() <= l.getMinBuildHeight() + 1 || p.getY() >= l.getMaxBuildHeight() - 2) return false;
        BlockState feet = l.getBlockState(p);
        BlockState head = l.getBlockState(p.above());
        BlockState below = l.getBlockState(p.below());
        return feet.getCollisionShape(l, p).isEmpty()
                && head.getCollisionShape(l, p.above()).isEmpty()
                && !below.getCollisionShape(l, p.below()).isEmpty()
                && l.getFluidState(p).isEmpty()
                && l.getFluidState(p.above()).isEmpty()
                && !feet.is(Blocks.FIRE) && !feet.is(Blocks.SOUL_FIRE)
                && !below.is(Blocks.MAGMA_BLOCK);
    }

    // ------------------------------------------------------------------ events

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide()) return;
        if (e.getEntity() instanceof Wolf w) w.goalSelector.addGoal(1, new HeelGoal(w));
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent e) {
        if (!(e.getLevel() instanceof ServerLevel sl)) return;
        if (!(e.getEntity() instanceof Wolf w) || !w.isTame() || w.getOwnerUUID() == null) return;
        Entity.RemovalReason r = w.getRemovalReason();
        if (r == null) return;
        ServerPlayer owner = sl.getServer().getPlayerList().getPlayer(w.getOwnerUUID());
        if (owner == null) return;
        PlayerData d = data(owner);
        if (r.shouldDestroy()) {
            forget(d, w.getUUID().toString());
        } else if (r == Entity.RemovalReason.UNLOADED_TO_CHUNK || r == Entity.RemovalReason.UNLOADED_WITH_PLAYER) {
            remember(d, w);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            PlayerData d = data(p);
            loadedDogs(p).values().forEach(w -> remember(d, w));
            LAST_USE.remove(p.getUUID());
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        if (PENDING.isEmpty()) return;
        MinecraftServer s = e.getServer();
        for (Pending pe : new ArrayList<>(PENDING)) {
            pe.age++;
            ServerLevel lvl = s.getLevel(pe.dim);
            boolean done = false;
            if (lvl == null) {
                done = true;
            } else if (pe.dog == null) {
                done = pe.age >= 100;
            } else if (pe.age % 5 == 0) {
                ServerPlayer p = s.getPlayerList().getPlayer(pe.player);
                Entity ent = lvl.getEntity(pe.dog);
                if (p == null) {
                    done = true;
                } else if (ent instanceof Wolf w) {
                    PlayerData d = data(p);
                    Dest dest = resolve(p, d, pe.dog, pe.mode, pe.home);
                    if (dest != null) dispatch(p, w, pe.mode, dest);
                    done = true;
                } else if (pe.age >= 100) {
                    forget(data(p), pe.dog.toString());
                    msg(p, "not_found");
                    done = true;
                }
            }
            if (done) {
                if (lvl != null && pe.forcedByUs) lvl.setChunkForced(pe.chunk.x, pe.chunk.z, false);
                PENDING.remove(pe);
            }
        }
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("dog")
                .then(Commands.literal("call").executes(ctx -> {
                    command(ctx.getSource().getPlayerOrException(), Mode.CALL, "", "");
                    return 1;
                }))
                .then(Commands.literal("home")
                        .then(Commands.literal("set")
                                .then(Commands.argument("name", StringArgumentType.word()).executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    setHome(p, StringArgumentType.getString(ctx, "name"));
                                    return 1;
                                })))
                        .then(Commands.literal("go")
                                .executes(ctx -> {
                                    command(ctx.getSource().getPlayerOrException(), Mode.HOME, "", "");
                                    return 1;
                                })
                                .then(Commands.argument("name", StringArgumentType.word()).executes(ctx -> {
                                    command(ctx.getSource().getPlayerOrException(), Mode.HOME, "",
                                            StringArgumentType.getString(ctx, "name"));
                                    return 1;
                                })))));
    }
}
