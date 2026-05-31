package com.mcnoita.network;

import com.mcnoita.MCNoita;
import com.mcnoita.player.NoitaHoverManager;
import com.mcnoita.wand.NoitaWandCaster;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

public final class ModNetworking {
    public static final Identifier CAST_WAND = MCNoita.id("cast_wand");
    public static final Identifier WAND_CAST_HUD_SYNC = MCNoita.id("wand_cast_hud_sync");
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
            float sidewaysInput = buf.readableBytes() >= Float.BYTES ? buf.readFloat() : 0.0f;
            float forwardInput = buf.readableBytes() >= Float.BYTES ? buf.readFloat() : 0.0f;
            server.execute(() -> NoitaHoverManager.setHoverInput(player, jumpHeld, sidewaysInput, forwardInput));
        });
    }
}
