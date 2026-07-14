package com.mcnoita.wand.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mcnoita.spell.plan.BudgetUsage;
import com.mcnoita.spell.plan.EffectPlan;
import com.mcnoita.spell.plan.ProjectileEffectNode;
import com.mcnoita.spell.plan.ProjectilePlan;
import com.mcnoita.spell.plan.ResolvedCast;
import com.mcnoita.spell.plan.SoundEffectNode;
import com.mcnoita.spell.plan.SoundPlan;
import com.mcnoita.spell.server.budget.BudgetKind;
import com.mcnoita.spell.server.budget.BudgetRequest;
import com.mcnoita.spell.server.budget.ChunkBudgetKey;
import com.mcnoita.wand.eval.EvaluationTrace;
import com.mcnoita.wand.model.DeckState;
import com.mcnoita.wand.model.NoitaDuration;
import com.mcnoita.wand.model.WandState;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Pure proof that frozen effect nodes map to the same releasable root budget shape as WP5. */
@Tag("regression")
class PlanBudgetRequestFactoryTest {
    @Test
    void mapsEvaluatorUsageAndRootNodesIntoOneOriginChunkRequest() {
        ChunkBudgetKey chunk = new ChunkBudgetKey("minecraft:overworld", 3, -4);
        BudgetRequest request = PlanBudgetRequestFactory.fromResolvedCast(UUID.randomUUID(), UUID.randomUUID(),
            chunk.dimensionId(), chunk, acceptedCast());

        assertEquals(7L, request.cost(BudgetKind.ACTION_NODES));
        assertEquals(1L, request.cost(BudgetKind.LOGICAL_PROJECTILES));
        assertEquals(1L, request.cost(BudgetKind.AUTHORITATIVE_ENTITIES));
        assertEquals(2L, request.cost(BudgetKind.VISUAL_EVENTS));
        assertEquals(1L, request.cost(BudgetKind.NETWORK_PACKETS));
        assertEquals(128L, request.cost(BudgetKind.NETWORK_BYTES));
        assertEquals(1L, request.chunkCosts().get(chunk).get(BudgetKind.LOGICAL_PROJECTILES));
        assertEquals(1L, request.chunkCosts().get(chunk).get(BudgetKind.AUTHORITATIVE_ENTITIES));
        assertEquals(2L, request.chunkCosts().get(chunk).get(BudgetKind.VISUAL_EVENTS));
    }

    @Test
    void rejectsPlansThatAreNotAcceptedOrWhoseChunkUsesAnotherDimension() {
        ResolvedCast rejected = new ResolvedCast(ResolvedCast.Status.REJECTED, emptyState(), EffectPlan.empty(), Map.of(), List.of(),
            NoitaDuration.ZERO, NoitaDuration.ZERO, 0L, 0L, "catalog", new BudgetUsage(0, 0, 0, 0, 0), List.of(), EvaluationTrace.EMPTY);
        ChunkBudgetKey overworld = new ChunkBudgetKey("minecraft:overworld", 0, 0);

        assertThrows(IllegalArgumentException.class, () -> PlanBudgetRequestFactory.fromResolvedCast(UUID.randomUUID(), UUID.randomUUID(),
            overworld.dimensionId(), overworld, rejected));
        assertThrows(IllegalArgumentException.class, () -> PlanBudgetRequestFactory.fromResolvedCast(UUID.randomUUID(), UUID.randomUUID(),
            "minecraft:the_nether", overworld, acceptedCast()));
    }

    private static ResolvedCast acceptedCast() {
        ProjectilePlan projectile = new ProjectilePlan("root/0", "spark_bolt", "BOLT", 1.0, 0.0,
            NoitaDuration.frames(60), 0, 0.0, 400.0, 0.0, 0.0, 0.99, 0.65, 1.0, 0.0,
            false, false, 1, 0.0, null, 0, List.of());
        EffectPlan plan = new EffectPlan(List.of(
            new ProjectileEffectNode(projectile),
            new SoundEffectNode("sound/0", new SoundPlan(SoundPlan.SoundKind.PROJECTILE_CAST))
        ));
        return new ResolvedCast(ResolvedCast.Status.ACCEPTED, emptyState(), plan, Map.of(), List.of(), NoitaDuration.ZERO,
            NoitaDuration.ZERO, 0L, 3L, "catalog", new BudgetUsage(7, 1, 0, 0, 1), List.of(), EvaluationTrace.EMPTY);
    }

    private static WandState emptyState() {
        return new WandState(new DeckState(Map.of(), List.of(), List.of(), List.of()), 0.0,
            NoitaDuration.ZERO, NoitaDuration.ZERO, false, 0L, 0);
    }
}
