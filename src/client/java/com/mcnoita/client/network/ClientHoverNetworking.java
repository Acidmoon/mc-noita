package com.mcnoita.client.network;

import com.mcnoita.client.player.ClientHoverEnergy;
import com.mcnoita.network.ModNetworking;
import com.mcnoita.network.NoitaNetworkProtocol;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ClientHoverNetworking {
    private ClientHoverNetworking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.HOVER_SYNC, (client, handler, buf, responseSender) -> {
            if (buf.readableBytes() < 1 || buf.readableBytes() > NoitaNetworkProtocol.MAX_HUD_PACKET_BYTES) {
                return;
            }
            try {
                int version = buf.readVarInt();
                int energy = buf.readVarInt();
                int maxEnergy = buf.readVarInt();
                if (version != NoitaNetworkProtocol.VERSION || buf.isReadable()) {
                    return;
                }
                client.execute(() -> ClientHoverEnergy.set(energy, maxEnergy));
            } catch (IndexOutOfBoundsException | IllegalArgumentException ignored) {
            }
        });
    }
}
