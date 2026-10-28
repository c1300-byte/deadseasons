package com.deadseasons.system;

import com.deadseasons.Config;
import com.deadseasons.block.entity.SupplyCrateBlockEntity;
import com.deadseasons.entity.BoxTruckEntity;
import com.deadseasons.entity.zombie.DSZombie;
import com.deadseasons.registry.ModBlocks;
import com.deadseasons.registry.ModEntities;
import com.deadseasons.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;

import java.util.ArrayList;
import java.util.List;

/** Blood moons, fog nights, airdrops, migrating herds, horde raids and season announcements. */
public final class WorldEvents {
    public static final net.minecraft.resources.ResourceLocation AIRDROP_LOOT = com.deadseasons.DeadSeasons.id("chests/airdrop");

    // ------------------------------------------------------------ queries used by sync / client

    public static boolean isBloodMoonDay(long day) {
        int interval = Config.BLOOD_MOON_INTERVAL.get();
        return day % interval == interval - 1;
    }

    public static boolean isBloodMoonActive(Level level) {
        long t = Seasons.timeOfDay(level);
        return isBloodMoonDay(Seasons.dayIndex(level)) && t >= 12500 && t < 23500;
    }

    public static boolean isFogActive(MinecraftServer server) {
        ApocalypseData data = ApocalypseData.get(server);
        long day = Seasons.dayIndex(server);
        long t = server.overworld().getDayTime() % 24000L;
        return data.fogDay == day && t >= 12500 && t < 23500;
    }

    public static long daysUntilBloodMoon(long day) {
        int interval = Config.BLOOD_MOON_INTERVAL.get();
        return (interval - 1 - (day % interval) + interval) % interval;
    }

    // ------------------------------------------------------------ main tick (overworld, once per second)

