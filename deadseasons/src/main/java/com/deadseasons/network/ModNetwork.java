package com.deadseasons.network;

import com.deadseasons.DeadSeasons;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            DeadSeasons.id("main"), () -> VERSION, VERSION::equals, VERSION::equals);

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(StatusPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(StatusPacket::encode)
                .decoder(StatusPacket::decode)
                .consumerMainThread(StatusPacket::handle)
                .add();
        CHANNEL.messageBuilder(ReloadPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ReloadPacket::encode)
                .decoder(ReloadPacket::decode)
                .consumerMainThread(ReloadPacket::handle)
                .add();
    }

    public static void sendTo(ServerPlayer player, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    private ModNetwork() {}
}
