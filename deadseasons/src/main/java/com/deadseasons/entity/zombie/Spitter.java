package com.deadseasons.entity.zombie;

import com.deadseasons.entity.AcidSpitEntity;
import com.deadseasons.entity.SurvivorEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Keeps its distance and spits poisonous acid. Burns in daylight. */
public class Spitter extends DSZombie implements RangedAttackMob {
    public Spitter(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 18.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 40, 60, 14.0f));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, SurvivorEntity.class, true));
        addApocalypseGoals();
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        AcidSpitEntity spit = new AcidSpitEntity(level(), this);
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.3333) - spit.getY();
        double dz = target.getZ() - this.getZ();
        double arc = Math.sqrt(dx * dx + dz * dz) * 0.2;
        spit.shoot(dx, dy + arc, dz, 1.3f, 6.0f);
        this.playSound(SoundEvents.LLAMA_SPIT, 1.0f, 0.6f + random.nextFloat() * 0.3f);
        level().addFreshEntity(spit);
        this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
    }
}
