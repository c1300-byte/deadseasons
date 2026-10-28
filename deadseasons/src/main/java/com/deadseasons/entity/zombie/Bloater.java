package com.deadseasons.entity.zombie;

import com.deadseasons.system.NoiseSystem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

/**
 * Swollen with gas. If it gets close it bursts, and it always bursts when killed, leaving a poison cloud.
 * Shoot it from range or wear a gas mask. Doesn't burn.
 */
public class Bloater extends DSZombie {
    private int fuse = 0;
    private boolean burst = false;

    public Bloater(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 26.0)
                .add(Attributes.MOVEMENT_SPEED, 0.19)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || burst) return;
        LivingEntity target = getTarget();
        if (target != null && target.isAlive() && distanceToSqr(target) < 2.5 * 2.5) {
            if (fuse == 0) playSound(SoundEvents.CREEPER_PRIMED, 1.0f, 0.6f);
            fuse++;
            if (fuse >= 30) {
                burst();
                discard();
            }
        } else if (fuse > 0) {
            fuse--;
        }
    }

    private void burst() {
        if (burst || !(level() instanceof ServerLevel server)) return;
        burst = true;
        server.explode(this, getX(), getY() + 0.5, getZ(), 2.0f, Level.ExplosionInteraction.NONE);
        AreaEffectCloud cloud = new AreaEffectCloud(server, getX(), getY(), getZ());
        cloud.setRadius(3.5f);
        cloud.setRadiusOnUse(-0.3f);
        cloud.setWaitTime(5);
        cloud.setDuration(200);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 160, 1));
        cloud.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0));
        cloud.setFixedColor(0x7FA33A);
        server.addFreshEntity(cloud);
        server.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY() + 1, getZ(), 30, 0.6, 0.6, 0.6, 0.2);
        NoiseSystem.makeNoise(server, blockPosition(), 32, null);
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) burst();
        super.die(source);
    }

    @Override
    protected boolean burnsInDaylight() {
        return false;
    }
}
