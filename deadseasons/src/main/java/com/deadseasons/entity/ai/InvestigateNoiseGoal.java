package com.deadseasons.entity.ai;

import com.deadseasons.entity.zombie.DSZombie;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Walk toward the last loud noise heard (gunshots, explosions, engines, alarms). */
public class InvestigateNoiseGoal extends Goal {
    private final DSZombie zombie;

    public InvestigateNoiseGoal(DSZombie zombie) {
        this.zombie = zombie;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return zombie.getTarget() == null && zombie.getNoiseTarget() != null;
    }

    @Override
    public void start() {
        BlockPos p = zombie.getNoiseTarget();
        if (p != null) zombie.getNavigation().moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 1.1);
    }

    @Override
    public boolean canContinueToUse() {
        BlockPos p = zombie.getNoiseTarget();
        if (zombie.getTarget() != null || p == null) return false;
        if (p.distToCenterSqr(zombie.position()) < 4.0) {
            zombie.clearNoiseTarget();
            return false;
        }
        return !zombie.getNavigation().isDone();
    }

    @Override
    public void stop() {
        if (zombie.getNavigation().isDone()) zombie.clearNoiseTarget();
    }
}
