package com.mcnoita.spell.exec;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mcnoita.spell.plan.ProjectileEffectNode;
import com.mcnoita.spell.plan.ProjectilePlan;
import com.mcnoita.spell.server.budget.BudgetKind;
import com.mcnoita.spell.server.budget.BudgetLimits;
import com.mcnoita.spell.server.budget.BudgetRequest;
import com.mcnoita.spell.server.budget.BudgetReservation;
import com.mcnoita.spell.server.budget.ChunkBudgetKey;
import com.mcnoita.spell.server.budget.SpellBudgetManager;
import com.mcnoita.wand.model.NoitaDuration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("regression")
class EffectNodeBudgetReleaserTest {
    private static final String OVERWORLD = "minecraft:overworld";

    @Test
    void failedProjectileMapsToABoundedUnchunkedReservationSlice() {
        UUID executionId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        BudgetRequest original = BudgetRequest.builder(executionId, OVERWORLD)
            .owner(ownerId)
            .add(BudgetKind.AUTHORITATIVE_ENTITIES, 5L)
            .build();

        BudgetRequest slice = ReservationEffectNodeBudgetReleaser.EffectNodeBudgetSlices.forFailedNode(original, projectile(2));

        assertEquals(executionId, slice.executionId());
        assertEquals(ownerId, slice.ownerId());
        assertEquals(OVERWORLD, slice.dimensionId());
        assertEquals(Map.of(BudgetKind.AUTHORITATIVE_ENTITIES, 2L), slice.costs());
        assertEquals(Map.of(), slice.chunkCosts());
    }

    @Test
    void defaultMapperDoesNotReleaseAChunkScopedCostWithoutTransactionSliceOwnership() {
        UUID executionId = UUID.randomUUID();
        ChunkBudgetKey chunk = new ChunkBudgetKey(OVERWORLD, 1, -2);
        BudgetRequest original = BudgetRequest.builder(executionId, OVERWORLD)
            .addInChunk(chunk, BudgetKind.AUTHORITATIVE_ENTITIES, 5L)
            .build();

        BudgetRequest slice = ReservationEffectNodeBudgetReleaser.EffectNodeBudgetSlices.forFailedNode(original, projectile(2));

        assertTrue(slice.isEmpty());
        assertEquals(Map.of(), slice.chunkCosts());
    }

    @Test
    void namedReleaseIsIdempotentAndNullReservationCallbackIsNoOp() {
        SpellBudgetManager manager = new SpellBudgetManager(BudgetLimits.unlimited());
        BudgetRequest request = BudgetRequest.builder(UUID.randomUUID(), OVERWORLD)
            .add(BudgetKind.AUTHORITATIVE_ENTITIES, 4L)
            .build();
        SpellBudgetManager.ReservationAttempt attempt = manager.reserve(request, 0L);
        assertTrue(attempt.accepted());
        BudgetReservation reservation = attempt.reservation();
        assertTrue(reservation.commit());

        EffectNodeBudgetReleaser releaser = new ReservationEffectNodeBudgetReleaser(reservation);
        releaser.releaseUnused(projectile(2), "effect-node/executor/projectile");
        releaser.releaseUnused(projectile(2), "effect-node/executor/projectile");

        assertEquals(Map.of(BudgetKind.AUTHORITATIVE_ENTITIES, 2L), manager.globalInFlightUsage());
        assertDoesNotThrow(() -> EffectNodeBudgetReleaser.forReservation(null)
            .releaseUnused(projectile(1), "ignored-without-reservation"));
        assertTrue(reservation.close());
        assertFalse(reservation.close());
    }

    private static ProjectileEffectNode projectile(int count) {
        ProjectilePlan plan = new ProjectilePlan("root/projectile/" + count, "spark_bolt", "BOLT", 1.0, 0.0,
            NoitaDuration.frames(60), 0, 0.0, 400.0, 0.0, 0.0, 0.99, 0.65, 1.0, 0.0, false, false, count,
            0.0, null, 0, List.of());
        return new ProjectileEffectNode(plan);
    }
}
