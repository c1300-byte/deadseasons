package com.deadseasons.world;

import com.deadseasons.Config;
import com.deadseasons.registry.ModStructures;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A grid of 16x16 lots with roads between them. "Ruined" towns are overrun, looted and full of zombies;
 * "settlements" are walled survivor camps with traders, guards and greenhouse farms.
 */
public class TownStructure extends Structure {
    public static final Codec<TownStructure> CODEC = RecordCodecBuilder.create(i -> i.group(
            settingsCodec(i),
            Codec.BOOL.fieldOf("settlement").forGetter(s -> s.settlement)
    ).apply(i, TownStructure::new));

    private final boolean settlement;

    public TownStructure(StructureSettings settings, boolean settlement) {
        super(settings);
        this.settlement = settlement;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkPos cp = ctx.chunkPos();
        WorldgenRandom random = ctx.random();
        int size = settlement ? 3 : 3 + random.nextInt(2);
        int x0 = cp.getMinBlockX();
        int z0 = cp.getMinBlockZ();

        // Each lot gets its own floor height from the natural terrain, like village buildings do.
        // Lots that are too steep or under water are left as empty overgrown lots.
        int total = size * size;
        int[] lotY = new int[total];
        boolean[] usable = new boolean[total];
        int wet = 0, min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int lx = 0; lx < size; lx++) {
            for (int lz = 0; lz < size; lz++) {
                int idx = lx * size + lz;
                int lmin = Integer.MAX_VALUE, lmax = Integer.MIN_VALUE, sum = 0, n = 0;
                boolean water = false;
                for (int[] off : new int[][]{{3, 3}, {12, 3}, {3, 12}, {12, 12}, {8, 8}}) {
                    int x = x0 + lx * 16 + off[0];
                    int z = z0 + lz * 16 + off[1];
                    int h = ctx.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
                    int floor = ctx.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, ctx.heightAccessor(), ctx.randomState());
                    if (h - floor > 1) water = true;
                    lmin = Math.min(lmin, h);
                    lmax = Math.max(lmax, h);
                    sum += h;
                    n++;
                }
                lotY[idx] = Math.round(sum / (float) n);
                usable[idx] = !water && lmax - lmin <= 6;
                if (water) wet++;
                min = Math.min(min, lotY[idx]);
                max = Math.max(max, lotY[idx]);
            }
        }
        if (wet * 3 > total || max - min > 24) return Optional.empty();
        int baseY = lotY[total / 2];

        // Decide what goes on each lot
        List<TownPiece.Kind> kinds = new ArrayList<>();
        if (settlement) {
            kinds.add(TownPiece.Kind.FARM);
            kinds.add(TownPiece.Kind.FARM);
            kinds.add(TownPiece.Kind.CLINIC);
            kinds.add(TownPiece.Kind.STORE);
            while (kinds.size() < total - 1) kinds.add(TownPiece.Kind.HOUSE);
        } else {
            kinds.add(TownPiece.Kind.STORE);
            kinds.add(TownPiece.Kind.POLICE);
            kinds.add(TownPiece.Kind.CLINIC);
            kinds.add(TownPiece.Kind.GAS_STATION);
            if (total >= 16) kinds.add(TownPiece.Kind.MILITARY);
            while (kinds.size() < total) kinds.add(random.nextInt(5) == 0 ? TownPiece.Kind.EMPTY : TownPiece.Kind.HOUSE);
        }
        Collections.shuffle(kinds, new java.util.Random(random.nextLong()));
        if (settlement) {
            int center = (size / 2) * size + size / 2;
            kinds.add(center, TownPiece.Kind.PLAZA);
        }

        for (int i = 0; i < total; i++) {
            if (!usable[i] && kinds.get(i) != TownPiece.Kind.PLAZA) kinds.set(i, TownPiece.Kind.EMPTY);
        }

        // Rare abandoned box truck in ruined towns
        int truckLot = -1;
        if (!settlement && random.nextDouble() < Config.TRUCK_TOWN_CHANCE.get()) {
            truckLot = random.nextInt(total);
        }

        final int fSize = size;
        final int fTruck = truckLot;
        final long seed = random.nextLong();
        return Optional.of(new GenerationStub(new BlockPos(x0, baseY, z0), builder -> {
            for (int lx = 0; lx < fSize; lx++) {
                for (int lz = 0; lz < fSize; lz++) {
                    int idx = lx * fSize + lz;
                    int edges = 0;
                    if (lz == 0) edges |= TownPiece.EDGE_NORTH;
                    if (lz == fSize - 1) edges |= TownPiece.EDGE_SOUTH;
                    if (lx == 0) edges |= TownPiece.EDGE_WEST;
                    if (lx == fSize - 1) edges |= TownPiece.EDGE_EAST;
                    boolean gate = lx == fSize / 2 && lz == fSize - 1;
                    builder.addPiece(new TownPiece(kinds.get(idx), x0 + lx * 16, lotY[idx], z0 + lz * 16, settlement,
                            edges, gate, idx == fTruck, seed + idx * 7919L));
                }
            }
        }));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.TOWN.get();
    }
}
