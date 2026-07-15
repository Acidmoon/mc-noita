package com.mcnoita.spell.exec;

import com.mcnoita.spell.plan.EffectNode;
import com.mcnoita.spell.server.budget.BudgetKind;
import com.mcnoita.spell.server.budget.BudgetRequest;
import com.mcnoita.spell.server.budget.ChunkBudgetKey;
import com.mcnoita.wand.server.PlanBudgetRequestFactory;
import com.mcnoita.world.mutation.WorldMutationBudget;
import com.mcnoita.world.mutation.WorldMutationBudgetCounter;
import com.mcnoita.world.mutation.WorldMutationPolicy;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/**
 * Partitions a frozen root reservation among immediate typed world nodes. A
 * rejected node's local slice is never reused by a later node after the
 * transaction releases that node's central unused slice.
 */
final class ImmediateWorldBudgetAllocator {
    static final WorldMutationBudget DENIED = (kind, amount) -> amount == 0;

    private ImmediateWorldBudgetAllocator() {
    }

    static Map<String, WorldMutationBudget> allocate(
        BudgetRequest root, List<EffectNode> nodes, Map<String, ChunkBudgetKey> synchronousNodeChunks
    ) {
        return allocate(root, nodes, synchronousNodeChunks, Map.of());
    }

    /** Allocates explicit cross-chunk entity-query admission frozen before the transaction commits. */
    static Map<String, WorldMutationBudget> allocate(
        BudgetRequest root, List<EffectNode> nodes, Map<String, ChunkBudgetKey> synchronousNodeChunks,
        Map<String, Set<ChunkBudgetKey>> synchronousEntityQueryChunks
    ) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(nodes, "nodes");
        synchronousNodeChunks = Map.copyOf(Objects.requireNonNull(synchronousNodeChunks, "synchronousNodeChunks"));
        synchronousEntityQueryChunks = Map.copyOf(Objects.requireNonNull(synchronousEntityQueryChunks,
            "synchronousEntityQueryChunks"));
        EnumMap<BudgetKind, Long> remaining = new EnumMap<>(BudgetKind.class);
        remaining.putAll(root.costs());
        Map<String, WorldMutationBudget> allocations = new LinkedHashMap<>();
        for (EffectNode node : nodes) {
            Map<BudgetKind, Long> costs = PlanBudgetRequestFactory.immediateWorldCosts(node);
            if (!fitsWithin(costs, remaining)) {
                allocations.put(node.nodePath(), DENIED);
                continue;
            }
            subtract(remaining, costs);
            WorldMutationBudget slice = WorldMutationBudgetCounter.fromBudgetCosts(costs);
            allocations.put(node.nodePath(), bindToReservedChunk(root, slice, synchronousNodeChunks.get(node.nodePath()),
                synchronousEntityQueryChunks.get(node.nodePath())));
        }
        return Map.copyOf(allocations);
    }

    static Map<String, WorldMutationBudget> allocate(BudgetRequest root, List<EffectNode> nodes) {
        return allocate(root, nodes, Map.of());
    }

    /** Immediate nodes may only use their primary chunk, except frozen explosion entity-query envelopes. */
    private static WorldMutationBudget bindToReservedChunk(
        BudgetRequest root, WorldMutationBudget slice, ChunkBudgetKey expectedChunk,
        Set<ChunkBudgetKey> entityQueryChunks
    ) {
        ChunkBudgetKey chunk = expectedChunk;
        if (chunk == null && root.chunkCosts().size() == 1) {
            chunk = root.chunkCosts().keySet().iterator().next();
        }
        if (chunk == null && root.chunkCosts().isEmpty()) {
            return slice;
        }
        if (chunk == null || !root.dimensionId().equals(chunk.dimensionId())) {
            return DENIED;
        }
        return new OriginChunkBudget(slice, chunk, entityQueryChunks);
    }

    private static boolean fitsWithin(Map<BudgetKind, Long> candidate, Map<BudgetKind, Long> capacity) {
        for (Map.Entry<BudgetKind, Long> entry : candidate.entrySet()) {
            if (entry.getValue() > capacity.getOrDefault(entry.getKey(), 0L)) {
                return false;
            }
        }
        return true;
    }

    private static void subtract(EnumMap<BudgetKind, Long> remaining, Map<BudgetKind, Long> costs) {
        for (Map.Entry<BudgetKind, Long> entry : costs.entrySet()) {
            long next = remaining.getOrDefault(entry.getKey(), 0L) - entry.getValue();
            if (next == 0L) {
                remaining.remove(entry.getKey());
            } else {
                remaining.put(entry.getKey(), next);
            }
        }
    }

    private record OriginChunkBudget(
        WorldMutationBudget delegate, ChunkBudgetKey origin, Set<ChunkBudgetKey> entityQueryChunks
    ) implements WorldMutationBudget {
        private OriginChunkBudget {
            Objects.requireNonNull(delegate, "delegate");
            Objects.requireNonNull(origin, "origin");
            entityQueryChunks = entityQueryChunks == null || entityQueryChunks.isEmpty() ? Set.of(origin)
                : Set.copyOf(entityQueryChunks);
            if (!entityQueryChunks.contains(origin)
                || entityQueryChunks.stream().anyMatch(chunk -> !origin.dimensionId().equals(chunk.dimensionId()))) {
                throw new IllegalArgumentException("entity query chunks must include the primary chunk in the same dimension");
            }
        }

        @Override
        public boolean tryReserve(com.mcnoita.world.mutation.WorldMutationKind kind, int amount) {
            return delegate.tryReserve(kind, amount);
        }

        @Override
        public boolean tryReserveAt(com.mcnoita.world.mutation.WorldMutationKind kind, int amount, BlockPos position) {
            return isInOrigin(position) && delegate.tryReserveAt(kind, amount, position);
        }

        @Override
        public boolean tryReserveIn(com.mcnoita.world.mutation.WorldMutationKind kind, int amount, Box area) {
            WorldMutationPolicy.ChunkEnvelope envelope = WorldMutationPolicy.chunkEnvelope(area).orElse(null);
            if (envelope == null) {
                return false;
            }
            if (kind == com.mcnoita.world.mutation.WorldMutationKind.ENTITY_QUERY
                && isWithinEntityQueryCoverage(envelope)) {
                return delegate.tryReserveIn(kind, amount, area);
            }
            return isInOrigin(envelope) && delegate.tryReserveIn(kind, amount, area);
        }

        private boolean isInOrigin(BlockPos position) {
            return (position.getX() >> 4) == origin.chunkX() && (position.getZ() >> 4) == origin.chunkZ();
        }

        private boolean isInOrigin(WorldMutationPolicy.ChunkEnvelope envelope) {
            return envelope.minChunkX() == origin.chunkX() && envelope.maxChunkX() == origin.chunkX()
                && envelope.minChunkZ() == origin.chunkZ() && envelope.maxChunkZ() == origin.chunkZ();
        }

        private boolean isWithinEntityQueryCoverage(WorldMutationPolicy.ChunkEnvelope envelope) {
            for (int chunkX = envelope.minChunkX(); chunkX <= envelope.maxChunkX(); chunkX++) {
                for (int chunkZ = envelope.minChunkZ(); chunkZ <= envelope.maxChunkZ(); chunkZ++) {
                    if (!entityQueryChunks.contains(new ChunkBudgetKey(origin.dimensionId(), chunkX, chunkZ))) {
                        return false;
                    }
                }
            }
            return true;
        }
    }
}
