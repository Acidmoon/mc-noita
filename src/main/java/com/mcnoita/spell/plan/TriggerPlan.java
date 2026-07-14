package com.mcnoita.spell.plan;

import com.mcnoita.wand.model.NoitaDuration;
import java.util.Objects;

/**
 * Immutable Trigger attachment on one projectile plan. The payload is already
 * evaluated and paid for before this plan crosses the server execution boundary.
 */
public record TriggerPlan(
    String nodePath,
    TriggerMode mode,
    NoitaDuration timerDelay,
    int payloadDepth,
    TriggerReleasePolicy releasePolicy,
    PayloadPlan payload
) {
    public TriggerPlan {
        if (nodePath == null || nodePath.isBlank()) {
            throw new IllegalArgumentException("trigger node path must not be blank");
        }
        mode = TriggerMode.normalize(Objects.requireNonNull(mode, "mode"));
        Objects.requireNonNull(timerDelay, "timerDelay");
        Objects.requireNonNull(releasePolicy, "releasePolicy");
        Objects.requireNonNull(payload, "payload");
        if (mode == TriggerMode.NONE) {
            throw new IllegalArgumentException("trigger plan requires a non-NONE mode");
        }
        if (payloadDepth != payload.depth()) {
            throw new IllegalArgumentException("trigger payload depth must match its payload plan");
        }
        if (!nodePath.equals(payload.nodePath())) {
            throw new IllegalArgumentException("trigger and payload must share their node path");
        }
        if (releasePolicy != TriggerReleasePolicy.forMode(mode)) {
            throw new IllegalArgumentException("trigger release policy must match its mode");
        }
    }
}
