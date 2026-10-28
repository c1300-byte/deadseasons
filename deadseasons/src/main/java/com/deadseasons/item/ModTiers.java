package com.deadseasons.item;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

/** Weapon tiers. Attack bonus is 0 so each weapon's damage is set directly on the item. */
public enum ModTiers implements Tier {
    WOOD_WEAPON(180, 2.0f, 1, 10, () -> Ingredient.of(Items.OAK_PLANKS, Items.SPRUCE_PLANKS, Items.BIRCH_PLANKS)),
    IRON_WEAPON(420, 6.0f, 2, 12, () -> Ingredient.of(Items.IRON_INGOT)),
    STEEL_WEAPON(900, 7.0f, 3, 14, () -> Ingredient.of(Items.IRON_INGOT)),
    DIAMOND_WEAPON(1400, 8.0f, 3, 10, () -> Ingredient.of(Items.DIAMOND));

    private final int uses;
    private final float speed;
    private final int level;
    private final int enchantability;
    private final Supplier<Ingredient> repair;

    ModTiers(int uses, float speed, int level, int enchantability, Supplier<Ingredient> repair) {
        this.uses = uses;
        this.speed = speed;
        this.level = level;
        this.enchantability = enchantability;
        this.repair = repair;
    }

    @Override public int getUses() { return uses; }
    @Override public float getSpeed() { return speed; }
    @Override public float getAttackDamageBonus() { return 0.0f; }
    @Override public int getLevel() { return level; }
    @Override public int getEnchantmentValue() { return enchantability; }
    @Override public Ingredient getRepairIngredient() { return repair.get(); }
}
