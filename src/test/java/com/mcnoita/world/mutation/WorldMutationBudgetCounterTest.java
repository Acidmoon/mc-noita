package com.mcnoita.world.mutation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mcnoita.spell.server.budget.BudgetKind;
import com.mcnoita.spell.server.budget.BudgetRequest;
import java.util.Map;
import java.util.UUID;
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

    @Test
    void centralRootRequestBecomesAWorldOperationCeiling() {
        BudgetRequest request = BudgetRequest.builder(UUID.randomUUID(), "minecraft:overworld")
            .add(BudgetKind.BLOCK_CHECKS, 2L)
            .add(BudgetKind.BLOCK_MUTATIONS, 1L)
            .add(BudgetKind.ENTITY_SCANS, 3L)
            .add(BudgetKind.AUTHORITATIVE_ENTITIES, 4L)
            .build();
        WorldMutationBudgetCounter budget = WorldMutationBudgetCounter.fromBudgetRequest(request);

        assertTrue(budget.tryReserve(WorldMutationKind.BLOCK_CHECK, 2));
        assertFalse(budget.tryReserve(WorldMutationKind.BLOCK_CHECK, 1));
        assertTrue(budget.tryReserve(WorldMutationKind.BLOCK_MUTATION, 1));
        assertFalse(budget.tryReserve(WorldMutationKind.BLOCK_MUTATION, 1));
        // WorldMutationPolicy charges an explosion's entity phase to this same
        // shared central ENTITY_SCANS projection, not a second local bucket.
        assertTrue(budget.tryReserve(WorldMutationKind.ENTITY_QUERY, 1));
        assertTrue(budget.tryReserve(WorldMutationKind.ENTITY_QUERY, 2));
        assertFalse(budget.tryReserve(WorldMutationKind.ENTITY_QUERY, 1));
        assertTrue(budget.tryReserve(WorldMutationKind.ENTITY_SPAWN, 4));
        assertEquals(0, budget.remaining(WorldMutationKind.BLOCK_CHECK));
    }
}
