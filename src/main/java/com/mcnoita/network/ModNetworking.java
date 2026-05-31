package com.mcnoita.network;

import com.mcnoita.MCNoita;
import com.mcnoita.player.NoitaHoverManager;
import com.mcnoita.wand.NoitaWandCaster;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

public final class ModNetworking {
    public static final Identifier CAST_WAND = MCNoita.id("cast_wand");
    public static final Identifier HOVER_INPUT = MCNoita.id("hover_input");
    public static final Identifier HOVER_SYNC = MCNoita.id("hover_sync");

    private ModNetworking() {
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(CAST_WAND, (server, player, handler, buf, responseSender) ->
            server.execute(() -> NoitaWandCaster.cast(player))
        );
        ServerPlayNetworking.registerGlobalReceiver(HOVER_INPUT, (server, player, handler, buf, responseSender) -> {
            boolean jumpHeld = buf.readBoolean();
            server.execute(() -> NoitaHoverManager.setJumpHeld(player, jumpHeld));
        });
    }
}
