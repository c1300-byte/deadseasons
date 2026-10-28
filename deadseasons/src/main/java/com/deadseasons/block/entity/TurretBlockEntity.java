package com.deadseasons.block.entity;

import com.deadseasons.registry.ModBlockEntities;
import com.deadseasons.registry.ModDamage;
import com.deadseasons.registry.ModItems;
import com.deadseasons.registry.ModSounds;
import com.deadseasons.system.NoiseSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public class TurretBlockEntity extends BlockEntity {
    public static final int MAX_AMMO = 256;
    private static final double RANGE = 18.0;
    private int ammo = 0;
    private int cooldown = 0;

    public TurretBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTO_TURRET.get(), pos, state);
    }

    public int getAmmo() {
        return ammo;
    }

    public void addAmmo(int amount) {
        ammo = Math.min(MAX_AMMO, ammo + amount);
        setChanged();
    }

    public void dropAmmo() {
        if (level == null || ammo <= 0) return;
        while (ammo > 0) {
            int n = Math.min(64, ammo);
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                    new ItemStack(ModItems.HANDGUN_AMMO.get(), n));
            ammo -= n;
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TurretBlockEntity be) {
        if (be.cooldown > 0) {
            be.cooldown--;
            return;
        }
        if (be.ammo <= 0 || level.getGameTime() % 4 != 0) return;

        Vec3 muzzle = new Vec3(pos.getX() + 0.5, pos.getY() + 0.95, pos.getZ() + 0.5);
        List<Mob> targets = level.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(RANGE),
                m -> m instanceof Enemy && m.isAlive() && !m.isInvisible());
        targets.sort(Comparator.comparingDouble(m -> m.distanceToSqr(muzzle)));
        for (Mob target : targets) {
            Vec3 aim = new Vec3(target.getX(), target.getY(0.6), target.getZ());
            if (aim.distanceTo(muzzle) > RANGE) continue;
            BlockHitResult hit = level.clip(new ClipContext(muzzle, aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target));
            if (hit.getType() != HitResult.Type.MISS && !hit.getBlockPos().equals(pos)) continue;
            fire(level, pos, be, muzzle, target, aim);
            return;
        }
    }

    private static void fire(Level level, BlockPos pos, TurretBlockEntity be, Vec3 muzzle, Mob target, Vec3 aim) {
        be.ammo--;
        be.cooldown = 12;
        be.setChanged();
        target.invulnerableTime = 0;
        target.hurt(ModDamage.of(level, ModDamage.BULLET), 5.0f);
        level.playSound(null, pos, ModSounds.PISTOL_SHOT.get(), SoundSource.BLOCKS, 2.0f, 1.2f);
        if (level instanceof ServerLevel server) {
            double dist = muzzle.distanceTo(aim);
            Vec3 dir = aim.subtract(muzzle).normalize();
            int steps = Mth.clamp((int) (dist / 1.5), 1, 14);
            for (int i = 1; i <= steps; i++) {
                Vec3 p = muzzle.add(dir.scale(dist * i / steps));
                server.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
            server.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y + 0.2, muzzle.z, 2, 0.05, 0.05, 0.05, 0.01);
            NoiseSystem.makeNoise(server, pos, 16, null);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Ammo", ammo);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ammo = tag.getInt("Ammo");
    }
}
