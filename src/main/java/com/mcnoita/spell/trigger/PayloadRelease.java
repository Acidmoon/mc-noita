package com.mcnoita.spell.trigger;

import com.mcnoita.spell.NoitaPayloadPlan;
import java.util.List;
import java.util.Objects;

/** A payload shot and the pre-partitioned runtime allowance for its spawned roots. */
public record PayloadRelease(NoitaPayloadPlan payload, List<TriggerRuntimeBudget> childBudgets) {
    public PayloadRelease {
        Objects.requireNonNull(payload, "payload");
        childBudgets = List.copyOf(childBudgets);
        if (childBudgets.size() != payload.spawnedEntityCount()) {
            throw new IllegalArgumentException("payload budget partition must match the frozen spawned entity count");
        }
    }
}
