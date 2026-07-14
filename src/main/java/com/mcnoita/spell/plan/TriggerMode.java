package com.mcnoita.spell.plan;

public enum TriggerMode {
    NONE,
    HIT,
    TIMER,
    EXPIRATION,
    /** Legacy plan spelling retained only so older callers can be normalized safely. */
    @Deprecated(forRemoval = false)
    DEATH;

    public boolean isExpiration() {
        return this == EXPIRATION || this == DEATH;
    }

    public static TriggerMode normalize(TriggerMode mode) {
        return mode == DEATH ? EXPIRATION : mode;
    }
}
