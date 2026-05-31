package com.mcnoita.client.network;

import com.mcnoita.client.player.ClientWandCastHudState;
import com.mcnoita.network.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ClientWandCastHudNetworking {
    private ClientWandCastHudNetworking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.WAND_CAST_HUD_SYNC, (client, handler, buf, responseSender) -> {
            int mode = buf.readVarInt();
            int progressTicks = buf.readVarInt();
            int totalTicks = buf.readVarInt();
            client.execute(() -> ClientWandCastHudState.set(mode, progressTicks, totalTicks));
        });
    }
}
