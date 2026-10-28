package com.deadseasons.registry;

import com.deadseasons.DeadSeasons;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Damage types are defined as JSON in data/deadseasons/damage_type/. */
public final class ModDamage {
    public static final ResourceKey<DamageType> BULLET = key("bullet");
    public static final ResourceKey<DamageType> INFECTION = key("infection");
    public static final ResourceKey<DamageType> DEHYDRATION = key("dehydration");
    public static final ResourceKey<DamageType> HYPOTHERMIA = key("hypothermia");
    public static final ResourceKey<DamageType> HEATSTROKE = key("heatstroke");
    public static final ResourceKey<DamageType> ROADKILL = key("roadkill");

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, DeadSeasons.id(name));
    }

    public static DamageSource of(Level level, ResourceKey<DamageType> key) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key));
    }

    public static DamageSource of(Level level, ResourceKey<DamageType> key, @Nullable Entity direct, @Nullable Entity cause) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key), direct, cause);
    }

    private ModDamage() {}
}
