package com.mcnoita.spell.trigger;

import java.util.List;
import java.util.Objects;

/**
 * The controller's committed result. A true decision means releaseSequence and
 * budgets were advanced before the caller allocates child entities.
 */
public record ReleaseDecision(
    boolean shouldRelease,
    int releaseSequence,
    String nodePath,
    TriggerReleaseReason reason,
    List<PayloadRelease> payloads,
    TriggerRuntimeBudget remainingBudget,
    TriggerBudgetExhaustion budgetExhaustion
) {
    public ReleaseDecision {
        Objects.requireNonNull(nodePath, "nodePath");
        payloads = List.copyOf(payloads);
        Objects.requireNonNull(remainingBudget, "remainingBudget");
        if (shouldRelease && (reason == null || budgetExhaustion != null)) {
            throw new IllegalArgumentException("release decisions require a reason");
        }
        if (!shouldRelease && (!payloads.isEmpty() || reason != null && budgetExhaustion == null)) {
            throw new IllegalArgumentException("non-release decisions cannot carry payloads");
        }
    }

    public static ReleaseDecision none(String nodePath, TriggerRuntimeBudget remainingBudget) {
        return new ReleaseDecision(false, 0, nodePath, null, List.of(), remainingBudget, null);
    }

    public static ReleaseDecision budgetExhausted(
        String nodePath, TriggerRuntimeBudget remainingBudget, TriggerBudgetExhaustion exhaustion
    ) {
        return new ReleaseDecision(false, 0, nodePath, null, List.of(), remainingBudget,
            Objects.requireNonNull(exhaustion, "exhaustion"));
    }
}
