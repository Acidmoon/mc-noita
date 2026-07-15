package com.mcnoita.network;

import com.mcnoita.MCNoita;
import com.mcnoita.catalog.CatalogSnapshot;
import com.mcnoita.catalog.SpellCatalogService;
import com.mcnoita.item.NoitaWandItem;
import com.mcnoita.player.NoitaHoverManager;
import com.mcnoita.wand.NoitaWandCaster;
import com.mcnoita.wand.adapter.MinecraftWandAdapter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

public final class ModNetworking {
    public static final Identifier CAST_WAND = MCNoita.id("cast_wand");
    public static final Identifier WAND_CAST_HUD_SYNC = MCNoita.id("wand_cast_hud_sync");
    public static final Identifier HOVER_INPUT = MCNoita.id("hover_input");
    public static final Identifier HOVER_SYNC = MCNoita.id("hover_sync");
    private static final NoitaRequestGuard REQUEST_GUARD = new NoitaRequestGuard();

    private ModNetworking() {
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(CAST_WAND, (server, player, handler, buf, responseSender) -> {
            WandCastRequest.read(buf).ifPresent(request -> server.execute(() -> handleCastRequest(player, request)));
        });
        ServerPlayNetworking.registerGlobalReceiver(HOVER_INPUT, (server, player, handler, buf, responseSender) -> {
            HoverInputRequest.read(buf).ifPresent(request -> server.execute(() -> handleHoverInput(player, request)));
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> REQUEST_GUARD.clear(handler.player.getUuid()));
    }

    private static void handleCastRequest(ServerPlayerEntity player, WandCastRequest request) {
        if (!isValidCastEnvelope(request, player.getInventory().selectedSlot)
            || !REQUEST_GUARD.acceptCast(player.getUuid(), request.sequence(), System.nanoTime())) {
            return;
        }

        ItemStack heldStack = player.getMainHandStack();
        CatalogSnapshot snapshot;
        try {
            snapshot = SpellCatalogService.getInstance().current();
        } catch (IllegalStateException unavailable) {
            return;
        }
        if (!(heldStack.getItem() instanceof NoitaWandItem)
            || request.stateHash() != NoitaNetworkProtocol.wandStateHash(heldStack)
            || request.wandRevision() != MinecraftWandAdapter.stateRevisionReadOnly(heldStack)
            || request.catalogEpoch() != snapshot.epoch()
            || !request.catalogHash().equals(snapshot.hash())
            || !NoitaWandCaster.canCast(player)) {
            return;
        }
        NoitaWandCaster.cast(
            player, request.sequence(), request.stateHash(), request.wandRevision(), request.catalogEpoch(), request.catalogHash()
        );
    }

    /** Packet-only preflight kept separate so malformed hand/slot claims never consume a request sequence. */
    static boolean isValidCastEnvelope(WandCastRequest request, int selectedSlot) {
        return request != null
            && request.protocolVersion() == NoitaNetworkProtocol.VERSION
            && request.hand() == Hand.MAIN_HAND
            && request.slot() == selectedSlot;
    }

    private static void handleHoverInput(ServerPlayerEntity player, HoverInputRequest request) {
        if (request.protocolVersion() != NoitaNetworkProtocol.VERSION
            || !REQUEST_GUARD.acceptHover(player.getUuid(), request.sequence(), System.nanoTime())) {
            return;
        }
        NoitaHoverManager.setHoverInput(player, request.jumpHeld(), request.sidewaysInput(), request.forwardInput());
    }
}
