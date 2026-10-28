package com.deadseasons.item;

import com.deadseasons.system.InfectionSystem;
import com.deadseasons.system.ThirstSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Food, drinks and medicine. Handles thirst, healing, effects, curing infection and returned containers.
 */
public class ConsumableItem extends Item {
    private final int thirst;
    private final float heal;
    private final boolean drink;
    private final boolean curesInfection;
    private final int useTicks;
    private final List<Supplier<MobEffectInstance>> effects;
    @Nullable private final Supplier<Item> remainder;
    @Nullable private final String tooltipKey;

    private ConsumableItem(Builder b) {
        super(b.props);
        this.thirst = b.thirst;
        this.heal = b.heal;
        this.drink = b.drink;
        this.curesInfection = b.curesInfection;
        this.useTicks = b.useTicks;
        this.effects = b.effects;
        this.remainder = b.remainder;
        this.tooltipKey = b.tooltipKey;
    }

    public int getThirst() { return thirst; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isEdible() && thirst <= 0 && heal <= 0 && !curesInfection && effects.isEmpty()) {
            if (!player.canEat(getFoodProperties() != null && getFoodProperties().canAlwaysEat())) {
                return InteractionResultHolder.fail(stack);
            }
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return useTicks;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return drink ? UseAnim.DRINK : UseAnim.EAT;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        boolean creative = entity instanceof Player p && p.getAbilities().instabuild;
        if (isEdible()) {
            // Handles hunger/saturation, eating sound and shrinking the stack
            stack = entity.eat(level, stack);
        } else {
            if (!creative) stack.shrink(1);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    drink ? SoundEvents.GENERIC_DRINK : SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.6f, 1.0f);
        }

        if (!level.isClientSide) {
            if (thirst > 0 && entity instanceof ServerPlayer sp) ThirstSystem.addThirst(sp, thirst);
            if (heal > 0) entity.heal(heal);
            for (Supplier<MobEffectInstance> e : effects) entity.addEffect(e.get());
            if (curesInfection && entity instanceof ServerPlayer sp) InfectionSystem.cure(sp);
        }

        if (remainder != null && !creative) {
            ItemStack rem = new ItemStack(remainder.get());
            if (stack.isEmpty()) return rem;
            if (entity instanceof Player p && !p.getInventory().add(rem)) p.drop(rem, false);
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (thirst > 0) tooltip.add(Component.translatable("tooltip.deadseasons.thirst", thirst).withStyle(ChatFormatting.BLUE));
        if (heal > 0) tooltip.add(Component.translatable("tooltip.deadseasons.heal", String.format("%.0f", heal / 2f)).withStyle(ChatFormatting.RED));
        if (curesInfection) tooltip.add(Component.translatable("tooltip.deadseasons.cures_infection").withStyle(ChatFormatting.GREEN));
        if (tooltipKey != null) tooltip.add(Component.translatable(tooltipKey).withStyle(ChatFormatting.GRAY));
    }

    public static class Builder {
        private final Item.Properties props;
        private int thirst = 0;
        private float heal = 0;
        private boolean drink = false;
        private boolean curesInfection = false;
        private int useTicks = 32;
        private final List<Supplier<MobEffectInstance>> effects = new ArrayList<>();
        private Supplier<Item> remainder = null;
        private String tooltipKey = null;

        public Builder(Item.Properties props) { this.props = props; }
        public Builder thirst(int t) { this.thirst = t; return this; }
        public Builder heal(float h) { this.heal = h; return this; }
        public Builder drink() { this.drink = true; return this; }
        public Builder curesInfection() { this.curesInfection = true; return this; }
        public Builder useTicks(int t) { this.useTicks = t; return this; }
        public Builder effect(Supplier<MobEffectInstance> e) { this.effects.add(e); return this; }
        public Builder remainder(Supplier<Item> r) { this.remainder = r; return this; }
        public Builder tooltip(String key) { this.tooltipKey = key; return this; }
        public ConsumableItem build() { return new ConsumableItem(this); }
    }
}
