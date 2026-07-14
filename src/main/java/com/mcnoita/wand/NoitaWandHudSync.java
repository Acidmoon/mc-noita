package com.mcnoita.wand;

import com.mcnoita.catalog.CatalogSnapshot;
import com.mcnoita.catalog.SpellCatalogService;
import com.mcnoita.item.NoitaWandItem;
import com.mcnoita.network.ModNetworking;
import com.mcnoita.network.NoitaNetworkProtocol;
import com.mcnoita.network.WandCastHudSnapshot;
import com.mcnoita.wand.adapter.MinecraftWandAdapter;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.item.ItemStack;
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
        CatalogSnapshot snapshot = SpellCatalogService.getInstance().current();
        ItemStack held = player.getMainHandStack();
        long revision = held.getItem() instanceof NoitaWandItem ? MinecraftWandAdapter.stateRevisionReadOnly(held) : 0L;
        int stateHash = held.getItem() instanceof NoitaWandItem ? NoitaNetworkProtocol.wandStateHash(held) : 0;
        int totalTicks = Math.max(1, state.totalTicks());
        int progressTicks = Math.max(0, Math.min(totalTicks, state.progressTicks()));
        int manaMax = Math.max(0, state.manaMax());
        float currentMana = Float.isFinite(state.currentMana())
            ? Math.max(0.0f, Math.min(manaMax, state.currentMana()))
            : 0.0f;
        PacketByteBuf buf = PacketByteBufs.create();
        new WandCastHudSnapshot(
            NoitaNetworkProtocol.VERSION, state.mode(), progressTicks, totalTicks, currentMana, manaMax,
            revision, snapshot.epoch(), stateHash, snapshot.hash()
        ).write(buf);
        ServerPlayNetworking.send(player, ModNetworking.WAND_CAST_HUD_SYNC, buf);
    }
}
