package com.mcnoita.spell.plan;

import java.util.List;
import java.util.Objects;

/** Values added to the current ShotState by an explicit modifier action. */
public record ShotModifier(
    double damage,
    double explosionRadius,
    double spreadDegrees,
    double speedMultiplier,
    double castDelayFrames,
    double rechargeFrames,
    double criticalChancePercent,
    double lifetimeFrames,
    double recoil,
    double knockbackForce,
    double gravity,
    int bounceCount,
    boolean removeBounce,
    boolean piercing,
    boolean friendlyFire,
    int trailLightStacks,
    List<String> effects
) {
    public static final ShotModifier EMPTY = new ShotModifier(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0,
        0.0, 0.0, 0.0, 0, false, false, false, 0, List.of());

    public ShotModifier {
        if (!Double.isFinite(damage) || !Double.isFinite(explosionRadius) || !Double.isFinite(spreadDegrees)
            || !Double.isFinite(speedMultiplier) || speedMultiplier < 0.0 || !Double.isFinite(castDelayFrames)
            || !Double.isFinite(rechargeFrames) || !Double.isFinite(criticalChancePercent)
            || !Double.isFinite(lifetimeFrames) || !Double.isFinite(recoil) || !Double.isFinite(knockbackForce)
            || !Double.isFinite(gravity) || trailLightStacks < 0) {
            throw new IllegalArgumentException("shot modifier values must be finite and valid");
        }
        effects = List.copyOf(Objects.requireNonNull(effects, "effects"));
    }
}
