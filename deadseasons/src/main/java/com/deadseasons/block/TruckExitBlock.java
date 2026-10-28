package com.deadseasons.block;

import com.deadseasons.system.TruckInteriors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The cargo doors inside a box truck. Right-click to step out behind the truck. */
public class TruckExitBlock extends Block {
    public TruckExitBlock(Properties props) {
        super(props);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            TruckInteriors.exit(sp);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
