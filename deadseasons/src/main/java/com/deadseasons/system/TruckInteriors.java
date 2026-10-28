package com.deadseasons.system;

import com.deadseasons.DeadSeasons;
import com.deadseasons.entity.BoxTruckEntity;
import com.deadseasons.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Each box truck owns a cargo room in the "deadseasons:truck_interior" dimension.
 * Room for interior id N starts at x = N * 512. Inside: 7 wide, 4 tall, 12 long.
 */
public final class TruckInteriors {
    public static final ResourceKey<Level> INTERIOR = ResourceKey.create(Registries.DIMENSION, DeadSeasons.id("truck_interior"));
    public static final int SPACING = 512;
    public static final int FLOOR_Y = 64;
    public static final int WIDTH = 7, HEIGHT = 4, LENGTH = 12;

    public static boolean isInterior(Level level) {
        return level.dimension().equals(INTERIOR);
    }

    public static int idAt(double x) {
        return Math.floorDiv(Mth.floor(x) + SPACING / 2, SPACING);
    }

    public static void enter(ServerPlayer player, BoxTruckEntity truck) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        ServerLevel interior = server.getLevel(INTERIOR);
        if (interior == null) {
            player.displayClientMessage(Component.literal("Truck interior dimension is missing (datapack failed to load).").withStyle(ChatFormatting.RED), false);
            return;
        }
        ApocalypseData data = ApocalypseData.get(server);
        if (truck.getInteriorId() < 0) {
            truck.setInteriorId(data.nextInteriorId++);
            data.setDirty();
        }
        int id = truck.getInteriorId();
        updateTruckPosition((ServerLevel) truck.level(), id, truck);
        ApocalypseData.TruckRecord rec = data.trucks.get(id);
        if (!rec.built) {
            build(interior, id);
            rec.built = true;
            data.setDirty();
        }
        int ox = id * SPACING;
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0f, 0.8f);
        player.teleportTo(interior, ox + 3.5, FLOOR_Y + 1, 10.5, 180f, 0f);
        player.displayClientMessage(Component.translatable("message.deadseasons.truck_inside").withStyle(ChatFormatting.GRAY), true);
    }

    public static void exit(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        ApocalypseData data = ApocalypseData.get(server);
        int id = idAt(player.getX());
        ApocalypseData.TruckRecord rec = data.trucks.get(id);

        ServerLevel target = server.overworld();
        Vec3 dest;
        if (rec != null) {
            ServerLevel l = server.getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(rec.dimension)));
            if (l != null) target = l;
            Vec3 back = new Vec3(0, 0, -3.8).yRot(-rec.yaw * Mth.DEG_TO_RAD);
            BlockPos col = BlockPos.containing(rec.x + back.x, rec.y, rec.z + back.z);
            BlockPos ground = target.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, col);
            double y = Math.abs(ground.getY() - rec.y) < 6 ? ground.getY() : rec.y + 1;
            dest = new Vec3(col.getX() + 0.5, y, col.getZ() + 0.5);
        } else {
            BlockPos spawn = target.getSharedSpawnPos();
            dest = new Vec3(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
        }
        player.teleportTo(target, dest.x, dest.y, dest.z, rec != null ? rec.yaw + 180f : player.getYRot(), 0f);
        target.playSound(null, dest.x, dest.y, dest.z, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0f, 0.8f);
    }

    public static void updateTruckPosition(ServerLevel level, int id, BoxTruckEntity truck) {
        ApocalypseData data = ApocalypseData.get(level.getServer());
        ApocalypseData.TruckRecord rec = data.trucks.computeIfAbsent(id, k -> new ApocalypseData.TruckRecord());
        rec.dimension = level.dimension().location().toString();
        rec.x = truck.getX();
        rec.y = truck.getY();
        rec.z = truck.getZ();
        rec.yaw = truck.getYRot();
        data.setDirty();
    }

    public static int playersInside(MinecraftServer server, int id) {
        ServerLevel interior = server.getLevel(INTERIOR);
        if (interior == null) return 0;
        int count = 0;
        for (ServerPlayer p : interior.players()) {
            if (idAt(p.getX()) == id) count++;
        }
        return count;
    }

    private static void build(ServerLevel level, int id) {
        int ox = id * SPACING;
        BlockState wall = ModBlocks.TRUCK_WALL.get().defaultBlockState();
        BlockState floor = ModBlocks.TRUCK_FLOOR.get().defaultBlockState();
        BlockState light = ModBlocks.TRUCK_LIGHT.get().defaultBlockState();
        BlockState exit = ModBlocks.TRUCK_EXIT.get().defaultBlockState();
        int top = FLOOR_Y + HEIGHT + 1;
        for (int x = -1; x <= WIDTH; x++) {
            for (int z = -1; z <= LENGTH; z++) {
                for (int y = FLOOR_Y; y <= top; y++) {
                    boolean edge = x == -1 || x == WIDTH || z == -1 || z == LENGTH;
                    BlockPos p = new BlockPos(ox + x, y, z);
                    BlockState s;
                    if (y == FLOOR_Y) s = floor;
                    else if (y == top) s = (x == 3 && (z == 2 || z == 6 || z == 10)) ? light : wall;
                    else if (edge) s = wall;
                    else s = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
                    level.setBlock(p, s, 2);
                }
            }
        }
        level.setBlock(new BlockPos(ox + 3, FLOOR_Y + 1, LENGTH), exit, 2);
        level.setBlock(new BlockPos(ox + 3, FLOOR_Y + 2, LENGTH), exit, 2);
    }

    private TruckInteriors() {}
}
