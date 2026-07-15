package com.mcnoita.world.mutation;

import com.mcnoita.spell.NoitaExecutionIdentity;
import com.mcnoita.spell.server.budget.BudgetKind;
import com.mcnoita.spell.server.budget.BudgetRequest;
import com.mcnoita.spell.server.budget.BudgetReservation;
import com.mcnoita.spell.server.budget.ChunkBudgetKey;
import com.mcnoita.spell.server.budget.SpellBudgetManager;
import com.mcnoita.spell.server.job.SpellJobServerService;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Charges one live projectile/payload world operation against the same central
 * server ledger used by casts and durable jobs. Root cast reservations are
 * closed after execution, so delayed entities cannot safely reuse their root
 * handle. Each operation therefore creates a fresh derived identity, commits
 * its bounded request, and closes it immediately to retain tick/window spend.
 */
public final class ServerWorldMutationBudget implements WorldMutationBudget {
    private static final AtomicLong NEXT_SEQUENCE = new AtomicLong();
    /**
     * A runtime query has no frozen per-chunk result distribution. Charging
     * its complete result cap to every covered chunk is deliberately
     * conservative, but keeps a boundary-spanning collision from silently
     * evading the chunk ledger or being rejected solely for touching a seam.
     */
    private static final int MAX_RUNTIME_QUERY_CHUNKS = WorldMutationPolicy.MAX_QUERY_CHUNKS;

    private final SpellBudgetManager budgetManager;
    private final UUID ownerId;
    private final String dimensionId;
    private final NoitaExecutionIdentity identity;
    private final LongSupplier worldTime;
    private final LongSupplier serverTick;
    private final IntSupplier chunkX;
    private final IntSupplier chunkZ;

    ServerWorldMutationBudget(
        SpellBudgetManager budgetManager, UUID ownerId, String dimensionId, NoitaExecutionIdentity identity,
        LongSupplier worldTime, LongSupplier serverTick, IntSupplier chunkX, IntSupplier chunkZ
    ) {
        this.budgetManager = Objects.requireNonNull(budgetManager, "budgetManager");
        this.ownerId = ownerId;
        this.dimensionId = Objects.requireNonNull(dimensionId, "dimensionId");
        this.identity = Objects.requireNonNull(identity, "identity");
        this.worldTime = Objects.requireNonNull(worldTime, "worldTime");
        this.serverTick = Objects.requireNonNull(serverTick, "serverTick");
        this.chunkX = Objects.requireNonNull(chunkX, "chunkX");
        this.chunkZ = Objects.requireNonNull(chunkZ, "chunkZ");
    }

