package com.deadseasons.block;

import com.deadseasons.system.ApocalypseData;
import com.deadseasons.system.Seasons;
import com.deadseasons.system.WorldEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Emergency radio: reports the season, the next Blood Moon, the last airdrop and horde activity. */
public class RadioBlock extends HorizontalDirectionalBlock {
    private static final VoxelShape SHAPE = Block.box(2, 0, 4, 14, 9, 12);

    public RadioBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        MinecraftServer server = level.getServer();
        if (server == null) return InteractionResult.PASS;
        ApocalypseData data = ApocalypseData.get(server);
        long day = Seasons.dayIndex(server);
        Seasons.Season s = Seasons.fromDay(day);

        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.6f, 0.6f);
        player.sendSystemMessage(Component.translatable("radio.deadseasons.header").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.translatable("radio.deadseasons.day", day + 1, Component.translatable(s.key()),
                Seasons.daysLeftInSeason(day)).withStyle(ChatFormatting.GREEN));
        long bm = WorldEvents.daysUntilBloodMoon(day);
        player.sendSystemMessage((bm == 0 ? Component.translatable("radio.deadseasons.blood_moon_tonight")
                : Component.translatable("radio.deadseasons.blood_moon", bm)).withStyle(ChatFormatting.RED));
        if (data.lastAirdrop != null && day - data.lastAirdropDay <= 3) {
            BlockPos a = data.lastAirdrop;
            player.sendSystemMessage(Component.translatable("radio.deadseasons.airdrop", a.getX(), a.getY(), a.getZ()).withStyle(ChatFormatting.GREEN));
        } else {
            player.sendSystemMessage(Component.translatable("radio.deadseasons.no_airdrop").withStyle(ChatFormatting.GRAY));
        }
        long raidIn = data.nextRaidDay - day;
        if (raidIn <= 1) {
            player.sendSystemMessage(Component.translatable("radio.deadseasons.raid_soon").withStyle(ChatFormatting.GOLD));
        }
        long herdIn = data.nextHerdDay - day;
        if (herdIn <= 1) {
            player.sendSystemMessage(Component.translatable("radio.deadseasons.herd_soon").withStyle(ChatFormatting.GOLD));
        }
        return InteractionResult.CONSUME;
    }
}
