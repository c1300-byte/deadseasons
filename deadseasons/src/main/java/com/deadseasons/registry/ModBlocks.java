package com.deadseasons.registry;

import com.deadseasons.DeadSeasons;
import com.deadseasons.block.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, DeadSeasons.MODID);

    // ---- Base defenses ----
    public static final RegistryObject<Block> BARBED_WIRE = BLOCKS.register("barbed_wire", () -> new BarbedWireBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).noCollission().noOcclusion().strength(2.0f, 6.0f)
                    .sound(SoundType.CHAIN).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> SPIKE_TRAP = BLOCKS.register("spike_trap", () -> new SpikeTrapBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).noOcclusion().strength(3.0f, 6.0f)
                    .sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> REINFORCED_WALL = BLOCKS.register("reinforced_wall", () -> new ReinforcedBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(25.0f, 1200.0f)
                    .sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> REINFORCED_DOOR = BLOCKS.register("reinforced_door", () -> new ReinforcedDoorBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(15.0f, 1200.0f).noOcclusion()
                    .sound(SoundType.METAL).requiresCorrectToolForDrops().pushReaction(PushReaction.DESTROY)));

    public static final RegistryObject<Block> WOODEN_BARRICADE = BLOCKS.register("wooden_barricade", () -> new BarricadeBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f, 3.0f).noOcclusion()
                    .sound(SoundType.WOOD), 4, false));

    public static final RegistryObject<Block> METAL_BARRICADE = BLOCKS.register("metal_barricade", () -> new BarricadeBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 8.0f).noOcclusion()
                    .sound(SoundType.METAL).requiresCorrectToolForDrops(), 8, true));

    public static final RegistryObject<Block> LANDMINE = BLOCKS.register("landmine", () -> new LandmineBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).noCollission().noOcclusion().strength(1.0f)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Block> ALARM_BELL = BLOCKS.register("alarm_bell", () -> new AlarmBellBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(3.0f, 6.0f).noOcclusion()
                    .sound(SoundType.ANVIL).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> FLOODLIGHT = BLOCKS.register("floodlight", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f, 6.0f)
                    .sound(SoundType.GLASS).lightLevel(s -> 15)));

    public static final RegistryObject<Block> AUTO_TURRET = BLOCKS.register("auto_turret", () -> new TurretBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 12.0f).noOcclusion()
                    .sound(SoundType.METAL).requiresCorrectToolForDrops()));

    // ---- Survival utilities ----
    public static final RegistryObject<Block> GROW_LAMP = BLOCKS.register("grow_lamp", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(1.5f)
                    .sound(SoundType.GLASS).lightLevel(s -> 15)));

    public static final RegistryObject<Block> RAIN_COLLECTOR = BLOCKS.register("rain_collector", () -> new RainCollectorBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion().sound(SoundType.WOOD).randomTicks()));

    public static final RegistryObject<Block> RADIO = BLOCKS.register("radio", () -> new RadioBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(1.5f).noOcclusion().sound(SoundType.METAL)));

    public static final RegistryObject<Block> SUPPLY_CRATE = BLOCKS.register("supply_crate", () -> new SupplyCrateBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(2.5f).sound(SoundType.WOOD)));

    public static final RegistryObject<Block> BLOOD_SPLATTER = BLOCKS.register("blood_splatter", () -> new CarpetBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).noCollission().noOcclusion().instabreak()
                    .sound(SoundType.SLIME_BLOCK)));

    // ---- Box truck interior (unbreakable, not obtainable) ----
    public static final RegistryObject<Block> TRUCK_WALL = BLOCKS.register("truck_wall", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(-1.0f, 3600000.0f).noLootTable()
                    .sound(SoundType.METAL)));
    public static final RegistryObject<Block> TRUCK_FLOOR = BLOCKS.register("truck_floor", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(-1.0f, 3600000.0f).noLootTable()
                    .sound(SoundType.METAL)));
    public static final RegistryObject<Block> TRUCK_LIGHT = BLOCKS.register("truck_light", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(-1.0f, 3600000.0f).noLootTable()
                    .sound(SoundType.GLASS).lightLevel(s -> 15)));
    public static final RegistryObject<Block> TRUCK_EXIT = BLOCKS.register("truck_exit", () -> new TruckExitBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(-1.0f, 3600000.0f).noLootTable()
                    .sound(SoundType.METAL)));

    private ModBlocks() {}
}
