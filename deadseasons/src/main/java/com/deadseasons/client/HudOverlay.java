package com.deadseasons.client;

import com.deadseasons.entity.BoxTruckEntity;
import com.deadseasons.item.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/** Thirst bar above the hunger bar, plus day/season, infection, temperature, ammo and truck info. */
public final class HudOverlay {
    private static final String[] SEASONS = {"summer", "autumn", "winter", "spring"};
    private static final int[] SEASON_COLORS = {0xFFE36B, 0xE08A3A, 0xBFE3FF, 0x8CE36B};

    public static void renderThirst(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || !ClientState.received || !ClientState.thirstOn) return;
        if (!gui.shouldDrawSurvivalElements()) return;
        if (player.getVehicle() instanceof net.minecraft.world.entity.LivingEntity) return; // mount health uses this row

        int right = width / 2 + 91;
        int top = height - gui.rightHeight;
        gui.rightHeight += 10;
        float thirst = ClientState.thirst;
        for (int i = 0; i < 10; i++) {
            int x = right - i * 8 - 9;
            float value = thirst - i * 2;
            drawDrop(g, x, top, value >= 2 ? 2 : value >= 1 ? 1 : 0, thirst < 6 && player.tickCount % 20 < 10);
        }
    }

    private static void drawDrop(GuiGraphics g, int x, int y, int fill, boolean flash) {
        int outline = flash ? 0xFFFFFFFF : 0xFF1B2A4A;
        int full = 0xFF3F8FE8;
        int empty = 0xFF2A3550;
        // droplet outline
        g.fill(x + 3, y, x + 5, y + 1, outline);
        g.fill(x + 2, y + 1, x + 6, y + 2, outline);
        g.fill(x + 1, y + 2, x + 7, y + 8, outline);
        g.fill(x + 2, y + 8, x + 6, y + 9, outline);
        // inside
        g.fill(x + 3, y + 1, x + 5, y + 2, empty);
        g.fill(x + 2, y + 2, x + 6, y + 8, empty);
        if (fill == 2) {
            g.fill(x + 3, y + 1, x + 5, y + 2, full);
            g.fill(x + 2, y + 2, x + 6, y + 8, full);
            g.fill(x + 3, y + 3, x + 4, y + 5, 0xFFBFE0FF);
        } else if (fill == 1) {
            g.fill(x + 2, y + 5, x + 6, y + 8, full);
        }
    }

    public static void renderStatus(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || !ClientState.received) return;
        if (mc.options.renderDebug) return;
        Font font = mc.font;

        // ---- top-left: day and season
        int y = 4;
        int s = Math.floorMod(ClientState.season, 4);
        Component dayLine = Component.translatable("hud.deadseasons.day", ClientState.day + 1,
                Component.translatable("season.deadseasons." + SEASONS[s]), ClientState.daysLeftInSeason);
        g.drawString(font, dayLine, 4, y, SEASON_COLORS[s], true);
        y += 10;
        if (ClientState.bloodMoon) {
            g.drawString(font, Component.translatable("hud.deadseasons.blood_moon"), 4, y, 0xFF3030, true);
            y += 10;
        } else if (ClientState.daysUntilBloodMoon <= 3) {
            Component c = ClientState.daysUntilBloodMoon == 0 ? Component.translatable("hud.deadseasons.blood_moon_tonight")
                    : Component.translatable("hud.deadseasons.blood_moon_in", ClientState.daysUntilBloodMoon);
            g.drawString(font, c, 4, y, 0xD04040, true);
            y += 10;
        }
        if (ClientState.fog) {
            g.drawString(font, Component.translatable("hud.deadseasons.fog"), 4, y, 0xB0B0B0, true);
            y += 10;
        }
        if (ClientState.infectionTicks >= 0) {
            double days = ClientState.infectionTicks / 24000.0;
            int color = player.tickCount % 40 < 20 ? 0x9BCB3C : 0x6E9A22;
            g.drawString(font, Component.translatable("hud.deadseasons.infected", String.format("%.1f", days)), 4, y, color, true);
            y += 10;
        }
        if (player.getVehicle() instanceof BoxTruckEntity truck) {
            g.drawString(font, Component.translatable("hud.deadseasons.truck", Math.round(truck.getFuel()),
                    Math.round(truck.getHealth()), Math.round(truck.getMaxHealth())), 4, y, 0xE0E0E0, true);
            y += 10;
        }

        if (player.isCreative() || player.isSpectator()) return;

        // ---- right of the hotbar: temperature and ammo
        int x = width / 2 + 96;
        if (ClientState.tempOn) {
            float t = ClientState.temperature;
            String key;
            int color;
            if (t <= -8) { key = "freezing"; color = 0x6FB7FF; }
            else if (t <= -5) { key = "cold"; color = 0xA8D8FF; }
            else if (t >= 8) { key = "overheating"; color = 0xFF5A2A; }
            else if (t >= 5) { key = "hot"; color = 0xFFB040; }
            else { key = "comfortable"; color = 0xA0E0A0; }
            g.drawString(font, Component.translatable("hud.deadseasons.temp." + key), x, height - 10, color, true);
            // little thermometer bar
            int barX = x, barY = height - 13, barW = 40;
            g.fill(barX, barY, barX + barW, barY + 2, 0xFF303030);
            int marker = (int) ((t + 10f) / 20f * (barW - 1));
            g.fill(barX + marker, barY - 1, barX + marker + 1, barY + 3, 0xFF000000 | color);
        }
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof GunItem gun) {
            int mag = GunItem.getAmmo(held);
            int reserve = GunItem.countAmmo(player, gun.getStats().ammo().get());
            String text = mag + "/" + gun.getStats().magazine() + "  [" + reserve + "]";
            g.drawString(font, text, x, height - 26, mag == 0 ? 0xFF5050 : 0xFFE36B, true);
        }
    }

    private HudOverlay() {}
}
