package com.deadseasons.registry;

import com.deadseasons.DeadSeasons;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, DeadSeasons.MODID);

    /** Display-only effect: the real infection timer lives in player data (see InfectionSystem). */
    public static final RegistryObject<MobEffect> INFECTED = EFFECTS.register("infected", InfectedEffect::new);

    public static class InfectedEffect extends MobEffect {
        public InfectedEffect() {
            super(MobEffectCategory.HARMFUL, 0x5A7A2B);
        }

        @Override
        public List<ItemStack> getCurativeItems() {
            // Milk can't cure a zombie infection. Only antibiotics can.
            return new ArrayList<>();
        }
    }

    private ModEffects() {}
}
