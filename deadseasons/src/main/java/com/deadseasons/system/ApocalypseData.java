package com.deadseasons.system;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/** World-wide apocalypse state, saved with the overworld. */
public class ApocalypseData extends SavedData {
    private static final String NAME = "deadseasons_world";

    public long nextAirdropDay = -1;
    public long nextHerdDay = -1;
    public long nextRaidDay = -1;
    public long fogRolledDay = -1;
    public long fogDay = -1;
    public long bloodMoonWarnedDay = -1;
    public long bloodMoonEndedDay = -1;
    public long bossSpawnedDay = -1;
    public int lastSeasonAnnounced = -1;
    public long lastRaidDay = -1;
    @Nullable public BlockPos lastAirdrop = null;
    public long lastAirdropDay = -1;

    // Truck interiors: id -> last known truck position and dimension
    public int nextInteriorId = 0;
    public final Map<Integer, TruckRecord> trucks = new HashMap<>();

    public static class TruckRecord {
        public String dimension = "minecraft:overworld";
        public double x, y, z;
        public float yaw;
        public boolean built = false;
    }

    public static ApocalypseData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(ApocalypseData::load, ApocalypseData::new, NAME);
    }

    public static ApocalypseData load(CompoundTag tag) {
        ApocalypseData d = new ApocalypseData();
        d.nextAirdropDay = tag.getLong("NextAirdrop");
        d.nextHerdDay = tag.getLong("NextHerd");
        d.nextRaidDay = tag.getLong("NextRaid");
        d.fogRolledDay = tag.getLong("FogRolled");
        d.fogDay = tag.getLong("FogDay");
        d.bloodMoonWarnedDay = tag.getLong("BMWarned");
        d.bloodMoonEndedDay = tag.getLong("BMEnded");
        d.bossSpawnedDay = tag.getLong("BossDay");
        d.lastSeasonAnnounced = tag.getInt("LastSeason");
        d.lastRaidDay = tag.getLong("LastRaid");
        d.lastAirdropDay = tag.getLong("LastAirdropDay");
        if (tag.contains("LastAirdrop")) d.lastAirdrop = NbtUtils.readBlockPos(tag.getCompound("LastAirdrop"));
        d.nextInteriorId = tag.getInt("NextInterior");
        ListTag list = tag.getList("Trucks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            TruckRecord r = new TruckRecord();
            r.dimension = t.getString("Dim");
            r.x = t.getDouble("X");
            r.y = t.getDouble("Y");
            r.z = t.getDouble("Z");
            r.yaw = t.getFloat("Yaw");
            r.built = t.getBoolean("Built");
            d.trucks.put(t.getInt("Id"), r);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("NextAirdrop", nextAirdropDay);
        tag.putLong("NextHerd", nextHerdDay);
        tag.putLong("NextRaid", nextRaidDay);
        tag.putLong("FogRolled", fogRolledDay);
        tag.putLong("FogDay", fogDay);
        tag.putLong("BMWarned", bloodMoonWarnedDay);
        tag.putLong("BMEnded", bloodMoonEndedDay);
        tag.putLong("BossDay", bossSpawnedDay);
        tag.putInt("LastSeason", lastSeasonAnnounced);
        tag.putLong("LastRaid", lastRaidDay);
        tag.putLong("LastAirdropDay", lastAirdropDay);
        if (lastAirdrop != null) tag.put("LastAirdrop", NbtUtils.writeBlockPos(lastAirdrop));
        tag.putInt("NextInterior", nextInteriorId);
        ListTag list = new ListTag();
        for (Map.Entry<Integer, TruckRecord> e : trucks.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putInt("Id", e.getKey());
            t.putString("Dim", e.getValue().dimension);
            t.putDouble("X", e.getValue().x);
            t.putDouble("Y", e.getValue().y);
            t.putDouble("Z", e.getValue().z);
            t.putFloat("Yaw", e.getValue().yaw);
            t.putBoolean("Built", e.getValue().built);
            list.add(t);
        }
        tag.put("Trucks", list);
        return tag;
    }
}
