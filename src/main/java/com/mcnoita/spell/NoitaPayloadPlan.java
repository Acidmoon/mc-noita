package com.mcnoita.spell;

import com.mcnoita.persistence.NoitaNbtLimits;
import java.util.List;
import java.util.Objects;

/** A resolved payload shot: one trigger event may spawn multiple frozen projectile plans. */
public record NoitaPayloadPlan(String nodePath, List<NoitaProjectilePayload> projectiles) {
    public NoitaPayloadPlan {
        nodePath = NoitaExecutionIdentity.requireNodePath(nodePath);
        projectiles = List.copyOf(Objects.requireNonNull(projectiles, "projectiles"));
        if (projectiles.size() > NoitaNbtLimits.MAX_PAYLOAD_CHILDREN) {
            throw new IllegalArgumentException("payload shot exceeds the persisted projectile child limit");
        }
    }

    public int spawnedEntityCount() {
        long count = 0L;
        for (NoitaProjectilePayload projectile : projectiles) {
            count += projectile.projectileCount();
            if (count > Integer.MAX_VALUE) {
                throw new IllegalStateException("frozen payload entity count overflow");
            }
        }
        return (int) count;
    }
}
