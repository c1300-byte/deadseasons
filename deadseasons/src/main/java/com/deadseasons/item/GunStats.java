package com.deadseasons.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/**
 * @param damage       damage per pellet (half-hearts)
 * @param pellets      projectiles per shot (shotguns fire several)
 * @param fireDelay    ticks between shots
 * @param automatic    hold right-click to keep firing
 * @param magazine     rounds per magazine
 * @param reloadTicks  reload time in ticks
 * @param spread       random spread in degrees
 * @param range        max range in blocks
 * @param noise        radius in blocks that zombies hear the shot
 * @param headshot     damage multiplier on headshots
 * @param recoil       upward camera kick in degrees
 */
public record GunStats(float damage, int pellets, int fireDelay, boolean automatic, int magazine, int reloadTicks,
                       float spread, double range, int noise, float headshot, float recoil,
                       Supplier<Item> ammo, Supplier<SoundEvent> sound) {
}
