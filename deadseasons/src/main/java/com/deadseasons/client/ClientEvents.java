package com.deadseasons.client;

import com.deadseasons.DeadSeasons;
import com.deadseasons.item.GunItem;
import com.deadseasons.network.ModNetwork;
import com.deadseasons.network.ReloadPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DeadSeasons.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        while (ClientSetup.RELOAD.consumeClick()) {
            if (mc.player.getMainHandItem().getItem() instanceof GunItem || mc.player.getOffhandItem().getItem() instanceof GunItem) {
                ModNetwork.CHANNEL.sendToServer(new ReloadPacket());
            }
        }

        boolean overworld = mc.level != null && mc.level.dimension().equals(Level.OVERWORLD);
        float fogTarget = overworld && ClientState.fog ? 1f : 0f;
        float bloodTarget = overworld && ClientState.bloodMoon ? 1f : 0f;
        ClientState.fogFade += Math.signum(fogTarget - ClientState.fogFade) * Math.min(0.01f, Math.abs(fogTarget - ClientState.fogFade));
        ClientState.bloodFade += Math.signum(bloodTarget - ClientState.bloodFade) * Math.min(0.01f, Math.abs(bloodTarget - ClientState.bloodFade));
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        if (event.getType() != FogType.NONE) return;
        float fog = ClientState.fogFade;
        float blood = ClientState.bloodFade;
        if (fog <= 0f && blood <= 0f) return;
        float far = event.getFarPlaneDistance();
        float target = Math.min(far, 64f);
        float targetFog = Math.min(far, 22f);
        float newFar = far;
        if (blood > 0f) newFar = lerp(newFar, target, blood);
        if (fog > 0f) newFar = lerp(newFar, targetFog, fog);
        event.setFarPlaneDistance(newFar);
        event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), newFar * 0.05f));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        float fog = ClientState.fogFade;
        float blood = ClientState.bloodFade;
        if (blood > 0f) {
            event.setRed(lerp(event.getRed(), 0.45f, blood));
            event.setGreen(lerp(event.getGreen(), 0.02f, blood));
            event.setBlue(lerp(event.getBlue(), 0.02f, blood));
        }
        if (fog > 0f) {
            event.setRed(lerp(event.getRed(), 0.55f, fog * 0.8f));
            event.setGreen(lerp(event.getGreen(), 0.57f, fog * 0.8f));
            event.setBlue(lerp(event.getBlue(), 0.6f, fog * 0.8f));
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientState.reset();
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private ClientEvents() {}
}
