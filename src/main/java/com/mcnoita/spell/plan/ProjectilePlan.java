package com.mcnoita.spell.plan;

import com.mcnoita.spell.damage.DamageProfile;
import com.mcnoita.wand.model.NoitaDuration;
import java.util.List;
import java.util.Objects;

/** Immutable world-independent projectile execution node with frozen mechanics. */
public record ProjectilePlan(
    String nodePath,
    String itemPath,
    String behavior,
    DamageProfile damageProfile,
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
    TriggerPlan trigger,
    int bounceCount,
    List<String> effects
) {
    /**
     * Frozen projectile NBT encodes modifier effects as one bounded list. Keep
     * this plan-level limit aligned with persistence so an accepted cast can
     * always survive its first entity save/reload cycle.
     */
    public static final int MAX_MODIFIER_EFFECTS = 64;

    public ProjectilePlan {
        if (nodePath == null || nodePath.isBlank()) {
            throw new IllegalArgumentException("projectile node path must not be blank");
        }
        Objects.requireNonNull(itemPath, "itemPath");
        Objects.requireNonNull(behavior, "behavior");
        Objects.requireNonNull(damageProfile, "damageProfile");
        Objects.requireNonNull(lifetime, "lifetime");
        effects = List.copyOf(Objects.requireNonNull(effects, "effects"));
        if (effects.size() > MAX_MODIFIER_EFFECTS) {
            throw new IllegalArgumentException("projectile modifier effects exceed frozen payload limit");
        }
        if (trigger != null && !trigger.nodePath().startsWith(nodePath + "/trigger")) {
            throw new IllegalArgumentException("trigger node path must descend from its projectile path");
        }
    }

    /**
     * Compatibility bridge for existing scalar spell definitions. A scalar
     * has always meant projectile damage, so it must not silently become a
     * total or generic damage value when plans gain multiple channels.
     */
    public ProjectilePlan(
        String nodePath,
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
        TriggerPlan trigger,
        int bounceCount,
        List<String> effects
    ) {
        this(nodePath, itemPath, behavior, DamageProfile.legacyProjectile(damage), criticalChancePercent, lifetime,
            trailLightStacks, explosionRadius, speed, spreadOffsetDegrees, gravity, drag, bounceDamping, renderScale,
            knockbackForce, friendlyFire, piercing, projectileCount, burstSpreadDegrees, trigger, bounceCount, effects);
    }

    /** Compatibility projection for executors that still consume scalar projectile damage. */
    public double damage() {
        return damageProfile.projectileDamage();
    }

    public boolean hasTrigger() {
        return trigger != null;
    }

    /**
     * Counts the one-release physical entity footprint of this frozen tree.
     * Repeated Piercing collisions are bounded separately at runtime; this
     * lower bound must fit before WandState is committed so a first valid hit
     * cannot discard an already-paid payload solely because it lacks capacity.
     */
    public long staticEntityFootprint() {
        return multiplyCapped(projectileCount, staticEntityFootprintPerInstance());
    }

    /** The future entity capacity needed by one already-spawned instance. */
    public long futureEntityFootprintPerInstance() {
        return staticEntityFootprintPerInstance() - 1L;
    }

    /** Counts first-release events for all physical instances of this plan. */
    public long staticReleaseEventFootprint() {
        return multiplyCapped(projectileCount, staticReleaseEventFootprintPerInstance());
    }

    /** The first-release event capacity needed by one already-spawned instance. */
    public long staticReleaseEventFootprintPerInstance() {
        if (trigger == null) {
            return 0L;
        }
        long events = 1L;
        for (ProjectilePlan child : trigger.payload().projectiles()) {
            events = addCapped(events, child.staticReleaseEventFootprint());
        }
        return events;
    }

    private long staticEntityFootprintPerInstance() {
        long entities = 1L;
        if (trigger == null) {
            return entities;
        }
        for (ProjectilePlan child : trigger.payload().projectiles()) {
            entities = addCapped(entities, child.staticEntityFootprint());
        }
        return entities;
    }

    private static long addCapped(long left, long right) {
        return right > Long.MAX_VALUE - left ? Long.MAX_VALUE : left + right;
    }

    private static long multiplyCapped(long left, long right) {
        return left == 0L || right == 0L ? 0L : left > Long.MAX_VALUE / right ? Long.MAX_VALUE : left * right;
    }

    /**
     * Transitional projection for executors that have not yet moved to
     * {@link #trigger()}. The immutable TriggerPlan remains the source of truth.
     */
    public TriggerMode triggerMode() {
        return trigger == null ? TriggerMode.NONE : trigger.mode();
    }

    /** Transitional projection for the frozen timer delay. */
    public NoitaDuration triggerDelay() {
        return trigger == null ? NoitaDuration.ZERO : trigger.timerDelay();
    }

    /** Transitional projection for legacy payload consumers. */
    public List<ProjectilePlan> payloads() {
        return trigger == null ? List.of() : trigger.payload().projectiles();
    }
}