    public static void tick(ServerLevel overworld) {
        MinecraftServer server = overworld.getServer();
        ApocalypseData data = ApocalypseData.get(server);
        RandomSource r = overworld.getRandom();
        long day = Seasons.dayIndex(server);
        long time = overworld.getDayTime() % 24000L;

        initSchedule(data, day, r);
        announceSeason(server, data, day);

        if (overworld.getDifficulty() == Difficulty.PEACEFUL) return;

        // ---- Blood moon
        if (isBloodMoonDay(day)) {
            if (time >= 10000 && time < 12500 && data.bloodMoonWarnedDay != day) {
                data.bloodMoonWarnedDay = day;
                data.setDirty();
                broadcast(server, Component.translatable("event.deadseasons.blood_moon_warning").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            }
            if (time >= 12500 && time < 23500) {
                if (time < 12520) {
                    broadcast(server, Component.translatable("event.deadseasons.blood_moon_start").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                    for (ServerPlayer p : overworld.players()) {
                        overworld.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.BLOOD_MOON.get(), SoundSource.AMBIENT, 1.0f, 1.0f);
                    }
                }
                if (overworld.getGameTime() % 200 == 0) bloodMoonWave(overworld, day, r);
                if (time >= 18000 && data.bossSpawnedDay != day) {
                    data.bossSpawnedDay = day;
                    data.setDirty();
                    for (ServerPlayer p : overworld.players()) {
                        if (p.isCreative() || p.isSpectator()) continue;
                        ZombieSpawner.spawnAround(overworld, ModEntities.ABOMINATION.get(), p.blockPosition(), 24, 36, p, true);
                    }
                    broadcast(server, Component.translatable("event.deadseasons.boss").withStyle(ChatFormatting.DARK_RED));
                }
            }
        }
        if (isBloodMoonDay(day - 1) && time < 1000 && data.bloodMoonEndedDay != day - 1) {
            data.bloodMoonEndedDay = day - 1;
            data.setDirty();
            broadcast(server, Component.translatable("event.deadseasons.blood_moon_end").withStyle(ChatFormatting.GOLD));
        }

        // ---- Fog nights
        if (time >= 12000 && time < 12500 && data.fogRolledDay != day) {
            data.fogRolledDay = day;
            if (!isBloodMoonDay(day) && r.nextDouble() < Config.FOG_NIGHT_CHANCE.get()) {
                data.fogDay = day;
                broadcast(server, Component.translatable("event.deadseasons.fog").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
            data.setDirty();
        }
        if (data.fogDay == day && time >= 12500 && time < 23500 && overworld.getGameTime() % 400 == 0) {
            for (ServerPlayer p : overworld.players()) {
                if (p.isCreative() || p.isSpectator()) continue;
                for (int i = 0; i < 2; i++) {
                    EntityType<? extends DSZombie> t = r.nextBoolean() ? ModEntities.CRAWLER.get() : ModEntities.RUNNER.get();
                    ZombieSpawner.spawnAround(overworld, t, p.blockPosition(), 20, 36, null, false);
                }
            }
        }

        // ---- Airdrops (daytime)
        if (day >= data.nextAirdropDay && time >= 5000 && time < 9000 && !overworld.players().isEmpty()) {
            airdrop(overworld, r);
            data.nextAirdropDay = day + randBetween(r, Config.AIRDROP_MIN_DAYS.get(), Config.AIRDROP_MAX_DAYS.get());
            data.setDirty();
        }

        // ---- Migrating herds (daytime)
        if (day >= data.nextHerdDay && time >= 2000 && time < 9000 && !overworld.players().isEmpty()) {
            herd(overworld, r, day);
            data.nextHerdDay = day + randBetween(r, Config.HERD_MIN_DAYS.get(), Config.HERD_MAX_DAYS.get());
            data.setDirty();
        }

        // ---- Horde raids (night)
        if (day >= data.nextRaidDay && time >= 14000 && time < 15000 && !isBloodMoonDay(day) && !overworld.players().isEmpty()) {
            raid(overworld, r, day);
            data.lastRaidDay = day;
            data.nextRaidDay = day + randBetween(r, Config.RAID_MIN_DAYS.get(), Config.RAID_MAX_DAYS.get());
            data.setDirty();
        }
    }

    private static void initSchedule(ApocalypseData data, long day, RandomSource r) {
        boolean changed = false;
        if (data.nextAirdropDay < 0) { data.nextAirdropDay = day + randBetween(r, 2, Config.AIRDROP_MAX_DAYS.get()); changed = true; }
        if (data.nextHerdDay < 0) { data.nextHerdDay = day + randBetween(r, Config.HERD_MIN_DAYS.get(), Config.HERD_MAX_DAYS.get()); changed = true; }
        if (data.nextRaidDay < 0) { data.nextRaidDay = day + randBetween(r, Config.RAID_MIN_DAYS.get(), Config.RAID_MAX_DAYS.get()); changed = true; }
        if (changed) data.setDirty();
    }

    private static void announceSeason(MinecraftServer server, ApocalypseData data, long day) {
        Seasons.Season s = Seasons.fromDay(day);
        if (data.lastSeasonAnnounced != s.ordinal()) {
            data.lastSeasonAnnounced = s.ordinal();
            data.setDirty();
            broadcast(server, Component.translatable("event.deadseasons.season_start", Component.translatable(s.key()))
                    .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
            broadcast(server, Component.translatable("event.deadseasons.season_tip." + s.name().toLowerCase()).withStyle(ChatFormatting.GRAY));
        }
    }

    private static int randBetween(RandomSource r, int min, int max) {
        if (max <= min) return min;
        return min + r.nextInt(max - min + 1);
    }

    public static void broadcast(MinecraftServer server, Component msg) {
        server.getPlayerList().broadcastSystemMessage(msg, false);
    }

    // ------------------------------------------------------------ events

    public static void bloodMoonWave(ServerLevel level, long day, RandomSource r) {
        for (ServerPlayer p : level.players()) {
            if (p.isCreative() || p.isSpectator()) continue;
            if (ZombieSpawner.countZombiesNear(level, p.blockPosition(), 64) > 45) continue;
            int count = 4 + (int) Math.min(10, day / 25) + r.nextInt(3);
            if (p.getVehicle() instanceof BoxTruckEntity) count = count / 2;
            for (int i = 0; i < count; i++) {
                EntityType<? extends DSZombie> type = ZombieSpawner.pickType(level, r, true, day + 30);
                Mob m = ZombieSpawner.spawnAround(level, type, p.blockPosition(), 22, 40, p, true);
                if (m != null) m.setPersistenceRequired();
            }
        }
    }

    public static void airdrop(ServerLevel level, RandomSource r) {
        List<ServerPlayer> players = new ArrayList<>(level.players());
        players.removeIf(p -> p.isSpectator());
        if (players.isEmpty()) return;
        ServerPlayer p = players.get(r.nextInt(players.size()));
        for (int attempt = 0; attempt < 10; attempt++) {
            double ang = r.nextDouble() * Math.PI * 2;
            int d = 50 + r.nextInt(60);
            BlockPos col = new BlockPos(p.getBlockX() + (int) (Math.cos(ang) * d), 0, p.getBlockZ() + (int) (Math.sin(ang) * d));
            level.getChunk(col); // make sure the chunk exists
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, col);
            if (!level.getFluidState(pos.below()).isEmpty() || !level.getFluidState(pos).isEmpty()) continue;
            level.setBlock(pos, ModBlocks.SUPPLY_CRATE.get().defaultBlockState(), 3);
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof RandomizableContainerBlockEntity c) c.setLootTable(AIRDROP_LOOT, r.nextLong());
            if (be instanceof SupplyCrateBlockEntity crate) crate.startSmoke();

            ApocalypseData data = ApocalypseData.get(level.getServer());
            data.lastAirdrop = pos;
            data.lastAirdropDay = Seasons.dayIndex(level);
            data.setDirty();

            for (ServerPlayer sp : level.players()) {
                level.playSound(null, sp.getX(), sp.getY() + 10, sp.getZ(), ModSounds.AIRDROP_PLANE.get(), SoundSource.AMBIENT, 1.0f, 1.0f);
            }
            broadcast(level.getServer(), Component.translatable("event.deadseasons.airdrop", pos.getX(), pos.getY(), pos.getZ())
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
            // Guards
            int guards = 3 + r.nextInt(3);
            for (int i = 0; i < guards; i++) {
                EntityType<? extends DSZombie> t = ZombieSpawner.pickType(level, r, true, Seasons.dayIndex(level));
                ZombieSpawner.spawnAround(level, t, pos, 6, 14, null, true);
            }
            NoiseSystem.makeNoise(level, pos, 64, null);
            return;
        }
    }

    public static void herd(ServerLevel level, RandomSource r, long day) {
        List<ServerPlayer> players = new ArrayList<>(level.players());
        players.removeIf(p -> p.isSpectator() || p.isCreative());
        if (players.isEmpty()) return;
        ServerPlayer p = players.get(r.nextInt(players.size()));
        double ang = r.nextDouble() * Math.PI * 2;
        int dist = 56;
        BlockPos start = p.blockPosition().offset((int) (Math.cos(ang) * dist), 0, (int) (Math.sin(ang) * dist));
        BlockPos end = p.blockPosition().offset((int) (-Math.cos(ang) * 120), 0, (int) (-Math.sin(ang) * 120));
        int size = 14 + r.nextInt(10) + (int) Math.min(16, day / 15);
        int spawned = 0;
        for (int i = 0; i < size; i++) {
            EntityType<? extends DSZombie> type = r.nextInt(5) == 0 ? ModEntities.CRAWLER.get()
                    : r.nextInt(8) == 0 ? ModEntities.SOLDIER.get() : ModEntities.WALKER.get();
            Mob m = ZombieSpawner.spawnAround(level, type, start, 0, 10, null, true);
            if (m instanceof DSZombie z) {
                z.setMigrateTarget(end);
                // Herds walk in the open during the day, so protect them from burning with a cap
                if (z.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty()) {
                    z.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER_HELMET));
                    z.setDropChance(net.minecraft.world.entity.EquipmentSlot.HEAD, 0.0f);
                }
                spawned++;
            }
        }
        if (spawned > 0) {
            String dir = direction(ang);
            broadcast(level.getServer(), Component.translatable("event.deadseasons.herd", Component.translatable("direction.deadseasons." + dir))
                    .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD));
        }
    }

