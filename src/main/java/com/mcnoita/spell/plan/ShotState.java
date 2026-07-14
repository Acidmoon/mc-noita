package com.mcnoita.spell.plan;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable modifier accumulator for one shot branch. Trigger branches create
 * a new instance, while all projectiles in one multicast retain this instance.
 */
public record ShotState(
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
    boolean piercing,
    boolean friendlyFire,
    int trailLightStacks,
    double patternDegrees,
    List<String> effects
) {
    public static final ShotState EMPTY = new ShotState(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0,
        0.0, 0.0, 0.0, 0, false, false, 0, 0.0, List.of());

    public ShotState {
        effects = List.copyOf(effects);
    }

    public ShotState applyModifier(ShotModifier modifier) {
        List<String> nextEffects = new ArrayList<>(effects);
        nextEffects.addAll(modifier.effects());
        return new ShotState(
            damage + modifier.damage(),
            explosionRadius + modifier.explosionRadius(),
            spreadDegrees + modifier.spreadDegrees(),
            speedMultiplier * modifier.speedMultiplier(),
            castDelayFrames + modifier.castDelayFrames(),
            rechargeFrames + modifier.rechargeFrames(),
            criticalChancePercent + modifier.criticalChancePercent(),
            lifetimeFrames + modifier.lifetimeFrames(),
            recoil + modifier.recoil(),
            knockbackForce + modifier.knockbackForce(),
            gravity + modifier.gravity(),
            modifier.removeBounce() ? 0 : bounceCount + modifier.bounceCount(),
            piercing || modifier.piercing(),
            friendlyFire || modifier.friendlyFire(),
            trailLightStacks + modifier.trailLightStacks(),
            patternDegrees,
            nextEffects
        );
    }

    public ShotState applyDividePenalty(double damagePenalty, double explosionPenalty, double addedPatternDegrees) {
        return new ShotState(damage + damagePenalty, explosionRadius + explosionPenalty,
            spreadDegrees, speedMultiplier, castDelayFrames, rechargeFrames, criticalChancePercent, lifetimeFrames,
            recoil, knockbackForce, gravity, bounceCount, piercing, friendlyFire, trailLightStacks,
            addedPatternDegrees, effects);
    }
}
