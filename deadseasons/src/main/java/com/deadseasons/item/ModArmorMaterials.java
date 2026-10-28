package com.deadseasons.item;

import com.deadseasons.DeadSeasons;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

/**
 * Armor/clothing materials. Texture files: assets/deadseasons/textures/models/armor/NAME_layer_1.png / _layer_2.png
 * Defense order: boots, leggings, chestplate, helmet.
 */
public enum ModArmorMaterials implements ArmorMaterial {
    MILITARY("military", 22, new int[]{2, 5, 6, 2}, 10, SoundEvents.ARMOR_EQUIP_IRON, 1.0f, 0.0f, () -> Ingredient.of(Items.IRON_INGOT)),
    RIOT("riot", 30, new int[]{3, 6, 7, 3}, 8, SoundEvents.ARMOR_EQUIP_NETHERITE, 2.0f, 0.1f, () -> Ingredient.of(Items.IRON_INGOT)),
    WINTER("winter", 10, new int[]{1, 2, 3, 1}, 12, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0f, 0.0f, () -> Ingredient.of(Items.WHITE_WOOL)),
    SUMMER("summer", 8, new int[]{1, 1, 2, 1}, 12, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0f, 0.0f, () -> Ingredient.of(Items.STRING)),
    GAS_MASK("gas_mask", 12, new int[]{1, 1, 1, 1}, 8, SoundEvents.ARMOR_EQUIP_GENERIC, 0.0f, 0.0f, () -> Ingredient.of(Items.LEATHER));

    private static final int[] BASE_DURABILITY = {13, 15, 16, 11};

    private final String name;
    private final int durabilityMultiplier;
    private final int[] defense;
    private final int enchantability;
    private final SoundEvent equipSound;
    private final float toughness;
    private final float knockbackResistance;
    private final Supplier<Ingredient> repair;

    ModArmorMaterials(String name, int durabilityMultiplier, int[] defense, int enchantability, SoundEvent equipSound,
                      float toughness, float knockbackResistance, Supplier<Ingredient> repair) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.defense = defense;
        this.enchantability = enchantability;
        this.equipSound = equipSound;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.repair = repair;
    }

    private static int index(ArmorItem.Type type) {
        return switch (type) {
            case BOOTS -> 0;
            case LEGGINGS -> 1;
            case CHESTPLATE -> 2;
            case HELMET -> 3;
        };
    }

    @Override public int getDurabilityForType(ArmorItem.Type type) { return BASE_DURABILITY[index(type)] * durabilityMultiplier; }
    @Override public int getDefenseForType(ArmorItem.Type type) { return defense[index(type)]; }
    @Override public int getEnchantmentValue() { return enchantability; }
    @Override public SoundEvent getEquipSound() { return equipSound; }
    @Override public Ingredient getRepairIngredient() { return repair.get(); }
    @Override public String getName() { return DeadSeasons.MODID + ":" + name; }
    @Override public float getToughness() { return toughness; }
    @Override public float getKnockbackResistance() { return knockbackResistance; }
}
