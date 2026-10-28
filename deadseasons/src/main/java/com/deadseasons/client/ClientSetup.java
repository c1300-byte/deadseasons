package com.deadseasons.client;

import com.deadseasons.DeadSeasons;
import com.deadseasons.client.render.*;
import com.deadseasons.entity.SurvivorEntity;
import com.deadseasons.entity.SurvivorTraderEntity;
import com.deadseasons.entity.zombie.DSZombie;
import com.deadseasons.registry.ModEntities;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DeadSeasons.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    public static final KeyMapping RELOAD = new KeyMapping("key.deadseasons.reload", InputConstants.KEY_R, "key.categories.deadseasons");

    private static ResourceLocation tex(String name) {
        return DeadSeasons.id("textures/entity/" + name + ".png");
    }

    private static <T extends DSZombie> void zombie(EntityRenderersEvent.RegisterRenderers e, EntityType<T> type, String texture, float size) {
        e.registerEntityRenderer(type, ctx -> new DSZombieRenderer<>(ctx, tex(texture), size));
    }

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers e) {
        zombie(e, ModEntities.WALKER.get(), "walker", 1.0f);
        zombie(e, ModEntities.RUNNER.get(), "runner", 0.95f);
        zombie(e, ModEntities.BRUTE.get(), "brute", 1.35f);
        zombie(e, ModEntities.SCREAMER.get(), "screamer", 1.0f);
        zombie(e, ModEntities.SOLDIER.get(), "soldier_zombie", 1.0f);
        zombie(e, ModEntities.RIOT.get(), "riot_zombie", 1.0f);
        zombie(e, ModEntities.SPITTER.get(), "spitter", 1.0f);
        zombie(e, ModEntities.BLOATER.get(), "bloater", 1.1f);
        zombie(e, ModEntities.CRAWLER.get(), "crawler", 0.52f);
        zombie(e, ModEntities.FROSTBITTEN.get(), "frostbitten", 1.0f);
        zombie(e, ModEntities.ABOMINATION.get(), "abomination", 1.75f);

        e.registerEntityRenderer(ModEntities.SURVIVOR.get(), ctx -> new HumanRenderer<SurvivorEntity>(ctx,
                s -> tex("survivor_" + Math.floorMod(s.getVariant(), SurvivorEntity.VARIANTS))));
        e.registerEntityRenderer(ModEntities.TRADER.get(), ctx -> new HumanRenderer<SurvivorTraderEntity>(ctx,
                s -> tex("trader")));
        e.registerEntityRenderer(ModEntities.ACID_SPIT.get(), ThrownItemRenderer::new);
        e.registerEntityRenderer(ModEntities.BOX_TRUCK.get(), TruckRenderer::new);
    }

    @SubscribeEvent
    public static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(TruckModel.LAYER, TruckModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onKeys(RegisterKeyMappingsEvent e) {
        e.register(RELOAD);
    }

    @SubscribeEvent
    public static void onOverlays(RegisterGuiOverlaysEvent e) {
        e.registerAbove(VanillaGuiOverlay.AIR_LEVEL.id(), "thirst", HudOverlay::renderThirst);
        e.registerAboveAll("status", HudOverlay::renderStatus);
    }

    @SubscribeEvent
    public static void onBlockColors(RegisterColorHandlersEvent.Block e) {
        SeasonColors.register(e);
    }

    private ClientSetup() {}
}
