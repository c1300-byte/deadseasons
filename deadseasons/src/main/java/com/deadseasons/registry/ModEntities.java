package com.deadseasons.registry;

import com.deadseasons.DeadSeasons;
import com.deadseasons.entity.AcidSpitEntity;
import com.deadseasons.entity.BoxTruckEntity;
import com.deadseasons.entity.SurvivorEntity;
import com.deadseasons.entity.SurvivorTraderEntity;
import com.deadseasons.entity.zombie.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DeadSeasons.MODID);

    public static final RegistryObject<EntityType<Walker>> WALKER = ENTITIES.register("walker",
            () -> EntityType.Builder.<Walker>of(Walker::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(8).build("walker"));
    public static final RegistryObject<EntityType<Runner>> RUNNER = ENTITIES.register("runner",
            () -> EntityType.Builder.<Runner>of(Runner::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(8).build("runner"));
    public static final RegistryObject<EntityType<Brute>> BRUTE = ENTITIES.register("brute",
            () -> EntityType.Builder.<Brute>of(Brute::new, MobCategory.MONSTER).sized(0.9f, 2.6f).clientTrackingRange(10).build("brute"));
    public static final RegistryObject<EntityType<Screamer>> SCREAMER = ENTITIES.register("screamer",
            () -> EntityType.Builder.<Screamer>of(Screamer::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(8).build("screamer"));
    public static final RegistryObject<EntityType<SoldierZombie>> SOLDIER = ENTITIES.register("soldier_zombie",
            () -> EntityType.Builder.<SoldierZombie>of(SoldierZombie::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(8).build("soldier_zombie"));
    public static final RegistryObject<EntityType<RiotZombie>> RIOT = ENTITIES.register("riot_zombie",
            () -> EntityType.Builder.<RiotZombie>of(RiotZombie::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(8).build("riot_zombie"));
    public static final RegistryObject<EntityType<Spitter>> SPITTER = ENTITIES.register("spitter",
            () -> EntityType.Builder.<Spitter>of(Spitter::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(8).build("spitter"));
    public static final RegistryObject<EntityType<Bloater>> BLOATER = ENTITIES.register("bloater",
            () -> EntityType.Builder.<Bloater>of(Bloater::new, MobCategory.MONSTER).sized(0.8f, 2.1f).clientTrackingRange(8).build("bloater"));
    public static final RegistryObject<EntityType<Crawler>> CRAWLER = ENTITIES.register("crawler",
            () -> EntityType.Builder.<Crawler>of(Crawler::new, MobCategory.MONSTER).sized(0.6f, 1.0f).clientTrackingRange(8).build("crawler"));
    public static final RegistryObject<EntityType<Frostbitten>> FROSTBITTEN = ENTITIES.register("frostbitten",
            () -> EntityType.Builder.<Frostbitten>of(Frostbitten::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(8).build("frostbitten"));
    public static final RegistryObject<EntityType<Abomination>> ABOMINATION = ENTITIES.register("abomination",
            () -> EntityType.Builder.<Abomination>of(Abomination::new, MobCategory.MONSTER).sized(1.2f, 3.4f).clientTrackingRange(12).build("abomination"));

    public static final RegistryObject<EntityType<AcidSpitEntity>> ACID_SPIT = ENTITIES.register("acid_spit",
            () -> EntityType.Builder.<AcidSpitEntity>of(AcidSpitEntity::new, MobCategory.MISC).sized(0.3f, 0.3f)
                    .clientTrackingRange(4).updateInterval(10).build("acid_spit"));

    public static final RegistryObject<EntityType<SurvivorEntity>> SURVIVOR = ENTITIES.register("survivor",
            () -> EntityType.Builder.<SurvivorEntity>of(SurvivorEntity::new, MobCategory.MISC).sized(0.6f, 1.8f).clientTrackingRange(10).build("survivor"));
    public static final RegistryObject<EntityType<SurvivorTraderEntity>> TRADER = ENTITIES.register("survivor_trader",
            () -> EntityType.Builder.<SurvivorTraderEntity>of(SurvivorTraderEntity::new, MobCategory.MISC).sized(0.6f, 1.95f).clientTrackingRange(10).build("survivor_trader"));

    public static final RegistryObject<EntityType<BoxTruckEntity>> BOX_TRUCK = ENTITIES.register("box_truck",
            () -> EntityType.Builder.<BoxTruckEntity>of(BoxTruckEntity::new, MobCategory.MISC).sized(3.0f, 2.9f).clientTrackingRange(10).build("box_truck"));

    private ModEntities() {}
}
