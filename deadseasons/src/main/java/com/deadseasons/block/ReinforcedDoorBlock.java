package com.deadseasons.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/** A door you can open by hand that zombies (even Brutes) cannot break down. */
public class ReinforcedDoorBlock extends DoorBlock {
    public ReinforcedDoorBlock(Properties props) {
        super(props, BlockSetType.OAK);
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return false;
    }
}
