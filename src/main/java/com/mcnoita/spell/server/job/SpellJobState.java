package com.mcnoita.spell.server.job;

/** Durable lifecycle for a bounded cross-tick spell job. */
public enum SpellJobState {
    QUEUED,
    PAUSED,
    RUNNING,
    COMPLETED,
    CANCELLED,
    INERT;

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == INERT;
    }

    /** Terminal records are immutable evidence and must never become executable again. */
    public boolean canTransitionTo(SpellJobState next) {
        return this == next || !isTerminal();
    }
}
