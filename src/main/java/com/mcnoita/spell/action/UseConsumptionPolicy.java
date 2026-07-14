package com.mcnoita.spell.action;

/**
 * Decides the normal Hand-to-Discard limited-use reduction. Copy/Call and
 * Always Cast never enter this policy because they do not normally enter Hand.
 */
public enum UseConsumptionPolicy {
    /** Consume once only when this normal shot drew a Projectile/Static/Material card. */
    WHEN_PROJECTILE_SHOT,
    /** Utility and Other cards consume once after a successful normal Draw. */
    ALWAYS_ON_HAND_DISCARD,
    /** Passive and custom-use cards require action-specific accounting. */
    NEVER
}
