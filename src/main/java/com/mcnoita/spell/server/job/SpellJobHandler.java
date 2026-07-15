package com.mcnoita.spell.server.job;

/**
 * Server adapter for one frozen job type. Implementations may touch the world
 * only through their own policy services after the manager reserves the step.
 */
public interface SpellJobHandler {
    String jobType();

    /**
     * True only when replaying an interrupted RUNNING step is safe by design.
     * Until the store provides a synchronous durable pre-effect checkpoint, the
     * manager requires this declaration before it invokes any persistent job.
     */
    boolean isRecoveryIdempotent();

    SpellJobStepResult execute(SpellJobExecutionContext context);
}
