package com.mcnoita.spell.plan;

public record BudgetUsage(int actionSteps, int projectileNodes, int payloadNodes, int payloadDepth, int spawnedEntities) {
    /** Compatibility shape used before root authoritative entity counting existed. */
    public BudgetUsage(int actionSteps, int projectileNodes, int payloadNodes, int payloadDepth) {
        this(actionSteps, projectileNodes, payloadNodes, payloadDepth, 0);
    }

    /** Compatibility shape for callers that only report node counts. */
    public BudgetUsage(int actionSteps, int projectileNodes, int payloadNodes) {
        this(actionSteps, projectileNodes, payloadNodes, 0, 0);
    }
}
