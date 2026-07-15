package com.mcnoita.world.mutation;

import com.mcnoita.spell.NoitaExecutionIdentity;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
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
        // Spell runtime contexts must fail closed when an integration forgets
        // to supply a centrally accountable budget bridge. Temporary-light is
        // the sole explicit caller allowed to pass UNTRACKED deliberately.
        budget = budget == null ? WorldMutationBudget.DENIED : budget;
    }

    public static Optional<WorldMutationContext> forEntity(
        Entity source, Entity owner, UUID ownerUuid, NoitaExecutionIdentity executionIdentity
    ) {
        if (!(source.getWorld() instanceof ServerWorld world)) {
            return Optional.empty();
        }
        Objects.requireNonNull(executionIdentity, "executionIdentity");
        UUID resolvedOwnerUuid = ownerUuid != null ? ownerUuid : owner == null ? null : owner.getUuid();
        WorldMutationBudget budget = executionIdentity.isBound()
            ? ServerWorldMutationBudget.forEntity(world, source, resolvedOwnerUuid, executionIdentity)
            : WorldMutationBudget.DENIED;
        return Optional.of(new WorldMutationContext(world, source, resolvedOwnerUuid, world.getRegistryKey(), executionIdentity,
            budget));
    }

    public static Optional<WorldMutationContext> forPayload(
        World world, Entity owner, NoitaExecutionIdentity executionIdentity
    ) {
        return forPayload(world, owner, executionIdentity, owner == null ? Vec3d.ZERO : owner.getPos());
    }

    /** Payload creation supplies its immediate spawn position for chunk-scoped charging. */
    public static Optional<WorldMutationContext> forPayload(
        World world, Entity owner, NoitaExecutionIdentity executionIdentity, Vec3d position
    ) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return Optional.empty();
        }
        Objects.requireNonNull(executionIdentity, "executionIdentity");
        Objects.requireNonNull(position, "position");
        UUID ownerUuid = owner == null ? null : owner.getUuid();
        WorldMutationBudget budget = executionIdentity.isBound()
            ? ServerWorldMutationBudget.forPayload(serverWorld, ownerUuid, executionIdentity, position)
            : WorldMutationBudget.DENIED;
        return Optional.of(new WorldMutationContext(serverWorld, owner, ownerUuid,
            serverWorld.getRegistryKey(), executionIdentity, budget));
    }

    /**
     * Temporary light is cosmetic, short-lived state rather than terrain
     * destruction. It still carries the loaded-chunk, border, adapter, and
     * budget checks enforced by the world-mutation boundary.
     */
    public static WorldMutationContext forTemporaryLight(ServerWorld world) {
        Objects.requireNonNull(world, "world");
        return new WorldMutationContext(world, null, null, world.getRegistryKey(),
            NoitaExecutionIdentity.unbound("temporary-light"), WorldMutationBudget.UNTRACKED);
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
        if (player == null || player.isRemoved() || !player.isAlive() || player.isSpectator() || player.getServerWorld() != world) {
            return null;
        }
        return player;
    }
}
