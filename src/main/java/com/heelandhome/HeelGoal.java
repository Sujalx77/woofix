package com.heelandhome;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Wolf;

import java.util.EnumSet;

/** Walks a dog to a target (its owner, or a home spot) and sits it down at homes. */
public class HeelGoal extends Goal {
    public static final String MODE = "hh_mode"; // 1 = call, 2 = home
    public static final String X = "hh_x";
    public static final String Y = "hh_y";
    public static final String Z = "hh_z";

    private final Wolf wolf;
    private int ticks;

    public HeelGoal(Wolf wolf) {
        this.wolf = wolf;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return wolf.isTame() && wolf.getPersistentData().getInt(MODE) != 0;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        ticks = 0;
        wolf.setOrderedToSit(false);
        wolf.setInSittingPose(false);
    }

    @Override
    public void stop() {
        wolf.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (!(wolf.level() instanceof ServerLevel level)) return;
        CompoundTag t = wolf.getPersistentData();
        int mode = t.getInt(MODE);
        if (mode == 0) return;
        ticks++;

        double tx = t.getInt(X) + 0.5, ty = t.getInt(Y), tz = t.getInt(Z) + 0.5;
        if (mode == 1) {
            LivingEntity owner = wolf.getOwner();
            if (owner != null && owner.level() == level) {
                tx = owner.getX();
                ty = owner.getY();
                tz = owner.getZ();
            }
        }

        double arrive = mode == 1 ? 3.0 : 1.5;
        if (wolf.distanceToSqr(tx, ty, tz) <= arrive * arrive) {
            t.remove(MODE);
            wolf.getNavigation().stop();
            if (mode == 2) {
                wolf.setOrderedToSit(true);
                wolf.setInSittingPose(true);
                DogManager.notifyArrived(wolf);
            }
            return;
        }

        boolean tp = Config.ALLOW_TELEPORT.get();
        if (ticks > (tp ? Config.WALK_TIMEOUT_TICKS.get() : Config.WALK_ONLY_TIMEOUT_TICKS.get())) {
            if (tp) {
                DogManager.teleportDog(wolf, level, BlockPos.containing(tx, ty, tz), mode == 2);
            } else {
                t.remove(MODE);
                wolf.getNavigation().stop();
                DogManager.notifyGaveUp(wolf);
            }
            return;
        }

        if (ticks % 10 == 1) {
            wolf.getNavigation().moveTo(tx, ty, tz, 1.4);
        }
    }
}
