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
        getState(player).jumpHeld = jumpHeld;
    }

    private static void tickPlayer(ServerPlayerEntity player) {
        HoverState state = getState(player);
        int before = state.energy;

        if (player.isCreative() || player.isSpectator()) {
            state.energy = MAX_ENERGY;
            state.jumpHeldTicks = 0;
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
            applyHoverVelocity(player);
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

    private static void applyHoverVelocity(ServerPlayerEntity player) {
        Vec3d velocity = player.getVelocity();
        player.setVelocity(velocity.x, HOVER_UPWARD_SPEED, velocity.z);
        player.velocityModified = true;
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
    }
}
