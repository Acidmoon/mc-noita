package com.mcnoita.client.network;

import com.mcnoita.client.player.ClientWandCastHudState;
import com.mcnoita.network.ModNetworking;
import com.mcnoita.network.NoitaNetworkProtocol;
import com.mcnoita.network.WandCastHudSnapshot;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ClientWandCastHudNetworking {
    private ClientWandCastHudNetworking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.WAND_CAST_HUD_SYNC, (client, handler, buf, responseSender) -> {
            WandCastHudSnapshot.read(buf)
                .filter(snapshot -> snapshot.protocolVersion() == NoitaNetworkProtocol.VERSION)
                .ifPresent(snapshot -> client.execute(() -> ClientWandCastHudState.set(
                    snapshot.mode(), snapshot.progressTicks(), snapshot.totalTicks(), snapshot.currentMana(), snapshot.manaMax(),
                    snapshot.wandRevision(), snapshot.catalogEpoch(), snapshot.stateHash(), snapshot.catalogHash()
                )));
        });
    }
}
