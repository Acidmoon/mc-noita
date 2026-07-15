package com.mcnoita.world.mutation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mcnoita.spell.NoitaExecutionIdentity;
import com.mcnoita.spell.server.budget.BudgetKind;
import com.mcnoita.spell.server.budget.BudgetLimits;
import com.mcnoita.spell.server.budget.SpellBudgetManager;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Regression coverage for centrally charged legacy projectile world operations. */
@Tag("regression")
class ServerWorldMutationBudgetTest {
    private static final String OVERWORLD = "minecraft:overworld";
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final NoitaExecutionIdentity IDENTITY = new NoitaExecutionIdentity(
        UUID.fromString("00000000-0000-0000-0000-000000000004"), "root/0/instance/0", 7L, "a".repeat(64));

    @Test
    void mapsEveryWorldOperationToItsCentralBudgetKind() {
        assertEquals(BudgetKind.BLOCK_CHECKS, ServerWorldMutationBudget.budgetKind(WorldMutationKind.BLOCK_CHECK));
        assertEquals(BudgetKind.BLOCK_MUTATIONS, ServerWorldMutationBudget.budgetKind(WorldMutationKind.BLOCK_MUTATION));
        assertEquals(BudgetKind.ENTITY_SCANS, ServerWorldMutationBudget.budgetKind(WorldMutationKind.ENTITY_QUERY));
        assertEquals(BudgetKind.ENTITY_SCANS, ServerWorldMutationBudget.budgetKind(WorldMutationKind.EXPLOSION));
        assertEquals(BudgetKind.AUTHORITATIVE_ENTITIES, ServerWorldMutationBudget.budgetKind(WorldMutationKind.ENTITY_SPAWN));
    }

    @Test
    void committedOperationsRetainTickHistoryButReleaseInFlightCapacity() {
        SpellBudgetManager manager = new SpellBudgetManager(BudgetLimits.unlimited());
        ServerWorldMutationBudget budget = budget(manager, () -> 40L, () -> 12L);

        assertTrue(budget.tryReserve(WorldMutationKind.BLOCK_CHECK, 1));
        assertTrue(budget.tryReserve(WorldMutationKind.BLOCK_MUTATION, 2));
        assertTrue(budget.tryReserve(WorldMutationKind.ENTITY_QUERY, 3));
        assertTrue(budget.tryReserve(WorldMutationKind.EXPLOSION, 1));
        assertTrue(budget.tryReserve(WorldMutationKind.ENTITY_SPAWN, 4));

        assertEquals(Map.of(
            BudgetKind.BLOCK_CHECKS, 1L,
            BudgetKind.BLOCK_MUTATIONS, 2L,
            BudgetKind.ENTITY_SCANS, 4L,
            BudgetKind.AUTHORITATIVE_ENTITIES, 4L
        ), manager.globalTickUsage());
        assertEquals(Map.of(), manager.globalInFlightUsage());
        assertEquals(0, manager.activeReservationCount());
    }

    @Test
    void centralTickLimitRejectsTheSecondRuntimeOperation() {
        BudgetLimits.ScopeLimits global = new BudgetLimits.ScopeLimits(Map.of(),
            Map.of(BudgetKind.BLOCK_CHECKS, 1L), Map.of());
        SpellBudgetManager manager = new SpellBudgetManager(new BudgetLimits(Map.of(), BudgetLimits.ScopeLimits.unlimited(),
            BudgetLimits.ScopeLimits.unlimited(), BudgetLimits.ScopeLimits.unlimited(), global, 20L));
        ServerWorldMutationBudget budget = budget(manager, () -> 4L, () -> 8L);

        assertTrue(budget.tryReserve(WorldMutationKind.BLOCK_CHECK, 1));
        assertFalse(budget.tryReserve(WorldMutationKind.BLOCK_CHECK, 1));
        assertEquals(Map.of(BudgetKind.BLOCK_CHECKS, 1L), manager.globalTickUsage());
    }

    @Test
    void serverTickRemainsMonotonicWhenWorldTimeMovesBackward() {
        SpellBudgetManager manager = new SpellBudgetManager(BudgetLimits.unlimited());
        AtomicLong worldTime = new AtomicLong(8_000L);
        AtomicLong serverTick = new AtomicLong(100L);
        ServerWorldMutationBudget budget = budget(manager, worldTime::get, serverTick::get);

        assertTrue(budget.tryReserve(WorldMutationKind.BLOCK_CHECK, 1));
        worldTime.set(0L);
        serverTick.incrementAndGet();
        assertTrue(budget.tryReserve(WorldMutationKind.BLOCK_MUTATION, 1));
    }

