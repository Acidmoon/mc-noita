package com.mcnoita.spell.server.budget;

/**
 * Resources that can be reserved before a server-side spell cast is committed.
 * Values are deliberately mechanical rather than spell-specific so the same
 * quotas apply to root casts, trigger releases, and persisted jobs.
 */
public enum BudgetKind {
    ACTION_NODES,
    LOGICAL_PROJECTILES,
    AUTHORITATIVE_ENTITIES,
    TRIGGER_RELEASES,
    ENTITY_SCANS,
    BLOCK_CHECKS,
    BLOCK_MUTATIONS,
    NBT_BYTES,
    NBT_NODES,
    NETWORK_PACKETS,
    NETWORK_BYTES,
    VISUAL_EVENTS,
    CROSS_TICK_JOB_STEPS,
    PERSISTENT_JOBS
}
