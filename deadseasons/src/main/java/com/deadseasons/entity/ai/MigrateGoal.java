package com.deadseasons.entity.ai;

import com.deadseasons.entity.zombie.DSZombie;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Herd zombies trek across the map toward a far-away point, in short legs so pathfinding stays cheap. */
public class MigrateGoal extends Goal {
    private final DSZombie zombie;
    private int repath;

    public MigrateGoal(DSZombie zombie) {
        this.zombie = zombie;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return zombie.getTarget() == null && zombie.getMigrateTarget() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        BlockPos dest = zombie.getMigrateTarget();
        if (dest == null) return;
        if (dest.distToCenterSqr(zombie.position()) < 64) {
            zombie.setMigrateTarget(null);
            return;
        }
        if (--repath <= 0 || zombie.getNavigation().isDone()) {
            repath = 60;
            Vec3 here = zombie.position();
            Vec3 dir = new Vec3(dest.getX() - here.x, 0, dest.getZ() - here.z).normalize().scale(14);
            zombie.getNavigation().moveTo(here.x + dir.x, here.y, here.z + dir.z, 0.9);
        }
    }

    @Override
    public void stop() {
        zombie.getNavigation().stop();
    }
}
