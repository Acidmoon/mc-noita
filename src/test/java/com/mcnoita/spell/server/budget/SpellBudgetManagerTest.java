package com.mcnoita.spell.server.budget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Pure regression coverage for the server reservation ledger. */
@Tag("regression")
class SpellBudgetManagerTest {
    private static final String OVERWORLD = "minecraft:overworld";

    @Test
    void failedReservationIsAllOrNothingAcrossScopes() {
        BudgetLimits.ScopeLimits global = scope(Map.of(BudgetKind.AUTHORITATIVE_ENTITIES, 4L), Map.of(), Map.of());
        SpellBudgetManager manager = new SpellBudgetManager(limits(Map.of(), unlimited(), unlimited(), unlimited(), global, 20));
        UUID owner = UUID.randomUUID();

        BudgetReservation first = accepted(manager, request(owner, OVERWORLD, BudgetKind.AUTHORITATIVE_ENTITIES, 2L), 0L);
        SpellBudgetManager.ReservationAttempt rejected = manager.reserve(
            request(owner, OVERWORLD, BudgetKind.AUTHORITATIVE_ENTITIES, 3L), 0L);

        assertFalse(rejected.accepted());
        assertEquals(BudgetDiagnostic.Scope.GLOBAL_IN_FLIGHT, rejected.diagnostic().scope());
        assertEquals(Map.of(BudgetKind.AUTHORITATIVE_ENTITIES, 2L), manager.globalInFlightUsage());

        BudgetReservation finalSlot = accepted(manager,
            request(owner, OVERWORLD, BudgetKind.AUTHORITATIVE_ENTITIES, 2L), 0L);
        assertEquals(Map.of(BudgetKind.AUTHORITATIVE_ENTITIES, 4L), manager.globalInFlightUsage());

        assertTrue(first.close());
        assertTrue(finalSlot.close());
    }

    @Test
    void defaultsExposeConservativeCapsAndHardMaximumsCannotBeBypassed() {
        assertEquals(2_048L, BudgetLimits.DEFAULT.perCast().get(BudgetKind.ACTION_NODES));
        assertEquals(128L, BudgetLimits.DEFAULT.perCast().get(BudgetKind.LOGICAL_PROJECTILES));
        assertEquals(32L, BudgetLimits.DEFAULT.perCast().get(BudgetKind.AUTHORITATIVE_ENTITIES));
        assertEquals(4_096L, BudgetLimits.DEFAULT.perCast().get(BudgetKind.BLOCK_CHECKS));
        assertEquals(512L, BudgetLimits.DEFAULT.perCast().get(BudgetKind.BLOCK_MUTATIONS));
        assertEquals(131_072L, BudgetLimits.DEFAULT.perCast().get(BudgetKind.NBT_BYTES));
        assertEquals(16L, BudgetLimits.DEFAULT.perCast().get(BudgetKind.NETWORK_PACKETS));
        assertEquals(256L, BudgetLimits.DEFAULT.perCast().get(BudgetKind.VISUAL_EVENTS));
        assertEquals(512L, BudgetLimits.DEFAULT.perCast().get(BudgetKind.CROSS_TICK_JOB_STEPS));
        assertEquals(8L, BudgetLimits.DEFAULT.perCast().get(BudgetKind.PERSISTENT_JOBS));

        long hardActionMaximum = BudgetLimits.HARD_MAXIMUMS.get(BudgetKind.ACTION_NODES);
        assertThrows(IllegalArgumentException.class, () -> new BudgetLimits(
            Map.of(BudgetKind.ACTION_NODES, hardActionMaximum + 1L), unlimited(), unlimited(), unlimited(), unlimited(), 20L));
        assertThrows(IllegalArgumentException.class, () -> scope(
            Map.of(BudgetKind.ACTION_NODES, hardActionMaximum + 1L), Map.of(), Map.of()));

        SpellBudgetManager manager = new SpellBudgetManager(BudgetLimits.unlimited());
        assertRejected(manager.reserve(request(null, OVERWORLD, BudgetKind.ACTION_NODES, hardActionMaximum + 1L), 0L),
            BudgetDiagnostic.Scope.PER_CAST);
    }

