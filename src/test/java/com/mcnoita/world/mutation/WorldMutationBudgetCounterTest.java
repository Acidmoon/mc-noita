package com.mcnoita.world.mutation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import net.minecraft.util.math.Box;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Pure regression tests for the service-to-central-budget-manager handoff point. */
@Tag("regression")
class WorldMutationBudgetCounterTest {
    @Test
    void reservationNeverPartiallyConsumesAnExceededKind() {
        WorldMutationBudgetCounter budget = new WorldMutationBudgetCounter(Map.of(
            WorldMutationKind.BLOCK_MUTATION, 2,
            WorldMutationKind.ENTITY_QUERY, 3
        ));

        assertTrue(budget.tryReserve(WorldMutationKind.BLOCK_MUTATION, 2));
        assertFalse(budget.tryReserve(WorldMutationKind.BLOCK_MUTATION, 1));
        assertTrue(budget.tryReserve(WorldMutationKind.ENTITY_QUERY, 3));
    }

    @Test
    void hostileQueryBoxesAreRejectedBeforeChunkEnumeration() {
        assertTrue(WorldMutationPolicy.isBoundedQueryBox(new Box(0.0, 0.0, 0.0, 64.0, 1.0, 64.0)));
        assertFalse(WorldMutationPolicy.isBoundedQueryBox(new Box(0.0, 0.0, 0.0, 64.1, 1.0, 1.0)));
        assertFalse(WorldMutationPolicy.isBoundedQueryBox(new Box(0.0, 0.0, 0.0, 64.0, 64.0, 64.1)));
    }
}
