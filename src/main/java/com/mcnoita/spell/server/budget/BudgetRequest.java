package com.mcnoita.spell.server.budget;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable resource footprint for one execution identity. A null owner marks a
 * system-owned job, which is still subject to dimension, chunk, and global caps.
 */
public record BudgetRequest(
    UUID executionId,
    UUID ownerId,
    String dimensionId,
    Map<BudgetKind, Long> costs,
    Map<ChunkBudgetKey, Map<BudgetKind, Long>> chunkCosts
) {
    public BudgetRequest {
        Objects.requireNonNull(executionId, "executionId");
        dimensionId = Objects.requireNonNull(dimensionId, "dimensionId");
        if (dimensionId.isBlank()) {
            throw new IllegalArgumentException("dimensionId must not be blank");
        }
        costs = BudgetValues.copyCosts(costs, "costs");
        chunkCosts = copyChunkCosts(dimensionId, chunkCosts);
        validateChunkTotals(costs, chunkCosts);
    }

    public static Builder builder(UUID executionId, String dimensionId) {
        return new Builder(executionId, dimensionId);
    }

    public long cost(BudgetKind kind) {
        return BudgetValues.amount(costs, kind);
    }

    public boolean hasOwner() {
        return ownerId != null;
    }

    public boolean isEmpty() {
        return costs.isEmpty();
    }

    private static Map<ChunkBudgetKey, Map<BudgetKind, Long>> copyChunkCosts(
        String dimensionId, Map<ChunkBudgetKey, Map<BudgetKind, Long>> values
    ) {
        Objects.requireNonNull(values, "chunkCosts");
        Map<ChunkBudgetKey, Map<BudgetKind, Long>> copy = new LinkedHashMap<>();
        for (Map.Entry<ChunkBudgetKey, Map<BudgetKind, Long>> entry : values.entrySet()) {
            ChunkBudgetKey key = Objects.requireNonNull(entry.getKey(), "chunkCosts key");
            if (!dimensionId.equals(key.dimensionId())) {
                throw new IllegalArgumentException("chunk scope must use the request dimension");
            }
            Map<BudgetKind, Long> chunk = BudgetValues.copyCosts(entry.getValue(), "chunkCosts");
            if (!chunk.isEmpty()) {
                copy.put(key, chunk);
            }
        }
        return Collections.unmodifiableMap(copy);
    }

    private static void validateChunkTotals(
        Map<BudgetKind, Long> total, Map<ChunkBudgetKey, Map<BudgetKind, Long>> chunks
    ) {
        EnumMap<BudgetKind, Long> sum = new EnumMap<>(BudgetKind.class);
        for (Map<BudgetKind, Long> chunk : chunks.values()) {
            BudgetValues.addInto(sum, chunk);
        }
        if (!BudgetValues.fitsWithin(sum, total)) {
            throw new IllegalArgumentException("chunk costs must not exceed total request costs");
        }
    }

    /** Builder avoids accidental omission of a request's total cost for chunk-local work. */
    public static final class Builder {
        private final UUID executionId;
        private final String dimensionId;
        private UUID ownerId;
        private final EnumMap<BudgetKind, Long> costs = new EnumMap<>(BudgetKind.class);
        private final Map<ChunkBudgetKey, EnumMap<BudgetKind, Long>> chunkCosts = new LinkedHashMap<>();

        private Builder(UUID executionId, String dimensionId) {
            this.executionId = Objects.requireNonNull(executionId, "executionId");
            this.dimensionId = Objects.requireNonNull(dimensionId, "dimensionId");
            if (dimensionId.isBlank()) {
                throw new IllegalArgumentException("dimensionId must not be blank");
            }
        }

        public Builder owner(UUID ownerId) {
            this.ownerId = ownerId;
            return this;
        }

        public Builder add(BudgetKind kind, long amount) {
            addTo(costs, kind, amount);
            return this;
        }

        /** Adds the same amount to total and chunk-local accounting. */
        public Builder addInChunk(ChunkBudgetKey chunk, BudgetKind kind, long amount) {
            Objects.requireNonNull(chunk, "chunk");
            if (!dimensionId.equals(chunk.dimensionId())) {
                throw new IllegalArgumentException("chunk scope must use the request dimension");
            }
            addTo(costs, kind, amount);
            addTo(chunkCosts.computeIfAbsent(chunk, ignored -> new EnumMap<>(BudgetKind.class)), kind, amount);
            return this;
        }

        public BudgetRequest build() {
            Map<ChunkBudgetKey, Map<BudgetKind, Long>> copiedChunks = new LinkedHashMap<>();
            for (Map.Entry<ChunkBudgetKey, EnumMap<BudgetKind, Long>> entry : chunkCosts.entrySet()) {
                copiedChunks.put(entry.getKey(), entry.getValue());
            }
            return new BudgetRequest(executionId, ownerId, dimensionId, costs, copiedChunks);
        }

        private static void addTo(EnumMap<BudgetKind, Long> target, BudgetKind kind, long amount) {
            Objects.requireNonNull(kind, "kind");
            if (amount < 0L) {
                throw new IllegalArgumentException("budget amounts must not be negative");
            }
            if (amount == 0L) {
                return;
            }
            long current = target.getOrDefault(kind, 0L);
            if (current > Long.MAX_VALUE - amount) {
                throw new IllegalArgumentException("budget amount overflow for " + kind);
            }
            target.put(kind, current + amount);
        }
    }
}
