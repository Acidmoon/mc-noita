package com.mcnoita.spell.plan;

/** Per-cast pure evaluation ceilings; server-wide reservation belongs to later execution policy. */
public record CastBudget(int actionSteps, int projectileNodes, int payloadNodes, int recursiveCallDepth) {
    public static final CastBudget DEFAULT = new CastBudget(2048, 128, 16, 2);

    public CastBudget {
        if (actionSteps < 1 || projectileNodes < 1 || payloadNodes < 1 || recursiveCallDepth < 0) {
            throw new IllegalArgumentException("cast budgets must be positive, except recursive depth may be zero");
        }
    }
}
