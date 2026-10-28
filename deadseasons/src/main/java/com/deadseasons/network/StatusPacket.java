package com.deadseasons.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client once per second: everything the HUD and seasonal visuals need. */
public record StatusPacket(float thirst, float temperature, long infectionTicks, long day, int season, int daysLeftInSeason,
                           boolean bloodMoon, boolean fog, int daysUntilBloodMoon, boolean thirstOn, boolean tempOn) {

    public static void encode(StatusPacket p, FriendlyByteBuf buf) {
        buf.writeFloat(p.thirst);
        buf.writeFloat(p.temperature);
        buf.writeLong(p.infectionTicks);
        buf.writeLong(p.day);
        buf.writeVarInt(p.season);
        buf.writeVarInt(p.daysLeftInSeason);
        buf.writeBoolean(p.bloodMoon);
        buf.writeBoolean(p.fog);
        buf.writeVarInt(p.daysUntilBloodMoon);
        buf.writeBoolean(p.thirstOn);
        buf.writeBoolean(p.tempOn);
    }

    public static StatusPacket decode(FriendlyByteBuf buf) {
        return new StatusPacket(buf.readFloat(), buf.readFloat(), buf.readLong(), buf.readLong(), buf.readVarInt(),
                buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(StatusPacket p, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.deadseasons.client.ClientState.apply(p));
    }
}
