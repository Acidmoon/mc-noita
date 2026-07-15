package com.mcnoita.spell.server.budget;

import com.mcnoita.spell.trigger.TriggerRuntimeBudget;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handle returned by a successful reservation. State transitions are owned by
 * SpellBudgetManager so all quota maps change atomically with this handle.
 */
public final class BudgetReservation {
    public enum State {
        RESERVED,
        COMMITTED,
        CLOSED
    }

    private final SpellBudgetManager manager;
    private final BudgetRequest request;
    private final long reservedTick;
    private final long reservedWindow;
    private final EnumMap<BudgetKind, Long> releasedCosts = new EnumMap<>(BudgetKind.class);
    private final Map<ChunkBudgetKey, EnumMap<BudgetKind, Long>> releasedChunkCosts = new LinkedHashMap<>();
    private final Set<String> releaseKeys = new HashSet<>();
    private State state = State.RESERVED;

    BudgetReservation(SpellBudgetManager manager, BudgetRequest request, long reservedTick, long reservedWindow) {
        this.manager = manager;
        this.request = request;
        this.reservedTick = reservedTick;
        this.reservedWindow = reservedWindow;
    }

    public UUID executionId() {
        return request.executionId();
    }

    public BudgetRequest request() {
        return request;
    }

    /** Returns the immutable per-cast Trigger ceiling used to preflight this reservation. */
    public TriggerRuntimeBudget triggerRuntimeBudgetCeiling() {
        return manager.triggerRuntimeBudgetCeiling();
    }

    public State state() {
        return manager.stateOf(this);
    }

    /** Marks the accepted plan durable. Repeating the call is a no-op. */
    public boolean commit() {
        return manager.commit(this);
    }

    /**
     * Releases a named, immutable unused slice. Repeating a completed release
     * key is a no-op, which makes executor failure cleanup retry-safe.
     */
    public boolean releaseUnused(String releaseKey, BudgetRequest unused) {
        return manager.releaseUnused(this, releaseKey, unused);
    }

    /**
     * Moves a committed, still-owned slice into a child lease without changing
     * any quota counters. Closing the parent then leaves the child slice
     * in-flight until the long-lived owner closes the returned reservation.
     */
    public BudgetReservation transferCommittedSlice(UUID leaseExecutionId, BudgetRequest sourceSlice) {
        return manager.transferCommittedSlice(this, leaseExecutionId, sourceSlice);
    }

    /**
     * Closes the handle once. A pre-commit close returns all quota capacity;
     * a committed close keeps spent tick/window history but releases in-flight
     * capacity.
     */
    public boolean close() {
        return manager.close(this);
    }

    State stateUnsafe() {
        return state;
    }

    SpellBudgetManager managerUnsafe() {
        return manager;
    }

    void stateUnsafe(State state) {
        this.state = state;
    }

    long reservedTickUnsafe() {
        return reservedTick;
    }

    long reservedWindowUnsafe() {
        return reservedWindow;
    }

    boolean hasReleaseKeyUnsafe(String releaseKey) {
        return releaseKeys.contains(releaseKey);
    }

    void recordReleaseUnsafe(String releaseKey, BudgetRequest unused) {
        releaseKeys.add(releaseKey);
        BudgetValues.addInto(releasedCosts, unused.costs());
        for (Map.Entry<ChunkBudgetKey, Map<BudgetKind, Long>> entry : unused.chunkCosts().entrySet()) {
            BudgetValues.addInto(releasedChunkCosts.computeIfAbsent(entry.getKey(), ignored -> new EnumMap<>(BudgetKind.class)),
                entry.getValue());
        }
    }

    Map<BudgetKind, Long> remainingCostsUnsafe() {
        return BudgetValues.subtract(request.costs(), releasedCosts);
    }

    Map<ChunkBudgetKey, Map<BudgetKind, Long>> remainingChunkCostsUnsafe() {
        Map<ChunkBudgetKey, Map<BudgetKind, Long>> remaining = new LinkedHashMap<>();
        for (Map.Entry<ChunkBudgetKey, Map<BudgetKind, Long>> entry : request.chunkCosts().entrySet()) {
            Map<BudgetKind, Long> released = releasedChunkCosts.getOrDefault(entry.getKey(), new EnumMap<>(BudgetKind.class));
            Map<BudgetKind, Long> value = BudgetValues.subtract(entry.getValue(), released);
            if (!value.isEmpty()) {
                remaining.put(entry.getKey(), value);
            }
        }
        return Collections.unmodifiableMap(remaining);
    }
}
