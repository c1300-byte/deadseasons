package com.deadseasons.entity.zombie;

import com.deadseasons.registry.ModDamage;
import com.deadseasons.registry.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Former riot police. Heavy armor and a riot shield stance: bullets and arrows hitting it from the
 * front do 70% less damage. Flank it or use melee. Doesn't burn.
 */
public class RiotZombie extends DSZombie {
    public RiotZombie(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        this.xpReward = 14;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 34.0)
                .add(Attributes.MOVEMENT_SPEED, 0.21)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.RIOT_HELMET.get()));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.RIOT_ARMOR.get()));
        if (random.nextBoolean()) this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.RIOT_LEGGINGS.get()));
        if (random.nextBoolean()) this.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.RIOT_BOOTS.get()));
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            this.setDropChance(slot, 0.05f);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean ranged = source.is(ModDamage.BULLET) || source.is(DamageTypeTags.IS_PROJECTILE);
        Entity attacker = source.getEntity();
        if (ranged && attacker != null) {
            Vec3 look = this.getViewVector(1.0f).multiply(1, 0, 1).normalize();
            Vec3 toAttacker = attacker.position().subtract(this.position()).multiply(1, 0, 1).normalize();
            if (look.dot(toAttacker) > 0.3) {
                amount *= 0.3f;
                this.playSound(SoundEvents.SHIELD_BLOCK, 1.0f, 0.8f + random.nextFloat() * 0.4f);
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    protected boolean burnsInDaylight() {
        return false;
    }

    @Override
    public int barricadeDamage() {
        return 2;
    }
}
