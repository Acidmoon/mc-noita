package com.mcnoita.network;

import com.mcnoita.MCNoita;
import com.mcnoita.wand.NoitaWandCaster;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

public final class ModNetworking {
    public static final Identifier CAST_WAND = MCNoita.id("cast_wand");

    private ModNetworking() {
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(CAST_WAND, (server, player, handler, buf, responseSender) ->
            server.execute(() -> NoitaWandCaster.cast(player))
        );
    }
}
