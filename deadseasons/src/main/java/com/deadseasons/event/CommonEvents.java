package com.deadseasons.event;

import com.deadseasons.Config;
import com.deadseasons.DeadSeasons;
import com.deadseasons.command.DSCommand;
import com.deadseasons.entity.SurvivorEntity;
import com.deadseasons.entity.zombie.DSZombie;
import com.deadseasons.item.ConsumableItem;
import com.deadseasons.network.ModNetwork;
import com.deadseasons.network.StatusPacket;
import com.deadseasons.registry.ModItems;
import com.deadseasons.system.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.SleepFinishedTimeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = DeadSeasons.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CommonEvents {
    private static final UUID HEALTH_SCALING = UUID.fromString("6f0d4a52-2b47-4f5e-9b1c-3c9b8a1d7e01");
    private static final UUID DAMAGE_SCALING = UUID.fromString("6f0d4a52-2b47-4f5e-9b1c-3c9b8a1d7e02");

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        DSCommand.register(event.getDispatcher());
    }

    // ------------------------------------------------------------ ticking

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide) return;
        if (event.level.dimension().equals(Level.OVERWORLD) && event.level.getGameTime() % 20 == 0) {
            WorldEvents.tick((ServerLevel) event.level);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        int t = player.tickCount;
        if (t % 20 == 0) {
            float temp = TemperatureSystem.tick(player);
            ThirstSystem.tick(player, temp);
            InfectionSystem.tick(player);
            if (player.isSprinting() && !player.isCreative()) {
                NoiseSystem.makeNoise(player.serverLevel(), player.blockPosition(), 10, player);
            }
            sync(player, temp);
        }
        if (t % 40 == 0) SeasonalWorld.tick(player);
        if (t % 200 == 0) ZombieSpawner.ambientTick(player);
    }

    public static void sync(ServerPlayer player, float temp) {
        ServerLevel ow = player.server.overworld();
        long day = Seasons.dayIndex(ow);
        ModNetwork.sendTo(player, new StatusPacket(
                ThirstSystem.get(player), temp, InfectionSystem.remainingTicks(player), day,
                Seasons.fromDay(day).ordinal(), Seasons.daysLeftInSeason(day),
                WorldEvents.isBloodMoonActive(ow), WorldEvents.isFogActive(player.server),
                (int) WorldEvents.daysUntilBloodMoon(day),
                Config.THIRST_ENABLED.get(), Config.TEMPERATURE_ENABLED.get()));
    }

    // ------------------------------------------------------------ infection & thirst

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAmount() > 0
                && event.getSource().getDirectEntity() instanceof Zombie z && !(z instanceof ZombifiedPiglin)) {
            InfectionSystem.tryInfect(player);
        }
    }

    @SubscribeEvent
    public static void onItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player && !(event.getItem().getItem() instanceof ConsumableItem)) {
            ThirstSystem.onVanillaConsumed(player, event.getItem());
        }
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity().getItemBySlot(EquipmentSlot.HEAD).is(ModItems.GAS_MASK.get())) {
            var effect = event.getEffectInstance().getEffect();
            if (effect == MobEffects.POISON || effect == MobEffects.CONFUSION) {
                event.setResult(Event.Result.DENY);
            }
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            event.getEntity().getPersistentData().merge(event.getOriginal().getPersistentData());
        }
    }

    /** Truck interior shell can't be broken by anyone, even in creative mode. */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        var state = event.getState();
        if (state.is(com.deadseasons.registry.ModBlocks.TRUCK_WALL.get())
                || state.is(com.deadseasons.registry.ModBlocks.TRUCK_FLOOR.get())
                || state.is(com.deadseasons.registry.ModBlocks.TRUCK_LIGHT.get())
                || state.is(com.deadseasons.registry.ModBlocks.TRUCK_EXIT.get())) {
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------ seasons

    @SubscribeEvent
    public static void onCropGrow(BlockEvent.CropGrowEvent.Pre event) {
        SeasonalWorld.onCropGrow(event);
    }

    @SubscribeEvent
    public static void onSpawnCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        SeasonalWorld.onSpawnCheck(event);
    }

    // ------------------------------------------------------------ sleeping

    @SubscribeEvent
    public static void onSleep(PlayerSleepInBedEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (WorldEvents.isBloodMoonActive(player.level())) {
            event.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);
            player.displayClientMessage(Component.translatable("message.deadseasons.no_sleep_blood_moon").withStyle(ChatFormatting.RED), true);
        }
    }

    @SubscribeEvent
    public static void onSleepFinished(SleepFinishedTimeEvent event) {
        // Sleeping inside a truck should still skip the night
        if (event.getLevel() instanceof ServerLevel level && TruckInteriors.isInterior(level)) {
            level.getServer().overworld().setDayTime(event.getNewTime());
        }
    }

    // ------------------------------------------------------------ zombies

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof Zombie zombie) || zombie instanceof ZombifiedPiglin) return;

        // Vanilla zombies also hunt survivors
        if (!(zombie instanceof DSZombie)) {
            zombie.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(zombie, SurvivorEntity.class, true));
        }

        if (event.loadedFromDisk() || !Config.DIFFICULTY_SCALING.get()) return;
        long day = Seasons.dayIndex(event.getLevel());
        double progress = Math.min(1.0, day / 150.0);
        double hp = Config.MAX_HEALTH_BONUS.get() * progress;
        double dmg = Config.MAX_DAMAGE_BONUS.get() * progress;
        AttributeInstance health = zombie.getAttribute(Attributes.MAX_HEALTH);
        if (health != null && hp > 0.01 && health.getModifier(HEALTH_SCALING) == null) {
            health.addPermanentModifier(new AttributeModifier(HEALTH_SCALING, "Apocalypse scaling", hp, AttributeModifier.Operation.MULTIPLY_BASE));
            zombie.setHealth(zombie.getMaxHealth());
        }
        AttributeInstance attack = zombie.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null && dmg > 0.01 && attack.getModifier(DAMAGE_SCALING) == null) {
            attack.addPermanentModifier(new AttributeModifier(DAMAGE_SCALING, "Apocalypse scaling", dmg, AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }

    private CommonEvents() {}
}
