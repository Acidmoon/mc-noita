package com.mcnoita.world.mutation;

import com.mcnoita.spell.server.budget.BudgetKind;
import com.mcnoita.spell.server.budget.BudgetRequest;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Local consumption view over a pre-accepted central reservation. It never
 * changes SpellBudgetManager accounting; it only prevents one execution from
 * spending more world-operation capacity than its frozen root request owns.
 */
public final class WorldMutationBudgetCounter implements WorldMutationBudget {
    private final EnumMap<WorldMutationKind, Integer> remaining;

    public WorldMutationBudgetCounter(Map<WorldMutationKind, Integer> limits) {
        this.remaining = new EnumMap<>(WorldMutationKind.class);
        for (WorldMutationKind kind : WorldMutationKind.values()) {
            this.remaining.put(kind, Math.max(0, limits.getOrDefault(kind, 0)));
        }
    }

    /**
     * Projects one already-reserved root request into the world boundary's
     * operation kinds. Explosion consumes ENTITY_SCANS at the policy boundary
     * because the central model has no separate explosion counter and its
     * entity phase is the bounded authoritative scan allocation.
     */
    public static WorldMutationBudgetCounter fromBudgetRequest(BudgetRequest request) {
        Objects.requireNonNull(request, "request");
        return fromBudgetCosts(request.costs());
    }

    /** Projects a frozen central cost map without mutating its reservation handle. */
    public static WorldMutationBudgetCounter fromBudgetCosts(Map<BudgetKind, Long> costs) {
        Objects.requireNonNull(costs, "costs");
        EnumMap<WorldMutationKind, Integer> limits = new EnumMap<>(WorldMutationKind.class);
        limits.put(WorldMutationKind.BLOCK_CHECK, capped(costs.getOrDefault(BudgetKind.BLOCK_CHECKS, 0L)));
        limits.put(WorldMutationKind.BLOCK_MUTATION, capped(costs.getOrDefault(BudgetKind.BLOCK_MUTATIONS, 0L)));
        limits.put(WorldMutationKind.ENTITY_QUERY, capped(costs.getOrDefault(BudgetKind.ENTITY_SCANS, 0L)));
        limits.put(WorldMutationKind.ENTITY_SPAWN, capped(costs.getOrDefault(BudgetKind.AUTHORITATIVE_ENTITIES, 0L)));
        return new WorldMutationBudgetCounter(limits);
    }

    @Override
    public synchronized boolean tryReserve(WorldMutationKind kind, int amount) {
        if (amount < 0) {
            return false;
        }
        int available = remaining.get(kind);
        if (amount > available) {
            return false;
        }
        remaining.put(kind, available - amount);
        return true;
    }

    public synchronized int remaining(WorldMutationKind kind) {
        return remaining.get(kind);
    }

    private static int capped(long value) {
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, value);
    }
}
