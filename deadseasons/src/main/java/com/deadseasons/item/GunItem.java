package com.deadseasons.item;

import com.deadseasons.entity.SurvivorEntity;
import com.deadseasons.registry.ModDamage;
import com.deadseasons.registry.ModItems;
import com.deadseasons.registry.ModSounds;
import com.deadseasons.system.NoiseSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * Hitscan firearm. Right-click to fire (hold for automatic guns), sneak + right-click or press R to reload.
 * Rounds in the magazine are stored on the item as NBT "Ammo".
 */
public class GunItem extends Item {
    private static final String AMMO_TAG = "Ammo";
    private final GunStats stats;

    public GunItem(GunStats stats, Properties props) {
        super(props);
        this.stats = stats;
    }

    public GunStats getStats() { return stats; }

    public static int getAmmo(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt(AMMO_TAG) : 0;
    }

    public static void setAmmo(ItemStack stack, int ammo) {
        stack.getOrCreateTag().putInt(AMMO_TAG, ammo);
    }

    // ------------------------------------------------------------------ use

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) tryReload(player, stack);
            return InteractionResultHolder.consume(stack);
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        fire(level, player, stack, hand);
        if (stats.automatic()) {
            player.startUsingItem(hand);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!stats.automatic() || !(entity instanceof Player player)) return;
        int elapsed = getUseDuration(stack) - remaining;
        if (elapsed > 0 && elapsed % stats.fireDelay() == 0 && !player.getCooldowns().isOnCooldown(this)) {
            fire(level, player, stack, player.getUsedItemHand());
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    private void fire(Level level, Player player, ItemStack stack, InteractionHand hand) {
        boolean creative = player.getAbilities().instabuild;
        int ammo = getAmmo(stack);
        if (ammo <= 0) {
            if (!level.isClientSide) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.GUN_EMPTY.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
                tryReload(player, stack);
            }
            player.releaseUsingItem();
            return;
        }

        if (!stats.automatic()) {
            player.getCooldowns().addCooldown(this, stats.fireDelay());
        }

        if (level.isClientSide) {
            // Camera recoil for the shooter
            player.setXRot(player.getXRot() - stats.recoil());
            player.setYRot(player.getYRot() + (player.getRandom().nextFloat() - 0.5f) * stats.recoil() * 0.5f);
            return;
        }

        if (!creative) setAmmo(stack, ammo - 1);

        shoot(level, player, player, player.getEyePosition(), player.getYRot(), player.getXRot(), stats, 1.0f,
                e -> !(e instanceof SurvivorEntity s && s.isOwnedBy(player)));

        if (!creative) {
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        }
    }

    /**
     * Server-side hitscan shot used by players, survivors and turrets.
     */
    public static void shoot(Level level, @Nullable LivingEntity shooter, Entity contextEntity, Vec3 eye, float yaw, float pitch,
                             GunStats stats, float damageMultiplier, Predicate<Entity> extraFilter) {
        if (!(level instanceof ServerLevel server)) return;
        RandomSource rand = level.getRandom();

        for (int i = 0; i < stats.pellets(); i++) {
            float y = yaw + (rand.nextFloat() - 0.5f) * 2f * stats.spread();
            float p = pitch + (rand.nextFloat() - 0.5f) * 2f * stats.spread();
            Vec3 dir = Vec3.directionFromRotation(p, y);
            Vec3 end = eye.add(dir.scale(stats.range()));

            BlockHitResult blockHit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, contextEntity));
            Vec3 stop = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();

            AABB search = new AABB(eye, stop).inflate(1.0);
            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, contextEntity, eye, stop, search,
                    e -> e instanceof LivingEntity && e.isAlive() && e.isPickable() && !e.isSpectator() && e != shooter
                            && (shooter == null || !e.isPassengerOfSameVehicle(shooter)) && extraFilter.test(e));

            Vec3 impact = stop;
            if (entityHit != null) {
                impact = entityHit.getLocation();
                LivingEntity target = (LivingEntity) entityHit.getEntity();
                float dmg = stats.damage() * damageMultiplier;
                // Approximate hit point on the target's box to check for headshots
                Vec3 onBox = target.getBoundingBox().clip(eye, stop).orElse(impact);
                if (onBox.y >= target.getEyeY() - 0.25) {
                    dmg *= stats.headshot();
                    server.sendParticles(ParticleTypes.CRIT, onBox.x, onBox.y, onBox.z, 6, 0.1, 0.1, 0.1, 0.2);
                }
                target.invulnerableTime = 0;
                target.hurt(ModDamage.of(level, ModDamage.BULLET, shooter, shooter), dmg);
                impact = onBox;
            } else if (blockHit.getType() != HitResult.Type.MISS) {
                server.sendParticles(ParticleTypes.SMOKE, impact.x, impact.y, impact.z, 3, 0.05, 0.05, 0.05, 0.01);
            }

            // Tracer
            double dist = eye.distanceTo(impact);
            int steps = Mth.clamp((int) (dist / 2.5), 1, 24);
            for (int s = 1; s <= steps; s++) {
                Vec3 pt = eye.add(dir.scale(dist * s / (double) steps));
                server.sendParticles(ParticleTypes.CRIT, pt.x, pt.y - 0.1, pt.z, 1, 0, 0, 0, 0);
            }
        }

        Vec3 muzzle = eye.add(Vec3.directionFromRotation(pitch, yaw).scale(0.8));
        server.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y - 0.1, muzzle.z, 3, 0.02, 0.02, 0.02, 0.01);
        level.playSound(null, eye.x, eye.y, eye.z, stats.sound().get(), SoundSource.PLAYERS, 4.0f,
                0.9f + rand.nextFloat() * 0.2f);
        NoiseSystem.makeNoise(server, net.minecraft.core.BlockPos.containing(eye), stats.noise(),
                shooter instanceof Player pl ? pl : null);
    }

    // ------------------------------------------------------------------ reload

    public static void tryReload(Player player, ItemStack stack) {
        if (!(stack.getItem() instanceof GunItem gun)) return;
        GunStats st = gun.stats;
        int ammo = getAmmo(stack);
        if (ammo >= st.magazine() || player.getCooldowns().isOnCooldown(gun)) return;

        int needed = st.magazine() - ammo;
        int found = 0;
        if (player.getAbilities().instabuild) {
            found = needed;
        } else {
            Item ammoItem = st.ammo().get();
            for (int i = 0; i < player.getInventory().getContainerSize() && found < needed; i++) {
                ItemStack s = player.getInventory().getItem(i);
                if (s.is(ammoItem)) {
                    int take = Math.min(s.getCount(), needed - found);
                    s.shrink(take);
                    found += take;
                }
            }
        }
        if (found <= 0) {
            player.displayClientMessage(Component.translatable("message.deadseasons.no_ammo",
                    Component.translatable(st.ammo().get().getDescriptionId())).withStyle(ChatFormatting.RED), true);
            return;
        }
        setAmmo(stack, ammo + found);
        player.releaseUsingItem();
        player.getCooldowns().addCooldown(gun, st.reloadTicks());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.GUN_RELOAD.get(),
                SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    public static int countAmmo(Player player, Item ammoItem) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(ammoItem)) count += s.getCount();
        }
        return count;
    }

    // ------------------------------------------------------------------ display

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0f * getAmmo(stack) / (float) stats.magazine());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xE8C547;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(ModItems.GUN_PARTS.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.deadseasons.gun_ammo", getAmmo(stack), stats.magazine(),
                Component.translatable(stats.ammo().get().getDescriptionId())).withStyle(ChatFormatting.YELLOW));
        String dmg = stats.pellets() > 1 ? stats.pellets() + " x " + fmt(stats.damage()) : fmt(stats.damage());
        tooltip.add(Component.translatable("tooltip.deadseasons.gun_damage", dmg).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(stats.automatic() ? "tooltip.deadseasons.gun_auto" : "tooltip.deadseasons.gun_semi")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.deadseasons.gun_durability", stack.getMaxDamage() - stack.getDamageValue(),
                stack.getMaxDamage()).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.deadseasons.gun_help").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static String fmt(float f) {
        return f == (int) f ? String.valueOf((int) f) : String.format("%.1f", f);
    }
}
