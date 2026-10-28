package com.deadseasons.block;

import com.deadseasons.block.entity.TurretBlockEntity;
import com.deadseasons.registry.ModBlockEntities;
import com.deadseasons.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Automatic sentry. Load it by right-clicking with any ammo (handgun, rifle or shells - 1 round = 1 shot).
 * Shoots hostile mobs within 18 blocks that it can see. Gunfire attracts more zombies!
 */
public class TurretBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 14, 14);

    public TurretBlock(Properties props) {
        super(props);
    }

    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TurretBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.AUTO_TURRET.get(), TurretBlockEntity::serverTick);
    }

    public static boolean isAmmo(ItemStack stack) {
        return stack.is(ModItems.HANDGUN_AMMO.get()) || stack.is(ModItems.RIFLE_AMMO.get()) || stack.is(ModItems.SHOTGUN_SHELLS.get());
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof TurretBlockEntity turret)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (isAmmo(held)) {
            int space = TurretBlockEntity.MAX_AMMO - turret.getAmmo();
            int add = Math.min(space, held.getCount());
            if (add > 0) {
                turret.addAmmo(add);
                if (!player.getAbilities().instabuild) held.shrink(add);
                level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_CHAIN, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
        }
        player.displayClientMessage(Component.translatable("message.deadseasons.turret_ammo", turret.getAmmo(), TurretBlockEntity.MAX_AMMO)
                .withStyle(ChatFormatting.YELLOW), true);
        return InteractionResult.CONSUME;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof TurretBlockEntity turret) turret.dropAmmo();
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
