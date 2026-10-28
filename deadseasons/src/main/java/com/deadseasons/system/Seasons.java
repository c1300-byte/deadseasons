package com.deadseasons.system;

import com.deadseasons.Config;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Seasons are calculated straight from the overworld day count, so they never drift.
 * Day 0 is the first day of Summer.
 */
public final class Seasons {
    public enum Season {
        SUMMER, AUTUMN, WINTER, SPRING;

        public String key() {
            return "season.deadseasons." + name().toLowerCase();
        }
    }

    public static long dayIndex(Level level) {
        Level overworld = level;
        if (level instanceof ServerLevel sl) overworld = sl.getServer().overworld();
        return overworld.getDayTime() / 24000L;
    }

    public static long dayIndex(MinecraftServer server) {
        return server.overworld().getDayTime() / 24000L;
    }

    public static long timeOfDay(Level level) {
        Level overworld = level;
        if (level instanceof ServerLevel sl) overworld = sl.getServer().overworld();
        return overworld.getDayTime() % 24000L;
    }

    public static Season fromDay(long day) {
        int len = Config.SEASON_LENGTH_DAYS.get();
        int idx = (int) ((day / len) % 4);
        return Season.values()[idx];
    }

    public static Season get(Level level) {
        return fromDay(dayIndex(level));
    }

    public static int daysLeftInSeason(long day) {
        int len = Config.SEASON_LENGTH_DAYS.get();
        return (int) (len - (day % len));
    }

    private Seasons() {}
}
