package com.mcnoita.client.network;

import com.mcnoita.client.player.ClientWandCastHudState;
import com.mcnoita.network.ModNetworking;
import com.mcnoita.network.NoitaNetworkProtocol;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ClientWandCastHudNetworking {
    private ClientWandCastHudNetworking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.WAND_CAST_HUD_SYNC, (client, handler, buf, responseSender) -> {
            if (buf.readableBytes() < 1 || buf.readableBytes() > NoitaNetworkProtocol.MAX_HUD_PACKET_BYTES) {
                return;
            }
            try {
                int version = buf.readVarInt();
                int mode = buf.readVarInt();
                int progressTicks = buf.readVarInt();
                int totalTicks = buf.readVarInt();
                float currentMana = buf.readFloat();
                int manaMax = buf.readVarInt();
                if (version != NoitaNetworkProtocol.VERSION || buf.isReadable() || !Float.isFinite(currentMana)) {
                    return;
                }
                client.execute(() -> ClientWandCastHudState.set(mode, progressTicks, totalTicks, currentMana, manaMax));
            } catch (IndexOutOfBoundsException | IllegalArgumentException ignored) {
            }
        });
    }
}
