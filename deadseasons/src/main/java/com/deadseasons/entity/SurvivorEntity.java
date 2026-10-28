package com.deadseasons.entity;

import com.deadseasons.item.GunItem;
import com.deadseasons.item.GunStats;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * A human survivor. Feed one food to try to recruit it (35% chance per try).
 * Owner controls:  right-click empty hand = change mode (Follow / Guard / Farm / Wander)
 *                  sneak + right-click with weapon or armor = give it to them
 *                  sneak + right-click empty hand = take their weapon back (or dismiss if unarmed)
 *                  right-click with food = heal them
 */
public class SurvivorEntity extends PathfinderMob {
    public enum Mode { FOLLOW, GUARD, FARM, WANDER }

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(SurvivorEntity.class, EntityDataSerializers.INT);
    public static final int VARIANTS = 6;
    private static final String[] NAMES = {"Rick", "Daryl", "Maggie", "Glenn", "Carol", "Abraham", "Rosita", "Ellie", "Joel",
            "Tommy", "Marlene", "Bill", "Riley", "Nick", "Coach", "Rochelle", "Zoey", "Louis", "Francis", "Hank", "Jess",
            "Sam", "Dina", "Lena", "Marcus", "Iris", "Owen", "Grace", "Wade", "Nora"};

    @Nullable private UUID owner;
    private Mode mode = Mode.WANDER;
    @Nullable private BlockPos guardPos;

