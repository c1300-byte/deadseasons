package com.deadseasons.entity;

import com.deadseasons.registry.ModDamage;
import com.deadseasons.registry.ModItems;
import com.deadseasons.registry.ModSounds;
import com.deadseasons.system.NoiseSystem;
import com.deadseasons.system.Seasons;
import com.deadseasons.system.TruckInteriors;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * The rare box truck. Drive it with W/A/S/D. It needs fuel (Gas Cans), runs over zombies, and has a
 * cargo area you can build inside (sneak + right-click to enter). Zombies have a harder time finding
 * players who keep moving it around.
 */
public class BoxTruckEntity extends LivingEntity {
    private static final EntityDataAccessor<Float> FUEL = SynchedEntityData.defineId(BoxTruckEntity.class, EntityDataSerializers.FLOAT);
    public static final float MAX_FUEL = 100f;

    private int interiorId = -1;
    @Nullable private BlockPos anchor;
    private boolean occupiedCache = false;

    public BoxTruckEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.1f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 150.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FUEL, 25f);
    }

    public float getFuel() {
        return this.entityData.get(FUEL);
    }

    public void setFuel(float fuel) {
        this.entityData.set(FUEL, Mth.clamp(fuel, 0f, MAX_FUEL));
    }

    public int getInteriorId() {
        return interiorId;
    }

    public void setInteriorId(int id) {
        this.interiorId = id;
    }

    /** True if someone is driving or hiding in the cargo area. Zombies only go after occupied trucks. */
    public boolean isOccupied() {
        return isVehicle() || occupiedCache;
    }

    // ------------------------------------------------------------ interaction

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (level().isClientSide) return InteractionResult.SUCCESS;

        if (held.is(ModItems.GAS_CAN.get())) {
            if (getFuel() >= MAX_FUEL - 1) {
                player.displayClientMessage(Component.translatable("message.deadseasons.truck_full"), true);
            } else {
                setFuel(getFuel() + 25f);
                if (!player.getAbilities().instabuild) held.shrink(1);
                playSound(SoundEvents.BUCKET_EMPTY, 1.0f, 0.8f);
                player.displayClientMessage(Component.translatable("message.deadseasons.truck_fuel", Math.round(getFuel())), true);
            }
            return InteractionResult.CONSUME;
        }
        if ((held.is(Items.IRON_INGOT) || held.is(ModItems.SCRAP_METAL.get())) && getHealth() < getMaxHealth()) {
            heal(held.is(Items.IRON_INGOT) ? 20f : 10f);
            if (!player.getAbilities().instabuild) held.shrink(1);
            playSound(SoundEvents.ANVIL_USE, 0.6f, 1.2f);
            player.displayClientMessage(Component.translatable("message.deadseasons.truck_repair",
                    Math.round(getHealth()), Math.round(getMaxHealth())), true);
            return InteractionResult.CONSUME;
        }
        if (player.isShiftKeyDown() && player instanceof ServerPlayer sp) {
            TruckInteriors.enter(sp, this);
            return InteractionResult.CONSUME;
        }
        if (getPassengers().size() < 2 && !player.isPassenger()) {
            player.startRiding(this);
            if (getPassengers().size() == 1) {
                player.displayClientMessage(Component.translatable("message.deadseasons.truck_drive").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().size() < 2 && passenger instanceof Player;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction move) {
        if (!this.hasPassenger(passenger)) return;
        int idx = this.getPassengers().indexOf(passenger);
        double side = idx == 0 ? 0.5 : -0.5;
        Vec3 off = new Vec3(side, 0, 1.35).yRot(-this.getYRot() * Mth.DEG_TO_RAD);
        move.accept(passenger, this.getX() + off.x, this.getY() + 0.55 + passenger.getMyRidingOffset(), this.getZ() + off.z);
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.55;
    }

    // ------------------------------------------------------------ driving

    @Override
    protected void tickRidden(Player player, Vec3 input) {
        super.tickRidden(player, input);
        float speed = (float) this.getDeltaMovement().horizontalDistance();
        float steer = player.xxa;
        if (Math.abs(player.zza) > 0.01f || speed > 0.03f) {
            float dir = player.zza < -0.01f ? -1f : 1f;
            float turnRate = 4.0f * Mth.clamp(speed * 6f + 0.3f, 0.3f, 1f);
            this.setYRot(this.getYRot() - steer * turnRate * dir);
        }
        this.yRotO = this.getYRot();
        this.setRot(this.getYRot(), 0f);
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 input) {
        if (getFuel() <= 0f) return Vec3.ZERO;
        float forward = player.zza;
        if (forward < 0) forward *= 0.4f;
        return new Vec3(0, 0, forward);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) {
            // exhaust smoke while driving
            if (isVehicle() && getDeltaMovement().horizontalDistanceSqr() > 0.002 && random.nextInt(3) == 0) {
                Vec3 back = new Vec3(0.9, 0.3, -2.6).yRot(-getYRot() * Mth.DEG_TO_RAD);
                level().addParticle(ParticleTypes.SMOKE, getX() + back.x, getY() + back.y, getZ() + back.z, 0, 0.02, 0);
            }
            return;
        }

        double speed = getDeltaMovement().horizontalDistance();
        LivingEntity driver = getControllingPassenger();

        if (driver != null && speed > 0.05) {
            setFuel(getFuel() - 0.002f);
            if (tickCount % 40 == 0) {
                server.playSound(null, getX(), getY(), getZ(), ModSounds.TRUCK_ENGINE.get(), SoundSource.NEUTRAL, 1.5f, 0.8f + (float) speed);
                NoiseSystem.makeNoise(server, blockPosition(), 24, null);
            }
        }

        // Run over zombies
        if (speed > 0.16) {
            List<Entity> hits = server.getEntities(this, getBoundingBox().inflate(0.4), e -> e instanceof LivingEntity && e instanceof Enemy && e.isAlive());
            for (Entity e : hits) {
                LivingEntity living = (LivingEntity) e;
                float dmg = 6f + (float) speed * 40f;
                living.hurt(ModDamage.of(server, ModDamage.ROADKILL, this, driver), dmg);
                Vec3 push = getDeltaMovement().normalize().scale(1.2);
                living.push(push.x, 0.4, push.z);
                this.hurt(server.damageSources().generic(), 0.5f);
            }
        }

        if (tickCount % 40 == 0) {
            occupiedCache = interiorId >= 0 && TruckInteriors.playersInside(server.getServer(), interiorId) > 0;
            if (interiorId >= 0) TruckInteriors.updateTruckPosition(server, interiorId, this);
        }

        // Nomad tracking: moving your base around makes raids less likely
        if (tickCount % 100 == 0 && driver instanceof ServerPlayer sp) {
            if (anchor == null) anchor = blockPosition();
            if (anchor.distSqr(blockPosition()) > 64 * 64) {
                anchor = blockPosition();
                sp.getPersistentData().putLong("DSNomadDay", Seasons.dayIndex(server));
                sp.displayClientMessage(Component.translatable("message.deadseasons.truck_nomad").withStyle(ChatFormatting.DARK_GREEN), true);
            }
        }
    }

    // ------------------------------------------------------------ damage & misc

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() != null && this.hasPassenger(source.getEntity())) return false;
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)) return false;
        return super.hurt(source, amount);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean hitByPlayer) {
        spawnAtLocation(new ItemStack(ModItems.SCRAP_METAL.get(), 10));
        spawnAtLocation(new ItemStack(ModItems.GUN_PARTS.get(), 1));
        if (getFuel() >= 25f) spawnAtLocation(new ItemStack(ModItems.GAS_CAN.get(), 1));
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.5, getZ(), 40, 1.2, 0.8, 1.2, 0.02);
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return false;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isAffectedByPotions() {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ZOMBIE_ATTACK_IRON_DOOR;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GENERIC_EXPLODE;
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return Collections.emptyList();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Fuel", getFuel());
        tag.putInt("InteriorId", interiorId);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setFuel(tag.getFloat("Fuel"));
        interiorId = tag.contains("InteriorId") ? tag.getInt("InteriorId") : -1;
    }
}
