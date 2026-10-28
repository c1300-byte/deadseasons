package com.deadseasons.entity.zombie;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

/** Huge, slow and tough. Smashes doors and barricades on any difficulty. Does not burn. */
public class Brute extends DSZombie {
    public Brute(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        if (this.getNavigation() instanceof GroundPathNavigation nav) nav.setCanOpenDoors(true);
        this.xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void addBehaviourGoals() {
        super.addBehaviourGoals();
        this.goalSelector.addGoal(1, new BreakDoorGoal(this, difficulty -> true));
    }

    @Override
    protected boolean burnsInDaylight() {
        return false;
    }

    @Override
    public int barricadeDamage() {
        return 3;
    }
}
