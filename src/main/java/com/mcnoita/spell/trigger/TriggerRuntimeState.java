package com.mcnoita.spell.trigger;

import java.util.Objects;

/**
 * Pure, persisted state for a single frozen trigger node. It intentionally has
 * separate final-event flags: a Timer may collide repeatedly before its one
 * timer-expiry release, while an Expiration payload can release at most once.
 */
public record TriggerRuntimeState(
    int releaseSequence,
    int timerElapsedTicks,
    boolean timerExpired,
    boolean expirationReleased,
    boolean inert,
    CollisionKey latestCollision,
    TriggerRuntimeBudget remainingBudget
) {
    public TriggerRuntimeState {
        if (releaseSequence < 0 || timerElapsedTicks < 0) {
            throw new IllegalArgumentException("releaseSequence and timerElapsedTicks must not be negative");
        }
        Objects.requireNonNull(remainingBudget, "remainingBudget");
    }

    public static TriggerRuntimeState fresh(TriggerRuntimeBudget budget) {
        return new TriggerRuntimeState(0, 0, false, false, false, null, budget);
    }

    /** Compatibility constructor for state without persisted Timer progress. */
    public TriggerRuntimeState(
        int releaseSequence, boolean timerExpired, boolean expirationReleased, boolean inert,
        CollisionKey latestCollision, TriggerRuntimeBudget remainingBudget
    ) {
        this(releaseSequence, 0, timerExpired, expirationReleased, inert, latestCollision, remainingBudget);
    }

    public TriggerRuntimeState withCollision(CollisionKey collision) {
        return new TriggerRuntimeState(releaseSequence, timerElapsedTicks, timerExpired, expirationReleased, inert, collision, remainingBudget);
    }

    public TriggerRuntimeState withTimerExpired() {
        return new TriggerRuntimeState(releaseSequence, timerElapsedTicks, true, expirationReleased, inert, latestCollision, remainingBudget);
    }

    public TriggerRuntimeState advanceTimer() {
        int advancedTicks = timerElapsedTicks == Integer.MAX_VALUE ? Integer.MAX_VALUE : timerElapsedTicks + 1;
        return new TriggerRuntimeState(releaseSequence, advancedTicks, timerExpired,
            expirationReleased, inert, latestCollision, remainingBudget);
    }

    public TriggerRuntimeState withExpirationReleased() {
        return new TriggerRuntimeState(releaseSequence, timerElapsedTicks, timerExpired, true, inert, latestCollision, remainingBudget);
    }

    public TriggerRuntimeState markInert() {
        return new TriggerRuntimeState(releaseSequence, timerElapsedTicks, timerExpired, expirationReleased, true, latestCollision, remainingBudget);
    }

    public TriggerRuntimeState reserve(TriggerRuntimeBudget nextBudget) {
        return new TriggerRuntimeState(releaseSequence + 1, timerElapsedTicks, timerExpired, expirationReleased, inert,
            latestCollision, nextBudget);
    }
}
