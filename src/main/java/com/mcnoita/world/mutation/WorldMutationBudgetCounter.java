package com.mcnoita.world.mutation;

import java.util.EnumMap;
import java.util.Map;

/** Small deterministic budget implementation used until the central manager owns these counters. */
public final class WorldMutationBudgetCounter implements WorldMutationBudget {
    private final EnumMap<WorldMutationKind, Integer> remaining;

    public WorldMutationBudgetCounter(Map<WorldMutationKind, Integer> limits) {
        this.remaining = new EnumMap<>(WorldMutationKind.class);
        for (WorldMutationKind kind : WorldMutationKind.values()) {
            this.remaining.put(kind, Math.max(0, limits.getOrDefault(kind, 0)));
        }
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
}
