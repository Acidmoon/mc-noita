package com.mcnoita.client.network;

import com.mcnoita.client.player.ClientHoverEnergy;
import com.mcnoita.network.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ClientHoverNetworking {
    private ClientHoverNetworking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.HOVER_SYNC, (client, handler, buf, responseSender) -> {
            int energy = buf.readVarInt();
            int maxEnergy = buf.readVarInt();
            client.execute(() -> ClientHoverEnergy.set(energy, maxEnergy));
        });
    }
}
