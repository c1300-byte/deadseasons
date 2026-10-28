package com.deadseasons.system;

import com.deadseasons.Config;
import com.deadseasons.item.ClothingItem;
import com.deadseasons.registry.ModDamage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Body temperature on a -10 (freezing) to +10 (overheating) scale. 0 is comfortable.
 * Affected by season, biome, time of day, weather, shelter, water, nearby fires and clothing.
 */
public final class TemperatureSystem {
    public static final float COLD = -5f;
    public static final float FREEZING = -8f;
    public static final float HOT = 5f;
    public static final float HEATSTROKE = 8f;
    private static final String KEY = "DSTemp";

    public static float get(ServerPlayer player) {
        return player.getPersistentData().getFloat(KEY);
    }

    public static void set(ServerPlayer player, float value) {
        player.getPersistentData().putFloat(KEY, Mth.clamp(value, -10f, 10f));
    }

    /** Called once per second. Returns the new body temperature. */
    public static float tick(ServerPlayer player) {
        if (!Config.TEMPERATURE_ENABLED.get() || player.isCreative() || player.isSpectator()) {
            set(player, 0f);
            return 0f;
        }
        float ambient = ambient(player);
        float body = get(player);
        float step = 0.25f;
        if (Math.abs(ambient - body) <= step) body = ambient;
        else body += ambient > body ? step : -step;
        set(player, body);
        applyEffects(player, body);
        return body;
    }

    public static float ambient(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();
        Seasons.Season season = Seasons.get(level);

        float t = switch (season) {
            case SUMMER -> 4f;
            case AUTUMN -> 0f;
            case WINTER -> -6f;
            case SPRING -> 1f;
        };
        if (!level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) t = 0f;
        float biomeTemp = level.getBiome(pos).value().getBaseTemperature();
        t += (biomeTemp - 0.8f) * 6f;

        boolean sky = level.canSeeSky(BlockPos.containing(player.getEyePosition()));
        if (!sky) {
            t *= 0.4f; // indoors / underground is milder
        } else {
            if (level.isNight()) t -= 2.5f;
            else if (season == Seasons.Season.SUMMER) t += 1f;
            if (level.isRainingAt(pos.above())) t -= 1.5f;
        }
        if (pos.getY() > 100) t -= (pos.getY() - 100) / 25f;
        if (player.isInWater()) t -= 3f;
        if (player.isOnFire()) t += 6f;
        if (nearHeat(level, pos)) t += 5f;

        float warmth = 0f, cooling = 0f;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack s = player.getItemBySlot(slot);
            if (s.getItem() instanceof ClothingItem c) {
                warmth += c.getWarmth();
                cooling += c.getCooling();
            } else if (s.getItem() instanceof ArmorItem a) {
                warmth += a.getMaterial() == ArmorMaterials.LEATHER ? 1.0f : 0.3f;
            }
        }
        if (t < 0f) {
            t = Math.min(t + warmth - cooling * 0.3f, 2f);
        } else {
            t = Math.max(t + warmth * 0.5f - cooling, -1f);
        }
        return Mth.clamp(t, -10f, 10f);
    }

    private static boolean nearHeat(ServerLevel level, BlockPos center) {
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-3, -1, -3), center.offset(3, 2, 3))) {
            BlockState s = level.getBlockState(p);
            if (s.is(Blocks.FIRE) || s.is(Blocks.SOUL_FIRE) || s.is(Blocks.LAVA) || s.is(Blocks.MAGMA_BLOCK)) return true;
            if (CampfireBlock.isLitCampfire(s)) return true;
            if (s.getBlock() instanceof AbstractFurnaceBlock && s.getValue(AbstractFurnaceBlock.LIT)) return true;
        }
        return false;
    }

    private static void applyEffects(ServerPlayer player, float body) {
        ServerLevel level = player.serverLevel();
        if (body <= FREEZING) {
            player.setTicksFrozen(Math.max(player.getTicksFrozen(), 120));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, true, false));
            if (player.tickCount % 80 < 20) player.hurt(ModDamage.of(level, ModDamage.HYPOTHERMIA), 1.0f);
        } else if (body <= COLD) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 0, true, false));
            if (player.tickCount % 1200 < 20) {
                player.displayClientMessage(Component.translatable("message.deadseasons.cold").withStyle(ChatFormatting.AQUA), true);
            }
        } else if (body >= HEATSTROKE) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, false));
            if (player.tickCount % 400 < 20) player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, true, false));
            if (player.tickCount % 80 < 20) player.hurt(ModDamage.of(level, ModDamage.HEATSTROKE), 1.0f);
        } else if (body >= HOT) {
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 0, true, false));
            if (player.tickCount % 1200 < 20) {
                player.displayClientMessage(Component.translatable("message.deadseasons.hot").withStyle(ChatFormatting.GOLD), true);
            }
        }
    }

    private TemperatureSystem() {}
}
