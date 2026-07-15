package com.mcnoita.wand.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mcnoita.spell.plan.BudgetUsage;
import com.mcnoita.spell.plan.BlockMutationEffectNode;
import com.mcnoita.spell.plan.EffectPlan;
import com.mcnoita.spell.plan.ExplosionEffectNode;
import com.mcnoita.spell.plan.ProjectileEffectNode;
import com.mcnoita.spell.plan.ProjectilePlan;
import com.mcnoita.spell.plan.PayloadPlan;
import com.mcnoita.spell.plan.PersistentJobEffectNode;
import com.mcnoita.spell.plan.ResolvedCast;
import com.mcnoita.spell.plan.SoundEffectNode;
import com.mcnoita.spell.plan.SoundPlan;
import com.mcnoita.spell.plan.TeleportEffectNode;
import com.mcnoita.spell.plan.TriggerMode;
import com.mcnoita.spell.plan.TriggerPlan;
import com.mcnoita.spell.plan.TriggerReleasePolicy;
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
import net.minecraft.util.math.Vec3d;
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

    @Test
    void reservesEveryBoundedLineOfSightCheckForABreakNode() {
        ChunkBudgetKey chunk = new ChunkBudgetKey("minecraft:overworld", 0, 0);
        EffectPlan plan = new EffectPlan(List.of(new BlockMutationEffectNode(
            "break", BlockMutationEffectNode.MutationKind.BREAK, 1, 2.1
        )));
        ResolvedCast cast = new ResolvedCast(ResolvedCast.Status.ACCEPTED, emptyState(), plan, Map.of(), List.of(),
            NoitaDuration.ZERO, NoitaDuration.ZERO, 0L, 3L, "catalog", new BudgetUsage(0, 0, 0, 0, 0), List.of(),
            EvaluationTrace.EMPTY);

        BudgetRequest request = PlanBudgetRequestFactory.fromResolvedCast(UUID.randomUUID(), UUID.randomUUID(),
            chunk.dimensionId(), chunk, cast);

        assertEquals(4L, request.cost(BudgetKind.BLOCK_CHECKS));
        assertEquals(1L, request.cost(BudgetKind.BLOCK_MUTATIONS));
        assertEquals(4L, request.chunkCosts().get(chunk).get(BudgetKind.BLOCK_CHECKS));
        assertEquals(1L, request.chunkCosts().get(chunk).get(BudgetKind.BLOCK_MUTATIONS));
    }

    @Test
    void reservesTheExplosionLoopUpperBoundRatherThanAContinuousSphereEstimate() {
        ChunkBudgetKey chunk = new ChunkBudgetKey("minecraft:overworld", 0, 0);
        EffectPlan plan = new EffectPlan(List.of(new ExplosionEffectNode("explosion", 1.0, 2.0, true)));
        ResolvedCast cast = new ResolvedCast(ResolvedCast.Status.ACCEPTED, emptyState(), plan, Map.of(), List.of(),
            NoitaDuration.ZERO, NoitaDuration.ZERO, 0L, 3L, "catalog", new BudgetUsage(0, 0, 0, 0, 0), List.of(),
            EvaluationTrace.EMPTY);

        BudgetRequest request = PlanBudgetRequestFactory.fromResolvedCast(UUID.randomUUID(), UUID.randomUUID(),
            chunk.dimensionId(), chunk, cast);

        assertEquals(5L, request.cost(BudgetKind.ENTITY_SCANS));
        assertEquals(27L, request.cost(BudgetKind.BLOCK_CHECKS));
        assertEquals(27L, request.cost(BudgetKind.BLOCK_MUTATIONS));
    }

    @Test
    void rootReservationCountsOnlySynchronousProjectilesWhileTheEvaluatorKeepsTheTriggerTreeBounded() {
        ChunkBudgetKey chunk = new ChunkBudgetKey("minecraft:overworld", 0, 0);
        ProjectilePlan child = new ProjectilePlan("root/0/trigger/0/0", "spark_bolt", "BOLT", 1.0, 0.0,
            NoitaDuration.frames(60), 0, 0.0, 1.0, 0.0, 0.0, 0.99, 0.65, 1.0, 0.0,
            false, false, 1, 0.0, null, 0, List.of());
        PayloadPlan payload = new PayloadPlan("root/0/trigger/0", 1, List.of(child));
        TriggerPlan trigger = new TriggerPlan("root/0/trigger/0", TriggerMode.HIT, NoitaDuration.ZERO, 1,
            TriggerReleasePolicy.COLLISION_WHILE_ALIVE, payload);
        ProjectilePlan root = new ProjectilePlan("root/0", "spark_bolt", "BOLT", 1.0, 0.0,
            NoitaDuration.frames(60), 0, 0.0, 1.0, 0.0, 0.0, 0.99, 0.65, 1.0, 0.0,
            false, false, 1, 0.0, trigger, 0, List.of());
        ResolvedCast cast = new ResolvedCast(ResolvedCast.Status.ACCEPTED, emptyState(),
            new EffectPlan(List.of(new ProjectileEffectNode(root))), Map.of(), List.of(), NoitaDuration.ZERO,
            NoitaDuration.ZERO, 0L, 3L, "catalog", new BudgetUsage(2, 2, 1, 1, 2), List.of(), EvaluationTrace.EMPTY);

        BudgetRequest request = PlanBudgetRequestFactory.fromResolvedCast(UUID.randomUUID(), UUID.randomUUID(),
            chunk.dimensionId(), chunk, cast);

        assertEquals(1L, request.cost(BudgetKind.AUTHORITATIVE_ENTITIES));
        assertEquals(1L, request.chunkCosts().get(chunk).get(BudgetKind.AUTHORITATIVE_ENTITIES));
    }

    @Test
    void rootChunkLogicalProjectileCostMatchesTheFrozenPhysicalFanout() {
        ChunkBudgetKey chunk = new ChunkBudgetKey("minecraft:overworld", 0, 0);
        ProjectilePlan root = new ProjectilePlan("root/0", "spark_bolt", "BOLT", 1.0, 0.0,
            NoitaDuration.frames(60), 0, 0.0, 1.0, 0.0, 0.0, 0.99, 0.65, 1.0, 0.0,
            false, false, 4, 0.0, null, 0, List.of());
        ResolvedCast cast = new ResolvedCast(ResolvedCast.Status.ACCEPTED, emptyState(),
            new EffectPlan(List.of(new ProjectileEffectNode(root))), Map.of(), List.of(), NoitaDuration.ZERO,
            NoitaDuration.ZERO, 0L, 3L, "catalog", new BudgetUsage(1, 4, 0, 0, 4), List.of(), EvaluationTrace.EMPTY);

        BudgetRequest request = PlanBudgetRequestFactory.fromResolvedCast(UUID.randomUUID(), UUID.randomUUID(),
            chunk.dimensionId(), chunk, cast);

        assertEquals(4L, request.chunkCosts().get(chunk).get(BudgetKind.LOGICAL_PROJECTILES));
        assertEquals(4L, request.chunkCosts().get(chunk).get(BudgetKind.AUTHORITATIVE_ENTITIES));
    }

    @Test
    void maximumPolicyExplosionUsesTheSameBoundedEntityScanLimitAsItsExecutor() {
        ChunkBudgetKey chunk = new ChunkBudgetKey("minecraft:overworld", 0, 0);
        EffectPlan plan = new EffectPlan(List.of(new ExplosionEffectNode("explosion", 16.0, 2.0, false)));
        ResolvedCast cast = new ResolvedCast(ResolvedCast.Status.ACCEPTED, emptyState(), plan, Map.of(), List.of(),
            NoitaDuration.ZERO, NoitaDuration.ZERO, 0L, 3L, "catalog", new BudgetUsage(0, 0, 0, 0, 0), List.of(),
            EvaluationTrace.EMPTY);

        BudgetRequest request = PlanBudgetRequestFactory.fromResolvedCast(UUID.randomUUID(), UUID.randomUUID(),
            chunk.dimensionId(), chunk, cast);

        assertEquals(128L, request.cost(BudgetKind.ENTITY_SCANS));
    }

    @Test
    void boundaryExplosionPreReservesEveryChunkItsEntityQueryCanTouch() {
        ChunkBudgetKey origin = new ChunkBudgetKey("minecraft:overworld", 0, 0);
        EffectPlan plan = new EffectPlan(List.of(new ExplosionEffectNode("explosion", 1.0, 2.0, false)));
        ResolvedCast cast = new ResolvedCast(ResolvedCast.Status.ACCEPTED, emptyState(), plan, Map.of(), List.of(),
            NoitaDuration.ZERO, NoitaDuration.ZERO, 0L, 3L, "catalog", new BudgetUsage(0, 0, 0, 0, 0), List.of(),
            EvaluationTrace.EMPTY);

        PlanBudgetRequestFactory.SynchronousNodeBindings bindings = PlanBudgetRequestFactory.synchronousNodeBindings(
            origin.dimensionId(), origin, cast, new Vec3d(15.5, 64.0, 8.0), new Vec3d(15.5, 64.0, 8.0),
            new Vec3d(1.0, 0.0, 0.0)
        );
        BudgetRequest request = PlanBudgetRequestFactory.fromResolvedCast(UUID.randomUUID(), UUID.randomUUID(),
            origin.dimensionId(), origin, cast, bindings);

        ChunkBudgetKey adjacent = new ChunkBudgetKey(origin.dimensionId(), 1, 0);
        assertEquals(2, bindings.entityQueryChunks().get("explosion").size());
        assertTrue(bindings.entityQueryChunks().get("explosion").containsAll(java.util.Set.of(origin, adjacent)));
        assertEquals(10L, request.cost(BudgetKind.ENTITY_SCANS));
        assertEquals(5L, request.chunkCosts().get(origin).get(BudgetKind.ENTITY_SCANS));
        assertEquals(5L, request.chunkCosts().get(adjacent).get(BudgetKind.ENTITY_SCANS));
    }

    @Test
    void rejectsImmediateTerrainThatCouldCrossAChunkBeforeReservation() {
        ChunkBudgetKey origin = new ChunkBudgetKey("minecraft:overworld", 0, 0);
        ResolvedCast breakCast = new ResolvedCast(ResolvedCast.Status.ACCEPTED, emptyState(),
            new EffectPlan(List.of(new BlockMutationEffectNode("break", BlockMutationEffectNode.MutationKind.BREAK, 1, 2.0))),
            Map.of(), List.of(), NoitaDuration.ZERO, NoitaDuration.ZERO, 0L, 3L, "catalog",
            new BudgetUsage(0, 0, 0, 0, 0), List.of(), EvaluationTrace.EMPTY);
        ResolvedCast terrainExplosion = new ResolvedCast(ResolvedCast.Status.ACCEPTED, emptyState(),
            new EffectPlan(List.of(new ExplosionEffectNode("terrain", 1.0, 2.0, true))), Map.of(), List.of(),
            NoitaDuration.ZERO, NoitaDuration.ZERO, 0L, 3L, "catalog", new BudgetUsage(0, 0, 0, 0, 0), List.of(),
            EvaluationTrace.EMPTY);

        assertThrows(IllegalArgumentException.class, () -> PlanBudgetRequestFactory.synchronousNodeBindings(
            origin.dimensionId(), origin, breakCast, new Vec3d(14.5, 64.0, 8.0), new Vec3d(14.5, 64.0, 8.0),
            new Vec3d(1.0, 0.0, 0.0)
        ));
        // The integer terrain loop includes x=16 when its center is x=15,
        // even though a half-open continuous Box would end exactly at x=16.
        assertThrows(IllegalArgumentException.class, () -> PlanBudgetRequestFactory.synchronousNodeBindings(
            origin.dimensionId(), origin, terrainExplosion, new Vec3d(15.0, 64.0, 8.0), new Vec3d(15.0, 64.0, 8.0),
            new Vec3d(1.0, 0.0, 0.0)
        ));
    }

    @Test
    void freezesAWhollyLocalBreakRayToItsActualSpawnChunk() {
        ChunkBudgetKey origin = new ChunkBudgetKey("minecraft:overworld", 0, 0);
        ResolvedCast cast = new ResolvedCast(ResolvedCast.Status.ACCEPTED, emptyState(),
            new EffectPlan(List.of(new BlockMutationEffectNode("break", BlockMutationEffectNode.MutationKind.BREAK, 1, 2.0))),
            Map.of(), List.of(), NoitaDuration.ZERO, NoitaDuration.ZERO, 0L, 3L, "catalog",
            new BudgetUsage(0, 0, 0, 0, 0), List.of(), EvaluationTrace.EMPTY);

        PlanBudgetRequestFactory.SynchronousNodeBindings bindings = PlanBudgetRequestFactory.synchronousNodeBindings(
            origin.dimensionId(), origin, cast, new Vec3d(14.5, 64.0, 8.0), new Vec3d(14.5, 64.0, 8.0),
            new Vec3d(-1.0, 0.0, 0.0)
        );

        assertEquals(origin, bindings.primaryChunks().get("break"));
    }

    @Test
    void deterministicRootSpawnAndTeleportTargetsReserveTheirActualChunksBeforeCommit() {
        ChunkBudgetKey origin = new ChunkBudgetKey("minecraft:overworld", 0, 0);
        ProjectilePlan root = new ProjectilePlan("root/0", "spark_bolt", "BOLT", 1.0, 0.0,
            NoitaDuration.frames(60), 0, 0.0, 1.0, 0.0, 0.0, 0.99, 0.65, 1.0, 0.0,
            false, false, 1, 0.0, null, 0, List.of());
        EffectPlan plan = new EffectPlan(List.of(
            new ProjectileEffectNode(root),
            new TeleportEffectNode("teleport", 32.0, true)
        ));
        ResolvedCast cast = new ResolvedCast(ResolvedCast.Status.ACCEPTED, emptyState(), plan, Map.of(), List.of(),
            NoitaDuration.ZERO, NoitaDuration.ZERO, 0L, 3L, "catalog", new BudgetUsage(1, 1, 0, 0, 1), List.of(),
            EvaluationTrace.EMPTY);

        Map<String, ChunkBudgetKey> chunks = PlanBudgetRequestFactory.synchronousNodeChunks(origin.dimensionId(), origin,
            cast, Vec3d.ZERO, new Vec3d(16.1, 64.0, 0.0), new Vec3d(1.0, 0.0, 0.0));
        BudgetRequest request = PlanBudgetRequestFactory.fromResolvedCast(UUID.randomUUID(), UUID.randomUUID(),
            origin.dimensionId(), origin, cast, chunks);

        ChunkBudgetKey spawnChunk = new ChunkBudgetKey(origin.dimensionId(), 1, 0);
        ChunkBudgetKey teleportChunk = new ChunkBudgetKey(origin.dimensionId(), 2, 0);
        assertEquals(1L, request.chunkCosts().get(spawnChunk).get(BudgetKind.AUTHORITATIVE_ENTITIES));
        assertEquals(64L, request.chunkCosts().get(teleportChunk).get(BudgetKind.BLOCK_CHECKS));
    }

    @Test
    void reservesOnlyTheTransferablePersistentJobLeaseAtCastCommit() {
        ChunkBudgetKey chunk = new ChunkBudgetKey("minecraft:overworld", 1, 1);
        EffectPlan plan = new EffectPlan(List.of(new PersistentJobEffectNode(
            "field/job", "field_tick", 12, NoitaDuration.frames(60)
        )));
        ResolvedCast cast = new ResolvedCast(ResolvedCast.Status.ACCEPTED, emptyState(), plan, Map.of(), List.of(),
            NoitaDuration.ZERO, NoitaDuration.ZERO, 0L, 3L, "catalog", new BudgetUsage(0, 0, 0, 0, 0), List.of(),
            EvaluationTrace.EMPTY);

        BudgetRequest request = PlanBudgetRequestFactory.fromResolvedCast(UUID.randomUUID(), UUID.randomUUID(),
            chunk.dimensionId(), chunk, cast);

        assertEquals(1L, request.cost(BudgetKind.PERSISTENT_JOBS));
        assertEquals(1L, request.chunkCosts().get(chunk).get(BudgetKind.PERSISTENT_JOBS));
        assertEquals(0L, request.cost(BudgetKind.CROSS_TICK_JOB_STEPS));
    }

    @Test
    void rejectsMultiplePersistentJobsBeforeCastCommit() {
        ChunkBudgetKey chunk = new ChunkBudgetKey("minecraft:overworld", 1, 1);
        EffectPlan plan = new EffectPlan(List.of(
            new PersistentJobEffectNode("field/first", "field_tick", 12, NoitaDuration.frames(60)),
            new PersistentJobEffectNode("field/second", "field_tick", 12, NoitaDuration.frames(60))
        ));
        ResolvedCast cast = new ResolvedCast(ResolvedCast.Status.ACCEPTED, emptyState(), plan, Map.of(), List.of(),
            NoitaDuration.ZERO, NoitaDuration.ZERO, 0L, 3L, "catalog", new BudgetUsage(0, 0, 0, 0, 0), List.of(),
            EvaluationTrace.EMPTY);

        assertThrows(IllegalArgumentException.class, () -> PlanBudgetRequestFactory.fromResolvedCast(
            UUID.randomUUID(), UUID.randomUUID(), chunk.dimensionId(), chunk, cast));
    }

    @Test
    void rejectsAnOverLimitPersistentJobBeforeWandStateCanCommit() {
        ChunkBudgetKey chunk = new ChunkBudgetKey("minecraft:overworld", 1, 1);
        EffectPlan plan = new EffectPlan(List.of(new PersistentJobEffectNode(
            "field/job", "field_tick", 4_097, NoitaDuration.frames(60), true
        )));
        ResolvedCast cast = new ResolvedCast(ResolvedCast.Status.ACCEPTED, emptyState(), plan, Map.of(), List.of(),
            NoitaDuration.ZERO, NoitaDuration.ZERO, 0L, 3L, "catalog", new BudgetUsage(0, 0, 0, 0, 0), List.of(),
            EvaluationTrace.EMPTY);

        assertThrows(IllegalArgumentException.class, () -> PlanBudgetRequestFactory.fromResolvedCast(
            UUID.randomUUID(), UUID.randomUUID(), chunk.dimensionId(), chunk, cast));
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