    public static void raid(ServerLevel level, RandomSource r, long day) {
        for (ServerPlayer p : level.players()) {
            if (p.isCreative() || p.isSpectator()) continue;
            // Nomads (moved their truck within the last 3 days) and drivers are harder to find
            long nomadDay = p.getPersistentData().contains("DSNomadDay") ? p.getPersistentData().getLong("DSNomadDay") : -100;
            boolean nomad = day - nomadDay <= 3 || p.getVehicle() instanceof BoxTruckEntity;
            if (nomad && r.nextFloat() < 0.6f) {
                p.displayClientMessage(Component.translatable("event.deadseasons.raid_avoided").withStyle(ChatFormatting.DARK_GREEN), false);
                continue;
            }
            int count = 8 + r.nextInt(5) + (int) Math.min(12, day / 12);
            for (int i = 0; i < count; i++) {
                EntityType<? extends DSZombie> type = ZombieSpawner.pickType(level, r, true, day + 10);
                Mob m = ZombieSpawner.spawnAround(level, type, p.blockPosition(), 26, 38, p, true);
                if (m != null) m.setPersistenceRequired();
            }
            p.displayClientMessage(Component.translatable("event.deadseasons.raid").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), false);
        }
        // Hidden in a truck? The horde finds the truck instead
        ServerLevel interior = level.getServer().getLevel(TruckInteriors.INTERIOR);
        if (interior != null && !interior.players().isEmpty()) {
            for (BoxTruckEntity truck : findOccupiedTrucks(level)) {
                if (r.nextFloat() < 0.4f) {
                    for (int i = 0; i < 6 + r.nextInt(5); i++) {
                        ZombieSpawner.spawnAround(level, ModEntities.WALKER.get(), truck.blockPosition(), 16, 28, null, true);
                    }
                }
            }
        }
    }

    private static List<BoxTruckEntity> findOccupiedTrucks(ServerLevel level) {
        List<BoxTruckEntity> list = new ArrayList<>();
        for (net.minecraft.world.entity.Entity e : level.getAllEntities()) {
            if (e instanceof BoxTruckEntity t && t.isOccupied()) list.add(t);
        }
        return list;
    }

    private static String direction(double ang) {
        // ang is the direction from the player to the herd's start
        double deg = Math.toDegrees(ang);
        deg = ((deg % 360) + 360) % 360;
        if (deg < 45 || deg >= 315) return "east";
        if (deg < 135) return "south";
        if (deg < 225) return "west";
        return "north";
    }

    private WorldEvents() {}
}
