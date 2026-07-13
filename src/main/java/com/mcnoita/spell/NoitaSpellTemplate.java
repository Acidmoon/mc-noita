package com.mcnoita.spell;

import java.util.List;
import java.util.Objects;

public record NoitaSpellTemplate(
    NoitaSpellType type,
    int maxUses,
    int manaDrain,
    float damage,
    float explosionRadius,
    float spreadDegrees,
    float speed,
    float castDelaySeconds,
    float rechargeTimeSeconds,
    float spreadModifierDegrees,
    float speedMultiplier,
    float criticalChancePercent,
    int lifetimeTicks,
    int maxLifetimeTicks,
    int lifetimeModifierTicks,
    float recoil,
    float knockbackForce,
    boolean piercing,
    boolean friendlyFire,
    int trailLightStacks,
    int drawCount,
    NoitaSpellTriggerMode triggerMode,
    int triggerDrawCount,
    int triggerDelayTicks,
    float gravity,
    int bounceCount,
    List<NoitaModifierEffect> modifierEffects
) {
    public static final int UNLIMITED_USES = -1;

    public NoitaSpellTemplate {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(triggerMode, "triggerMode");
        modifierEffects = List.copyOf(Objects.requireNonNull(modifierEffects, "modifierEffects"));
        requireFinite(damage, "damage");
        requireFinite(explosionRadius, "explosionRadius");
        requireFinite(spreadDegrees, "spreadDegrees");
        requireFinite(speed, "speed");
        requireFinite(castDelaySeconds, "castDelaySeconds");
        requireFinite(rechargeTimeSeconds, "rechargeTimeSeconds");
        requireFinite(spreadModifierDegrees, "spreadModifierDegrees");
        requireFinite(speedMultiplier, "speedMultiplier");
        requireFinite(criticalChancePercent, "criticalChancePercent");
        requireFinite(recoil, "recoil");
        requireFinite(knockbackForce, "knockbackForce");
        requireFinite(gravity, "gravity");
        if (maxUses < UNLIMITED_USES) {
            throw new IllegalArgumentException("maxUses must be unlimited or non-negative");
        }
        if (type != NoitaSpellType.PROJECTILE_MODIFIER && explosionRadius < 0.0f) {
            throw new IllegalArgumentException("explosionRadius must not be negative for non-modifier spells");
        }
        if (speed < 0.0f) {
            throw new IllegalArgumentException("speed must not be negative");
        }
        if (speedMultiplier < 0.0f) {
            throw new IllegalArgumentException("speedMultiplier must not be negative");
        }
        if (lifetimeTicks < 0) {
            throw new IllegalArgumentException("lifetimeTicks must not be negative");
        }
        if (maxLifetimeTicks < 0) {
            throw new IllegalArgumentException("maxLifetimeTicks must not be negative");
        }
        if (trailLightStacks < 0) {
            throw new IllegalArgumentException("trailLightStacks must not be negative");
        }
        if (drawCount < 0) {
            throw new IllegalArgumentException("drawCount must not be negative");
        }
        if (triggerDrawCount < 0) {
            throw new IllegalArgumentException("triggerDrawCount must not be negative");
        }
        if (triggerDelayTicks < 0) {
            throw new IllegalArgumentException("triggerDelayTicks must not be negative");
        }
        if (bounceCount < 0) {
            throw new IllegalArgumentException("bounceCount must not be negative");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean hasLimitedUses() {
        return maxUses != UNLIMITED_USES;
    }

    private static void requireFinite(float value, String field) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(field + " must be finite");
        }
    }

    public static final class Builder {
        private NoitaSpellType type = NoitaSpellType.PROJECTILE;
        private int maxUses = UNLIMITED_USES;
        private int manaDrain;
        private float damage;
        private float explosionRadius;
        private float spreadDegrees;
        private float speed;
        private float castDelaySeconds;
        private float rechargeTimeSeconds;
        private float spreadModifierDegrees;
        private float speedMultiplier = 1.0f;
        private float criticalChancePercent;
        private int lifetimeTicks;
        private int maxLifetimeTicks;
        private int lifetimeModifierTicks;
        private float recoil;
        private float knockbackForce;
        private boolean piercing;
        private boolean friendlyFire;
        private int trailLightStacks;
        private int drawCount = 1;
        private NoitaSpellTriggerMode triggerMode = NoitaSpellTriggerMode.NONE;
        private int triggerDrawCount;
        private int triggerDelayTicks;
        private float gravity;
        private int bounceCount;
        private List<NoitaModifierEffect> modifierEffects = List.of();

        private Builder() {
        }

        public Builder type(NoitaSpellType type) {
            this.type = type;
            return this;
        }

        public Builder maxUses(int maxUses) {
            this.maxUses = maxUses;
            return this;
        }

        public Builder manaDrain(int manaDrain) {
            this.manaDrain = manaDrain;
            return this;
        }

        public Builder damage(float damage) {
            this.damage = damage;
            return this;
        }

        public Builder explosionRadius(float explosionRadius) {
            this.explosionRadius = explosionRadius;
            return this;
        }

        public Builder spreadDegrees(float spreadDegrees) {
            this.spreadDegrees = spreadDegrees;
            return this;
        }

        public Builder speed(float speed) {
            this.speed = speed;
            return this;
        }

        public Builder castDelaySeconds(float castDelaySeconds) {
            this.castDelaySeconds = castDelaySeconds;
            return this;
        }

        public Builder rechargeTimeSeconds(float rechargeTimeSeconds) {
            this.rechargeTimeSeconds = rechargeTimeSeconds;
            return this;
        }

        public Builder spreadModifierDegrees(float spreadModifierDegrees) {
            this.spreadModifierDegrees = spreadModifierDegrees;
            return this;
        }

        public Builder speedMultiplier(float speedMultiplier) {
            this.speedMultiplier = speedMultiplier;
            return this;
        }

        public Builder criticalChancePercent(float criticalChancePercent) {
            this.criticalChancePercent = criticalChancePercent;
            return this;
        }

        public Builder lifetimeTicks(int lifetimeTicks) {
            this.lifetimeTicks = lifetimeTicks;
            return this;
        }

        public Builder maxLifetimeTicks(int maxLifetimeTicks) {
            this.maxLifetimeTicks = maxLifetimeTicks;
            return this;
        }

        public Builder lifetimeModifierTicks(int lifetimeModifierTicks) {
            this.lifetimeModifierTicks = lifetimeModifierTicks;
            return this;
        }

        public Builder recoil(float recoil) {
            this.recoil = recoil;
            return this;
        }

        public Builder knockbackForce(float knockbackForce) {
            this.knockbackForce = knockbackForce;
            return this;
        }

        public Builder piercing(boolean piercing) {
            this.piercing = piercing;
            return this;
        }

        public Builder friendlyFire(boolean friendlyFire) {
            this.friendlyFire = friendlyFire;
            return this;
        }

        public Builder trailLightStacks(int trailLightStacks) {
            this.trailLightStacks = trailLightStacks;
            return this;
        }

        public Builder drawCount(int drawCount) {
            this.drawCount = drawCount;
            return this;
        }

        public Builder triggerMode(NoitaSpellTriggerMode triggerMode) {
            this.triggerMode = triggerMode;
            return this;
        }

        public Builder triggerDrawCount(int triggerDrawCount) {
            this.triggerDrawCount = triggerDrawCount;
            return this;
        }

        public Builder triggerDelayTicks(int triggerDelayTicks) {
            this.triggerDelayTicks = triggerDelayTicks;
            return this;
        }

        public Builder gravity(float gravity) {
            this.gravity = gravity;
            return this;
        }

        public Builder bounceCount(int bounceCount) {
            this.bounceCount = bounceCount;
            return this;
        }

        public Builder modifierEffect(NoitaModifierEffect modifierEffect) {
            this.modifierEffects = List.of(modifierEffect);
            return this;
        }

        public Builder modifierEffects(NoitaModifierEffect... modifierEffects) {
            this.modifierEffects = List.of(modifierEffects);
            return this;
        }

        public Builder modifierEffects(List<NoitaModifierEffect> modifierEffects) {
            this.modifierEffects = List.copyOf(modifierEffects);
            return this;
        }

        public NoitaSpellTemplate build() {
            return new NoitaSpellTemplate(
                type,
                maxUses,
                manaDrain,
                damage,
                explosionRadius,
                spreadDegrees,
                speed,
                castDelaySeconds,
                rechargeTimeSeconds,
                spreadModifierDegrees,
                speedMultiplier,
                criticalChancePercent,
                lifetimeTicks,
                maxLifetimeTicks,
                lifetimeModifierTicks,
                recoil,
                knockbackForce,
                piercing,
                friendlyFire,
                trailLightStacks,
                drawCount,
                triggerMode,
                triggerDrawCount,
                triggerDelayTicks,
                gravity,
                bounceCount,
                modifierEffects
            );
        }
    }
}
