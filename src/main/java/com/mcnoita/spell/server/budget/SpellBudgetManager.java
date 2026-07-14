package com.mcnoita.spell.server.budget;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Server-owned, thread-confined quota ledger. It has no Minecraft dependency so
 * transactions and persisted-job adapters can make the same deterministic
 * reservation decision before any world mutation is committed.
 */
public final class SpellBudgetManager {
    private static final Comparator<ChunkBudgetKey> CHUNK_ORDER = Comparator
        .comparing(ChunkBudgetKey::dimensionId)
        .thenComparingInt(ChunkBudgetKey::chunkX)
        .thenComparingInt(ChunkBudgetKey::chunkZ);

    private final BudgetLimits limits;
    private final Map<UUID, BudgetReservation> activeReservations = new HashMap<>();

    private final Map<UUID, EnumMap<BudgetKind, Long>> ownerInFlight = new HashMap<>();
    private final Map<ChunkBudgetKey, EnumMap<BudgetKind, Long>> chunkInFlight = new HashMap<>();
    private final Map<String, EnumMap<BudgetKind, Long>> dimensionInFlight = new HashMap<>();
    private final EnumMap<BudgetKind, Long> globalInFlight = new EnumMap<>(BudgetKind.class);

    private final Map<UUID, EnumMap<BudgetKind, Long>> ownerTick = new HashMap<>();
    private final Map<ChunkBudgetKey, EnumMap<BudgetKind, Long>> chunkTick = new HashMap<>();
    private final Map<String, EnumMap<BudgetKind, Long>> dimensionTick = new HashMap<>();
    private final EnumMap<BudgetKind, Long> globalTick = new EnumMap<>(BudgetKind.class);

    private final Map<UUID, EnumMap<BudgetKind, Long>> ownerWindow = new HashMap<>();
    private final Map<ChunkBudgetKey, EnumMap<BudgetKind, Long>> chunkWindow = new HashMap<>();
    private final Map<String, EnumMap<BudgetKind, Long>> dimensionWindow = new HashMap<>();
    private final EnumMap<BudgetKind, Long> globalWindow = new EnumMap<>(BudgetKind.class);

    private long lastTick = -1L;
    private long activeTick = -1L;
    private long activeWindow = -1L;

