package com.mcnoita.spell;

/** Explicitly states which runtime events are allowed to release a frozen tree. */
public enum NoitaTriggerReleasePolicy {
    NONE(false),
    VALID_COLLISION(true),
    VALID_COLLISION_AND_TIMER(true),
    EXPIRATION(false);

    private final boolean releasesOnCollision;

    NoitaTriggerReleasePolicy(boolean releasesOnCollision) {
        this.releasesOnCollision = releasesOnCollision;
    }

    public boolean releasesOnCollision() {
        return releasesOnCollision;
    }

    public static NoitaTriggerReleasePolicy forMode(NoitaSpellTriggerMode mode) {
        return switch (mode) {
            case NONE -> NONE;
            case HIT -> VALID_COLLISION;
            case TIMER -> VALID_COLLISION_AND_TIMER;
            case EXPIRATION, DEATH -> EXPIRATION;
        };
    }
}
