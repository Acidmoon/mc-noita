package com.mcnoita.spell.plan;

/**
 * Frozen event contract for a trigger payload. The server runtime decides
 * whether a collision is valid, while this policy prevents it from
 * reinterpreting a spell definition after the parent projectile was spawned.
 */
public enum TriggerReleasePolicy {
    COLLISION_WHILE_ALIVE,
    COLLISION_WHILE_ALIVE_AND_TIMER_ONCE,
    TERMINATION_ONCE;

    public static TriggerReleasePolicy forMode(TriggerMode mode) {
        return switch (mode) {
            case HIT -> COLLISION_WHILE_ALIVE;
            case TIMER -> COLLISION_WHILE_ALIVE_AND_TIMER_ONCE;
            case EXPIRATION, DEATH -> TERMINATION_ONCE;
            case NONE -> throw new IllegalArgumentException("a trigger plan requires a non-NONE mode");
        };
    }
}