    /** Creates a live budget view anchored to the current projectile/entity chunk. */
    static ServerWorldMutationBudget forEntity(
        ServerWorld world, Entity source, UUID ownerId, NoitaExecutionIdentity identity
    ) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(source, "source");
        return new ServerWorldMutationBudget(SpellJobServerService.getInstance().budgetManager(), ownerId,
            world.getRegistryKey().getValue().toString(), identity, world::getTime,
            () -> world.getServer().getTicks(), () -> source.getBlockX() >> 4, () -> source.getBlockZ() >> 4);
    }

    /** Creates a live budget view anchored to the payload's immediate spawn position. */
    static ServerWorldMutationBudget forPayload(
        ServerWorld world, UUID ownerId, NoitaExecutionIdentity identity, Vec3d position
    ) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(position, "position");
        if (!Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)) {
            throw new IllegalArgumentException("payload position must be finite");
        }
        int initialChunkX = ((int) Math.floor(position.x)) >> 4;
        int initialChunkZ = ((int) Math.floor(position.z)) >> 4;
        return new ServerWorldMutationBudget(SpellJobServerService.getInstance().budgetManager(), ownerId,
            world.getRegistryKey().getValue().toString(), identity, world::getTime,
            () -> world.getServer().getTicks(), () -> initialChunkX, () -> initialChunkZ);
    }

    @Override
    public boolean tryReserve(WorldMutationKind kind, int amount) {
        return tryReserveInChunks(kind, amount, chunkX.getAsInt(), chunkX.getAsInt(), chunkZ.getAsInt(), chunkZ.getAsInt());
    }

    @Override
    public boolean tryReserveAt(WorldMutationKind kind, int amount, BlockPos position) {
        Objects.requireNonNull(position, "position");
        int targetChunkX = position.getX() >> 4;
        int targetChunkZ = position.getZ() >> 4;
        return tryReserveInChunks(kind, amount, targetChunkX, targetChunkX, targetChunkZ, targetChunkZ);
    }

    @Override
    public boolean tryReserveIn(WorldMutationKind kind, int amount, Box area) {
        Objects.requireNonNull(area, "area");
        if (!Double.isFinite(area.minX) || !Double.isFinite(area.maxX)
            || !Double.isFinite(area.minZ) || !Double.isFinite(area.maxZ)) {
            return false;
        }
        WorldMutationPolicy.ChunkEnvelope envelope = WorldMutationPolicy.chunkEnvelope(area).orElse(null);
        return envelope != null && tryReserveInChunks(kind, amount, envelope.minChunkX(), envelope.maxChunkX(),
            envelope.minChunkZ(), envelope.maxChunkZ());
    }

    private boolean tryReserveInChunks(
        WorldMutationKind kind, int amount, int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ
    ) {
        Objects.requireNonNull(kind, "kind");
        if (!identity.isBound() || amount < 0) {
            return false;
        }
        if (amount == 0) {
            return true;
        }

        long width = (long) maxChunkX - minChunkX + 1L;
        long depth = (long) maxChunkZ - minChunkZ + 1L;
        if (width < 1L || depth < 1L || width > MAX_RUNTIME_QUERY_CHUNKS
            || depth > MAX_RUNTIME_QUERY_CHUNKS || width > MAX_RUNTIME_QUERY_CHUNKS / depth) {
            return false;
        }

        long observedWorldTime = worldTime.getAsLong();
        long observedServerTick = serverTick.getAsLong();
        long sequence = NEXT_SEQUENCE.getAndIncrement();
        UUID operationId = deriveOperationId(identity, observedWorldTime, kind, sequence);
        BudgetRequest.Builder request = BudgetRequest.builder(operationId, dimensionId)
            // Each chunk can independently contain the query's entire result
            // cap. Reserve that cap atomically for every covered chunk rather
            // than committing a partial request or charging only the source.
            ;
        for (long chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (long chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                request.addInChunk(new ChunkBudgetKey(dimensionId, (int) chunkX, (int) chunkZ), budgetKind(kind), amount);
            }
        }
        if (ownerId != null) {
            request.owner(ownerId);
        }

        try {
            SpellBudgetManager.ReservationAttempt attempt = budgetManager.reserve(request.build(), observedServerTick);
            if (!attempt.accepted()) {
                return false;
            }
            BudgetReservation reservation = attempt.reservation();
            try {
                return reservation.commit();
            } finally {
                reservation.close();
            }
        } catch (IllegalArgumentException | IllegalStateException failure) {
            // World effects must fail closed when a stale world clock or a
            // malformed runtime identity cannot be charged centrally.
            return false;
        }
    }

    static BudgetKind budgetKind(WorldMutationKind kind) {
        return switch (Objects.requireNonNull(kind, "kind")) {
            case BLOCK_CHECK -> BudgetKind.BLOCK_CHECKS;
            case BLOCK_MUTATION -> BudgetKind.BLOCK_MUTATIONS;
            case ENTITY_QUERY, EXPLOSION -> BudgetKind.ENTITY_SCANS;
            case ENTITY_SPAWN -> BudgetKind.AUTHORITATIVE_ENTITIES;
        };
    }

    /** Fresh operation identities prevent a closed earlier charge from masking a later action in the same tick. */
    static UUID deriveOperationId(NoitaExecutionIdentity identity, long worldTime, WorldMutationKind kind, long sequence) {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(kind, "kind");
        String key = "mc-noita/world-mutation-budget/v1/" + identity.executionId() + '/' + identity.nodePath() + '/'
            + identity.catalogEpoch() + '/' + identity.catalogHash() + '/' + worldTime + '/' + kind.name() + '/' + sequence;
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }
}
