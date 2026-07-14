package com.mcnoita.spell.trigger;

/** The observable event which released a frozen trigger payload. */
public enum TriggerReleaseReason {
    COLLISION,
    TIMER_EXPIRED,
    EXPIRATION
}
