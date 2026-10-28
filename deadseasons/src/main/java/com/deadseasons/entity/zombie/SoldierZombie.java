package com.deadseasons.entity.zombie;

import com.deadseasons.registry.ModItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A fallen soldier still wearing military gear. Tough, doesn't burn, and drops ammo. */
public class SoldierZombie extends DSZombie {
    public SoldierZombie(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        this.xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.MILITARY_HELMET.get()));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.MILITARY_VEST.get()));
        if (random.nextBoolean()) this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.MILITARY_PANTS.get()));
        if (random.nextBoolean()) this.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.MILITARY_BOOTS.get()));
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            this.setDropChance(slot, 0.06f);
        }
    }

    @Override
    protected boolean burnsInDaylight() {
        return false;
    }
}
