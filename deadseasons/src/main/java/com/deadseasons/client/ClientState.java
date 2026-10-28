package com.deadseasons.client;

import com.deadseasons.network.StatusPacket;
import net.minecraft.client.Minecraft;

/** Latest survival state from the server, used by the HUD and seasonal visuals. Client only. */
public final class ClientState {
    public static boolean received = false;
    public static float thirst = 20f;
    public static float temperature = 0f;
    public static long infectionTicks = -1;
    public static long day = 0;
    public static int season = 0;
    public static int daysLeftInSeason = 35;
    public static boolean bloodMoon = false;
    public static boolean fog = false;
    public static int daysUntilBloodMoon = 59;
    public static boolean thirstOn = true;
    public static boolean tempOn = true;

    /** 0..1 fade for fog / blood moon visuals. */
    public static float fogFade = 0f;
    public static float bloodFade = 0f;

    public static void apply(StatusPacket p) {
        int oldSeason = season;
        boolean first = !received;
        received = true;
        thirst = p.thirst();
        temperature = p.temperature();
        infectionTicks = p.infectionTicks();
        day = p.day();
        season = p.season();
        daysLeftInSeason = p.daysLeftInSeason();
        bloodMoon = p.bloodMoon();
        fog = p.fog();
        daysUntilBloodMoon = p.daysUntilBloodMoon();
        thirstOn = p.thirstOn();
        tempOn = p.tempOn();
        if (first || oldSeason != season) {
            // Re-tint grass and leaves for the new season
            Minecraft mc = Minecraft.getInstance();
            if (mc.levelRenderer != null) mc.levelRenderer.allChanged();
        }
    }

    public static void reset() {
        received = false;
        fogFade = 0f;
        bloodFade = 0f;
        bloodMoon = false;
        fog = false;
    }

    private ClientState() {}
}