    public SurvivorEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setCanPickUpLoot(false);
        for (EquipmentSlot slot : EquipmentSlot.values()) this.setDropChance(slot, 2.0f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT, 0);
    }

    public int getVariant() {
        return this.entityData.get(VARIANT);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new GunAttackGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true) {
            @Override public boolean canUse() { return !holdingGun() && super.canUse(); }
            @Override public boolean canContinueToUse() { return !holdingGun() && super.canContinueToUse(); }
        });
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this));
        this.goalSelector.addGoal(4, new ReturnToGuardGoal(this));
        this.goalSelector.addGoal(5, new FarmGoal(this));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override public boolean canUse() { return (mode == Mode.WANDER || mode == Mode.FARM) && super.canUse(); }
        });
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, SurvivorEntity.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                e -> e instanceof Enemy && !(e instanceof Creeper)));
    }

    // ------------------------------------------------------------ ownership

    public boolean isOwnedBy(Player player) {
        return owner != null && owner.equals(player.getUUID());
    }

    public boolean hasOwner() {
        return owner != null;
    }

    @Nullable
    public Player getOwnerPlayer() {
        return owner == null ? null : level().getPlayerByUUID(owner);
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        if (mode == Mode.GUARD || mode == Mode.FARM) this.guardPos = this.blockPosition();
    }

    public Mode getMode() {
        return mode;
    }

    public boolean holdingGun() {
        return getMainHandItem().getItem() instanceof GunItem;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (level().isClientSide) return InteractionResult.SUCCESS;

        if (!hasOwner()) {
            if (held.isEdible()) {
                if (!player.getAbilities().instabuild) held.shrink(1);
                if (random.nextFloat() < 0.35f) {
                    this.owner = player.getUUID();
                    setMode(Mode.FOLLOW);
                    setPersistenceRequired();
                    particles(ParticleTypes.HEART);
                    player.displayClientMessage(Component.translatable("message.deadseasons.survivor_joined", getDisplayName()).withStyle(ChatFormatting.GREEN), false);
                } else {
                    particles(ParticleTypes.SMOKE);
                    player.displayClientMessage(Component.translatable("message.deadseasons.survivor_unsure", getDisplayName()), true);
                }
                return InteractionResult.CONSUME;
            }
            player.displayClientMessage(Component.translatable("message.deadseasons.survivor_hint"), true);
            return InteractionResult.CONSUME;
        }

        if (!isOwnedBy(player)) return InteractionResult.PASS;

        if (player.isShiftKeyDown()) {
            Item item = held.getItem();
            if (item instanceof SwordItem || item instanceof AxeItem || item instanceof GunItem || item instanceof BowItem) {
                ItemStack old = getMainHandItem();
                setItemSlot(EquipmentSlot.MAINHAND, held.copyWithCount(1));
                held.shrink(1);
                if (!old.isEmpty()) giveTo(player, old);
                player.displayClientMessage(Component.translatable("message.deadseasons.survivor_armed", getDisplayName()), true);
                return InteractionResult.CONSUME;
            }
            if (item instanceof ArmorItem armor) {
                EquipmentSlot slot = armor.getEquipmentSlot();
                ItemStack old = getItemBySlot(slot);
                setItemSlot(slot, held.copyWithCount(1));
                held.shrink(1);
                if (!old.isEmpty()) giveTo(player, old);
                return InteractionResult.CONSUME;
            }
            if (held.isEmpty()) {
                ItemStack weapon = getMainHandItem();
                if (!weapon.isEmpty()) {
                    setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                    giveTo(player, weapon);
                } else {
                    owner = null;
                    setMode(Mode.WANDER);
                    player.displayClientMessage(Component.translatable("message.deadseasons.survivor_dismissed", getDisplayName()), false);
                }
                return InteractionResult.CONSUME;
            }
        }

        if (held.isEdible()) {
            if (getHealth() < getMaxHealth()) {
                heal(6.0f);
                if (!player.getAbilities().instabuild) held.shrink(1);
                particles(ParticleTypes.HEART);
                playSound(SoundEvents.GENERIC_EAT, 1.0f, 1.0f);
            }
            return InteractionResult.CONSUME;
        }

        if (held.isEmpty()) {
            Mode next = Mode.values()[(mode.ordinal() + 1) % Mode.values().length];
            setMode(next);
            this.getNavigation().stop();
            player.displayClientMessage(Component.translatable("message.deadseasons.survivor_mode", getDisplayName(),
                    Component.translatable("mode.deadseasons." + next.name().toLowerCase())), true);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    private void giveTo(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private void particles(net.minecraft.core.particles.SimpleParticleType type) {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(type, getX(), getEyeY() + 0.3, getZ(), 5, 0.3, 0.3, 0.3, 0.02);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player p && isOwnedBy(p)) return false;
        if (source.getEntity() instanceof SurvivorEntity) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // Defend the owner
        if (!level().isClientSide && tickCount % 20 == 0 && getTarget() == null) {
            Player p = getOwnerPlayer();
            if (p != null && p.getLastHurtByMob() instanceof Monster m && m.isAlive() && distanceToSqr(m) < 24 * 24) {
                setTarget(m);
            }
        }
        // Slowly heal over time
        if (!level().isClientSide && tickCount % 100 == 0 && getHealth() < getMaxHealth()) heal(1.0f);
    }

    // ------------------------------------------------------------ spawning & saving

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        RandomSource r = level.getRandom();
        this.entityData.set(VARIANT, r.nextInt(VARIANTS));
        this.setCustomName(Component.literal(NAMES[r.nextInt(NAMES.length)]));
        this.setCustomNameVisible(true);
        int roll = r.nextInt(10);
        ItemStack weapon = switch (roll) {
            case 0, 1, 2 -> new ItemStack(com.deadseasons.registry.ModItems.BASEBALL_BAT.get());
            case 3, 4 -> new ItemStack(com.deadseasons.registry.ModItems.MACHETE.get());
            case 5 -> new ItemStack(com.deadseasons.registry.ModItems.CROWBAR.get());
            case 6, 7 -> new ItemStack(com.deadseasons.registry.ModItems.PISTOL.get());
            case 8 -> new ItemStack(com.deadseasons.registry.ModItems.SHOTGUN.get());
            default -> new ItemStack(Items.IRON_SWORD);
        };
        this.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        this.setDropChance(EquipmentSlot.MAINHAND, 0.15f);
        this.setPersistenceRequired();
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", getVariant());
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putString("Mode", mode.name());
        if (guardPos != null) tag.put("GuardPos", NbtUtils.writeBlockPos(guardPos));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(VARIANT, tag.getInt("Variant"));
        this.owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        try {
            this.mode = Mode.valueOf(tag.getString("Mode"));
        } catch (IllegalArgumentException e) {
            this.mode = Mode.WANDER;
        }
        this.guardPos = tag.contains("GuardPos") ? NbtUtils.readBlockPos(tag.getCompound("GuardPos")) : null;
    }

    /** Used by structures to place a settlement guard. */
    public void makeGuard(BlockPos pos) {
        this.mode = Mode.GUARD;
        this.guardPos = pos;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    // ================================================================== goals

    static class FollowOwnerGoal extends Goal {
        private final SurvivorEntity mob;
        private int recalc;

        FollowOwnerGoal(SurvivorEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (mob.mode != Mode.FOLLOW || mob.getTarget() != null) return false;
            Player p = mob.getOwnerPlayer();
            return p != null && !p.isSpectator() && mob.distanceToSqr(p) > 36;
        }

        @Override
        public boolean canContinueToUse() {
            Player p = mob.getOwnerPlayer();
            return mob.mode == Mode.FOLLOW && p != null && mob.getTarget() == null && mob.distanceToSqr(p) > 9;
        }

        @Override
        public void tick() {
            Player p = mob.getOwnerPlayer();
            if (p == null) return;
            mob.getLookControl().setLookAt(p, 10.0f, mob.getMaxHeadXRot());
            if (--recalc <= 0) {
                recalc = 10;
                if (mob.distanceToSqr(p) > 24 * 24 && !p.isPassenger()) {
                    mob.moveTo(p.getX(), p.getY(), p.getZ(), mob.getYRot(), mob.getXRot());
                    mob.getNavigation().stop();
                } else {
                    mob.getNavigation().moveTo(p, 1.15);
                }
            }
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
        }
    }

    static class ReturnToGuardGoal extends Goal {
        private final SurvivorEntity mob;

        ReturnToGuardGoal(SurvivorEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return mob.mode == Mode.GUARD && mob.guardPos != null && mob.getTarget() == null
                    && mob.guardPos.distToCenterSqr(mob.position()) > 4;
        }

        @Override
        public void start() {
            BlockPos g = mob.guardPos;
            if (g != null) mob.getNavigation().moveTo(g.getX() + 0.5, g.getY(), g.getZ() + 0.5, 1.0);
        }

        @Override
        public boolean canContinueToUse() {
            return mob.mode == Mode.GUARD && !mob.getNavigation().isDone() && mob.getTarget() == null;
        }
    }

    static class FarmGoal extends Goal {
        private final SurvivorEntity mob;
        @Nullable private BlockPos crop;
        private int cooldown;

        FarmGoal(SurvivorEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (mob.mode != Mode.FARM || mob.getTarget() != null) return false;
            if (--cooldown > 0) return false;
            cooldown = 60;
            BlockPos center = mob.guardPos != null ? mob.guardPos : mob.blockPosition();
            BlockPos best = null;
            double bestDist = Double.MAX_VALUE;
            for (BlockPos p : BlockPos.betweenClosed(center.offset(-8, -2, -8), center.offset(8, 2, 8))) {
                BlockState s = mob.level().getBlockState(p);
                if (s.getBlock() instanceof CropBlock c && c.isMaxAge(s)) {
                    double d = p.distSqr(mob.blockPosition());
                    if (d < bestDist) {
                        bestDist = d;
                        best = p.immutable();
                    }
                }
            }
            crop = best;
            return crop != null;
        }

        @Override
        public void start() {
            if (crop != null) mob.getNavigation().moveTo(crop.getX() + 0.5, crop.getY(), crop.getZ() + 0.5, 0.9);
        }

        @Override
        public boolean canContinueToUse() {
            return crop != null && mob.mode == Mode.FARM && mob.getTarget() == null;
        }

        @Override
        public void tick() {
            if (crop == null) return;
            if (crop.distToCenterSqr(mob.position()) < 4.0) {
                BlockState s = mob.level().getBlockState(crop);
                if (s.getBlock() instanceof CropBlock c && c.isMaxAge(s)) {
                    mob.level().destroyBlock(crop, true, mob);
                    mob.level().setBlock(crop, c.getStateForAge(0), 3);
                    mob.swing(InteractionHand.MAIN_HAND);
                }
                crop = null;
            } else if (mob.getNavigation().isDone()) {
                mob.getNavigation().moveTo(crop.getX() + 0.5, crop.getY(), crop.getZ() + 0.5, 0.9);
            }
        }
    }

    static class GunAttackGoal extends Goal {
        private final SurvivorEntity mob;
        private int shotTimer;

        GunAttackGoal(SurvivorEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = mob.getTarget();
            return mob.holdingGun() && t != null && t.isAlive();
        }

        @Override
        public void tick() {
            LivingEntity target = mob.getTarget();
            if (target == null || !(mob.getMainHandItem().getItem() instanceof GunItem gun)) return;
            GunStats stats = gun.getStats();
            double dist = mob.distanceTo(target);
            boolean canSee = mob.getSensing().hasLineOfSight(target);
            mob.getLookControl().setLookAt(target, 30.0f, 30.0f);

            if (dist > 14 || !canSee) {
                mob.getNavigation().moveTo(target, 1.1);
            } else {
                mob.getNavigation().stop();
            }

            if (--shotTimer <= 0 && canSee && dist < stats.range() * 0.7) {
                shotTimer = Math.max(stats.fireDelay() * 3, 12);
                Vec3 eye = mob.getEyePosition();
                double dx = target.getX() - eye.x;
                double dy = target.getY(0.6) - eye.y;
                double dz = target.getZ() - eye.z;
                double horiz = Math.sqrt(dx * dx + dz * dz);
                float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
                float pitch = (float) -(Mth.atan2(dy, horiz) * (180.0 / Math.PI));
                Player owner = mob.getOwnerPlayer();
                GunItem.shoot(mob.level(), mob, mob, eye, yaw, pitch, stats, 0.75f,
                        e -> !(e instanceof SurvivorEntity) && e != owner && !(e instanceof Player));
                mob.swing(InteractionHand.MAIN_HAND);
            }
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
        }
    }
}
