package com.mcnoita.client.network;

import com.mcnoita.item.NoitaWandItem;
import com.mcnoita.network.ModNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;

public final class ClientWandCastEvents {
    private ClientWandCastEvents() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(ClientWandCastEvents::handleClientTick);
    }

    private static void handleClientTick(MinecraftClient client) {
        boolean isAttackPressed = client.options.attackKey.isPressed();
        if (client.player != null
            && client.world != null
            && client.currentScreen == null
            && isAttackPressed
            && client.player.getStackInHand(Hand.MAIN_HAND).getItem() instanceof NoitaWandItem) {
            ClientPlayNetworking.send(ModNetworking.CAST_WAND, PacketByteBufs.empty());
        }
    }
}