    @Test
    void perCastAndEveryScopedQuotaRejectsTheCorrectReservation() {
        SpellBudgetManager perCast = new SpellBudgetManager(limits(
            Map.of(BudgetKind.BLOCK_MUTATIONS, 1L), unlimited(), unlimited(), unlimited(), unlimited(), 20));
        assertRejected(perCast.reserve(request(null, OVERWORLD, BudgetKind.BLOCK_MUTATIONS, 2L), 0L),
            BudgetDiagnostic.Scope.PER_CAST);

        UUID sharedOwner = UUID.randomUUID();
        SpellBudgetManager owner = new SpellBudgetManager(limits(Map.of(),
            scope(Map.of(BudgetKind.BLOCK_CHECKS, 1L), Map.of(), Map.of()), unlimited(), unlimited(), unlimited(), 20));
        accepted(owner, request(sharedOwner, OVERWORLD, BudgetKind.BLOCK_CHECKS, 1L), 0L);
        assertRejected(owner.reserve(request(sharedOwner, OVERWORLD, BudgetKind.BLOCK_CHECKS, 1L), 0L),
            BudgetDiagnostic.Scope.OWNER_IN_FLIGHT);

        ChunkBudgetKey firstChunk = new ChunkBudgetKey(OVERWORLD, 0, 0);
        SpellBudgetManager chunk = new SpellBudgetManager(limits(Map.of(), unlimited(),
            scope(Map.of(BudgetKind.BLOCK_CHECKS, 1L), Map.of(), Map.of()), unlimited(), unlimited(), 20));
        accepted(chunk, inChunk(UUID.randomUUID(), firstChunk, BudgetKind.BLOCK_CHECKS, 1L), 0L);
        assertRejected(chunk.reserve(inChunk(UUID.randomUUID(), firstChunk, BudgetKind.BLOCK_CHECKS, 1L), 0L),
            BudgetDiagnostic.Scope.CHUNK_IN_FLIGHT);

        SpellBudgetManager dimension = new SpellBudgetManager(limits(Map.of(), unlimited(), unlimited(),
            scope(Map.of(BudgetKind.BLOCK_CHECKS, 1L), Map.of(), Map.of()), unlimited(), 20));
        accepted(dimension, inChunk(UUID.randomUUID(), new ChunkBudgetKey(OVERWORLD, 0, 0), BudgetKind.BLOCK_CHECKS, 1L), 0L);
        assertRejected(dimension.reserve(inChunk(UUID.randomUUID(), new ChunkBudgetKey(OVERWORLD, 1, 0),
            BudgetKind.BLOCK_CHECKS, 1L), 0L), BudgetDiagnostic.Scope.DIMENSION_IN_FLIGHT);

        SpellBudgetManager global = new SpellBudgetManager(limits(Map.of(), unlimited(), unlimited(), unlimited(),
            scope(Map.of(BudgetKind.BLOCK_CHECKS, 1L), Map.of(), Map.of()), 20));
        accepted(global, request(UUID.randomUUID(), OVERWORLD, BudgetKind.BLOCK_CHECKS, 1L), 0L);
        assertRejected(global.reserve(request(UUID.randomUUID(), "minecraft:the_nether", BudgetKind.BLOCK_CHECKS, 1L), 0L),
            BudgetDiagnostic.Scope.GLOBAL_IN_FLIGHT);

        BudgetLimits.ScopeLimits tickScope = scope(Map.of(), Map.of(BudgetKind.NETWORK_PACKETS, 1L), Map.of());
        SpellBudgetManager tick = new SpellBudgetManager(limits(Map.of(), unlimited(), unlimited(), unlimited(), tickScope, 20));
        BudgetReservation tickFirst = accepted(tick, request(null, OVERWORLD, BudgetKind.NETWORK_PACKETS, 1L), 4L);
        assertTrue(tickFirst.commit());
        assertTrue(tickFirst.close());
        assertRejected(tick.reserve(request(null, OVERWORLD, BudgetKind.NETWORK_PACKETS, 1L), 4L),
            BudgetDiagnostic.Scope.GLOBAL_TICK);
        assertTrue(tick.reserve(request(null, OVERWORLD, BudgetKind.NETWORK_PACKETS, 1L), 5L).accepted());

        BudgetLimits.ScopeLimits windowScope = scope(Map.of(), Map.of(), Map.of(BudgetKind.NETWORK_BYTES, 1L));
        SpellBudgetManager window = new SpellBudgetManager(limits(Map.of(), unlimited(), unlimited(), unlimited(), windowScope, 10));
        BudgetReservation windowFirst = accepted(window, request(null, OVERWORLD, BudgetKind.NETWORK_BYTES, 1L), 0L);
        assertTrue(windowFirst.commit());
        assertTrue(windowFirst.close());
        assertRejected(window.reserve(request(null, OVERWORLD, BudgetKind.NETWORK_BYTES, 1L), 9L),
            BudgetDiagnostic.Scope.GLOBAL_WINDOW);
        assertTrue(window.reserve(request(null, OVERWORLD, BudgetKind.NETWORK_BYTES, 1L), 10L).accepted());
    }

