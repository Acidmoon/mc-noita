package com.mcnoita.spell.exec;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mcnoita.spell.plan.BlockMutationEffectNode;
import com.mcnoita.spell.plan.EffectNode;
import com.mcnoita.spell.plan.ExplosionEffectNode;
import com.mcnoita.spell.plan.ProjectileEffectNode;
import com.mcnoita.spell.plan.ProjectilePlan;
import com.mcnoita.spell.server.budget.BudgetKind;
import com.mcnoita.spell.server.budget.BudgetRequest;
import com.mcnoita.spell.server.budget.ChunkBudgetKey;
import com.mcnoita.world.mutation.WorldMutationBudget;
import com.mcnoita.world.mutation.WorldMutationKind;
import com.mcnoita.wand.model.NoitaDuration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Proves a released or denied node cannot donate its root capacity to a later node. */
@Tag("regression")
class ImmediateWorldBudgetAllocatorTest {
    @Test
    void partitionsOneRootRequestIntoNonReusableNodeSlices() {
        BudgetRequest root = BudgetRequest.builder(UUID.randomUUID(), "minecraft:overworld")
            .add(BudgetKind.BLOCK_CHECKS, 2L)
            .add(BudgetKind.BLOCK_MUTATIONS, 1L)
            .build();
        List<EffectNode> nodes = List.of(
            new BlockMutationEffectNode("first", BlockMutationEffectNode.MutationKind.BREAK, 1, 1.0),
            new BlockMutationEffectNode("second", BlockMutationEffectNode.MutationKind.BREAK, 1, 1.0)
        );

        Map<String, WorldMutationBudget> slices = ImmediateWorldBudgetAllocator.allocate(root, nodes);

        assertTrue(slices.get("first").tryReserve(WorldMutationKind.BLOCK_CHECK, 2));
        assertTrue(slices.get("first").tryReserve(WorldMutationKind.BLOCK_MUTATION, 1));
        assertFalse(slices.get("second").tryReserve(WorldMutationKind.BLOCK_CHECK, 1));
        assertFalse(slices.get("second").tryReserve(WorldMutationKind.BLOCK_MUTATION, 1));
    }

    @Test
    void assignsRootProjectileSpawnCapacityFromTheCommittedPlan() {
        ProjectilePlan projectile = new ProjectilePlan("root/projectile", "spark_bolt", "BOLT", 1.0, 0.0,
            NoitaDuration.frames(60), 0, 0.0, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0, 0.0,
            false, false, 2, 0.0, null, 0, List.of());
        BudgetRequest root = BudgetRequest.builder(UUID.randomUUID(), "minecraft:overworld")
            .add(BudgetKind.AUTHORITATIVE_ENTITIES, 2L)
            .build();

        WorldMutationBudget budget = ImmediateWorldBudgetAllocator.allocate(root,
            List.of(new ProjectileEffectNode(projectile))).get("root/projectile");

        assertTrue(budget.tryReserve(WorldMutationKind.ENTITY_SPAWN, 1));
        assertTrue(budget.tryReserve(WorldMutationKind.ENTITY_SPAWN, 1));
        assertFalse(budget.tryReserve(WorldMutationKind.ENTITY_SPAWN, 1));
    }

    @Test
    void rootSliceRejectsAnOperationOutsideItsCommittedOriginChunk() {
        ChunkBudgetKey origin = new ChunkBudgetKey("minecraft:overworld", 0, 0);
        BudgetRequest root = BudgetRequest.builder(UUID.randomUUID(), "minecraft:overworld")
            .addInChunk(origin, BudgetKind.BLOCK_CHECKS, 2L)
            .addInChunk(origin, BudgetKind.BLOCK_MUTATIONS, 1L)
            .build();
        List<EffectNode> nodes = List.of(new BlockMutationEffectNode(
            "first", BlockMutationEffectNode.MutationKind.BREAK, 1, 1.0
        ));

        WorldMutationBudget budget = ImmediateWorldBudgetAllocator.allocate(root, nodes).get("first");

        assertFalse(budget.tryReserveAt(WorldMutationKind.BLOCK_CHECK, 1, new BlockPos(16, 64, 0)));
        assertTrue(budget.tryReserveAt(WorldMutationKind.BLOCK_CHECK, 1, new BlockPos(15, 64, 0)));
    }

    @Test
    void frozenExplosionEntityQueryMayCrossOnlyItsPreReservedChunks() {
        ChunkBudgetKey origin = new ChunkBudgetKey("minecraft:overworld", 0, 0);
        ChunkBudgetKey adjacent = new ChunkBudgetKey("minecraft:overworld", 1, 0);
        ExplosionEffectNode explosion = new ExplosionEffectNode("explosion", 1.0, 2.0, false);
        BudgetRequest root = BudgetRequest.builder(UUID.randomUUID(), origin.dimensionId())
            .addInChunk(origin, BudgetKind.ENTITY_SCANS, 5L)
            .addInChunk(adjacent, BudgetKind.ENTITY_SCANS, 5L)
            .build();
        Map<String, ChunkBudgetKey> primary = Map.of("explosion", origin);
        Map<String, Set<ChunkBudgetKey>> coverage = Map.of("explosion", Set.of(origin, adjacent));

        WorldMutationBudget admitted = ImmediateWorldBudgetAllocator.allocate(root, List.of(explosion), primary, coverage)
            .get("explosion");
        assertTrue(admitted.tryReserveIn(WorldMutationKind.ENTITY_QUERY, 5,
            Box.of(new Vec3d(15.5, 64.0, 8.0), 2.0, 2.0, 2.0)));

        WorldMutationBudget outsideCoverage = ImmediateWorldBudgetAllocator.allocate(root, List.of(explosion), primary, coverage)
            .get("explosion");
        assertFalse(outsideCoverage.tryReserveIn(WorldMutationKind.ENTITY_QUERY, 1,
            Box.of(new Vec3d(24.0, 64.0, 8.0), 20.0, 2.0, 2.0)));
    }
}
