package com.deadseasons.event;

import com.deadseasons.DeadSeasons;
import com.deadseasons.entity.BoxTruckEntity;
import com.deadseasons.entity.SurvivorEntity;
import com.deadseasons.entity.SurvivorTraderEntity;
import com.deadseasons.entity.zombie.*;
import com.deadseasons.registry.ModEntities;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DeadSeasons.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModBusEvents {

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.WALKER.get(), Walker.createAttributes().build());
        event.put(ModEntities.RUNNER.get(), Runner.createAttributes().build());
        event.put(ModEntities.BRUTE.get(), Brute.createAttributes().build());
        event.put(ModEntities.SCREAMER.get(), Screamer.createAttributes().build());
        event.put(ModEntities.SOLDIER.get(), SoldierZombie.createAttributes().build());
        event.put(ModEntities.RIOT.get(), RiotZombie.createAttributes().build());
        event.put(ModEntities.SPITTER.get(), Spitter.createAttributes().build());
        event.put(ModEntities.BLOATER.get(), Bloater.createAttributes().build());
        event.put(ModEntities.CRAWLER.get(), Crawler.createAttributes().build());
        event.put(ModEntities.FROSTBITTEN.get(), Frostbitten.createAttributes().build());
        event.put(ModEntities.ABOMINATION.get(), Abomination.createAttributes().build());
        event.put(ModEntities.SURVIVOR.get(), SurvivorEntity.createAttributes().build());
        event.put(ModEntities.TRADER.get(), SurvivorTraderEntity.createAttributes().build());
        event.put(ModEntities.BOX_TRUCK.get(), BoxTruckEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onSpawnPlacements(SpawnPlacementRegisterEvent event) {
        SpawnPlacements.Type ground = SpawnPlacements.Type.ON_GROUND;
        Heightmap.Types hm = Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
        SpawnPlacementRegisterEvent.Operation op = SpawnPlacementRegisterEvent.Operation.REPLACE;
        // Burning types use normal darkness rules
        event.register(ModEntities.WALKER.get(), ground, hm, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules, op);
        event.register(ModEntities.RUNNER.get(), ground, hm, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules, op);
        event.register(ModEntities.SCREAMER.get(), ground, hm, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules, op);
        event.register(ModEntities.SPITTER.get(), ground, hm, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules, op);
        // Daylight-proof types can also roam in the open during the day
        event.register(ModEntities.BRUTE.get(), ground, hm, DSZombie::checkApocalypseSpawn, op);
        event.register(ModEntities.SOLDIER.get(), ground, hm, DSZombie::checkApocalypseSpawn, op);
        event.register(ModEntities.RIOT.get(), ground, hm, DSZombie::checkApocalypseSpawn, op);
        event.register(ModEntities.BLOATER.get(), ground, hm, DSZombie::checkApocalypseSpawn, op);
        event.register(ModEntities.CRAWLER.get(), ground, hm, DSZombie::checkApocalypseSpawn, op);
        event.register(ModEntities.FROSTBITTEN.get(), ground, hm, DSZombie::checkColdSpawn, op);
        event.register(ModEntities.ABOMINATION.get(), ground, hm, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules, op);
    }

    private ModBusEvents() {}
}
