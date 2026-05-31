package com.mcnoita.client.network;

import com.mcnoita.client.player.ClientHoverEnergy;
import com.mcnoita.network.ModNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.Vec3d;

public final class ClientHoverInputEvents {
    private static final int HOVER_GRACE_TICKS = 2;
    private static final double HOVER_UPWARD_SPEED = 0.36;
    private static final double HOVER_HORIZONTAL_SPEED = 0.22;
    private static final double HOVER_HORIZONTAL_RESPONSE = 0.65;
    private static final double HOVER_SPRINT_MULTIPLIER = 1.3;

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

        boolean acceptsInput = client.currentScreen == null;
        boolean jumpHeld = acceptsInput && client.options.jumpKey.isPressed();
        float sidewaysInput = acceptsInput ? sanitizeMovementInput(client.player.input.movementSideways) : 0.0f;
        float forwardInput = acceptsInput ? sanitizeMovementInput(client.player.input.movementForward) : 0.0f;
        if (jumpHeld) {
            jumpHeldTicks++;
        } else {
            jumpHeldTicks = 0;
        }

        if (jumpHeld && jumpHeldTicks > HOVER_GRACE_TICKS && !client.player.isOnGround() && ClientHoverEnergy.hasEnergy()) {
            applyHoverVelocity(client.player, sidewaysInput, forwardInput);
        }

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(jumpHeld);
        buf.writeFloat(sidewaysInput);
        buf.writeFloat(forwardInput);
        ClientPlayNetworking.send(ModNetworking.HOVER_INPUT, buf);
    }

    private static void applyHoverVelocity(ClientPlayerEntity player, float sidewaysInput, float forwardInput) {
        Vec3d velocity = player.getVelocity();
        Vec3d horizontalVelocity = getCreativeLikeHorizontalVelocity(
            player.getYaw(),
            player.isSprinting(),
            velocity,
            sidewaysInput,
            forwardInput
        );
        player.setVelocity(horizontalVelocity.x, HOVER_UPWARD_SPEED, horizontalVelocity.z);
        player.fallDistance = 0.0f;
    }

    private static Vec3d getCreativeLikeHorizontalVelocity(float yaw, boolean sprinting, Vec3d velocity, float sidewaysInput, float forwardInput) {
        Vec3d direction = getInputDirection(yaw, sidewaysInput, forwardInput);
        if (direction.lengthSquared() <= 1.0E-7) {
            return new Vec3d(velocity.x, 0.0, velocity.z);
        }

        double speed = HOVER_HORIZONTAL_SPEED * (sprinting ? HOVER_SPRINT_MULTIPLIER : 1.0);
        double targetX = direction.x * speed;
        double targetZ = direction.z * speed;
        return new Vec3d(
            lerp(velocity.x, targetX, HOVER_HORIZONTAL_RESPONSE),
            0.0,
            lerp(velocity.z, targetZ, HOVER_HORIZONTAL_RESPONSE)
        );
    }

    private static Vec3d getInputDirection(float yaw, float sidewaysInput, float forwardInput) {
        double sideways = sidewaysInput;
        double forward = forwardInput;
        double lengthSquared = sideways * sideways + forward * forward;
        if (lengthSquared <= 1.0E-7) {
            return Vec3d.ZERO;
        }

        if (lengthSquared > 1.0) {
            double length = Math.sqrt(lengthSquared);
            sideways /= length;
            forward /= length;
        }

        double yawRadians = Math.toRadians(yaw);
        double sin = Math.sin(yawRadians);
        double cos = Math.cos(yawRadians);
        return new Vec3d(sideways * cos - forward * sin, 0.0, forward * cos + sideways * sin);
    }

    private static float sanitizeMovementInput(float input) {
        return Math.max(-1.0f, Math.min(1.0f, input));
    }

    private static double lerp(double start, double end, double delta) {
        return start + (end - start) * delta;
    }
}
