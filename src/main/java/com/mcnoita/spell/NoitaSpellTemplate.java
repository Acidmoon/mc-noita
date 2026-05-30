package com.mcnoita.spell;

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
    boolean piercing,
    boolean friendlyFire
) {
    public static final int UNLIMITED_USES = -1;

    public NoitaSpellTemplate {
        Objects.requireNonNull(type, "type");
        if (maxUses < UNLIMITED_USES) {
            throw new IllegalArgumentException("maxUses must be unlimited or non-negative");
        }
        if (explosionRadius < 0.0f) {
            throw new IllegalArgumentException("explosionRadius must not be negative");
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
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean hasLimitedUses() {
        return maxUses != UNLIMITED_USES;
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
        private boolean piercing;
        private boolean friendlyFire;

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

        public Builder piercing(boolean piercing) {
            this.piercing = piercing;
            return this;
        }

        public Builder friendlyFire(boolean friendlyFire) {
            this.friendlyFire = friendlyFire;
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
                piercing,
                friendlyFire
            );
        }
    }
}
