package com.deadseasons.entity;

import com.deadseasons.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Settlement trader. Trades emeralds, scrap and zombie trophies for ammo, medicine, food and gear.
 * Never despawns. Flees from zombies.
 */
public class SurvivorTraderEntity extends WanderingTrader {
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(SurvivorTraderEntity.class, EntityDataSerializers.INT);

    public SurvivorTraderEntity(EntityType<? extends WanderingTrader> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WanderingTrader.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.5);
    }

    @Override
    protected void registerGoals() {
        // Same as the wandering trader, minus the invisibility potions
        this.goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal(this));
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.AvoidEntityGoal<>(this,
                net.minecraft.world.entity.monster.Zombie.class, 8.0f, 0.5, 0.6));
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.PanicGoal(this, 0.6));
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal(this));
        this.goalSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal(this, 0.35));
        this.goalSelector.addGoal(8, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 0.35));
        this.goalSelector.addGoal(9, new net.minecraft.world.entity.ai.goal.InteractGoal(this,
                net.minecraft.world.entity.player.Player.class, 3.0f, 1.0f));
        this.goalSelector.addGoal(10, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this,
                net.minecraft.world.entity.Mob.class, 8.0f));
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
    protected void updateTrades() {
        MerchantOffers offers = this.getOffers();
        List<Supplier<MerchantOffer>> pool = new ArrayList<>();
        // Selling to the player
        pool.add(() -> sell(Items.EMERALD, 2, ModItems.HANDGUN_AMMO.get(), 12));
        pool.add(() -> sell(Items.EMERALD, 3, ModItems.SHOTGUN_SHELLS.get(), 8));
        pool.add(() -> sell(Items.EMERALD, 3, ModItems.RIFLE_AMMO.get(), 10));
        pool.add(() -> sell(Items.EMERALD, 6, ModItems.ANTIBIOTICS.get(), 1));
        pool.add(() -> sell(Items.EMERALD, 2, ModItems.BANDAGE.get(), 3));
        pool.add(() -> sell(Items.EMERALD, 5, ModItems.MEDKIT.get(), 1));
        pool.add(() -> sell(Items.EMERALD, 1, ModItems.CLEAN_WATER.get(), 2));
        pool.add(() -> sell(Items.EMERALD, 2, ModItems.CANNED_BEANS.get(), 3));
        pool.add(() -> sell(Items.EMERALD, 3, ModItems.MRE.get(), 2));
        pool.add(() -> sell(Items.EMERALD, 4, ModItems.GAS_CAN.get(), 1));
        pool.add(() -> sell(Items.EMERALD, 5, ModItems.GUN_PARTS.get(), 1));
        pool.add(() -> sell(Items.EMERALD, 12, ModItems.PISTOL.get(), 1));
        pool.add(() -> sell(Items.EMERALD, 18, ModItems.SHOTGUN.get(), 1));
        pool.add(() -> sell(Items.EMERALD, 6, ModItems.WINTER_COAT.get(), 1));
        pool.add(() -> sell(Items.EMERALD, 4, ModItems.LIGHT_SHIRT.get(), 1));
        pool.add(() -> sell(Items.EMERALD, 4, ModItems.GAS_MASK.get(), 1));
        pool.add(() -> sell(Items.EMERALD, 3, ModItems.BARBED_WIRE.get(), 6));
        pool.add(() -> sell(Items.EMERALD, 2, Items.WHEAT_SEEDS, 16));
        pool.add(() -> sell(Items.EMERALD, 2, ModItems.PAINKILLERS.get(), 2));
        Collections.shuffle(pool, new java.util.Random(this.getUUID().getLeastSignificantBits()));
        for (int i = 0; i < 7 && i < pool.size(); i++) offers.add(pool.get(i).get());

        // Buying from the player (always available) - the trader pays emeralds
        offers.add(new MerchantOffer(new ItemStack(Items.ROTTEN_FLESH, 24), new ItemStack(Items.EMERALD, 1), 32, 2, 0.05f));
        offers.add(new MerchantOffer(new ItemStack(ModItems.SCRAP_METAL.get(), 8), new ItemStack(Items.EMERALD, 1), 32, 2, 0.05f));
        offers.add(new MerchantOffer(new ItemStack(Items.IRON_INGOT, 4), new ItemStack(Items.EMERALD, 1), 24, 2, 0.05f));
        offers.add(new MerchantOffer(new ItemStack(Items.WHEAT, 20), new ItemStack(Items.EMERALD, 1), 24, 2, 0.05f));
    }

    private static MerchantOffer sell(net.minecraft.world.item.Item cost, int price, net.minecraft.world.item.Item result, int count) {
        return new MerchantOffer(new ItemStack(cost, price), new ItemStack(result, count), 8, 5, 0.05f);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        RandomSource r = level.getRandom();
        this.entityData.set(VARIANT, r.nextInt(SurvivorEntity.VARIANTS));
        this.setCustomName(Component.translatable("entity.deadseasons.survivor_trader"));
        this.setPersistenceRequired();
        this.setDespawnDelay(0);
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(VARIANT, tag.getInt("Variant"));
    }
}
