package com.deadseasons;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Common config (config/deadseasons-common.toml). Every value can be changed without touching code.
 */
public final class Config {
    public static final ForgeConfigSpec SPEC;

    // Seasons
    public static final ForgeConfigSpec.IntValue SEASON_LENGTH_DAYS;
    public static final ForgeConfigSpec.BooleanValue SEASON_SNOW;
    public static final ForgeConfigSpec.DoubleValue AUTUMN_CROP_FAIL_CHANCE;
    public static final ForgeConfigSpec.DoubleValue WINTER_ANIMAL_SPAWN_CHANCE;
    public static final ForgeConfigSpec.DoubleValue AUTUMN_ANIMAL_SPAWN_CHANCE;

    // Survival
    public static final ForgeConfigSpec.BooleanValue THIRST_ENABLED;
    public static final ForgeConfigSpec.DoubleValue THIRST_RATE;
    public static final ForgeConfigSpec.BooleanValue TEMPERATURE_ENABLED;
    public static final ForgeConfigSpec.DoubleValue INFECTION_CHANCE;
    public static final ForgeConfigSpec.IntValue INFECTION_DAYS;

    // Zombies
    public static final ForgeConfigSpec.BooleanValue EXTRA_ZOMBIE_SPAWNS;
    public static final ForgeConfigSpec.IntValue MAX_NEARBY_ZOMBIES;
    public static final ForgeConfigSpec.BooleanValue DIFFICULTY_SCALING;
    public static final ForgeConfigSpec.DoubleValue MAX_HEALTH_BONUS;
    public static final ForgeConfigSpec.DoubleValue MAX_DAMAGE_BONUS;
    public static final ForgeConfigSpec.BooleanValue NOISE_ENABLED;

    // Events
    public static final ForgeConfigSpec.IntValue BLOOD_MOON_INTERVAL;
    public static final ForgeConfigSpec.DoubleValue FOG_NIGHT_CHANCE;
    public static final ForgeConfigSpec.IntValue AIRDROP_MIN_DAYS;
    public static final ForgeConfigSpec.IntValue AIRDROP_MAX_DAYS;
    public static final ForgeConfigSpec.IntValue HERD_MIN_DAYS;
    public static final ForgeConfigSpec.IntValue HERD_MAX_DAYS;
    public static final ForgeConfigSpec.IntValue RAID_MIN_DAYS;
    public static final ForgeConfigSpec.IntValue RAID_MAX_DAYS;

    // World
    public static final ForgeConfigSpec.DoubleValue TRUCK_TOWN_CHANCE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("seasons");
        SEASON_LENGTH_DAYS = b.comment("Length of each season in Minecraft days. Worlds always start in Summer.")
                .defineInRange("seasonLengthDays", 35, 1, 365);
        SEASON_SNOW = b.comment("Place snow layers and freeze water around players in Winter, melt it in Spring.")
                .define("winterSnow", true);
        AUTUMN_CROP_FAIL_CHANCE = b.comment("Chance that an outdoor crop growth tick fails in Autumn (Winter always fails outdoors).")
                .defineInRange("autumnCropFailChance", 0.4, 0.0, 1.0);
        WINTER_ANIMAL_SPAWN_CHANCE = b.comment("Fraction of natural animal spawns that still happen in Winter.")
                .defineInRange("winterAnimalSpawnChance", 0.25, 0.0, 1.0);
        AUTUMN_ANIMAL_SPAWN_CHANCE = b.comment("Fraction of natural animal spawns that still happen in Autumn.")
                .defineInRange("autumnAnimalSpawnChance", 0.6, 0.0, 1.0);
        b.pop();

        b.push("survival");
        THIRST_ENABLED = b.define("thirstEnabled", true);
        THIRST_RATE = b.comment("Multiplier for how fast thirst drains.").defineInRange("thirstRate", 1.0, 0.1, 5.0);
        TEMPERATURE_ENABLED = b.define("temperatureEnabled", true);
        INFECTION_CHANCE = b.comment("Chance a zombie hit infects the player.").defineInRange("infectionChance", 0.10, 0.0, 1.0);
        INFECTION_DAYS = b.comment("Days an infected player has to find a cure before dying.").defineInRange("infectionDays", 7, 1, 100);
        b.pop();

        b.push("zombies");
        EXTRA_ZOMBIE_SPAWNS = b.comment("Spawn extra zombies around players on top of normal mob spawning (day and night).")
                .define("extraZombieSpawns", true);
        MAX_NEARBY_ZOMBIES = b.comment("Extra spawns stop when this many zombies are within 64 blocks of a player.")
                .defineInRange("maxNearbyZombies", 24, 0, 200);
        DIFFICULTY_SCALING = b.comment("Zombies slowly get tougher the more days pass.").define("difficultyScaling", true);
        MAX_HEALTH_BONUS = b.comment("Max extra zombie health from scaling (1.0 = +100%). Reached at day 150.")
                .defineInRange("maxHealthBonus", 1.0, 0.0, 10.0);
        MAX_DAMAGE_BONUS = b.comment("Max extra zombie damage from scaling (0.5 = +50%). Reached at day 150.")
                .defineInRange("maxDamageBonus", 0.5, 0.0, 10.0);
        NOISE_ENABLED = b.comment("Gunshots, explosions, sprinting and engines attract nearby zombies.").define("noiseEnabled", true);
        b.pop();

        b.push("events");
        BLOOD_MOON_INTERVAL = b.comment("A Blood Moon happens every N days.").defineInRange("bloodMoonInterval", 60, 2, 1000);
        FOG_NIGHT_CHANCE = b.comment("Chance each night becomes a Fog Night.").defineInRange("fogNightChance", 0.12, 0.0, 1.0);
        AIRDROP_MIN_DAYS = b.defineInRange("airdropMinDays", 3, 1, 100);
        AIRDROP_MAX_DAYS = b.defineInRange("airdropMaxDays", 7, 1, 100);
        HERD_MIN_DAYS = b.defineInRange("herdMinDays", 5, 1, 100);
        HERD_MAX_DAYS = b.defineInRange("herdMaxDays", 9, 1, 100);
        RAID_MIN_DAYS = b.comment("Horde raids attack your position at night every so often.").defineInRange("raidMinDays", 8, 1, 100);
        RAID_MAX_DAYS = b.defineInRange("raidMaxDays", 14, 1, 100);
        b.pop();

        b.push("world");
        TRUCK_TOWN_CHANCE = b.comment("Chance a ruined town contains an abandoned box truck (very rare by design).")
                .defineInRange("truckTownChance", 0.04, 0.0, 1.0);
        b.pop();

        SPEC = b.build();
    }

    private Config() {}
}
