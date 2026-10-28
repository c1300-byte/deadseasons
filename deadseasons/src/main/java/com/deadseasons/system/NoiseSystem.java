package com.deadseasons.system;

import com.deadseasons.Config;
import com.deadseasons.entity.zombie.DSZombie;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Loud things attract zombies. Zombies that hear a noise walk to it; if the noisy player is close and
 * visible they'll go straight for them.
 */
public final class NoiseSystem {

    public static void makeNoise(ServerLevel level, BlockPos pos, int radius, @Nullable Player source) {
        if (radius <= 0 || !Config.NOISE_ENABLED.get()) return;
        List<Zombie> zombies = level.getEntitiesOfClass(Zombie.class, new AABB(pos).inflate(radius),
                z -> z.isAlive() && !(z instanceof ZombifiedPiglin) && z.getTarget() == null);
        for (Zombie z : zombies) {
            double dist = Math.sqrt(z.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
            if (dist > radius) continue;
            if (source != null && !source.isCreative() && !source.isSpectator() && dist < 20 && z.hasLineOfSight(source)) {
                z.setTarget(source);
            } else if (z instanceof DSZombie ds) {
                ds.hearNoise(pos);
            } else {
                z.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
            }
        }
    }

    /** Screamers: every zombie in range targets the player directly. */
    public static void alertZombies(ServerLevel level, BlockPos pos, int radius, Player target) {
        List<Zombie> zombies = level.getEntitiesOfClass(Zombie.class, new AABB(pos).inflate(radius),
                z -> z.isAlive() && !(z instanceof ZombifiedPiglin));
        for (Zombie z : zombies) {
            if (z.getTarget() == null) z.setTarget(target);
        }
    }

    private NoiseSystem() {}
}
