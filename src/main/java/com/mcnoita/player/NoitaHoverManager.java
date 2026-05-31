package com.mcnoita.player;

import com.mcnoita.network.ModNetworking;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class NoitaHoverManager {
    public static final int MAX_ENERGY = 1000;

    private static final int HOVER_DRAIN_PER_TICK = 12;
    private static final int GROUND_RECHARGE_PER_TICK = MAX_ENERGY * 80 / 100 / 20;
    private static final int AIR_RECHARGE_PER_TICK = 2;
    private static final int HOVER_GRACE_TICKS = 2;
    private static final double HOVER_UPWARD_SPEED = 0.36;
    private static final double HOVER_HORIZONTAL_SPEED = 0.22;
    private static final double HOVER_HORIZONTAL_RESPONSE = 0.65;
    private static final double HOVER_SPRINT_MULTIPLIER = 1.3;

    private static final Map<UUID, HoverState> STATES = new HashMap<>();

    private NoitaHoverManager() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Set<UUID> activePlayers = new HashSet<>();
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                activePlayers.add(player.getUuid());
                tickPlayer(player);
            }
            STATES.keySet().removeIf(uuid -> !activePlayers.contains(uuid));
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
            !(entity instanceof ServerPlayerEntity && source.isOf(DamageTypes.FALL))
        );
    }

    public static void setJumpHeld(ServerPlayerEntity player, boolean jumpHeld) {
        setHoverInput(player, jumpHeld, 0.0f, 0.0f);
    }

    public static void setHoverInput(ServerPlayerEntity player, boolean jumpHeld, float sidewaysInput, float forwardInput) {
        HoverState state = getState(player);
        state.jumpHeld = jumpHeld;
        state.sidewaysInput = sanitizeMovementInput(sidewaysInput);
        state.forwardInput = sanitizeMovementInput(forwardInput);
    }

    private static void tickPlayer(ServerPlayerEntity player) {
        HoverState state = getState(player);
        int before = state.energy;

        if (player.isCreative() || player.isSpectator()) {
            state.energy = MAX_ENERGY;
            state.jumpHeldTicks = 0;
            state.sidewaysInput = 0.0f;
            state.forwardInput = 0.0f;
            syncIfChanged(player, before, state.energy);
            return;
        }

        if (state.jumpHeld) {
            state.jumpHeldTicks++;
        } else {
            state.jumpHeldTicks = 0;
        }

        boolean onGround = player.isOnGround();
        boolean canHover = state.jumpHeld && state.jumpHeldTicks > HOVER_GRACE_TICKS && !onGround && state.energy > 0;
        if (canHover) {
            state.energy = Math.max(0, state.energy - HOVER_DRAIN_PER_TICK);
            applyHoverVelocity(player, state);
        } else if (onGround) {
            state.energy = Math.min(MAX_ENERGY, state.energy + GROUND_RECHARGE_PER_TICK);
        } else if (!state.jumpHeld) {
            state.energy = Math.min(MAX_ENERGY, state.energy + AIR_RECHARGE_PER_TICK);
        }

        if (state.energy > 0 || canHover) {
            player.fallDistance = 0.0f;
        }

        syncIfChanged(player, before, state.energy);
    }

    private static void applyHoverVelocity(ServerPlayerEntity player, HoverState state) {
        Vec3d velocity = player.getVelocity();
        Vec3d horizontalVelocity = getCreativeLikeHorizontalVelocity(
            player.getYaw(),
            player.isSprinting(),
            velocity,
            state.sidewaysInput,
            state.forwardInput
        );
        player.setVelocity(horizontalVelocity.x, HOVER_UPWARD_SPEED, horizontalVelocity.z);
        player.velocityModified = true;
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

    private static HoverState getState(ServerPlayerEntity player) {
        return STATES.computeIfAbsent(player.getUuid(), uuid -> new HoverState());
    }

    private static void syncIfChanged(ServerPlayerEntity player, int before, int after) {
        if (before == after && player.age % 20 != 0) {
            return;
        }

        net.minecraft.network.PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(after);
        buf.writeVarInt(MAX_ENERGY);
        ServerPlayNetworking.send(player, ModNetworking.HOVER_SYNC, buf);
    }

    private static final class HoverState {
        private boolean jumpHeld;
        private int jumpHeldTicks;
        private int energy = MAX_ENERGY;
        private float sidewaysInput;
        private float forwardInput;
    }
}
