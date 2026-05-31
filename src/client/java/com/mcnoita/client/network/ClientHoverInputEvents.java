package com.mcnoita.client.network;

import com.mcnoita.client.player.ClientHoverEnergy;
import com.mcnoita.network.ModNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.Vec3d;

public final class ClientHoverInputEvents {
    private static final int HOVER_GRACE_TICKS = 2;
    private static final double HOVER_UPWARD_SPEED = 0.36;

    private static int jumpHeldTicks;

    private ClientHoverInputEvents() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(ClientHoverInputEvents::handleClientTick);
    }

    private static void handleClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }

        boolean jumpHeld = client.currentScreen == null && client.options.jumpKey.isPressed();
        if (jumpHeld) {
            jumpHeldTicks++;
        } else {
            jumpHeldTicks = 0;
        }

        if (jumpHeld && jumpHeldTicks > HOVER_GRACE_TICKS && !client.player.isOnGround() && ClientHoverEnergy.hasEnergy()) {
            Vec3d velocity = client.player.getVelocity();
            client.player.setVelocity(velocity.x, HOVER_UPWARD_SPEED, velocity.z);
            client.player.fallDistance = 0.0f;
        }

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(jumpHeld);
        ClientPlayNetworking.send(ModNetworking.HOVER_INPUT, buf);
    }
}
