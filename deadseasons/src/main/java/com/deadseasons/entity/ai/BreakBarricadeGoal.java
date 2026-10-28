package com.deadseasons.entity.ai;

import com.deadseasons.block.BarricadeBlock;
import com.deadseasons.entity.zombie.DSZombie;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/** When a zombie is chasing something and a barricade is in reach, it stops and claws through it. */
public class BreakBarricadeGoal extends Goal {
    private final DSZombie zombie;
    @Nullable private BlockPos barricade;
    private int hitTimer;

    public BreakBarricadeGoal(DSZombie zombie) {
        this.zombie = zombie;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (zombie.tickCount % 10 != 0) return false;
        LivingEntity target = zombie.getTarget();
        if (target == null && zombie.getNoiseTarget() == null) return false;
        barricade = findBarricade();
        return barricade != null;
    }

    @Override
    public boolean canContinueToUse() {
        return barricade != null && zombie.level().getBlockState(barricade).getBlock() instanceof BarricadeBlock
                && barricade.distToCenterSqr(zombie.position()) < 9.0;
    }

    @Override
    public void start() {
        zombie.getNavigation().stop();
        hitTimer = 20;
    }

    @Override
    public void tick() {
        if (barricade == null) return;
        zombie.getLookControl().setLookAt(barricade.getX() + 0.5, barricade.getY() + 0.5, barricade.getZ() + 0.5);
        if (--hitTimer <= 0) {
            hitTimer = 30;
            BlockState state = zombie.level().getBlockState(barricade);
            if (state.getBlock() instanceof BarricadeBlock b) {
                zombie.swing(InteractionHand.MAIN_HAND);
                if (b.hit(zombie.level(), barricade, state, zombie.barricadeDamage())) barricade = null;
            }
        }
    }

    @Nullable
    private BlockPos findBarricade() {
        BlockPos base = zombie.blockPosition();
        LivingEntity target = zombie.getTarget();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(base.offset(-1, 0, -1), base.offset(1, 1, 1))) {
            if (zombie.level().getBlockState(p).getBlock() instanceof BarricadeBlock) {
                // Prefer barricades between us and the target
                double d = target != null ? p.distToCenterSqr(target.position()) : 0;
                if (d < bestDist) {
                    bestDist = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }
}
