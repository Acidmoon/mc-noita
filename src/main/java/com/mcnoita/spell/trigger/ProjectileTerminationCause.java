package com.mcnoita.spell.trigger;

/**
 * Describes a real game-play terminal event. Persistence and administrative
 * removal are deliberately not terminal trigger events because they must not
 * manufacture a spell while a chunk is saving or unloading.
 */
public enum ProjectileTerminationCause {
    NATURAL_EXPIRY,
    TERMINAL_COLLISION,
    KILLED,
    UNLOAD,
    INVALID_DATA,
    ADMIN_CLEANUP;

    public boolean releasesExpirationPayload() {
        return this == NATURAL_EXPIRY || this == TERMINAL_COLLISION || this == KILLED;
    }
}
