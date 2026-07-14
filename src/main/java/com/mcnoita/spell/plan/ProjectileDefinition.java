package com.mcnoita.spell.plan;

import com.mcnoita.wand.model.NoitaDuration;
import java.util.List;
import java.util.Objects;

/** Frozen pure projectile definition before the current ShotState is applied. */
public record ProjectileDefinition(
    String itemPath,
    String behavior,
    double damage,
    double criticalChancePercent,
    NoitaDuration lifetime,
    double castDelayFrames,
    double rechargeFrames,
    int trailLightStacks,
    double explosionRadius,
    double speed,
    double spreadDegrees,
    double gravity,
    double drag,
    double bounceDamping,
    double renderScale,
    double knockbackForce,
    boolean friendlyFire,
    boolean piercing,
    int projectileCount,
    double burstSpreadDegrees,
    NoitaDuration triggerDelay,
    int bounceCount,
    List<String> effects
) {
    public ProjectileDefinition {
        if (itemPath == null || itemPath.isBlank() || behavior == null || behavior.isBlank()) {
            throw new IllegalArgumentException("projectile identifiers must not be blank");
        }
        Objects.requireNonNull(lifetime, "lifetime");
        Objects.requireNonNull(triggerDelay, "triggerDelay");
        if (!Double.isFinite(damage) || !Double.isFinite(criticalChancePercent) || !Double.isFinite(explosionRadius)
            || !Double.isFinite(castDelayFrames) || !Double.isFinite(rechargeFrames) || !Double.isFinite(speed)
            || !Double.isFinite(spreadDegrees) || !Double.isFinite(gravity)
            || !Double.isFinite(drag) || !Double.isFinite(bounceDamping) || !Double.isFinite(renderScale)
            || !Double.isFinite(knockbackForce) || !Double.isFinite(burstSpreadDegrees) || projectileCount < 1
            || trailLightStacks < 0 || bounceCount < 0) {
            throw new IllegalArgumentException("projectile values must be finite and valid");
        }
        effects = List.copyOf(Objects.requireNonNull(effects, "effects"));
    }
}
