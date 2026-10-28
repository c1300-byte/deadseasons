package com.deadseasons.entity.zombie;

import com.deadseasons.entity.BoxTruckEntity;
import com.deadseasons.entity.SurvivorEntity;
import com.deadseasons.entity.ai.BreakBarricadeGoal;
import com.deadseasons.entity.ai.InvestigateNoiseGoal;
import com.deadseasons.entity.ai.MigrateGoal;
import com.deadseasons.system.Seasons;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for every Dead Seasons zombie. Adds: per-type daylight burning, noise investigation,
 * herd migration, barricade breaking, and targeting survivors / occupied trucks.
 */
public class DSZombie extends Zombie {
    @Nullable private BlockPos noiseTarget;
    private int noiseTicks;
    @Nullable private BlockPos migrateTarget;

    public DSZombie(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    /** Whether this zombie type burns in sunlight. */
    protected boolean burnsInDaylight() {
        return true;
    }

    /** How much damage this zombie does to barricades per hit. */
    public int barricadeDamage() {
        return 1;
    }

    @Override
    protected boolean isSunSensitive() {
        return burnsInDaylight();
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    public void setBaby(boolean baby) {
        super.setBaby(false);
    }

    @Override
    protected void addBehaviourGoals() {
        super.addBehaviourGoals();
        addApocalypseGoals();
    }

    protected void addApocalypseGoals() {
        this.goalSelector.addGoal(1, new BreakBarricadeGoal(this));
        this.goalSelector.addGoal(4, new MigrateGoal(this));
        this.goalSelector.addGoal(5, new InvestigateNoiseGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, SurvivorEntity.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, BoxTruckEntity.class, 10, true, false,
                t -> t instanceof BoxTruckEntity truck && truck.isOccupied()));
    }

    // ------------------------------------------------------------ noise & migration

    public void hearNoise(BlockPos pos) {
        if (getTarget() == null) {
            this.noiseTarget = pos;
            this.noiseTicks = 400;
        }
    }

    @Nullable
    public BlockPos getNoiseTarget() {
        return noiseTicks > 0 ? noiseTarget : null;
    }

    public void clearNoiseTarget() {
        this.noiseTarget = null;
        this.noiseTicks = 0;
    }

    public void setMigrateTarget(@Nullable BlockPos pos) {
        this.migrateTarget = pos;
    }

    @Nullable
    public BlockPos getMigrateTarget() {
        return migrateTarget;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (noiseTicks > 0) noiseTicks--;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (migrateTarget != null) tag.put("MigrateTarget", NbtUtils.writeBlockPos(migrateTarget));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("MigrateTarget")) migrateTarget = NbtUtils.readBlockPos(tag.getCompound("MigrateTarget"));
    }

    // ------------------------------------------------------------ spawn rules

    /**
     * Spawn rule for zombies that roam in daylight: they can spawn in sunlight (35% of attempts)
     * but never near block light such as torches or floodlights, so lit bases stay safer.
     */
    public static boolean checkApocalypseSpawn(EntityType<? extends Monster> type, ServerLevelAccessor level,
                                               MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false;
        if (level.getBrightness(LightLayer.BLOCK, pos) >= 8) return false;
        if (!Mob.checkMobSpawnRules(type, level, reason, pos, random)) return false;
        return Monster.isDarkEnoughToSpawn(level, pos, random) || random.nextFloat() < 0.35f;
    }

    /** Frostbitten only spawn in cold biomes, or anywhere during Winter. */
    public static boolean checkColdSpawn(EntityType<? extends Monster> type, ServerLevelAccessor level,
                                         MobSpawnType reason, BlockPos pos, RandomSource random) {
        boolean cold = level.getBiome(pos).value().coldEnoughToSnow(pos)
                || Seasons.get(level.getLevel()) == Seasons.Season.WINTER;
        return cold && checkApocalypseSpawn(type, level, reason, pos, random);
    }
}
