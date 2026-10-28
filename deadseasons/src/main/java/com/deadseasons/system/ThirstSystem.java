package com.deadseasons.system;

import com.deadseasons.Config;
import com.deadseasons.registry.ModDamage;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;

/**
 * Thirst: 0-20. Drains over time (faster in Summer, when hot, and when sprinting).
 * Water bottles are "dirty" and can make you sick - boil them in a furnace/campfire or use a rain collector.
 */
public final class ThirstSystem {
    public static final float MAX = 20f;
    private static final String KEY = "DSThirst";

    public static float get(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        return data.contains(KEY) ? data.getFloat(KEY) : MAX;
    }

    public static void set(ServerPlayer player, float value) {
        player.getPersistentData().putFloat(KEY, Mth.clamp(value, 0f, MAX));
    }

    public static void addThirst(ServerPlayer player, float amount) {
        set(player, get(player) + amount);
    }

    /** Called once per second. */
    public static void tick(ServerPlayer player, float bodyTemp) {
        if (!Config.THIRST_ENABLED.get() || player.isCreative() || player.isSpectator()) return;

        float drain = (MAX / 900f) * Config.THIRST_RATE.get().floatValue(); // empty in ~15 minutes at rest
        if (Seasons.get(player.level()) == Seasons.Season.SUMMER) drain *= 1.25f;
        if (bodyTemp >= TemperatureSystem.HOT) drain *= 1.6f;
        if (player.isSprinting()) drain *= 1.8f;
        if (player.isInWater()) drain *= 0.5f;

        float thirst = get(player) - drain;
        set(player, thirst);

        if (thirst <= 0.01f) {
            if (player.tickCount % 80 < 20) {
                player.hurt(ModDamage.of(player.level(), ModDamage.DEHYDRATION), 1.0f);
            }
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, false));
        } else if (thirst < 6f) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, true, false));
            if (player.tickCount % 1200 < 20) {
                player.displayClientMessage(Component.translatable("message.deadseasons.thirsty").withStyle(ChatFormatting.AQUA), true);
            }
        }
    }

    /** Thirst from vanilla food and drink (our own items handle themselves). */
    public static void onVanillaConsumed(ServerPlayer player, ItemStack stack) {
        if (stack.is(Items.POTION)) {
            if (PotionUtils.getPotion(stack) == Potions.WATER) {
                addThirst(player, 5f);
                if (player.getRandom().nextFloat() < 0.4f) {
                    player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
                    player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 400, 1));
                    player.displayClientMessage(Component.translatable("message.deadseasons.dirty_water").withStyle(ChatFormatting.DARK_GREEN), true);
                }
            } else {
                addThirst(player, 3f);
            }
        } else if (stack.is(Items.MILK_BUCKET)) {
            addThirst(player, 6f);
        } else if (stack.is(Items.MELON_SLICE)) {
            addThirst(player, 2f);
        } else if (stack.is(Items.HONEY_BOTTLE)) {
            addThirst(player, 2f);
        } else if (stack.is(Items.MUSHROOM_STEW) || stack.is(Items.BEETROOT_SOUP) || stack.is(Items.RABBIT_STEW) || stack.is(Items.SUSPICIOUS_STEW)) {
            addThirst(player, 4f);
        } else if (stack.is(Items.APPLE) || stack.is(Items.SWEET_BERRIES) || stack.is(Items.GLOW_BERRIES) || stack.is(Items.CARROT)) {
            addThirst(player, 1f);
        }
    }

    private ThirstSystem() {}
}
