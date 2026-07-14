package com.mcnoita.spell.plan;

/** Per-cast pure evaluation ceilings; server-wide reservation belongs to later execution policy. */
public record CastBudget(
    int actionSteps, int projectileNodes, int payloadNodes, int payloadDepth, int spawnedEntities, int recursiveCallDepth
) {
    public static final int DEFAULT_SPAWNED_ENTITIES = 32;
    /** Runtime Trigger releases use an independent per-tree ceiling. */
    public static final int DEFAULT_RUNTIME_RELEASE_EVENTS = 32;
    public static final CastBudget DEFAULT = new CastBudget(2048, 128, 128, 16, DEFAULT_SPAWNED_ENTITIES, 2);

    public CastBudget {
        if (actionSteps < 1 || projectileNodes < 1 || payloadNodes < 1 || payloadDepth < 1 || spawnedEntities < 1
            || recursiveCallDepth < 0) {
            throw new IllegalArgumentException("cast budgets must be positive, except recursive depth may be zero");
        }
    }

    /** G03-compatible shape for callers that customize pure node/depth budgets. */
    public CastBudget(int actionSteps, int projectileNodes, int payloadNodes, int payloadDepth, int recursiveCallDepth) {
        this(actionSteps, projectileNodes, payloadNodes, payloadDepth, Math.min(DEFAULT_SPAWNED_ENTITIES, projectileNodes),
            recursiveCallDepth);
    }

    /**
     * Preserves the G01/G02 constructor shape. Before G03 its sole payload
     * ceiling accidentally constrained both width and depth, so retaining that
     * interpretation is safer for callers that supplied a custom value.
     */
    public CastBudget(int actionSteps, int projectileNodes, int payloadNodes, int recursiveCallDepth) {
        this(actionSteps, projectileNodes, payloadNodes, payloadNodes, recursiveCallDepth);
    }
}
