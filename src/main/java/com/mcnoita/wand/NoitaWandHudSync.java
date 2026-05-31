package com.mcnoita.wand;

import com.mcnoita.network.ModNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public final class NoitaWandHudSync {
    private NoitaWandHudSync() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                sync(player);
            }
        });
    }

    private static void sync(ServerPlayerEntity player) {
        NoitaWandCaster.CastHudState state = NoitaWandCaster.getHudState(player);
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(state.mode());
        buf.writeVarInt(state.progressTicks());
        buf.writeVarInt(state.totalTicks());
        ServerPlayNetworking.send(player, ModNetworking.WAND_CAST_HUD_SYNC, buf);
    }
}