    @Test
    void reservationTransitionsOnlyFromReservedToCommittedToClosed() {
        SpellBudgetManager manager = new SpellBudgetManager(BudgetLimits.unlimited());
        BudgetReservation reservation = accepted(manager,
            request(UUID.randomUUID(), OVERWORLD, BudgetKind.LOGICAL_PROJECTILES, 1L), 0L);

        assertEquals(BudgetReservation.State.RESERVED, reservation.state());
        assertTrue(reservation.commit());
        assertFalse(reservation.commit());
        assertEquals(BudgetReservation.State.COMMITTED, reservation.state());
        assertTrue(reservation.close());
        assertEquals(BudgetReservation.State.CLOSED, reservation.state());
        assertFalse(reservation.close());
        assertFalse(reservation.commit());
        assertFalse(reservation.releaseUnused("after-close", requestForExistingExecution(
            reservation, BudgetKind.LOGICAL_PROJECTILES, 1L)));
        assertEquals(0, manager.activeReservationCount());
    }

    @Test
    void namedUnusedReleaseIsIdempotentAndFreesOnlyTheReleasedCapacity() {
        ChunkBudgetKey chunk = new ChunkBudgetKey(OVERWORLD, 3, -2);
        BudgetLimits.ScopeLimits chunkLimits = scope(Map.of(BudgetKind.AUTHORITATIVE_ENTITIES, 4L), Map.of(), Map.of());
        SpellBudgetManager manager = new SpellBudgetManager(limits(Map.of(), unlimited(), chunkLimits, unlimited(), unlimited(), 20));
        UUID owner = UUID.randomUUID();
        BudgetReservation reservation = accepted(manager,
            inChunk(owner, chunk, BudgetKind.AUTHORITATIVE_ENTITIES, 4L), 0L);
        assertTrue(reservation.commit());

        BudgetRequest unused = requestForExistingExecution(reservation, chunk, BudgetKind.AUTHORITATIVE_ENTITIES, 2L);
        assertTrue(reservation.releaseUnused("failed-projectile-node", unused));
        assertFalse(reservation.releaseUnused("failed-projectile-node", unused));

        BudgetReservation second = accepted(manager,
            inChunk(owner, chunk, BudgetKind.AUTHORITATIVE_ENTITIES, 2L), 0L);
        assertRejected(manager.reserve(inChunk(owner, chunk, BudgetKind.AUTHORITATIVE_ENTITIES, 1L), 0L),
            BudgetDiagnostic.Scope.CHUNK_IN_FLIGHT);

        assertTrue(second.close());
        assertTrue(reservation.close());
        assertFalse(reservation.close());
        assertTrue(manager.reserve(inChunk(owner, chunk, BudgetKind.AUTHORITATIVE_ENTITIES, 4L), 0L).accepted());
    }

