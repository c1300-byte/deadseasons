package com.deadseasons.block;

import com.deadseasons.entity.BoxTruckEntity;
import com.deadseasons.entity.SurvivorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Slows and cuts anything that walks through it. Zombies take double damage. Sneak to carefully cross. */
public class BarbedWireBlock extends Block {
    public BarbedWireBlock(Properties props) {
        super(props);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity instanceof BoxTruckEntity) return;
        if (entity instanceof SurvivorEntity) return;
        entity.makeStuckInBlock(state, new Vec3(0.35, 0.6, 0.35));
        if (!level.isClientSide && entity instanceof LivingEntity living) {
            if (living instanceof Player p && (p.isShiftKeyDown() || p.isCreative())) return;
            float dmg = living instanceof Enemy ? 2.0f : 1.0f;
            living.hurt(level.damageSources().cactus(), dmg);
        }
    }
}
