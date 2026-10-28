package com.deadseasons.world;

import com.deadseasons.DeadSeasons;
import com.deadseasons.entity.BoxTruckEntity;
import com.deadseasons.entity.SurvivorEntity;
import com.deadseasons.entity.SurvivorTraderEntity;
import com.deadseasons.registry.ModBlocks;
import com.deadseasons.registry.ModEntities;
import com.deadseasons.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.ChunkPos;

/**
 * One 16x16 lot of a town. Blocks are placed procedurally in absolute coordinates. All random choices use
 * a hash of the position so the lot looks the same no matter which chunk is generated first.
 */
public class TownPiece extends StructurePiece {
    public enum Kind { HOUSE, STORE, POLICE, CLINIC, GAS_STATION, MILITARY, EMPTY, PLAZA, FARM }

    public static final int EDGE_NORTH = 1, EDGE_SOUTH = 2, EDGE_WEST = 4, EDGE_EAST = 8;

    private static ResourceLocation loot(String name) {
        return DeadSeasons.id("chests/" + name);
    }

    private final Kind kind;
    private final int ox, baseY, oz;
    private final boolean settlement;
    private final int edges;
    private final boolean gate;
    private final boolean truck;
    private final long seed;

    public TownPiece(Kind kind, int ox, int baseY, int oz, boolean settlement, int edges, boolean gate, boolean truck, long seed) {
        super(ModStructures.TOWN_LOT.get(), 0, new BoundingBox(ox, baseY - 16, oz, ox + 15, baseY + 20, oz + 15));
        this.kind = kind;
        this.ox = ox;
        this.baseY = baseY;
        this.oz = oz;
        this.settlement = settlement;
        this.edges = edges;
        this.gate = gate;
        this.truck = truck;
        this.seed = seed;
    }

