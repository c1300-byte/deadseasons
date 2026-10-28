package com.deadseasons.system;

import com.deadseasons.Config;
import com.deadseasons.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;

/** Crop growth, animal spawning and snow/ice changes driven by the season. */
public final class SeasonalWorld {

    /**
     * Outdoor crops grow normally in Summer and Spring, slower in Autumn and not at all in Winter.
     * Crops with glass, glass panes or a Grow Lamp anywhere above them (a greenhouse) always grow.
     */
    public static void onCropGrow(BlockEvent.CropGrowEvent.Pre event) {
        LevelAccessor la = event.getLevel();
        if (!(la instanceof ServerLevel level) || !level.dimension().equals(Level.OVERWORLD)) return;
        Seasons.Season season = Seasons.get(level);
        if (season == Seasons.Season.SUMMER || season == Seasons.Season.SPRING) return;
        if (isGreenhouse(level, event.getPos())) return;
        if (season == Seasons.Season.WINTER) {
            event.setResult(Event.Result.DENY);
        } else if (level.getRandom().nextDouble() < Config.AUTUMN_CROP_FAIL_CHANCE.get()) {
            event.setResult(Event.Result.DENY);
        }
    }

    public static boolean isGreenhouse(LevelAccessor level, BlockPos pos) {
        for (int dy = 1; dy <= 12; dy++) {
            BlockState s = level.getBlockState(pos.above(dy));
            if (s.is(Tags.Blocks.GLASS) || s.is(Tags.Blocks.GLASS_PANES) || s.is(ModBlocks.GROW_LAMP.get())) return true;
        }
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, 0, -2), pos.offset(2, 2, 2))) {
            if (level.getBlockState(p).is(ModBlocks.GROW_LAMP.get())) return true;
        }
        return false;
    }

    /** Fewer animals spawn in Autumn and Winter. */
    public static void onSpawnCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        if (event.getEntityType().getCategory() != MobCategory.CREATURE) return;
        if (event.getSpawnType() != MobSpawnType.NATURAL && event.getSpawnType() != MobSpawnType.CHUNK_GENERATION) return;
        ServerLevel level = event.getLevel().getLevel();
        if (!level.dimension().equals(Level.OVERWORLD)) return;
        Seasons.Season season = Seasons.get(level);
        double keep = switch (season) {
            case WINTER -> Config.WINTER_ANIMAL_SPAWN_CHANCE.get();
            case AUTUMN -> Config.AUTUMN_ANIMAL_SPAWN_CHANCE.get();
            default -> 1.0;
        };
        if (keep < 1.0 && event.getRandom().nextDouble() > keep) {
            event.setResult(Event.Result.DENY);
        }
    }

    /** Snow builds up and water freezes around players in Winter, then melts in Spring. Called every 2 seconds. */
    public static void tick(ServerPlayer player) {
        if (!Config.SEASON_SNOW.get()) return;
        ServerLevel level = player.serverLevel();
        if (!level.dimension().equals(Level.OVERWORLD)) return;
        Seasons.Season season = Seasons.get(level);
        if (season != Seasons.Season.WINTER && season != Seasons.Season.SPRING) return;
        RandomSource r = level.getRandom();

        for (int i = 0; i < 8; i++) {
            int x = player.getBlockX() + r.nextInt(81) - 40;
            int z = player.getBlockZ() + r.nextInt(81) - 40;
            BlockPos column = new BlockPos(x, 0, z);
            if (!level.hasChunkAt(column)) continue;
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column);
            BlockPos below = top.below();
            boolean naturallyCold = level.getBiome(top).value().coldEnoughToSnow(top);

            if (season == Seasons.Season.WINTER) {
                if (level.getFluidState(below).is(Fluids.WATER) && level.getBlockState(below).is(Blocks.WATER)
                        && level.getFluidState(below).isSource()) {
                    if (r.nextInt(3) == 0) level.setBlockAndUpdate(below, Blocks.ICE.defaultBlockState());
                } else if (level.isEmptyBlock(top) && level.getBrightness(LightLayer.BLOCK, top) < 10
                        && Blocks.SNOW.defaultBlockState().canSurvive(level, top)) {
                    level.setBlockAndUpdate(top, Blocks.SNOW.defaultBlockState());
                }
            } else if (!naturallyCold) {
                // Spring thaw
                if (level.getBlockState(top).is(Blocks.SNOW)) {
                    level.setBlockAndUpdate(top, Blocks.AIR.defaultBlockState());
                } else if (level.getBlockState(below).is(Blocks.SNOW)) {
                    level.setBlockAndUpdate(below, Blocks.AIR.defaultBlockState());
                } else if (level.getBlockState(below).is(Blocks.ICE) && level.canSeeSky(top)) {
                    level.setBlockAndUpdate(below, Blocks.WATER.defaultBlockState());
                }
            }
        }
    }

    private SeasonalWorld() {}
}
