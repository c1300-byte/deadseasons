package com.deadseasons.registry;

import com.deadseasons.DeadSeasons;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, DeadSeasons.MODID);

    public static final RegistryObject<SoundEvent> PISTOL_SHOT = reg("pistol_shot");
    public static final RegistryObject<SoundEvent> RIFLE_SHOT = reg("rifle_shot");
    public static final RegistryObject<SoundEvent> SHOTGUN_SHOT = reg("shotgun_shot");
    public static final RegistryObject<SoundEvent> SMG_SHOT = reg("smg_shot");
    public static final RegistryObject<SoundEvent> GUN_RELOAD = reg("gun_reload");
    public static final RegistryObject<SoundEvent> GUN_EMPTY = reg("gun_empty");
    public static final RegistryObject<SoundEvent> SCREAMER_SCREAM = reg("screamer_scream");
    public static final RegistryObject<SoundEvent> AIRDROP_PLANE = reg("airdrop_plane");
    public static final RegistryObject<SoundEvent> TRUCK_ENGINE = reg("truck_engine");
    public static final RegistryObject<SoundEvent> BLOOD_MOON = reg("blood_moon");

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(DeadSeasons.id(name)));
    }

    private ModSounds() {}
}
