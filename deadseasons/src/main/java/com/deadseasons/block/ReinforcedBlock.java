package com.deadseasons.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Reinforced wall: very blast resistant and mobs (withers, zombies) can't destroy it. */
public class ReinforcedBlock extends Block {
    public ReinforcedBlock(Properties props) {
        super(props);
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return false;
    }
}
