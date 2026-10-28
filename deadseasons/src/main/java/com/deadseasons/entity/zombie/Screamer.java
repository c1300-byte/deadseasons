package com.deadseasons.entity.zombie;

import com.deadseasons.registry.ModEntities;
import com.deadseasons.registry.ModSounds;
import com.deadseasons.system.NoiseSystem;
import com.deadseasons.system.ZombieSpawner;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Weak but dangerous: when it spots a player it screams, pulling every zombie within 48 blocks
 * onto the player and calling a couple of extra zombies. Kill it fast. Burns in daylight.
 */
public class Screamer extends DSZombie {
    private int screamCooldown = 60;

    public Screamer(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        if (screamCooldown > 0) {
            screamCooldown--;
            return;
        }
        LivingEntity target = getTarget();
        if (target instanceof Player player && player.isAlive() && !player.isCreative() && !player.isSpectator()
                && distanceToSqr(player) < 24 * 24 && getSensing().hasLineOfSight(player)) {
            scream(player);
            screamCooldown = 300 + random.nextInt(200);
        }
    }

    private void scream(Player player) {
        ServerLevel server = (ServerLevel) level();
        server.playSound(null, getX(), getY(), getZ(), ModSounds.SCREAMER_SCREAM.get(), SoundSource.HOSTILE, 4.0f,
                0.9f + random.nextFloat() * 0.2f);
        server.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getEyeY(), getZ(), 1, 0, 0, 0, 0);
        NoiseSystem.alertZombies(server, blockPosition(), 48, player);

        // Call a few reinforcements if the area isn't already crowded
        if (ZombieSpawner.countZombiesNear(server, blockPosition(), 32) < 14) {
            int count = 1 + random.nextInt(2);
            for (int i = 0; i < count; i++) {
                EntityType<? extends DSZombie> type = random.nextInt(3) == 0 ? ModEntities.RUNNER.get() : ModEntities.WALKER.get();
                ZombieSpawner.spawnAround(server, type, blockPosition(), 8, 16, player, false);
            }
        }
    }

    @Override
    public void playAmbientSound() {
        // A little higher pitched than normal zombies
        net.minecraft.sounds.SoundEvent s = getAmbientSound();
        if (s != null) playSound(s, getSoundVolume(), 1.5f + random.nextFloat() * 0.2f);
    }
}
