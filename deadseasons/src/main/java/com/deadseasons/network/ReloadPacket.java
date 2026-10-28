package com.deadseasons.network;

import com.deadseasons.item.GunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: the player pressed the reload key (R). */
public record ReloadPacket() {
    public static void encode(ReloadPacket p, FriendlyByteBuf buf) {
    }

    public static ReloadPacket decode(FriendlyByteBuf buf) {
        return new ReloadPacket();
    }

    public static void handle(ReloadPacket p, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof GunItem) {
            GunItem.tryReload(player, main);
            return;
        }
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof GunItem) GunItem.tryReload(player, off);
    }
}
