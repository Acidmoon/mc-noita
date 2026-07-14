package com.mcnoita.spell.exec;

import com.mcnoita.spell.NoitaExecutionIdentity;
import com.mcnoita.spell.NoitaModifierEffect;
import com.mcnoita.spell.NoitaPayloadPlan;
import com.mcnoita.spell.NoitaProjectileBehavior;
import com.mcnoita.spell.NoitaProjectilePayload;
import com.mcnoita.spell.NoitaSpellTriggerMode;
import com.mcnoita.spell.NoitaTriggerPlan;
import com.mcnoita.spell.NoitaTriggerReleasePolicy;
import com.mcnoita.spell.plan.ProjectilePlan;
import com.mcnoita.spell.plan.TriggerMode;
import com.mcnoita.wand.adapter.MinecraftTimeAdapter;
import java.util.ArrayList;
import java.util.List;

/** Converts a pure frozen ProjectilePlan into the persisted runtime payload form. */
final class ProjectilePlanPayloadAdapter {
    private static final double NOITA_SPEED_TO_ARROW_SPEED = 300.0;

    private ProjectilePlanPayloadAdapter() {
    }

    static NoitaProjectilePayload payload(ProjectilePlan plan, EffectExecutionContext context) {
        NoitaExecutionIdentity identity = new NoitaExecutionIdentity(context.executionId(), plan.nodePath(),
            context.catalogEpoch(), context.catalogHash());
        NoitaTriggerPlan triggerPlan = triggerPlan(plan, context, identity.nodePath());
        return new NoitaProjectilePayload(
            plan.itemPath(), behavior(plan.behavior()), (float) plan.damage(), (float) plan.criticalChancePercent(),
            MinecraftTimeAdapter.toMinecraftTicks(plan.lifetime(), 1), plan.trailLightStacks(), (float) plan.explosionRadius(),
            (float) Math.max(0.2, Math.min(8.0, plan.speed() / NOITA_SPEED_TO_ARROW_SPEED)), 0.0f, (float) plan.gravity(),
            (float) plan.drag(), (float) plan.bounceDamping(), (float) plan.renderScale(), (float) plan.knockbackForce(),
            plan.friendlyFire(), plan.piercing(), plan.projectileCount(), (float) plan.burstSpreadDegrees(), plan.bounceCount(),
            effects(plan.effects()), triggerPlan, identity, com.mcnoita.spell.trigger.TriggerRuntimeBudget.DEFAULT
        );
    }

    private static NoitaTriggerPlan triggerPlan(ProjectilePlan plan, EffectExecutionContext context, String ownerNodePath) {
        if (!plan.hasTrigger()) {
            return NoitaTriggerPlan.none(ownerNodePath);
        }
        com.mcnoita.spell.plan.TriggerPlan trigger = plan.trigger();
        List<NoitaProjectilePayload> projectiles = trigger.payload().projectiles().stream()
            .map(payload -> payload(payload, context)).toList();
        // NBT child identities must be distinct even though the pure TriggerPlan
        // and PayloadPlan intentionally share one readable path.
        NoitaPayloadPlan payload = new NoitaPayloadPlan(trigger.nodePath() + "/payload", projectiles);
        return new NoitaTriggerPlan(triggerMode(trigger.mode()), MinecraftTimeAdapter.toMinecraftTicks(trigger.timerDelay(), 0),
            List.of(payload), trigger.nodePath(), trigger.payloadDepth(), NoitaTriggerReleasePolicy.forMode(triggerMode(trigger.mode())));
    }

    private static NoitaProjectileBehavior behavior(String name) {
        try {
            return NoitaProjectileBehavior.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return NoitaProjectileBehavior.BOLT;
        }
    }

    private static NoitaSpellTriggerMode triggerMode(TriggerMode mode) {
        return switch (mode) {
            case HIT -> NoitaSpellTriggerMode.HIT;
            case TIMER -> NoitaSpellTriggerMode.TIMER;
            case EXPIRATION, DEATH -> NoitaSpellTriggerMode.EXPIRATION;
            case NONE -> NoitaSpellTriggerMode.NONE;
        };
    }

    private static List<NoitaModifierEffect> effects(List<String> names) {
        List<NoitaModifierEffect> effects = new ArrayList<>();
        for (String name : names) {
            try {
                effects.add(NoitaModifierEffect.valueOf(name));
            } catch (IllegalArgumentException ignored) {
                // The frozen payload keeps mechanics stable; stale visual tags are harmless here.
            }
        }
        return List.copyOf(effects);
    }
}
