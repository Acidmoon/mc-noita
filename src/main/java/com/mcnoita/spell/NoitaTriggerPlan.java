package com.mcnoita.spell;

import java.util.List;
import java.util.Objects;

/**
 * Fully resolved trigger metadata. It is a data object only: collision and
 * timer decisions are made later by TriggerPayloadController without a catalog.
 */
public record NoitaTriggerPlan(
    NoitaSpellTriggerMode mode,
    int timerDelayTicks,
    List<NoitaPayloadPlan> payloads,
    String nodePath,
    int payloadDepth,
    NoitaTriggerReleasePolicy releasePolicy
) {
    public NoitaTriggerPlan {
        mode = normalize(Objects.requireNonNull(mode, "mode"));
        timerDelayTicks = Math.max(0, timerDelayTicks);
        payloads = List.copyOf(Objects.requireNonNull(payloads, "payloads"));
        nodePath = NoitaExecutionIdentity.requireNodePath(nodePath);
        if (payloadDepth < 0) {
            throw new IllegalArgumentException("payloadDepth must not be negative");
        }
        releasePolicy = Objects.requireNonNull(releasePolicy, "releasePolicy");
        if (releasePolicy != NoitaTriggerReleasePolicy.forMode(mode)) {
            throw new IllegalArgumentException("release policy must match trigger mode");
        }
    }

    public static NoitaTriggerPlan none(String ownerNodePath) {
        return new NoitaTriggerPlan(NoitaSpellTriggerMode.NONE, 0, List.of(), ownerNodePath + "/trigger", 0,
            NoitaTriggerReleasePolicy.NONE);
    }

    public static NoitaTriggerPlan legacy(
        NoitaSpellTriggerMode mode, int timerDelayTicks, List<NoitaProjectilePayload> projectiles, String ownerNodePath
    ) {
        NoitaSpellTriggerMode normalized = normalize(mode == null ? NoitaSpellTriggerMode.NONE : mode);
        List<NoitaPayloadPlan> payloads = projectiles.isEmpty() ? List.of()
            : List.of(new NoitaPayloadPlan(ownerNodePath + "/trigger/0", projectiles));
        int payloadDepth = normalized == NoitaSpellTriggerMode.NONE ? 0 : 1;
        return new NoitaTriggerPlan(normalized, timerDelayTicks, payloads, ownerNodePath + "/trigger", payloadDepth,
            NoitaTriggerReleasePolicy.forMode(normalized));
    }

    public boolean isActive() {
        return mode != NoitaSpellTriggerMode.NONE;
    }

    public int spawnedEntityCount() {
        long count = 0L;
        for (NoitaPayloadPlan payload : payloads) {
            count += payload.spawnedEntityCount();
            if (count > Integer.MAX_VALUE) {
                throw new IllegalStateException("frozen trigger entity count overflow");
            }
        }
        return (int) count;
    }

    public static NoitaSpellTriggerMode normalize(NoitaSpellTriggerMode mode) {
        return mode == NoitaSpellTriggerMode.DEATH ? NoitaSpellTriggerMode.EXPIRATION : mode;
    }
}
