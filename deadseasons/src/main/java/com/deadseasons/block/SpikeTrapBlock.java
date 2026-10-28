package com.deadseasons.block;

import com.deadseasons.entity.SurvivorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Floor spikes. Hurts zombies badly, players only a little (sneak to walk over safely). */
public class SpikeTrapBlock extends Block {
    private static final VoxelShape OUTLINE = Block.box(0, 0, 0, 16, 7, 16);
    private static final VoxelShape COLLISION = Block.box(0, 0, 0, 16, 1, 16);

    public SpikeTrapBlock(Properties props) {
        super(props);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return OUTLINE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return COLLISION;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof LivingEntity living) || entity instanceof SurvivorEntity) return;
        if (living instanceof Player p && (p.isShiftKeyDown() || p.isCreative())) return;
        entity.makeStuckInBlock(state, new Vec3(0.8, 1.0, 0.8));
        living.hurt(level.damageSources().cactus(), living instanceof Enemy ? 4.0f : 1.0f);
    }
}
