package com.mcnoita.spell.plan;

import com.mcnoita.wand.model.NoitaDuration;
import java.util.Objects;

/** Frozen cross-tick job intent. Persistence and idempotent retry policy remain explicit. */
public record PersistentJobEffectNode(
    String nodePath, String jobType, int maximumSteps, NoitaDuration expiresAfter, boolean recoveryIdempotent
) implements EffectNode {
    public PersistentJobEffectNode {
        EffectNode.requireNodePath(nodePath);
        if (jobType == null || jobType.isBlank() || maximumSteps < 1) {
            throw new IllegalArgumentException("persistent job requires a type and positive step budget");
        }
        expiresAfter = Objects.requireNonNull(expiresAfter, "expiresAfter");
        if (expiresAfter.isZero()) {
            throw new IllegalArgumentException("persistent job expiry must be positive");
        }
    }

    /** Existing callers remain deliberately non-idempotent until they opt in explicitly. */
    public PersistentJobEffectNode(String nodePath, String jobType, int maximumSteps, NoitaDuration expiresAfter) {
        this(nodePath, jobType, maximumSteps, expiresAfter, false);
    }
}
