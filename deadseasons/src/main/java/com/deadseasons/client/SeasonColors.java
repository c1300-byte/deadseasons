package com.deadseasons.client;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import org.jetbrains.annotations.Nullable;

/** Tints grass and leaves by season: golden/orange Autumn, pale frosty Winter, fresh green Spring. */
public final class SeasonColors {

    public static void register(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> season(grassBase(state, level, pos), Kind.GRASS),
                Blocks.GRASS_BLOCK, Blocks.GRASS, Blocks.FERN, Blocks.TALL_GRASS, Blocks.LARGE_FERN, Blocks.POTTED_FERN, Blocks.SUGAR_CANE);
        event.register((state, level, pos, tint) -> season(level != null && pos != null
                        ? BiomeColors.getAverageFoliageColor(level, pos) : FoliageColor.getDefaultColor(), Kind.FOLIAGE),
                Blocks.OAK_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.MANGROVE_LEAVES, Blocks.VINE);
        event.register((state, level, pos, tint) -> season(FoliageColor.getBirchColor(), Kind.BIRCH), Blocks.BIRCH_LEAVES);
        event.register((state, level, pos, tint) -> season(FoliageColor.getEvergreenColor(), Kind.EVERGREEN), Blocks.SPRUCE_LEAVES);
    }

    private enum Kind { GRASS, FOLIAGE, BIRCH, EVERGREEN }

    private static int grassBase(BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos) {
        if (level == null || pos == null) return GrassColor.get(0.5, 1.0);
        BlockPos p = pos;
        if (state.hasProperty(DoublePlantBlock.HALF) && state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER) p = pos.below();
        return BiomeColors.getAverageGrassColor(level, p);
    }

    private static int season(int color, Kind kind) {
        switch (ClientState.season) {
            case 1: // Autumn
                return switch (kind) {
                    case GRASS -> blend(color, 0xC2A04A, 0.45f);
                    case FOLIAGE -> blend(color, 0xD2641E, 0.7f);
                    case BIRCH -> blend(color, 0xF0C430, 0.75f);
                    case EVERGREEN -> blend(color, 0x6B6B3A, 0.15f);
                };
            case 2: // Winter
                return switch (kind) {
                    case GRASS -> blend(color, 0xCFCFC2, 0.5f);
                    case FOLIAGE -> blend(color, 0x7A5C3E, 0.6f);
                    case BIRCH -> blend(color, 0x9C8460, 0.6f);
                    case EVERGREEN -> blend(color, 0xE8EEF2, 0.3f);
                };
            case 3: // Spring
                return kind == Kind.EVERGREEN ? color : blend(color, 0x74D84C, 0.2f);
            default: // Summer
                return color;
        }
    }

    private static int blend(int a, int b, float t) {
        int r = (int) (((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = (int) (((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = (int) ((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private SeasonColors() {}
}
