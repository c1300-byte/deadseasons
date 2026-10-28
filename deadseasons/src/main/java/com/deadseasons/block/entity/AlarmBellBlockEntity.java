package com.deadseasons.block.entity;

import com.deadseasons.block.AlarmBellBlock;
import com.deadseasons.registry.ModBlockEntities;
import com.deadseasons.system.NoiseSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class AlarmBellBlockEntity extends BlockEntity {
    private int cooldown = 0;
    private int poweredTicks = 0;

    public AlarmBellBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALARM_BELL.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlarmBellBlockEntity be) {
        if (be.poweredTicks > 0 && --be.poweredTicks == 0 && state.getValue(AlarmBellBlock.POWERED)) {
            level.setBlock(pos, state.setValue(AlarmBellBlock.POWERED, false), 3);
        }
        if (be.cooldown > 0) {
            be.cooldown--;
            return;
        }
        if (level.getGameTime() % 20 != 0) return;

        boolean hostile = !level.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(12),
                m -> m instanceof Enemy && m.isAlive()).isEmpty();
        if (!hostile) return;

        be.cooldown = 100;
        be.poweredTicks = 60;
        level.setBlock(pos, state.setValue(AlarmBellBlock.POWERED, true), 3);
        level.playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.5f, 1.0f);
        level.playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.5f, 1.2f);
        if (level instanceof ServerLevel server) {
            for (ServerPlayer p : server.players()) {
                if (p.blockPosition().distSqr(pos) < 96 * 96) {
                    p.displayClientMessage(Component.translatable("message.deadseasons.alarm", pos.getX(), pos.getY(), pos.getZ())
                            .withStyle(ChatFormatting.RED), true);
                }
            }
            NoiseSystem.makeNoise(server, pos, 16, null);
        }
    }
}
