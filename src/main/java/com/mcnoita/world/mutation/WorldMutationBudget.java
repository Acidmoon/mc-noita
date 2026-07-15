package com.mcnoita.world.mutation;

import java.util.Objects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/**
 * Execution-time view of a central SpellBudgetManager reservation. Immediate
 * typed nodes receive a projected root counter, while legacy projectile/entity
 * operations re-reserve one bounded operation through ServerWorldMutationBudget.
 */
@FunctionalInterface
public interface WorldMutationBudget {
    WorldMutationBudget UNTRACKED = (kind, amount) -> true;
    /** Safe default for malformed/unbound spell runtime identities. */
    WorldMutationBudget DENIED = (kind, amount) -> false;

    boolean tryReserve(WorldMutationKind kind, int amount);

    /**
     * Charges one point operation to the chunk that will actually be touched.
     * Legacy in-memory counters deliberately retain their total-only behavior;
     * the central runtime bridge overrides this method for delayed entities.
     */
    default boolean tryReserveAt(WorldMutationKind kind, int amount, BlockPos position) {
        Objects.requireNonNull(position, "position");
        return tryReserve(kind, amount);
    }

    /**
     * Charges a bounded read against every covered chunk. The live server
     * bridge reserves an atomic conservative slice for a multi-chunk envelope;
     * simpler local counters retain total-only behavior for already-frozen
     * synchronous nodes.
     */
    default boolean tryReserveIn(WorldMutationKind kind, int amount, Box area) {
        Objects.requireNonNull(area, "area");
        return tryReserve(kind, amount);
    }
}