    @Test
    void unboundIdentityFailsClosedAndDerivedIdsAreFreshPerSequence() {
        SpellBudgetManager manager = new SpellBudgetManager(BudgetLimits.unlimited());
        ServerWorldMutationBudget unbound = new ServerWorldMutationBudget(manager, OWNER, OVERWORLD,
            NoitaExecutionIdentity.unbound("legacy"), () -> 0L, () -> 0L, () -> 0, () -> 0);

        assertFalse(unbound.tryReserve(WorldMutationKind.ENTITY_SPAWN, 1));
        assertEquals(Map.of(), manager.globalTickUsage());
        assertNotEquals(ServerWorldMutationBudget.deriveOperationId(IDENTITY, 10L, WorldMutationKind.BLOCK_CHECK, 1L),
            ServerWorldMutationBudget.deriveOperationId(IDENTITY, 10L, WorldMutationKind.BLOCK_CHECK, 2L));
    }

    @Test
    void delayedOperationChargesItsTouchedChunkAndEveryCoveredQueryChunk() {
        BudgetLimits.ScopeLimits onlyOneCheckPerChunk = new BudgetLimits.ScopeLimits(Map.of(),
            Map.of(BudgetKind.BLOCK_CHECKS, 1L, BudgetKind.ENTITY_SCANS, 1L), Map.of());
        SpellBudgetManager manager = new SpellBudgetManager(new BudgetLimits(Map.of(), BudgetLimits.ScopeLimits.unlimited(),
            onlyOneCheckPerChunk, BudgetLimits.ScopeLimits.unlimited(), BudgetLimits.ScopeLimits.unlimited(), 20L));
        ServerWorldMutationBudget budget = budget(manager, () -> 4L, () -> 8L);

        BlockPos touchedChunk = new BlockPos(48, 64, 0);
        assertTrue(budget.tryReserveAt(WorldMutationKind.BLOCK_CHECK, 1, touchedChunk));
        assertFalse(budget.tryReserveAt(WorldMutationKind.BLOCK_CHECK, 1, touchedChunk));
        assertTrue(budget.tryReserveAt(WorldMutationKind.BLOCK_CHECK, 1, new BlockPos(64, 64, 0)));
        Box crossingQuery = new Box(15.0, 0.0, 0.0, 17.0, 1.0, 1.0);
        assertTrue(budget.tryReserveIn(WorldMutationKind.ENTITY_QUERY, 1, crossingQuery));
        assertFalse(budget.tryReserveIn(WorldMutationKind.ENTITY_QUERY, 1, crossingQuery));
        assertEquals(Map.of(BudgetKind.BLOCK_CHECKS, 2L, BudgetKind.ENTITY_SCANS, 2L), manager.globalTickUsage());
    }

    @Test
    void crossChunkQueryReservationIsAtomicWhenOneCoveredChunkIsFull() {
        BudgetLimits.ScopeLimits onlyOneScanPerChunk = new BudgetLimits.ScopeLimits(Map.of(),
            Map.of(BudgetKind.ENTITY_SCANS, 1L), Map.of());
        SpellBudgetManager manager = new SpellBudgetManager(new BudgetLimits(Map.of(), BudgetLimits.ScopeLimits.unlimited(),
            onlyOneScanPerChunk, BudgetLimits.ScopeLimits.unlimited(), BudgetLimits.ScopeLimits.unlimited(), 20L));
        ServerWorldMutationBudget budget = budget(manager, () -> 4L, () -> 8L);

        assertTrue(budget.tryReserveAt(WorldMutationKind.ENTITY_QUERY, 1, new BlockPos(16, 64, 0)));
        assertFalse(budget.tryReserveIn(WorldMutationKind.ENTITY_QUERY, 1, new Box(15.0, 0.0, 0.0, 17.0, 1.0, 1.0)));
        assertTrue(budget.tryReserveAt(WorldMutationKind.ENTITY_QUERY, 1, new BlockPos(0, 64, 0)),
            "the rejected two-chunk request must not consume the first chunk's slice");
        assertEquals(Map.of(BudgetKind.ENTITY_SCANS, 2L), manager.globalTickUsage());
    }

    private static ServerWorldMutationBudget budget(
        SpellBudgetManager manager, java.util.function.LongSupplier worldTime, java.util.function.LongSupplier serverTick
    ) {
        return new ServerWorldMutationBudget(manager, OWNER, OVERWORLD, IDENTITY, worldTime, serverTick, () -> 3, () -> -2);
    }
}
