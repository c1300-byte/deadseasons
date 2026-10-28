package com.deadseasons.system;

import com.deadseasons.Config;
import com.deadseasons.registry.ModDamage;
import com.deadseasons.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Zombie bites have a 10% chance to infect. Infected players have 7 in-game days to take Antibiotics,
 * getting sicker as time passes, and die if they don't. Players never turn into zombies.
 */
public final class InfectionSystem {
    private static final String KEY = "DSInfectedAt";

    public static boolean isInfected(ServerPlayer player) {
        return player.getPersistentData().contains(KEY);
    }

    public static long durationTicks() {
        return Config.INFECTION_DAYS.get() * 24000L;
    }

    /** Ticks left before death, or -1 if not infected. */
    public static long remainingTicks(ServerPlayer player) {
        if (!isInfected(player)) return -1;
        long at = player.getPersistentData().getLong(KEY);
        long elapsed = player.serverLevel().getGameTime() - at;
        return Math.max(0, durationTicks() - elapsed);
    }

    public static void tryInfect(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator() || isInfected(player)) return;
        if (player.getRandom().nextFloat() >= Config.INFECTION_CHANCE.get()) return;
        player.getPersistentData().putLong(KEY, player.serverLevel().getGameTime());
        player.displayClientMessage(Component.translatable("message.deadseasons.infected", Config.INFECTION_DAYS.get())
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ZOMBIE_INFECT, SoundSource.PLAYERS, 1.0f, 0.8f);
        refreshEffect(player);
    }

    public static void cure(ServerPlayer player) {
        if (!isInfected(player)) {
            player.displayClientMessage(Component.translatable("message.deadseasons.not_infected"), true);
            return;
        }
        player.getPersistentData().remove(KEY);
        player.removeEffect(ModEffects.INFECTED.get());
        player.displayClientMessage(Component.translatable("message.deadseasons.cured").withStyle(ChatFormatting.GREEN), false);
    }

    /** Called once per second. */
    public static void tick(ServerPlayer player) {
        if (!isInfected(player)) return;
        if (player.isCreative() || player.isSpectator()) return;
        long remaining = remainingTicks(player);
        if (remaining <= 0) {
            player.getPersistentData().remove(KEY);
            player.removeEffect(ModEffects.INFECTED.get());
            player.hurt(ModDamage.of(player.level(), ModDamage.INFECTION), Float.MAX_VALUE);
            return;
        }
        refreshEffect(player);

        double progress = 1.0 - remaining / (double) durationTicks();
        int t = player.tickCount;
        if (progress > 0.3 && t % 600 < 20) {
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 0));
        }
        if (progress > 0.55) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, false));
            if (t % 2400 < 20) player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0));
        }
        if (progress > 0.8) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, true, false));
            if (t % 1200 < 20) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                player.hurt(ModDamage.of(player.level(), ModDamage.INFECTION), 2.0f);
            }
        }
        // Daily reminder
        if (t % 6000 < 20) {
            double days = remaining / 24000.0;
            player.displayClientMessage(Component.translatable("message.deadseasons.infection_reminder", String.format("%.1f", days))
                    .withStyle(ChatFormatting.RED), true);
        }
    }

    private static void refreshEffect(ServerPlayer player) {
        long remaining = remainingTicks(player);
        if (remaining <= 0) return;
        int dur = (int) Math.min(remaining, Integer.MAX_VALUE);
        MobEffectInstance current = player.getEffect(ModEffects.INFECTED.get());
        if (current == null || Math.abs(current.getDuration() - dur) > 100) {
            if (current != null) player.removeEffect(ModEffects.INFECTED.get());
            player.addEffect(new MobEffectInstance(ModEffects.INFECTED.get(), dur, 0, false, true, true));
        }
    }

    private InfectionSystem() {}
}
