package com.deadseasons.entity;

import com.deadseasons.registry.ModEntities;
import com.deadseasons.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Acid projectile spat by Spitters. Poisons on hit. */
public class AcidSpitEntity extends ThrowableItemProjectile {
    public AcidSpitEntity(EntityType<? extends AcidSpitEntity> type, Level level) {
        super(type, level);
    }

    public AcidSpitEntity(Level level, LivingEntity shooter) {
        super(ModEntities.ACID_SPIT.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.ACID_GLOB.get();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        Entity owner = getOwner();
        target.hurt(damageSources().mobProjectile(this, owner instanceof LivingEntity l ? l : null), 4.0f);
        if (target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0), owner);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            ((ServerLevel) level()).sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY(), getZ(), 8, 0.2, 0.2, 0.2, 0.05);
            discard();
        }
    }
}
