package com.deadseasons.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Board up doors and windows. Zombies that are chasing you will claw at barricades until they break.
 * Right-click with planks (wooden) or iron ingots (metal) to repair.
 */
public class BarricadeBlock extends Block {
    public static final IntegerProperty DAMAGE = IntegerProperty.create("damage", 0, 7);
    private final int maxHits;
    private final boolean metal;

    public BarricadeBlock(Properties props, int maxHits, boolean metal) {
        super(props);
        this.maxHits = maxHits;
        this.metal = metal;
        this.registerDefaultState(this.stateDefinition.any().setValue(DAMAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DAMAGE);
    }

    /** Called by zombies. Returns true if the barricade broke. */
    public boolean hit(Level level, BlockPos pos, BlockState state, int amount) {
        int dmg = state.getValue(DAMAGE) + amount;
        level.playSound(null, pos, metal ? SoundEvents.ZOMBIE_ATTACK_IRON_DOOR : SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,
                SoundSource.HOSTILE, 1.0f, 0.9f + level.random.nextFloat() * 0.2f);
        if (dmg >= maxHits) {
            level.destroyBlock(pos, false);
            level.playSound(null, pos, metal ? SoundEvents.ANVIL_BREAK : SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                    SoundSource.HOSTILE, 1.0f, 1.0f);
            return true;
        }
        level.setBlock(pos, state.setValue(DAMAGE, Math.min(7, dmg)), 3);
        if (level instanceof ServerLevel server) {
            server.levelEvent(2001, pos, Block.getId(state));
        }
        return false;
    }

    /** Visual damage stage 0-3 for models. */
    public int stage(BlockState state) {
        return Math.min(3, state.getValue(DAMAGE) * 4 / maxHits);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        boolean repairItem = metal ? held.is(Items.IRON_INGOT) : held.is(ItemTags.PLANKS);
        if (state.getValue(DAMAGE) > 0 && repairItem) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(DAMAGE, Math.max(0, state.getValue(DAMAGE) - (metal ? 3 : 2))), 3);
                if (!player.getAbilities().instabuild) held.shrink(1);
                level.playSound(null, pos, metal ? SoundEvents.ANVIL_USE : SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.8f, 1.0f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && held.isEmpty()) {
            int health = maxHits - state.getValue(DAMAGE);
            player.displayClientMessage(Component.translatable("message.deadseasons.barricade", health, maxHits).withStyle(ChatFormatting.GRAY), true);
        }
        return InteractionResult.PASS;
    }
}