    public TownPiece(CompoundTag tag) {
        super(ModStructures.TOWN_LOT.get(), tag);
        Kind k;
        try {
            k = Kind.valueOf(tag.getString("Kind"));
        } catch (IllegalArgumentException e) {
            k = Kind.EMPTY;
        }
        this.kind = k;
        this.ox = tag.getInt("OX");
        this.baseY = tag.getInt("BY");
        this.oz = tag.getInt("OZ");
        this.settlement = tag.getBoolean("Settlement");
        this.edges = tag.getInt("Edges");
        this.gate = tag.getBoolean("Gate");
        this.truck = tag.getBoolean("Truck");
        this.seed = tag.getLong("Seed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putString("Kind", kind.name());
        tag.putInt("OX", ox);
        tag.putInt("BY", baseY);
        tag.putInt("OZ", oz);
        tag.putBoolean("Settlement", settlement);
        tag.putInt("Edges", edges);
        tag.putBoolean("Gate", gate);
        tag.putBoolean("Truck", truck);
        tag.putLong("Seed", seed);
    }

    // ================================================================== helpers

    private WorldGenLevel level;
    private BoundingBox box;

    private double rand(int x, int y, int z, int salt) {
        long h = seed ^ (x * 3129871L) ^ (z * 116129781L) ^ ((long) y * 42317861L) ^ (salt * 2654435761L);
        h = h * h * 42317861L + h * 11L;
        h ^= (h >>> 29);
        return ((h >>> 11) & 0xFFFFFFL) / (double) 0x1000000;
    }

    private boolean chance(int x, int y, int z, int salt, double p) {
        return rand(x, y, z, salt) < p;
    }

    private void set(int x, int y, int z, BlockState s) {
        BlockPos p = new BlockPos(x, y, z);
        if (box.isInside(p)) level.setBlock(p, s, 2);
    }

    /** Places a block unless the building is ruined and this spot has decayed away. */
    private void wall(int x, int y, int z, BlockState s, double decay) {
        if (!settlement && chance(x, y, z, 1, decay)) {
            set(x, y, z, Blocks.AIR.defaultBlockState());
        } else {
            set(x, y, z, s);
        }
    }

    private BlockState ruin(BlockState s, int x, int y, int z) {
        if (settlement) return s;
        if (s.is(Blocks.STONE_BRICKS)) {
            double r = rand(x, y, z, 2);
            if (r < 0.2) return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
            if (r < 0.35) return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        }
        if (s.is(Blocks.BRICKS) && chance(x, y, z, 3, 0.15)) return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        return s;
    }

    private boolean inChunk(int x, int z) {
        return x >= box.minX() && x <= box.maxX() && z >= box.minZ() && z <= box.maxZ();
    }

    /** Y of the top natural block in this column (before features like trees). */
    private int ground(int x, int z) {
        return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
    }

    /**
     * Builds a foundation under a building footprint only, like vanilla villages: fills dips below the floor
     * and cuts away terrain inside the footprint. Everything outside the footprint keeps its natural shape.
     */
    private void foundation(int x1, int z1, int x2, int z2, int clear, BlockState fill) {
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                if (!inChunk(x, z)) continue;
                int g = ground(x, z);
                for (int y = baseY - 2; y > g && y >= baseY - 14; y--) set(x, y, z, fill);
                set(x, baseY - 1, z, fill);
                int top = Math.max(baseY + clear, Math.min(g + 1, baseY + 18));
                for (int y = baseY; y <= top; y++) set(x, y, z, Blocks.AIR.defaultBlockState());
            }
        }
    }

    /** Spawns a mob standing on the natural ground (for spots outside buildings). */
    private void spawnOnGround(EntityType<? extends Mob> type, int x, int z) {
        if (!inChunk(x, z)) return;
        spawn(type, x, ground(x, z) + 1, z, true);
    }

    private void fillBox(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
        for (int x = x1; x <= x2; x++)
            for (int y = y1; y <= y2; y++)
                for (int z = z1; z <= z2; z++)
                    set(x, y, z, s);
    }

    /** Hollow building shell with floor, walls (with windows) and flat roof. Door on the south wall. */
    private void shell(int x1, int z1, int x2, int z2, int height, BlockState wallState, BlockState floorState,
                       BlockState roofState, BlockState window, int doorX) {
        int y = baseY;
        double decay = 0.12;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                set(x, y, z, floorState);
                boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                for (int dy = 1; dy <= height; dy++) {
                    if (edge) {
                        boolean windowSpot = (dy == 2 || dy == 3) && ((x - x1) % 3 == 2 || (z - z1) % 3 == 2)
                                && !(x == x1 && z == z1) && !(x == x2 && z == z2) && !(x == x1 && z == z2) && !(x == x2 && z == z1);
                        if (windowSpot) {
                            if (!settlement && chance(x, y + dy, z, 4, 0.5)) set(x, y + dy, z, Blocks.AIR.defaultBlockState());
                            else set(x, y + dy, z, window);
                        } else {
                            wall(x, y + dy, z, ruin(wallState, x, y + dy, z), decay);
                        }
                    } else {
                        set(x, y + dy, z, Blocks.AIR.defaultBlockState());
                    }
                }
                wall(x, y + height + 1, z, roofState, settlement ? 0 : 0.18);
            }
        }
        // Door opening
        set(doorX, y + 1, z2, Blocks.AIR.defaultBlockState());
        set(doorX, y + 2, z2, Blocks.AIR.defaultBlockState());
        if (settlement || chance(doorX, y, z2, 5, 0.4)) {
            door(doorX, y + 1, z2, Blocks.SPRUCE_DOOR, Direction.SOUTH);
        }
    }

    private void door(int x, int y, int z, Block door, Direction facing) {
        BlockState lower = door.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        set(x, y, z, lower);
        set(x, y + 1, z, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    private void bed(int x, int y, int z, Block bed) {
        // Foot at z, head at z-1 (facing north)
        BlockState foot = bed.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.FOOT);
        set(x, y, z, foot);
        set(x, y, z - 1, foot.setValue(BedBlock.PART, BedPart.HEAD));
    }

    private void chest(int x, int y, int z, String lootName, Direction facing, RandomSource random) {
        BlockPos p = new BlockPos(x, y, z);
        if (!box.isInside(p)) return;
        level.setBlock(p, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 2);
        RandomizableContainerBlockEntity.setLootTable(level, random, p, loot(lootName));
    }

    private void barrel(int x, int y, int z, String lootName, RandomSource random) {
        BlockPos p = new BlockPos(x, y, z);
        if (!box.isInside(p)) return;
        level.setBlock(p, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), 2);
        RandomizableContainerBlockEntity.setLootTable(level, random, p, loot(lootName));
    }

    private void decorateRuin(int x1, int z1, int x2, int z2, int height) {
        if (settlement) return;
        for (int x = x1 + 1; x < x2; x++) {
            for (int z = z1 + 1; z < z2; z++) {
                BlockPos floor = new BlockPos(x, baseY + 1, z);
                if (!box.isInside(floor) || !level.isEmptyBlock(floor)) continue;
                double r = rand(x, baseY, z, 9);
                if (r < 0.05) set(x, baseY + 1, z, ModBlocks.BLOOD_SPLATTER.get().defaultBlockState());
                else if (r < 0.08) set(x, baseY + height, z, Blocks.COBWEB.defaultBlockState());
            }
        }
    }

    private void spawn(EntityType<? extends Mob> type, int x, int y, int z, boolean persistent) {
        BlockPos p = new BlockPos(x, y, z);
        if (!box.isInside(p)) return;
        Mob mob = type.create(level.getLevel());
        if (mob == null) return;
        mob.moveTo(x + 0.5, y, z + 0.5, (float) (rand(x, y, z, 11) * 360), 0f);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(p), MobSpawnType.STRUCTURE, null, null);
        if (persistent) mob.setPersistenceRequired();
        if (mob instanceof SurvivorEntity s) s.makeGuard(p);
        level.addFreshEntityWithPassengers(mob);
    }

    private EntityType<? extends Mob> ruinZombie(int x, int z, Kind k) {
        double r = rand(x, baseY, z, 12);
        return switch (k) {
            case POLICE -> r < 0.5 ? ModEntities.RIOT.get() : ModEntities.SOLDIER.get();
            case MILITARY -> ModEntities.SOLDIER.get();
            case GAS_STATION -> r < 0.4 ? ModEntities.BLOATER.get() : ModEntities.WALKER.get();
            case CLINIC -> r < 0.3 ? ModEntities.SCREAMER.get() : ModEntities.WALKER.get();
            default -> r < 0.15 ? ModEntities.CRAWLER.get() : r < 0.3 ? ModEntities.RUNNER.get() : ModEntities.WALKER.get();
        };
    }

    // ================================================================== generation

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        this.level = level;
        this.box = chunkBox;

        roads();

        switch (kind) {
            case HOUSE -> house(random);
            case STORE -> store(random);
            case POLICE -> police(random);
            case CLINIC -> clinic(random);
            case GAS_STATION -> gasStation(random);
            case MILITARY -> military(random);
            case PLAZA -> plaza(random);
            case FARM -> farm();
            case EMPTY -> emptyLot();
        }
        if (settlement) settlementWalls();
        if (truck) placeTruck();
    }

    private void roads() {
        for (int x = ox; x <= ox + 15; x++) {
            for (int z = oz; z <= oz + 15; z++) {
                int lx = x - ox, lz = z - oz;
                boolean road = lx <= 1 || lx >= 14 || lz <= 1 || lz >= 14;
                if (!road) continue;
                if (!inChunk(x, z)) continue;
                int g = ground(x, z);
                BlockPos gp = new BlockPos(x, g, z);
                if (!level.getFluidState(gp).isEmpty()) {
                    set(x, g, z, Blocks.OAK_PLANKS.defaultBlockState()); // little bridge over water
                    continue;
                }
                BlockState s;
                if (settlement) {
                    s = chance(x, 0, z, 14, 0.3) ? Blocks.GRAVEL.defaultBlockState() : Blocks.DIRT_PATH.defaultBlockState();
                } else {
                    double r = rand(x, 0, z, 15);
                    s = r < 0.6 ? Blocks.GRAY_CONCRETE.defaultBlockState()
                            : r < 0.75 ? Blocks.COBBLESTONE.defaultBlockState()
                            : r < 0.88 ? Blocks.GRAVEL.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState();
                }
                set(x, g, z, s);
                for (int dy = 1; dy <= 2; dy++) {
                    BlockPos above = new BlockPos(x, g + dy, z);
                    if (level.getBlockState(above).canBeReplaced()) set(x, g + dy, z, Blocks.AIR.defaultBlockState());
                }
            }
        }
        // Wrecked car on the road
        if (!settlement && chance(ox, 0, oz, 16, 0.25)) {
            int cx = ox + 4 + (int) (rand(ox, 1, oz, 17) * 6);
            int cz = oz + 14;
            if (!inChunk(cx, cz)) return;
            int cy = ground(cx, cz) + 1;
            BlockState body = chance(ox, 2, oz, 18, 0.5) ? Blocks.RED_TERRACOTTA.defaultBlockState() : Blocks.BLUE_TERRACOTTA.defaultBlockState();
            fillBox(cx, cy, cz, cx + 3, cy, cz + 1, body);
            fillBox(cx + 1, cy + 1, cz, cx + 2, cy + 1, cz + 1, Blocks.BLACK_STAINED_GLASS.defaultBlockState());
            set(cx, cy, cz, Blocks.COAL_BLOCK.defaultBlockState());
        }
    }

    private void house(RandomSource random) {
        int x1 = ox + 3, z1 = oz + 3, x2 = ox + 11, z2 = oz + 11;
        double style = rand(ox, 0, oz, 20);
        BlockState walls = style < 0.33 ? Blocks.OAK_PLANKS.defaultBlockState()
                : style < 0.66 ? Blocks.BRICKS.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState();
        foundation(x1, z1, x2, z2, 6, Blocks.COBBLESTONE.defaultBlockState());
        shell(x1, z1, x2, z2, 4, walls, Blocks.OAK_PLANKS.defaultBlockState(),
                Blocks.DARK_OAK_SLAB.defaultBlockState(), Blocks.GLASS_PANE.defaultBlockState(), ox + 7);
        int y = baseY + 1;
        bed(x1 + 2, y, z1 + 3, settlement ? Blocks.BLUE_BED : Blocks.WHITE_BED);
        set(x1 + 1, y, z2 - 1, Blocks.CRAFTING_TABLE.defaultBlockState());
        set(x2 - 1, y, z1 + 1, Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.SOUTH));
        chest(x2 - 2, y, z1 + 1, "house", Direction.SOUTH, random);
        barrel(x2 - 1, y, z2 - 1, settlement ? "settlement" : "house", random);
        set(x1 + 4, y, z1 + 4, Blocks.RED_CARPET.defaultBlockState());
        set(x1 + 5, y, z1 + 4, Blocks.RED_CARPET.defaultBlockState());
        decorateRuin(x1, z1, x2, z2, 4);
        if (settlement) {
            spawnOnGround(ModEntities.SURVIVOR.get(), ox + 7, z2 + 2);
        } else {
            int count = 1 + (int) (rand(ox, 3, oz, 21) * 3);
            for (int i = 0; i < count; i++) spawn(ruinZombie(x1 + 2 + i * 2, z1 + 5, kind), x1 + 2 + i * 2, y, z1 + 5, true);
        }
    }

    private void store(RandomSource random) {
        int x1 = ox + 2, z1 = oz + 2, x2 = ox + 13, z2 = oz + 12;
        foundation(x1, z1, x2, z2, 6, Blocks.STONE.defaultBlockState());
        shell(x1, z1, x2, z2, 4, Blocks.WHITE_CONCRETE.defaultBlockState(), Blocks.SMOOTH_STONE.defaultBlockState(),
                Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.GLASS_PANE.defaultBlockState(), ox + 7);
        int y = baseY + 1;
        // Shelves
        for (int x = x1 + 2; x <= x2 - 2; x += 3) {
            for (int z = z1 + 2; z <= z1 + 6; z++) {
                if ((z - z1) % 2 == 0) barrel(x, y, z, "store", random);
                else set(x, y, z, Blocks.SPRUCE_SLAB.defaultBlockState());
            }
        }
        // Counter
        fillBox(x2 - 3, y, z2 - 2, x2 - 1, y, z2 - 2, Blocks.SPRUCE_PLANKS.defaultBlockState());
        chest(x2 - 1, y, z2 - 1, "store", Direction.WEST, random);
        decorateRuin(x1, z1, x2, z2, 4);
        if (settlement) {
            spawn(ModEntities.TRADER.get(), x2 - 2, y, z2 - 1, true);
        } else {
            for (int i = 0; i < 3; i++) spawn(ruinZombie(x1 + 3 + i * 3, z2 - 2, kind), x1 + 3 + i * 3, y, z2 - 2, true);
        }
    }

    private void police(RandomSource random) {
        int x1 = ox + 2, z1 = oz + 2, x2 = ox + 13, z2 = oz + 12;
        foundation(x1, z1, x2, z2, 7, Blocks.STONE.defaultBlockState());
        shell(x1, z1, x2, z2, 5, Blocks.STONE_BRICKS.defaultBlockState(), Blocks.POLISHED_ANDESITE.defaultBlockState(),
                Blocks.STONE_BRICK_SLAB.defaultBlockState(), Blocks.IRON_BARS.defaultBlockState(), ox + 7);
        int y = baseY + 1;
        // Cells
        for (int z = z1 + 1; z <= z1 + 3; z++) set(x1 + 4, y, z, Blocks.IRON_BARS.defaultBlockState());
        for (int z = z1 + 1; z <= z1 + 3; z++) set(x1 + 4, y + 1, z, Blocks.IRON_BARS.defaultBlockState());
        chest(x2 - 1, y, z1 + 1, "police", Direction.WEST, random);
        chest(x2 - 1, y, z1 + 2, "police", Direction.WEST, random);
        set(x2 - 3, y, z2 - 2, Blocks.LECTERN.defaultBlockState());
        decorateRuin(x1, z1, x2, z2, 5);
        for (int i = 0; i < 3; i++) spawn(ruinZombie(x1 + 5 + i * 2, z1 + 5, Kind.POLICE), x1 + 5 + i * 2, y, z1 + 5, true);
    }

    private void clinic(RandomSource random) {
        int x1 = ox + 2, z1 = oz + 2, x2 = ox + 13, z2 = oz + 12;
        foundation(x1, z1, x2, z2, 6, Blocks.STONE.defaultBlockState());
        shell(x1, z1, x2, z2, 4, Blocks.WHITE_CONCRETE.defaultBlockState(), Blocks.WHITE_TERRACOTTA.defaultBlockState(),
                Blocks.QUARTZ_SLAB.defaultBlockState(), Blocks.WHITE_STAINED_GLASS_PANE.defaultBlockState(), ox + 7);
        int y = baseY + 1;
        // Red cross on the front
        int cx = ox + 10;
        set(cx, y + 2, z2, Blocks.RED_CONCRETE.defaultBlockState());
        set(cx - 1, y + 2, z2, Blocks.RED_CONCRETE.defaultBlockState());
        set(cx + 1, y + 2, z2, Blocks.RED_CONCRETE.defaultBlockState());
        set(cx, y + 1, z2, Blocks.RED_CONCRETE.defaultBlockState());
        set(cx, y + 3, z2, Blocks.RED_CONCRETE.defaultBlockState());
        for (int i = 0; i < 3; i++) bed(x1 + 2 + i * 3, y, z1 + 3, Blocks.WHITE_BED);
        chest(x2 - 1, y, z1 + 1, "clinic", Direction.WEST, random);
        barrel(x2 - 1, y, z1 + 3, "clinic", random);
        decorateRuin(x1, z1, x2, z2, 4);
        if (settlement) {
            spawnOnGround(ModEntities.SURVIVOR.get(), ox + 7, z2 + 2);
        } else {
            for (int i = 0; i < 3; i++) spawn(ruinZombie(x1 + 3 + i * 3, z1 + 6, Kind.CLINIC), x1 + 3 + i * 3, y, z1 + 6, true);
        }
    }

    private void gasStation(RandomSource random) {
        int y = baseY;
        // Forecourt
        foundation(ox + 2, oz + 2, ox + 13, oz + 13, 6, Blocks.GRAY_CONCRETE.defaultBlockState());
        // Canopy
        for (int[] post : new int[][]{{ox + 3, oz + 8}, {ox + 12, oz + 8}, {ox + 3, oz + 12}, {ox + 12, oz + 12}}) {
            fillBox(post[0], y, post[1], post[0], y + 3, post[1], Blocks.IRON_BARS.defaultBlockState());
        }
        for (int x = ox + 2; x <= ox + 13; x++)
            for (int z = oz + 7; z <= oz + 13; z++)
                wall(x, y + 4, z, Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState(), 0.15);
        // Pumps
        for (int px : new int[]{ox + 6, ox + 9}) {
            set(px, y, oz + 10, Blocks.IRON_BLOCK.defaultBlockState());
            set(px, y + 1, oz + 10, Blocks.RED_CONCRETE.defaultBlockState());
        }
        // Little shop
        shell(ox + 3, oz + 2, ox + 10, oz + 6, 3, Blocks.WHITE_CONCRETE.defaultBlockState(), Blocks.SMOOTH_STONE.defaultBlockState(),
                Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.GLASS_PANE.defaultBlockState(), ox + 6);
        chest(ox + 4, y + 1, oz + 3, "gas_station", Direction.SOUTH, random);
        barrel(ox + 9, y + 1, oz + 3, "store", random);
        for (int i = 0; i < 3; i++) spawn(ruinZombie(ox + 5 + i * 2, oz + 11, Kind.GAS_STATION), ox + 5 + i * 2, y, oz + 11, true);
    }

    private void military(RandomSource random) {
        int y = baseY;
        foundation(ox + 2, oz + 2, ox + 13, oz + 13, 5, Blocks.COARSE_DIRT.defaultBlockState());
        // Sandbag ring
        for (int x = ox + 2; x <= ox + 13; x++) {
            for (int z = oz + 2; z <= oz + 13; z++) {
                boolean edge = x == ox + 2 || x == ox + 13 || z == oz + 2 || z == oz + 13;
                if (edge && !(z == oz + 13 && (x == ox + 7 || x == ox + 8))) {
                    wall(x, y, z, Blocks.MUD_BRICKS.defaultBlockState(), 0.1);
                    wall(x, y + 1, z, Blocks.MUD_BRICK_SLAB.defaultBlockState(), 0.2);
                }
            }
        }
        // Tent
        for (int x = ox + 4; x <= ox + 11; x++) {
            set(x, y + 3, oz + 7, Blocks.GREEN_WOOL.defaultBlockState());
            set(x, y + 2, oz + 5, Blocks.GREEN_WOOL.defaultBlockState());
            set(x, y + 2, oz + 9, Blocks.GREEN_WOOL.defaultBlockState());
            set(x, y + 1, oz + 4, Blocks.GREEN_WOOL.defaultBlockState());
            set(x, y + 1, oz + 10, Blocks.GREEN_WOOL.defaultBlockState());
        }
        chest(ox + 5, y, oz + 7, "military", Direction.SOUTH, random);
        chest(ox + 10, y, oz + 7, "military", Direction.SOUTH, random);
        set(ox + 7, y, oz + 7, ModBlocks.SUPPLY_CRATE.get().defaultBlockState());
        BlockPos crate = new BlockPos(ox + 7, y, oz + 7);
        if (box.isInside(crate)) RandomizableContainerBlockEntity.setLootTable(level, random, crate, loot("military"));
        set(ox + 3, y, oz + 3, ModBlocks.BARBED_WIRE.get().defaultBlockState());
        set(ox + 12, y, oz + 3, ModBlocks.BARBED_WIRE.get().defaultBlockState());
        for (int i = 0; i < 4; i++) spawn(ModEntities.SOLDIER.get(), ox + 5 + i * 2, y, oz + 11, true);
    }

    private void emptyLot() {
        // Overgrown lot with an abandoned campsite, sitting on the natural ground
        int cx = ox + 7, cz = oz + 7;
        if (inChunk(cx, cz) && level.getFluidState(new BlockPos(cx, ground(cx, cz), cz)).isEmpty()) {
            int g = ground(cx, cz) + 1;
            set(cx, g, cz, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
            if (inChunk(cx - 2, cz)) set(cx - 2, ground(cx - 2, cz) + 1, cz, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
            if (inChunk(cx + 2, cz)) set(cx + 2, ground(cx + 2, cz) + 1, cz, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
        }
        spawnOnGround(ModEntities.CRAWLER.get(), ox + 7, oz + 10);
    }

    private void plaza(RandomSource random) {
        int y = baseY;
        foundation(ox + 2, oz + 2, ox + 13, oz + 13, 5, Blocks.STONE_BRICKS.defaultBlockState());
        set(ox + 7, y, oz + 7, Blocks.CAMPFIRE.defaultBlockState());
        set(ox + 5, y, oz + 7, Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
        set(ox + 9, y, oz + 7, Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
        set(ox + 7, y, oz + 5, Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH));
        set(ox + 7, y, oz + 9, Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
        set(ox + 3, y, oz + 3, ModBlocks.RADIO.get().defaultBlockState());
        set(ox + 12, y, oz + 3, ModBlocks.RAIN_COLLECTOR.get().defaultBlockState());
        set(ox + 3, y, oz + 12, ModBlocks.RAIN_COLLECTOR.get().defaultBlockState());
        // Market stalls
        for (int x : new int[]{ox + 3, ox + 11}) {
            fillBox(x, y + 3, oz + 9, x + 2, y + 3, oz + 11, Blocks.RED_WOOL.defaultBlockState());
            for (int[] p : new int[][]{{x, oz + 9}, {x + 2, oz + 9}, {x, oz + 11}, {x + 2, oz + 11}})
                fillBox(p[0], y, p[1], p[0], y + 2, p[1], Blocks.OAK_FENCE.defaultBlockState());
        }
        barrel(ox + 12, y, oz + 12, "settlement", random);
        spawn(ModEntities.TRADER.get(), ox + 4, y, oz + 10, true);
        spawn(ModEntities.TRADER.get(), ox + 12, y, oz + 10, true);
        spawn(ModEntities.SURVIVOR.get(), ox + 7, y, oz + 4, true);
    }

    private void farm() {
        int y = baseY;
        int x1 = ox + 2, z1 = oz + 2, x2 = ox + 13, z2 = oz + 13;
        foundation(x1, z1, x2, z2, 6, Blocks.DIRT.defaultBlockState());
        // Greenhouse: glass roof keeps crops growing through Winter
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                if (edge) {
                    set(x, y - 1, z, Blocks.OAK_PLANKS.defaultBlockState());
                    for (int dy = 0; dy <= 3; dy++) set(x, y + dy, z, dy == 0 ? Blocks.OAK_LOG.defaultBlockState() : Blocks.GLASS.defaultBlockState());
                } else if (x == x1 + 5 || x == x1 + 6) {
                    set(x, y - 1, z, Blocks.WATER.defaultBlockState());
                } else {
                    set(x, y - 1, z, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
                    double r = rand(x, y, z, 23);
                    CropBlock crop = (CropBlock) (r < 0.4 ? Blocks.WHEAT : r < 0.7 ? Blocks.CARROTS : Blocks.POTATOES);
                    set(x, y, z, crop.getStateForAge((int) (rand(x, y, z, 24) * 8)));
                }
                set(x, y + 4, z, Blocks.GLASS.defaultBlockState());
            }
        }
        // Entrance
        set(ox + 7, y, z2, Blocks.AIR.defaultBlockState());
        set(ox + 7, y + 1, z2, Blocks.AIR.defaultBlockState());
        door(ox + 7, y, z2, Blocks.OAK_DOOR, Direction.SOUTH);
        set(x1 + 1, y + 3, z1 + 1, ModBlocks.GROW_LAMP.get().defaultBlockState());
        spawnOnGround(ModEntities.SURVIVOR.get(), ox + 3, oz + 14);
    }

    private void settlementWalls() {
        BlockState wallBlock = Blocks.COBBLESTONE_WALL.defaultBlockState();
        BlockState stone = Blocks.STONE_BRICKS.defaultBlockState();
        for (int i = 0; i <= 15; i++) {
            if ((edges & EDGE_NORTH) != 0) wallColumn(ox + i, oz, stone, wallBlock, false);
            if ((edges & EDGE_SOUTH) != 0) wallColumn(ox + i, oz + 15, stone, wallBlock, gate && i >= 6 && i <= 9);
            if ((edges & EDGE_WEST) != 0) wallColumn(ox, oz + i, stone, wallBlock, false);
            if ((edges & EDGE_EAST) != 0) wallColumn(ox + 15, oz + i, stone, wallBlock, false);
        }
        // Floodlights on the corners
        if ((edges & EDGE_NORTH) != 0 && (edges & EDGE_WEST) != 0) cornerLight(ox, oz);
        if ((edges & EDGE_NORTH) != 0 && (edges & EDGE_EAST) != 0) cornerLight(ox + 15, oz);
        if ((edges & EDGE_SOUTH) != 0 && (edges & EDGE_WEST) != 0) cornerLight(ox, oz + 15);
        if ((edges & EDGE_SOUTH) != 0 && (edges & EDGE_EAST) != 0) cornerLight(ox + 15, oz + 15);
        if (gate) {
            if (inChunk(ox + 5, oz + 15)) set(ox + 5, ground(ox + 5, oz + 15) + 4, oz + 15, ModBlocks.ALARM_BELL.get().defaultBlockState());
            spawnOnGround(ModEntities.SURVIVOR.get(), ox + 7, oz + 13);
        }
    }

    private void cornerLight(int x, int z) {
        if (inChunk(x, z)) set(x, ground(x, z) + 4, z, ModBlocks.FLOODLIGHT.get().defaultBlockState());
    }

    /** Wall follows the terrain: 3 blocks of stone and a wall cap on top of the natural ground. */
    private void wallColumn(int x, int z, BlockState stone, BlockState top, boolean gateGap) {
        if (!inChunk(x, z)) return;
        int g = ground(x, z);
        if (gateGap) {
            set(x, g, z, Blocks.GRAVEL.defaultBlockState());
            set(x, g + 4, z, stone);
            return;
        }
        for (int dy = 1; dy <= 3; dy++) set(x, g + dy, z, stone);
        set(x, g + 4, z, top);
    }

    private void placeTruck() {
        int x = ox + 8, z = oz + 15;
        if (!inChunk(x, z)) return;
        int y = ground(x, z) + 1;
        BoxTruckEntity t = ModEntities.BOX_TRUCK.get().create(level.getLevel());
        if (t == null) return;
        t.moveTo(x + 0.5, y, z + 0.5, 90f, 0f);
        t.setYBodyRot(90f);
        t.setFuel(10f);
        t.setHealth(t.getMaxHealth() * 0.6f);
        level.addFreshEntity(t);
    }
}
