package com.mcnoita.world.mutation;

import com.mcnoita.spell.NoitaExecutionIdentity;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

/**
 * Immutable authority binding carried into every server world operation. The
 * owner UUID remains meaningful after an entity reload, while the resolved
 * player is deliberately re-read immediately before a destructive operation.
 */
public record WorldMutationContext(
    ServerWorld world,
    Entity source,
    UUID ownerUuid,
    RegistryKey<World> expectedDimension,
    NoitaExecutionIdentity executionIdentity,
    WorldMutationBudget budget
) {
    public WorldMutationContext {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(expectedDimension, "expectedDimension");
        Objects.requireNonNull(executionIdentity, "executionIdentity");
        budget = budget == null ? WorldMutationBudget.UNTRACKED : budget;
    }

    public static Optional<WorldMutationContext> forEntity(
        Entity source, Entity owner, UUID ownerUuid, NoitaExecutionIdentity executionIdentity
    ) {
        if (!(source.getWorld() instanceof ServerWorld world)) {
            return Optional.empty();
        }
        UUID resolvedOwnerUuid = ownerUuid != null ? ownerUuid : owner == null ? null : owner.getUuid();
        return Optional.of(new WorldMutationContext(world, source, resolvedOwnerUuid, world.getRegistryKey(), executionIdentity,
            WorldMutationBudget.UNTRACKED));
    }

    public static Optional<WorldMutationContext> forPayload(
        World world, Entity owner, NoitaExecutionIdentity executionIdentity
    ) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return Optional.empty();
        }
        return Optional.of(new WorldMutationContext(serverWorld, null, owner == null ? null : owner.getUuid(),
            serverWorld.getRegistryKey(), executionIdentity, WorldMutationBudget.UNTRACKED));
    }

    public WorldMutationContext withBudget(WorldMutationBudget nextBudget) {
        return new WorldMutationContext(world, source, ownerUuid, expectedDimension, executionIdentity, nextBudget);
    }

    /** Returns null when the player logged out, died, changed dimension, or was never a player owner. */
    public ServerPlayerEntity resolveOnlineOwner() {
        if (ownerUuid == null) {
            return null;
        }
        ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(ownerUuid);
        if (player == null || player.isRemoved() || !player.isAlive() || player.getServerWorld() != world) {
            return null;
        }
        return player;
    }
}
