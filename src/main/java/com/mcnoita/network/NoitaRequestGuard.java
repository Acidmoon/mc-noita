package com.mcnoita.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player, per-input-stream replay and rate guard. It contains no world
 * access so sequence and token bucket behavior can be unit-tested directly.
 */
public final class NoitaRequestGuard {
    private static final double CAST_CAPACITY = 8.0;
    private static final double CAST_REFILL_PER_SECOND = 8.0;
    private static final double HOVER_CAPACITY = 12.0;
    private static final double HOVER_REFILL_PER_SECOND = 12.0;

    private final Map<UUID, StreamState> casts = new HashMap<>();
    private final Map<UUID, StreamState> hovers = new HashMap<>();

    public boolean acceptCast(UUID playerId, int sequence, long nowNanos) {
        return accept(casts, playerId, sequence, nowNanos, CAST_CAPACITY, CAST_REFILL_PER_SECOND);
    }

    public boolean acceptHover(UUID playerId, int sequence, long nowNanos) {
        return accept(hovers, playerId, sequence, nowNanos, HOVER_CAPACITY, HOVER_REFILL_PER_SECOND);
    }

    public void clear(UUID playerId) {
        casts.remove(playerId);
        hovers.remove(playerId);
    }

    private static boolean accept(
        Map<UUID, StreamState> states,
        UUID playerId,
        int sequence,
        long nowNanos,
        double capacity,
        double refillPerSecond
    ) {
        if (sequence < 0) {
            return false;
        }
        StreamState state = states.computeIfAbsent(playerId, ignored -> new StreamState(capacity, nowNanos));
        if (sequence <= state.lastSequence) {
            return false;
        }
        state.refill(nowNanos, capacity, refillPerSecond);
        state.lastSequence = sequence;
        if (state.tokens < 1.0) {
            return false;
        }
        state.tokens -= 1.0;
        return true;
    }

    private static final class StreamState {
        private int lastSequence = -1;
        private double tokens;
        private long lastRefillNanos;

        private StreamState(double capacity, long nowNanos) {
            this.tokens = capacity;
            this.lastRefillNanos = nowNanos;
        }

        private void refill(long nowNanos, double capacity, double refillPerSecond) {
            long elapsedNanos = Math.max(0L, nowNanos - lastRefillNanos);
            tokens = Math.min(capacity, tokens + elapsedNanos / 1_000_000_000.0 * refillPerSecond);
            lastRefillNanos = nowNanos;
        }
    }
}
