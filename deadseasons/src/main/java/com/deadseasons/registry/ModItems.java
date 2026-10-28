package com.deadseasons.registry;

import com.deadseasons.DeadSeasons;
import com.deadseasons.item.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, DeadSeasons.MODID);

    // ------------------------------------------------------------ ammo & parts
    public static final RegistryObject<Item> HANDGUN_AMMO = simple("handgun_ammo", 64);
    public static final RegistryObject<Item> SHOTGUN_SHELLS = simple("shotgun_shells", 64);
    public static final RegistryObject<Item> RIFLE_AMMO = simple("rifle_ammo", 64);
    public static final RegistryObject<Item> GUN_PARTS = simple("gun_parts", 64);
    public static final RegistryObject<Item> SCRAP_METAL = simple("scrap_metal", 64);
    public static final RegistryObject<Item> CLOTH = simple("cloth", 64);
    public static final RegistryObject<Item> GAS_CAN = simple("gas_can", 16);
    public static final RegistryObject<Item> ACID_GLOB = simple("acid_glob", 64);

    // ------------------------------------------------------------ guns
    public static final RegistryObject<Item> PISTOL = gun("pistol", 500, new GunStats(6f, 1, 6, false, 12, 30, 1.5f, 48, 40, 1.5f, 3f, HANDGUN_AMMO::get, ModSounds.PISTOL_SHOT::get));
    public static final RegistryObject<Item> REVOLVER = gun("revolver", 600, new GunStats(11f, 1, 14, false, 6, 50, 0.8f, 64, 48, 1.5f, 6f, HANDGUN_AMMO::get, ModSounds.RIFLE_SHOT::get));
    public static final RegistryObject<Item> SMG = gun("smg", 800, new GunStats(4f, 1, 2, true, 30, 40, 4.0f, 40, 40, 1.4f, 1.2f, HANDGUN_AMMO::get, ModSounds.SMG_SHOT::get));
    public static final RegistryObject<Item> SHOTGUN = gun("shotgun", 450, new GunStats(3.5f, 8, 18, false, 6, 60, 7.0f, 24, 64, 1.25f, 8f, SHOTGUN_SHELLS::get, ModSounds.SHOTGUN_SHOT::get));
    public static final RegistryObject<Item> HUNTING_RIFLE = gun("hunting_rifle", 500, new GunStats(18f, 1, 25, false, 5, 55, 0.2f, 96, 64, 2.0f, 7f, RIFLE_AMMO::get, ModSounds.RIFLE_SHOT::get));
    public static final RegistryObject<Item> ASSAULT_RIFLE = gun("assault_rifle", 900, new GunStats(6.5f, 1, 3, true, 30, 50, 2.5f, 72, 56, 1.5f, 1.6f, RIFLE_AMMO::get, ModSounds.RIFLE_SHOT::get));

    // ------------------------------------------------------------ melee (damage = modifier + 1)
    public static final RegistryObject<Item> BASEBALL_BAT = ITEMS.register("baseball_bat", () -> new SwordItem(ModTiers.WOOD_WEAPON, 4, -2.6f, new Item.Properties()));
    public static final RegistryObject<Item> SPIKED_BAT = ITEMS.register("spiked_bat", () -> new SwordItem(ModTiers.IRON_WEAPON, 5, -2.7f, new Item.Properties()));
    public static final RegistryObject<Item> MACHETE = ITEMS.register("machete", () -> new SwordItem(ModTiers.IRON_WEAPON, 5, -2.2f, new Item.Properties()));
    public static final RegistryObject<Item> CROWBAR = ITEMS.register("crowbar", () -> new SwordItem(ModTiers.STEEL_WEAPON, 4, -2.4f, new Item.Properties()));
    public static final RegistryObject<Item> FIRE_AXE = ITEMS.register("fire_axe", () -> new AxeItem(ModTiers.STEEL_WEAPON, 8f, -3.1f, new Item.Properties()));
    public static final RegistryObject<Item> KATANA = ITEMS.register("katana", () -> new SwordItem(ModTiers.DIAMOND_WEAPON, 6, -2.2f, new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> SLEDGEHAMMER = ITEMS.register("sledgehammer", () -> new SwordItem(ModTiers.STEEL_WEAPON, 10, -3.3f, new Item.Properties()));

    // ------------------------------------------------------------ armor & clothing
    public static final RegistryObject<Item> MILITARY_HELMET = clothing("military_helmet", ModArmorMaterials.MILITARY, ArmorItem.Type.HELMET, 0.5f, 0f, null);
    public static final RegistryObject<Item> MILITARY_VEST = clothing("military_vest", ModArmorMaterials.MILITARY, ArmorItem.Type.CHESTPLATE, 0.5f, 0f, null);
    public static final RegistryObject<Item> MILITARY_PANTS = clothing("military_pants", ModArmorMaterials.MILITARY, ArmorItem.Type.LEGGINGS, 0.5f, 0f, null);
    public static final RegistryObject<Item> MILITARY_BOOTS = clothing("military_boots", ModArmorMaterials.MILITARY, ArmorItem.Type.BOOTS, 0.5f, 0f, null);

    public static final RegistryObject<Item> RIOT_HELMET = clothing("riot_helmet", ModArmorMaterials.RIOT, ArmorItem.Type.HELMET, 0.5f, 0f, null);
    public static final RegistryObject<Item> RIOT_ARMOR = clothing("riot_armor", ModArmorMaterials.RIOT, ArmorItem.Type.CHESTPLATE, 0.8f, 0f, null);
    public static final RegistryObject<Item> RIOT_LEGGINGS = clothing("riot_leggings", ModArmorMaterials.RIOT, ArmorItem.Type.LEGGINGS, 0.5f, 0f, null);
    public static final RegistryObject<Item> RIOT_BOOTS = clothing("riot_boots", ModArmorMaterials.RIOT, ArmorItem.Type.BOOTS, 0.5f, 0f, null);

    public static final RegistryObject<Item> WINTER_HAT = clothing("winter_hat", ModArmorMaterials.WINTER, ArmorItem.Type.HELMET, 2.0f, 0f, null);
    public static final RegistryObject<Item> WINTER_COAT = clothing("winter_coat", ModArmorMaterials.WINTER, ArmorItem.Type.CHESTPLATE, 3.5f, 0f, null);
    public static final RegistryObject<Item> SNOW_PANTS = clothing("snow_pants", ModArmorMaterials.WINTER, ArmorItem.Type.LEGGINGS, 2.5f, 0f, null);
    public static final RegistryObject<Item> SNOW_BOOTS = clothing("snow_boots", ModArmorMaterials.WINTER, ArmorItem.Type.BOOTS, 2.0f, 0f, null);

    public static final RegistryObject<Item> SUN_HAT = clothing("sun_hat", ModArmorMaterials.SUMMER, ArmorItem.Type.HELMET, 0f, 2.5f, null);
    public static final RegistryObject<Item> LIGHT_SHIRT = clothing("light_shirt", ModArmorMaterials.SUMMER, ArmorItem.Type.CHESTPLATE, 0f, 2.5f, null);
    public static final RegistryObject<Item> CARGO_SHORTS = clothing("cargo_shorts", ModArmorMaterials.SUMMER, ArmorItem.Type.LEGGINGS, 0f, 2.0f, null);
    public static final RegistryObject<Item> SNEAKERS = clothing("sneakers", ModArmorMaterials.SUMMER, ArmorItem.Type.BOOTS, 0f, 1.0f, null);

    public static final RegistryObject<Item> GAS_MASK = clothing("gas_mask", ModArmorMaterials.GAS_MASK, ArmorItem.Type.HELMET, 0.3f, 0f, "tooltip.deadseasons.gas_mask");

    // ------------------------------------------------------------ food & drink
    public static final RegistryObject<Item> CLEAN_WATER = ITEMS.register("clean_water", () -> new ConsumableItem.Builder(new Item.Properties().stacksTo(16))
            .drink().thirst(8).remainder(() -> Items.GLASS_BOTTLE).build());
    public static final RegistryObject<Item> SODA_CAN = ITEMS.register("soda_can", () -> new ConsumableItem.Builder(new Item.Properties().stacksTo(16)
            .food(new FoodProperties.Builder().nutrition(1).saturationMod(0.1f).alwaysEat().build()))
            .drink().thirst(6).effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 0)).build());
    public static final RegistryObject<Item> CANNED_BEANS = ITEMS.register("canned_beans", () -> new ConsumableItem.Builder(new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(7).saturationMod(0.7f).build())).build());
    public static final RegistryObject<Item> CANNED_PEACHES = ITEMS.register("canned_peaches", () -> new ConsumableItem.Builder(new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(5).saturationMod(0.5f).build())).thirst(3).build());
    public static final RegistryObject<Item> MRE = ITEMS.register("mre", () -> new ConsumableItem.Builder(new Item.Properties().stacksTo(16)
            .food(new FoodProperties.Builder().nutrition(10).saturationMod(0.9f).build())).thirst(2).useTicks(48).build());
    public static final RegistryObject<Item> ENERGY_BAR = ITEMS.register("energy_bar", () -> new ConsumableItem.Builder(new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(4).saturationMod(0.4f).fast().build()))
            .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 0)).useTicks(16).build());
    public static final RegistryObject<Item> COOKED_FLESH = ITEMS.register("cooked_flesh", () -> new ConsumableItem.Builder(new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(4).saturationMod(0.3f).meat()
                    .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 300, 0), 0.25f).build())).build());

    // ------------------------------------------------------------ medical
    public static final RegistryObject<Item> BANDAGE = ITEMS.register("bandage", () -> new ConsumableItem.Builder(new Item.Properties().stacksTo(16))
            .heal(4f).useTicks(24).effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 100, 0)).tooltip("tooltip.deadseasons.bandage").build());
    public static final RegistryObject<Item> MEDKIT = ITEMS.register("medkit", () -> new ConsumableItem.Builder(new Item.Properties().stacksTo(4).rarity(Rarity.UNCOMMON))
            .heal(12f).useTicks(40).effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 200, 1)).build());
    public static final RegistryObject<Item> ANTIBIOTICS = ITEMS.register("antibiotics", () -> new ConsumableItem.Builder(new Item.Properties().stacksTo(8).rarity(Rarity.UNCOMMON))
            .curesInfection().useTicks(24).build());
    public static final RegistryObject<Item> PAINKILLERS = ITEMS.register("painkillers", () -> new ConsumableItem.Builder(new Item.Properties().stacksTo(16))
            .useTicks(16).effect(() -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 0))
            .effect(() -> new MobEffectInstance(MobEffects.ABSORPTION, 600, 0)).build());

    // ------------------------------------------------------------ misc
    public static final RegistryObject<Item> BOX_TRUCK_SPAWNER = ITEMS.register("box_truck_spawner", () -> new TruckSpawnerItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    // ------------------------------------------------------------ block items
    public static final RegistryObject<Item> BARBED_WIRE = block(ModBlocks.BARBED_WIRE);
    public static final RegistryObject<Item> SPIKE_TRAP = block(ModBlocks.SPIKE_TRAP);
    public static final RegistryObject<Item> REINFORCED_WALL = block(ModBlocks.REINFORCED_WALL);
    public static final RegistryObject<Item> REINFORCED_DOOR = ITEMS.register("reinforced_door", () -> new DoubleHighBlockItem(ModBlocks.REINFORCED_DOOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> WOODEN_BARRICADE = block(ModBlocks.WOODEN_BARRICADE);
    public static final RegistryObject<Item> METAL_BARRICADE = block(ModBlocks.METAL_BARRICADE);
    public static final RegistryObject<Item> LANDMINE = block(ModBlocks.LANDMINE);
    public static final RegistryObject<Item> ALARM_BELL = block(ModBlocks.ALARM_BELL);
    public static final RegistryObject<Item> FLOODLIGHT = block(ModBlocks.FLOODLIGHT);
    public static final RegistryObject<Item> AUTO_TURRET = block(ModBlocks.AUTO_TURRET);
    public static final RegistryObject<Item> GROW_LAMP = block(ModBlocks.GROW_LAMP);
    public static final RegistryObject<Item> RAIN_COLLECTOR = block(ModBlocks.RAIN_COLLECTOR);
    public static final RegistryObject<Item> RADIO = block(ModBlocks.RADIO);
    public static final RegistryObject<Item> SUPPLY_CRATE = block(ModBlocks.SUPPLY_CRATE);
    public static final RegistryObject<Item> BLOOD_SPLATTER = block(ModBlocks.BLOOD_SPLATTER);

    // ------------------------------------------------------------ spawn eggs
    public static final RegistryObject<Item> WALKER_SPAWN_EGG = egg("walker", ModEntities.WALKER, 0x4C6B3A, 0x3B3B5C);
    public static final RegistryObject<Item> RUNNER_SPAWN_EGG = egg("runner", ModEntities.RUNNER, 0x6B7F3A, 0xB03A2E);
    public static final RegistryObject<Item> BRUTE_SPAWN_EGG = egg("brute", ModEntities.BRUTE, 0x3E5530, 0x5A4632);
    public static final RegistryObject<Item> SCREAMER_SPAWN_EGG = egg("screamer", ModEntities.SCREAMER, 0x8FA27A, 0xE0E0E0);
    public static final RegistryObject<Item> SOLDIER_SPAWN_EGG = egg("soldier_zombie", ModEntities.SOLDIER, 0x4B5320, 0x2F3A1C);
    public static final RegistryObject<Item> RIOT_SPAWN_EGG = egg("riot_zombie", ModEntities.RIOT, 0x1C1C24, 0x3D5AA8);
    public static final RegistryObject<Item> SPITTER_SPAWN_EGG = egg("spitter", ModEntities.SPITTER, 0x5F8B2C, 0xB7E33B);
    public static final RegistryObject<Item> BLOATER_SPAWN_EGG = egg("bloater", ModEntities.BLOATER, 0x7A8F4E, 0x9B6B9B);
    public static final RegistryObject<Item> CRAWLER_SPAWN_EGG = egg("crawler", ModEntities.CRAWLER, 0x52633F, 0x2A2A2A);
    public static final RegistryObject<Item> FROSTBITTEN_SPAWN_EGG = egg("frostbitten", ModEntities.FROSTBITTEN, 0x8FB3C9, 0xEAF6FF);
    public static final RegistryObject<Item> ABOMINATION_SPAWN_EGG = egg("abomination", ModEntities.ABOMINATION, 0x3A1F1F, 0x8B0000);
    public static final RegistryObject<Item> SURVIVOR_SPAWN_EGG = egg("survivor", ModEntities.SURVIVOR, 0x8A6A4A, 0x3E6B8A);
    public static final RegistryObject<Item> TRADER_SPAWN_EGG = egg("survivor_trader", ModEntities.TRADER, 0x6E5A3A, 0x2E8B57);

    // ------------------------------------------------------------ helpers
    private static RegistryObject<Item> simple(String name, int stack) {
        return ITEMS.register(name, () -> new Item(new Item.Properties().stacksTo(stack)));
    }

    private static RegistryObject<Item> gun(String name, int durability, GunStats stats) {
        return ITEMS.register(name, () -> new GunItem(stats, new Item.Properties().durability(durability)));
    }

    private static RegistryObject<Item> clothing(String name, ModArmorMaterials mat, ArmorItem.Type type, float warmth, float cooling, String tooltip) {
        return ITEMS.register(name, () -> new ClothingItem(mat, type, new Item.Properties(), warmth, cooling, tooltip));
    }

    private static RegistryObject<Item> block(RegistryObject<Block> block) {
        return ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static <T extends net.minecraft.world.entity.Mob> RegistryObject<Item> egg(String name,
            Supplier<? extends net.minecraft.world.entity.EntityType<T>> type, int bg, int fg) {
        return ITEMS.register(name + "_spawn_egg", () -> new ForgeSpawnEggItem(type, bg, fg, new Item.Properties()));
    }

    private ModItems() {}
}
