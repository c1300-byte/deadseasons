package com.deadseasons.system;

import com.deadseasons.Config;
import com.deadseasons.entity.zombie.DSZombie;
import com.deadseasons.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Extra apocalypse spawning around players (day and night), plus helpers used by events. */
public final class ZombieSpawner {

    public static int countZombiesNear(ServerLevel level, BlockPos pos, int radius) {
        return level.getEntitiesOfClass(Zombie.class, new AABB(pos).inflate(radius),
                z -> z.isAlive() && !(z instanceof ZombifiedPiglin)).size();
    }

    /** Called every 10 seconds per player. */
    public static void ambientTick(ServerPlayer player) {
        if (!Config.EXTRA_ZOMBIE_SPAWNS.get()) return;
        ServerLevel level = player.serverLevel();
        if (!level.dimension().equals(Level.OVERWORLD) || level.getDifficulty() == Difficulty.PEACEFUL) return;
        if (player.isCreative() || player.isSpectator()) return;

        long day = Seasons.dayIndex(level);
        // Pressure ramps up over the first 60 days
        float ramp = Mth.clamp(0.5f + day / 120f, 0.5f, 1.0f);
        int cap = Math.round(Config.MAX_NEARBY_ZOMBIES.get() * ramp);
        if (countZombiesNear(level, player.blockPosition(), 64) >= cap) return;

        RandomSource r = level.getRandom();
        boolean night = !level.isDay();
        if (!night && r.nextFloat() < 0.4f) return; // fewer daytime spawns

        int group = 1 + r.nextInt(night ? 3 : 2);
        EntityType<? extends DSZombie> type = pickType(level, r, night, day);
        for (int i = 0; i < group; i++) {
            spawnAround(level, type, player.blockPosition(), 28, 52, null, false);
        }
    }

    public static EntityType<? extends DSZombie> pickType(ServerLevel level, RandomSource r, boolean night, long day) {
        List<EntityType<? extends DSZombie>> types = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        boolean winter = Seasons.get(level) == Seasons.Season.WINTER;
        if (night) {
            add(types, weights, ModEntities.WALKER.get(), 40);
            add(types, weights, ModEntities.RUNNER.get(), day >= 3 ? 14 : 4);
            add(types, weights, ModEntities.SCREAMER.get(), day >= 4 ? 6 : 0);
            add(types, weights, ModEntities.SPITTER.get(), day >= 6 ? 6 : 0);
        }
        add(types, weights, ModEntities.CRAWLER.get(), 10);
        add(types, weights, ModEntities.SOLDIER.get(), day >= 2 ? 6 : 2);
        add(types, weights, ModEntities.RIOT.get(), day >= 8 ? 4 : 0);
        add(types, weights, ModEntities.BLOATER.get(), day >= 5 ? 5 : 0);
        add(types, weights, ModEntities.BRUTE.get(), day >= 10 ? 3 : 0);
        add(types, weights, ModEntities.FROSTBITTEN.get(), winter ? 16 : 0);

        int total = 0;
        for (int w : weights) total += w;
        int roll = r.nextInt(Math.max(1, total));
        for (int i = 0; i < types.size(); i++) {
            roll -= weights.get(i);
            if (roll < 0) return types.get(i);
        }
        return ModEntities.WALKER.get();
    }

    private static void add(List<EntityType<? extends DSZombie>> types, List<Integer> weights, EntityType<? extends DSZombie> t, int w) {
        if (w > 0) {
            types.add(t);
            weights.add(w);
        }
    }

    /**
     * Spawns a mob on the surface between minR and maxR blocks from center.
     * @param ignoreLight if false, spots lit by torches/floodlights are skipped
     */
    @Nullable
    public static Mob spawnAround(ServerLevel level, EntityType<? extends Mob> type, BlockPos center, int minR, int maxR,
                                  @Nullable Player target, boolean ignoreLight) {
        RandomSource r = level.getRandom();
        for (int attempt = 0; attempt < 12; attempt++) {
            double ang = r.nextDouble() * Math.PI * 2;
            int d = minR + r.nextInt(Math.max(1, maxR - minR + 1));
            int x = center.getX() + (int) Math.round(Math.cos(ang) * d);
            int z = center.getZ() + (int) Math.round(Math.sin(ang) * d);
            BlockPos column = new BlockPos(x, center.getY(), z);
            if (!level.hasChunkAt(column)) continue;
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            if (Math.abs(pos.getY() - center.getY()) > 40) continue;
            if (!level.getWorldBorder().isWithinBounds(pos)) continue;
            if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.below()).isEmpty()) continue;
            if (!ignoreLight && level.getBrightness(LightLayer.BLOCK, pos) >= 8) continue;
            if (!NaturalSpawner.isSpawnPositionOk(SpawnPlacements.Type.ON_GROUND, level, pos, type)) continue;
            if (level.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, Math.max(6, minR * 0.7), false) != null) continue;

            Mob mob = type.create(level);
            if (mob == null) return null;
            mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, r.nextFloat() * 360f, 0f);
            if (!level.noCollision(mob)) {
                mob.discard();
                continue;
            }
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
            if (target != null) mob.setTarget(target);
            level.addFreshEntity(mob);
            return mob;
        }
        return null;
    }

    private ZombieSpawner() {}
}
