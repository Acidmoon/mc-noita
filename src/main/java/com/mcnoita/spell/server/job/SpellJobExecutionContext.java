package com.mcnoita.spell.server.job;

import java.util.Objects;

/** Immutable context for exactly one already-reserved persistent-job step. */
public record SpellJobExecutionContext(SpellJobPersistentState job, long serverTick) {
    public SpellJobExecutionContext {
        job = Objects.requireNonNull(job, "job");
        if (serverTick < 0L) {
            throw new IllegalArgumentException("serverTick must not be negative");
        }
    }
}