    public SpellBudgetManager(BudgetLimits limits) {
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    public synchronized ReservationAttempt reserve(BudgetRequest request, long serverTick) {
        Objects.requireNonNull(request, "request");
        advanceTo(serverTick);
        if (activeReservations.containsKey(request.executionId())) {
            return ReservationAttempt.rejected(new BudgetDiagnostic(
                BudgetDiagnostic.Code.DUPLICATE_EXECUTION, BudgetDiagnostic.Scope.EXECUTION, null,
                request.executionId(), request.ownerId(), request.dimensionId(), null, 0L, 0L, 0L,
                "an active reservation already owns this execution identity"
            ));
        }

        BudgetDiagnostic diagnostic = firstExceeded(request);
        if (diagnostic != null) {
            return ReservationAttempt.rejected(diagnostic);
        }

        long window = windowFor(serverTick);
        BudgetReservation reservation = new BudgetReservation(this, request, serverTick, window);
        addAllScopes(request);
        activeReservations.put(request.executionId(), reservation);
        return ReservationAttempt.accepted(reservation);
    }

    synchronized boolean commit(BudgetReservation reservation) {
        requireOwned(reservation);
        if (reservation.stateUnsafe() == BudgetReservation.State.CLOSED) {
            return false;
        }
        requireActive(reservation);
        if (reservation.stateUnsafe() == BudgetReservation.State.COMMITTED) {
            return false;
        }
        reservation.stateUnsafe(BudgetReservation.State.COMMITTED);
        return true;
    }

    synchronized boolean releaseUnused(BudgetReservation reservation, String releaseKey, BudgetRequest unused) {
        requireOwned(reservation);
        Objects.requireNonNull(releaseKey, "releaseKey");
        if (releaseKey.isBlank()) {
            throw new IllegalArgumentException("releaseKey must not be blank");
        }
        Objects.requireNonNull(unused, "unused");
        if (reservation.stateUnsafe() == BudgetReservation.State.CLOSED || reservation.hasReleaseKeyUnsafe(releaseKey)) {
            return false;
        }
        requireActive(reservation);
        validateUnusedSlice(reservation, unused);
        if (unused.isEmpty()) {
            return false;
        }

        subtractAllScopes(unused, reservation.reservedTickUnsafe(), reservation.reservedWindowUnsafe(), true);
        reservation.recordReleaseUnsafe(releaseKey, unused);
        return true;
    }

    synchronized boolean close(BudgetReservation reservation) {
        requireOwned(reservation);
        if (reservation.stateUnsafe() == BudgetReservation.State.CLOSED) {
            return false;
        }
        requireActive(reservation);

        Map<BudgetKind, Long> remainingCosts = reservation.remainingCostsUnsafe();
        Map<ChunkBudgetKey, Map<BudgetKind, Long>> remainingChunks = reservation.remainingChunkCostsUnsafe();
        BudgetRequest remaining = new BudgetRequest(reservation.executionId(), reservation.request().ownerId(),
            reservation.request().dimensionId(), remainingCosts, remainingChunks);
        boolean releaseRateHistory = reservation.stateUnsafe() == BudgetReservation.State.RESERVED;
        subtractAllScopes(remaining, reservation.reservedTickUnsafe(), reservation.reservedWindowUnsafe(), releaseRateHistory);
        reservation.stateUnsafe(BudgetReservation.State.CLOSED);
        activeReservations.remove(reservation.executionId());
        return true;
    }

    synchronized BudgetReservation.State stateOf(BudgetReservation reservation) {
        requireOwned(reservation);
        return reservation.stateUnsafe();
    }

    public synchronized int activeReservationCount() {
        return activeReservations.size();
    }

    public synchronized Map<BudgetKind, Long> globalInFlightUsage() {
        return BudgetValues.snapshot(globalInFlight);
    }

    public synchronized Map<BudgetKind, Long> globalTickUsage() {
        return BudgetValues.snapshot(globalTick);
    }

    public synchronized Map<BudgetKind, Long> globalWindowUsage() {
        return BudgetValues.snapshot(globalWindow);
    }

    private void advanceTo(long serverTick) {
        if (serverTick < 0L) {
            throw new IllegalArgumentException("serverTick must not be negative");
        }
        if (lastTick > serverTick) {
            throw new IllegalArgumentException("serverTick must be monotonic for one budget manager lifetime");
        }
        lastTick = serverTick;
        if (activeTick != serverTick) {
            ownerTick.clear();
            chunkTick.clear();
            dimensionTick.clear();
            globalTick.clear();
            activeTick = serverTick;
        }
        long window = windowFor(serverTick);
        if (activeWindow != window) {
            ownerWindow.clear();
            chunkWindow.clear();
            dimensionWindow.clear();
            globalWindow.clear();
            activeWindow = window;
        }
    }

    private long windowFor(long serverTick) {
        return serverTick / limits.windowTicks();
    }

    private BudgetDiagnostic firstExceeded(BudgetRequest request) {
        BudgetDiagnostic diagnostic = check(request, limits.perCast(), new EnumMap<>(BudgetKind.class),
            BudgetDiagnostic.Scope.PER_CAST, null, request.dimensionId(), null);
        if (diagnostic != null) {
            return diagnostic;
        }

        if (request.hasOwner()) {
            diagnostic = check(request, limits.owner().inFlight(), ownerInFlight.get(request.ownerId()),
                BudgetDiagnostic.Scope.OWNER_IN_FLIGHT, request.ownerId(), request.dimensionId(), null);
            if (diagnostic != null) {
                return diagnostic;
            }
            diagnostic = check(request, limits.owner().perTick(), ownerTick.get(request.ownerId()),
                BudgetDiagnostic.Scope.OWNER_TICK, request.ownerId(), request.dimensionId(), null);
            if (diagnostic != null) {
                return diagnostic;
            }
            diagnostic = check(request, limits.owner().perWindow(), ownerWindow.get(request.ownerId()),
                BudgetDiagnostic.Scope.OWNER_WINDOW, request.ownerId(), request.dimensionId(), null);
            if (diagnostic != null) {
                return diagnostic;
            }
        }

        for (Map.Entry<ChunkBudgetKey, Map<BudgetKind, Long>> entry : request.chunkCosts().entrySet().stream()
            .sorted(Map.Entry.comparingByKey(CHUNK_ORDER)).toList()) {
            ChunkBudgetKey chunk = entry.getKey();
            diagnostic = check(request, entry.getValue(), limits.chunk().inFlight(), chunkInFlight.get(chunk),
                BudgetDiagnostic.Scope.CHUNK_IN_FLIGHT, request.ownerId(), request.dimensionId(), chunk);
            if (diagnostic != null) {
                return diagnostic;
            }
            diagnostic = check(request, entry.getValue(), limits.chunk().perTick(), chunkTick.get(chunk),
                BudgetDiagnostic.Scope.CHUNK_TICK, request.ownerId(), request.dimensionId(), chunk);
            if (diagnostic != null) {
                return diagnostic;
            }
            diagnostic = check(request, entry.getValue(), limits.chunk().perWindow(), chunkWindow.get(chunk),
                BudgetDiagnostic.Scope.CHUNK_WINDOW, request.ownerId(), request.dimensionId(), chunk);
            if (diagnostic != null) {
                return diagnostic;
            }
        }

        diagnostic = check(request, limits.dimension().inFlight(), dimensionInFlight.get(request.dimensionId()),
            BudgetDiagnostic.Scope.DIMENSION_IN_FLIGHT, request.ownerId(), request.dimensionId(), null);
        if (diagnostic != null) {
            return diagnostic;
        }
        diagnostic = check(request, limits.dimension().perTick(), dimensionTick.get(request.dimensionId()),
            BudgetDiagnostic.Scope.DIMENSION_TICK, request.ownerId(), request.dimensionId(), null);
        if (diagnostic != null) {
            return diagnostic;
        }
        diagnostic = check(request, limits.dimension().perWindow(), dimensionWindow.get(request.dimensionId()),
            BudgetDiagnostic.Scope.DIMENSION_WINDOW, request.ownerId(), request.dimensionId(), null);
        if (diagnostic != null) {
            return diagnostic;
        }

        diagnostic = check(request, limits.global().inFlight(), globalInFlight, BudgetDiagnostic.Scope.GLOBAL_IN_FLIGHT,
            request.ownerId(), request.dimensionId(), null);
        if (diagnostic != null) {
            return diagnostic;
        }
        diagnostic = check(request, limits.global().perTick(), globalTick, BudgetDiagnostic.Scope.GLOBAL_TICK,
            request.ownerId(), request.dimensionId(), null);
        if (diagnostic != null) {
            return diagnostic;
        }
        return check(request, limits.global().perWindow(), globalWindow, BudgetDiagnostic.Scope.GLOBAL_WINDOW,
            request.ownerId(), request.dimensionId(), null);
    }

    private BudgetDiagnostic check(
        BudgetRequest request, Map<BudgetKind, Long> limits, Map<BudgetKind, Long> usage,
        BudgetDiagnostic.Scope scope, UUID owner, String dimension, ChunkBudgetKey chunk
    ) {
        return check(request, request.costs(), limits, usage, scope, owner, dimension, chunk);
    }

    private BudgetDiagnostic check(
        BudgetRequest request, Map<BudgetKind, Long> requested, Map<BudgetKind, Long> limits,
        Map<BudgetKind, Long> usage, BudgetDiagnostic.Scope scope, UUID owner, String dimension, ChunkBudgetKey chunk
    ) {
        Map<BudgetKind, Long> existing = usage == null ? Map.of() : usage;
        for (BudgetKind kind : BudgetKind.values()) {
            long amount = BudgetValues.amount(requested, kind);
            if (amount == 0L) {
                continue;
            }
            long used = BudgetValues.amount(existing, kind);
            long limit = BudgetLimits.effectiveLimit(limits, kind);
            if (amount > limit || used > limit - amount) {
                return new BudgetDiagnostic(BudgetDiagnostic.Code.LIMIT_EXCEEDED, scope, kind,
                    request.executionId(), owner, dimension, chunk, amount, used, limit, "budget limit exceeded");
            }
        }
        return null;
    }

    private void addAllScopes(BudgetRequest request) {
        if (request.hasOwner()) {
            add(ownerInFlight, request.ownerId(), request.costs());
            add(ownerTick, request.ownerId(), request.costs());
            add(ownerWindow, request.ownerId(), request.costs());
        }
        for (Map.Entry<ChunkBudgetKey, Map<BudgetKind, Long>> entry : request.chunkCosts().entrySet()) {
            add(chunkInFlight, entry.getKey(), entry.getValue());
            add(chunkTick, entry.getKey(), entry.getValue());
            add(chunkWindow, entry.getKey(), entry.getValue());
        }
        add(dimensionInFlight, request.dimensionId(), request.costs());
        add(dimensionTick, request.dimensionId(), request.costs());
        add(dimensionWindow, request.dimensionId(), request.costs());
        BudgetValues.addInto(globalInFlight, request.costs());
        BudgetValues.addInto(globalTick, request.costs());
        BudgetValues.addInto(globalWindow, request.costs());
    }

    private void subtractAllScopes(
        BudgetRequest request, long reservedTick, long reservedWindow, boolean releaseRateHistory
    ) {
        if (request.hasOwner()) {
            subtract(ownerInFlight, request.ownerId(), request.costs());
            if (releaseRateHistory && reservedTick == activeTick) {
                subtract(ownerTick, request.ownerId(), request.costs());
            }
            if (releaseRateHistory && reservedWindow == activeWindow) {
                subtract(ownerWindow, request.ownerId(), request.costs());
            }
        }
        for (Map.Entry<ChunkBudgetKey, Map<BudgetKind, Long>> entry : request.chunkCosts().entrySet()) {
            subtract(chunkInFlight, entry.getKey(), entry.getValue());
            if (releaseRateHistory && reservedTick == activeTick) {
                subtract(chunkTick, entry.getKey(), entry.getValue());
            }
            if (releaseRateHistory && reservedWindow == activeWindow) {
                subtract(chunkWindow, entry.getKey(), entry.getValue());
            }
        }
        subtract(dimensionInFlight, request.dimensionId(), request.costs());
        if (releaseRateHistory && reservedTick == activeTick) {
            subtract(dimensionTick, request.dimensionId(), request.costs());
        }
        if (releaseRateHistory && reservedWindow == activeWindow) {
            subtract(dimensionWindow, request.dimensionId(), request.costs());
        }
        BudgetValues.subtractFrom(globalInFlight, request.costs());
        if (releaseRateHistory && reservedTick == activeTick) {
            BudgetValues.subtractFrom(globalTick, request.costs());
        }
        if (releaseRateHistory && reservedWindow == activeWindow) {
            BudgetValues.subtractFrom(globalWindow, request.costs());
        }
    }

    private void validateUnusedSlice(BudgetReservation reservation, BudgetRequest unused) {
        BudgetRequest original = reservation.request();
        if (!original.executionId().equals(unused.executionId())
            || !Objects.equals(original.ownerId(), unused.ownerId())
            || !original.dimensionId().equals(unused.dimensionId())) {
            throw new IllegalArgumentException("unused slice must retain the reservation identity and scope");
        }
        if (!BudgetValues.fitsWithin(unused.costs(), reservation.remainingCostsUnsafe())) {
            throw new IllegalArgumentException("unused slice exceeds remaining reservation capacity");
        }
        Map<ChunkBudgetKey, Map<BudgetKind, Long>> remainingChunks = reservation.remainingChunkCostsUnsafe();
        for (Map.Entry<ChunkBudgetKey, Map<BudgetKind, Long>> entry : unused.chunkCosts().entrySet()) {
            Map<BudgetKind, Long> remaining = remainingChunks.get(entry.getKey());
            if (remaining == null || !BudgetValues.fitsWithin(entry.getValue(), remaining)) {
                throw new IllegalArgumentException("unused chunk slice exceeds remaining reservation capacity");
            }
        }
        // A slice can include non-chunk work, but it cannot release total work
        // while leaving more chunk-local work than the remaining total permits.
        Map<ChunkBudgetKey, Map<BudgetKind, Long>> nextChunks = new LinkedHashMap<>();
        for (Map.Entry<ChunkBudgetKey, Map<BudgetKind, Long>> entry : remainingChunks.entrySet()) {
            Map<BudgetKind, Long> released = unused.chunkCosts().getOrDefault(entry.getKey(), Map.of());
            Map<BudgetKind, Long> next = BudgetValues.subtract(entry.getValue(), released);
            if (!next.isEmpty()) {
                nextChunks.put(entry.getKey(), next);
            }
        }
        new BudgetRequest(original.executionId(), original.ownerId(), original.dimensionId(),
            BudgetValues.subtract(reservation.remainingCostsUnsafe(), unused.costs()), nextChunks);
    }

    private void requireActive(BudgetReservation reservation) {
        requireOwned(reservation);
        if (activeReservations.get(reservation.executionId()) != reservation) {
            throw new IllegalArgumentException("reservation is not active in this manager");
        }
    }

    private void requireOwned(BudgetReservation reservation) {
        Objects.requireNonNull(reservation, "reservation");
        if (reservation.managerUnsafe() != this) {
            throw new IllegalArgumentException("reservation belongs to another budget manager");
        }
    }

    private static <K> void add(Map<K, EnumMap<BudgetKind, Long>> usage, K key, Map<BudgetKind, Long> costs) {
        BudgetValues.addInto(usage.computeIfAbsent(key, ignored -> new EnumMap<>(BudgetKind.class)), costs);
    }

    private static <K> void subtract(Map<K, EnumMap<BudgetKind, Long>> usage, K key, Map<BudgetKind, Long> costs) {
        EnumMap<BudgetKind, Long> values = usage.get(key);
        if (values == null) {
            if (costs.isEmpty()) {
                return;
            }
            throw new IllegalStateException("budget scope was released twice");
        }
        BudgetValues.subtractFrom(values, costs);
        if (values.isEmpty()) {
            usage.remove(key);
        }
    }

    public record ReservationAttempt(BudgetReservation reservation, BudgetDiagnostic diagnostic) {
        public ReservationAttempt {
            if ((reservation == null) == (diagnostic == null)) {
                throw new IllegalArgumentException("reservation attempt must contain exactly one outcome");
            }
        }

        public static ReservationAttempt accepted(BudgetReservation reservation) {
            return new ReservationAttempt(Objects.requireNonNull(reservation, "reservation"), null);
        }

        public static ReservationAttempt rejected(BudgetDiagnostic diagnostic) {
            return new ReservationAttempt(null, Objects.requireNonNull(diagnostic, "diagnostic"));
        }

        public boolean accepted() {
            return reservation != null;
        }
    }
}
