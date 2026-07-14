package com.mcnoita.spell.trigger;

import java.util.ArrayList;
import java.util.List;

/**
 * Runtime ceilings travel with a frozen projectile tree. A child receives a
 * partition of its parent's remaining allowance, never a fresh default budget.
 */
public record TriggerRuntimeBudget(int remainingReleaseEvents, int remainingSpawnedEntities) {
    public static final TriggerRuntimeBudget DEFAULT = new TriggerRuntimeBudget(32, 32);

    public TriggerRuntimeBudget {
        if (remainingReleaseEvents < 0 || remainingSpawnedEntities < 0) {
            throw new IllegalArgumentException("runtime budgets must not be negative");
        }
    }

    public boolean canReserve(int releaseEvents, int spawnedEntities) {
        return releaseEvents >= 0 && spawnedEntities >= 0
            && releaseEvents <= remainingReleaseEvents && spawnedEntities <= remainingSpawnedEntities;
    }

    public TriggerRuntimeBudget reserve(int releaseEvents, int spawnedEntities) {
        if (!canReserve(releaseEvents, spawnedEntities)) {
            throw new IllegalArgumentException("runtime trigger budget exhausted");
        }
        return new TriggerRuntimeBudget(remainingReleaseEvents - releaseEvents, remainingSpawnedEntities - spawnedEntities);
    }

    /** Splits all remaining capacity deterministically among newly spawned child entities. */
    public List<TriggerRuntimeBudget> partition(int children) {
        if (children < 1) {
            return List.of();
        }
        List<TriggerRuntimeBudget> partitions = new ArrayList<>(children);
        for (int index = 0; index < children; index++) {
            partitions.add(new TriggerRuntimeBudget(
                share(remainingReleaseEvents, children, index),
                share(remainingSpawnedEntities, children, index)
            ));
        }
        return List.copyOf(partitions);
    }

    private static int share(int total, int parts, int index) {
        int base = total / parts;
        return base + (index < total % parts ? 1 : 0);
    }
}
