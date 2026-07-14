package com.mcnoita.spell.plan;

import com.mcnoita.wand.model.NoitaDuration;
import java.util.List;
import java.util.Objects;

/** Immutable world-independent projectile execution node with frozen mechanics. */
public record ProjectilePlan(
    String itemPath,
    String behavior,
    double damage,
    double criticalChancePercent,
    NoitaDuration lifetime,
    int trailLightStacks,
    double explosionRadius,
    double speed,
    double spreadOffsetDegrees,
    double gravity,
    double drag,
    double bounceDamping,
    double renderScale,
    double knockbackForce,
    boolean friendlyFire,
    boolean piercing,
    int projectileCount,
    double burstSpreadDegrees,
    TriggerMode triggerMode,
    NoitaDuration triggerDelay,
    int bounceCount,
    List<String> effects,
    List<ProjectilePlan> payloads
) {
    public ProjectilePlan {
        Objects.requireNonNull(lifetime, "lifetime");
        Objects.requireNonNull(triggerMode, "triggerMode");
        Objects.requireNonNull(triggerDelay, "triggerDelay");
        effects = List.copyOf(effects);
        payloads = List.copyOf(payloads);
    }

    public ProjectilePlan withPayloads(List<ProjectilePlan> nextPayloads) {
        return new ProjectilePlan(itemPath, behavior, damage, criticalChancePercent, lifetime, trailLightStacks,
            explosionRadius, speed, spreadOffsetDegrees, gravity, drag, bounceDamping, renderScale, knockbackForce,
            friendlyFire, piercing, projectileCount, burstSpreadDegrees, triggerMode, triggerDelay, bounceCount,
            effects, nextPayloads);
    }
}
