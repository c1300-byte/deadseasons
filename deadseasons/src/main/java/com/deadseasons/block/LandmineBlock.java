package com.deadseasons.block;

import com.deadseasons.entity.BoxTruckEntity;
import com.deadseasons.entity.SurvivorEntity;
import com.deadseasons.system.NoiseSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Explodes when a mob steps on it (no terrain damage). Sneaking players can walk over it safely.
 * Survivors and your truck won't set it off.
 */
public class LandmineBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 2, 13);

    public LandmineBlock(Properties props) {
        super(props);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return canSupportCenter(level, pos.below(), net.minecraft.core.Direction.UP);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, net.minecraft.core.Direction dir, BlockState neighbor,
                                  net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return dir == net.minecraft.core.Direction.DOWN && !canSurvive(state, level, pos)
                ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof LivingEntity)) return;
        if (entity instanceof SurvivorEntity || entity instanceof BoxTruckEntity) return;
        if (entity instanceof Player p && (p.isShiftKeyDown() || p.isCreative() || p.isSpectator())) return;
        level.removeBlock(pos, false);
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 3.0f, Level.ExplosionInteraction.NONE);
        NoiseSystem.makeNoise((ServerLevel) level, pos, 48, null);
    }
}