    @Test
    void invalidUnusedSliceLeavesEveryScopeUntouched() {
        ChunkBudgetKey chunk = new ChunkBudgetKey(OVERWORLD, 6, 1);
        BudgetLimits.ScopeLimits chunkLimits = scope(Map.of(BudgetKind.AUTHORITATIVE_ENTITIES, 4L), Map.of(), Map.of());
        SpellBudgetManager manager = new SpellBudgetManager(limits(Map.of(), unlimited(), chunkLimits, unlimited(), unlimited(), 20));
        UUID owner = UUID.randomUUID();
        BudgetReservation reservation = accepted(manager,
            inChunk(owner, chunk, BudgetKind.AUTHORITATIVE_ENTITIES, 4L), 0L);
        assertTrue(reservation.commit());

        BudgetRequest malformed = new BudgetRequest(reservation.executionId(), owner, OVERWORLD,
            Map.of(BudgetKind.AUTHORITATIVE_ENTITIES, 4L),
            Map.of(chunk, Map.of(BudgetKind.AUTHORITATIVE_ENTITIES, 2L)));
        assertThrows(IllegalArgumentException.class, () -> reservation.releaseUnused("malformed", malformed));

        assertRejected(manager.reserve(inChunk(owner, chunk, BudgetKind.AUTHORITATIVE_ENTITIES, 1L), 0L),
            BudgetDiagnostic.Scope.CHUNK_IN_FLIGHT);
        assertTrue(reservation.close());
    }

    private static BudgetReservation accepted(SpellBudgetManager manager, BudgetRequest request, long tick) {
        SpellBudgetManager.ReservationAttempt attempt = manager.reserve(request, tick);
        assertTrue(attempt.accepted(), () -> "expected reservation but got " + attempt.diagnostic());
        return attempt.reservation();
    }

    private static void assertRejected(SpellBudgetManager.ReservationAttempt attempt, BudgetDiagnostic.Scope expectedScope) {
        assertFalse(attempt.accepted());
        assertEquals(expectedScope, attempt.diagnostic().scope());
    }

    private static BudgetRequest request(UUID owner, String dimension, BudgetKind kind, long amount) {
        return BudgetRequest.builder(UUID.randomUUID(), dimension)
            .owner(owner)
            .add(kind, amount)
            .build();
    }

    private static BudgetRequest inChunk(UUID owner, ChunkBudgetKey chunk, BudgetKind kind, long amount) {
        return BudgetRequest.builder(UUID.randomUUID(), chunk.dimensionId())
            .owner(owner)
            .addInChunk(chunk, kind, amount)
            .build();
    }

    private static BudgetRequest requestForExistingExecution(BudgetReservation reservation, BudgetKind kind, long amount) {
        return BudgetRequest.builder(reservation.executionId(), reservation.request().dimensionId())
            .owner(reservation.request().ownerId())
            .add(kind, amount)
            .build();
    }

    private static BudgetRequest requestForExistingExecution(
        BudgetReservation reservation, ChunkBudgetKey chunk, BudgetKind kind, long amount
    ) {
        return BudgetRequest.builder(reservation.executionId(), reservation.request().dimensionId())
            .owner(reservation.request().ownerId())
            .addInChunk(chunk, kind, amount)
            .build();
    }

    private static BudgetLimits limits(
        Map<BudgetKind, Long> perCast,
        BudgetLimits.ScopeLimits owner,
        BudgetLimits.ScopeLimits chunk,
        BudgetLimits.ScopeLimits dimension,
        BudgetLimits.ScopeLimits global,
        long windowTicks
    ) {
        return new BudgetLimits(perCast, owner, chunk, dimension, global, windowTicks);
    }

    private static BudgetLimits.ScopeLimits scope(
        Map<BudgetKind, Long> inFlight, Map<BudgetKind, Long> perTick, Map<BudgetKind, Long> perWindow
    ) {
        return new BudgetLimits.ScopeLimits(inFlight, perTick, perWindow);
    }

    private static BudgetLimits.ScopeLimits unlimited() {
        return BudgetLimits.ScopeLimits.unlimited();
    }
}
