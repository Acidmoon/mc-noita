package com.mcnoita.wand.eval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mcnoita.spell.action.AddProjectileAction;
import com.mcnoita.spell.action.BeginTriggerAction;
import com.mcnoita.spell.action.DrawAction;
import com.mcnoita.spell.action.ModifyShotAction;
import com.mcnoita.spell.action.SpellAction;
import com.mcnoita.spell.action.SpellCategory;
import com.mcnoita.spell.action.SpellDefinition;
import com.mcnoita.spell.plan.CastBudget;
import com.mcnoita.spell.plan.ProjectileDefinition;
import com.mcnoita.spell.plan.ProjectilePlan;
import com.mcnoita.spell.plan.ResolvedCast;
import com.mcnoita.spell.plan.ShotModifier;
import com.mcnoita.spell.plan.TriggerMode;
import com.mcnoita.spell.plan.TriggerReleasePolicy;
import com.mcnoita.wand.model.CardRef;
import com.mcnoita.wand.model.NoitaDuration;
import com.mcnoita.wand.model.SpellCardState;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Regression coverage for G03's pure, pre-paid Trigger plan tree. */
@Tag("regression")
class G03TriggerPlanTest {
    @Test
    void legacyDeathActionNormalizesToExpirationBeforePlanConstruction() {
        BeginTriggerAction action = new BeginTriggerAction(TriggerMode.DEATH, 1);

        assertEquals(TriggerMode.EXPIRATION, action.mode());
        assertEquals(TriggerReleasePolicy.TERMINATION_ONCE, TriggerReleasePolicy.forMode(action.mode()));
    }

    @Test
    void payloadCardsAreChargedAndConsumedWhileTheOuterCastIsEvaluated() {
        SpellDefinition trigger = spell("trigger", 10,
            new AddProjectileAction(projectile("trigger", 1.0, 0.0, 0.0, NoitaDuration.ZERO)),
            new BeginTriggerAction(TriggerMode.HIT, 1));
        SpellDefinition payload = spell("payload", 25,
            new AddProjectileAction(projectile("payload", 2.0, 0.0, 0.0, NoitaDuration.ZERO)));

        ResolvedCast result = new WandCastEvaluator().evaluate(
            PureWandFixtures.wand(1, false),
            PureWandFixtures.state(100.0, List.of(PureWandFixtures.card(0, "trigger", 1), PureWandFixtures.card(1, "payload", 2))),
            PureWandFixtures.catalog(trigger, payload), NoitaDuration.ZERO, 71L
        );

        assertEquals(ResolvedCast.Status.ACCEPTED, result.status());
        assertEquals(65.0, result.nextState().mana());
        assertEquals(0, result.remainingUses().get(CardRef.forSlot(0)));
        assertEquals(1, result.remainingUses().get(CardRef.forSlot(1)));
        assertTrue(result.drawOutcomes().stream().anyMatch(outcome -> outcome.origin() == DrawOrigin.PAYLOAD
            && outcome.drawnCards().equals(List.of(CardRef.forSlot(1)))));

        ProjectilePlan root = result.effectPlan().projectiles().get(0);
        assertEquals("root/0", root.nodePath());
        assertEquals("root/0/trigger", root.trigger().nodePath());
        assertEquals("root/0/trigger/0", root.trigger().payload().projectiles().get(0).nodePath());
    }

    @Test
    void outerAndPayloadShotStatesAndCastDelayRemainIsolatedWhileRechargeIsGlobal() {
        SpellDefinition outerModifier = new SpellDefinition("outer_modifier", SpellCategory.PROJECTILE_MODIFIER, 0, false,
            List.of(
                new ModifyShotAction(new ShotModifier(4.0, 0.0, 0.0, 1.0, 6.0, 0.0, 0.0, 0.0,
                    0.0, 0.0, 0.0, 0, false, false, false, 0, List.of())),
                new DrawAction(1)
            ));
        SpellDefinition trigger = spell("trigger", 0,
            new AddProjectileAction(projectile("trigger", 1.0, 3.0, 7.0, NoitaDuration.ZERO)),
            new BeginTriggerAction(TriggerMode.HIT, 1));
        SpellDefinition payload = spell("payload", 0,
            new AddProjectileAction(projectile("payload", 2.0, 11.0, 13.0, NoitaDuration.ZERO)));

        ResolvedCast result = new WandCastEvaluator().evaluate(
            PureWandFixtures.wand(1, false),
            PureWandFixtures.state(100.0, List.of(
                PureWandFixtures.card(0, "outer_modifier"),
                PureWandFixtures.card(1, "trigger"),
                PureWandFixtures.card(2, "payload")
            )),
            PureWandFixtures.catalog(outerModifier, trigger, payload), NoitaDuration.ZERO, 72L
        );

        ProjectilePlan root = result.effectPlan().projectiles().get(0);
        ProjectilePlan payloadPlan = root.trigger().payload().projectiles().get(0);
        assertEquals(5.0, root.damage());
        assertEquals(2.0, payloadPlan.damage());
        assertEquals(NoitaDuration.frames(9.0), result.nextState().castDelayRemaining());
        assertEquals(NoitaDuration.frames(20.0), result.nextState().rechargeRemaining());
    }

    @Test
    void nestedPayloadTreeUsesStablePathsAndSeparatesDepthFromNodeBudgets() {
        SpellDefinition root = spell("root", 0,
            new AddProjectileAction(projectile("root", 1.0, 0.0, 0.0, NoitaDuration.ZERO)),
            new BeginTriggerAction(TriggerMode.HIT, 1));
        SpellDefinition nested = spell("nested", 0,
            new AddProjectileAction(projectile("nested", 2.0, 0.0, 0.0, NoitaDuration.frames(12.0))),
            new BeginTriggerAction(TriggerMode.TIMER, 1));
        SpellDefinition leaf = spell("leaf", 0,
            new AddProjectileAction(projectile("leaf", 3.0, 0.0, 0.0, NoitaDuration.ZERO)));
        List<SpellCardState> cards = List.of(
            PureWandFixtures.card(0, "root"),
            PureWandFixtures.card(1, "nested"),
            PureWandFixtures.card(2, "leaf")
        );

        ResolvedCast result = new WandCastEvaluator(new CastBudget(2048, 128, 8, 4, 2)).evaluate(
            PureWandFixtures.wand(1, false), PureWandFixtures.state(100.0, cards),
            PureWandFixtures.catalog(root, nested, leaf), NoitaDuration.ZERO, 73L
        );

        ProjectilePlan rootPlan = result.effectPlan().projectiles().get(0);
        ProjectilePlan nestedPlan = rootPlan.trigger().payload().projectiles().get(0);
        ProjectilePlan leafPlan = nestedPlan.trigger().payload().projectiles().get(0);
        assertEquals("root/0", rootPlan.nodePath());
        assertEquals("root/0/trigger", rootPlan.trigger().nodePath());
        assertEquals("root/0/trigger/0", nestedPlan.nodePath());
        assertEquals("root/0/trigger/0/trigger", nestedPlan.trigger().nodePath());
        assertEquals("root/0/trigger/0/trigger/0", leafPlan.nodePath());
        assertEquals(1, rootPlan.trigger().payloadDepth());
        assertEquals(2, nestedPlan.trigger().payloadDepth());
        assertEquals(NoitaDuration.frames(12.0), nestedPlan.trigger().timerDelay());
        assertEquals(TriggerReleasePolicy.COLLISION_WHILE_ALIVE_AND_TIMER_ONCE, nestedPlan.trigger().releasePolicy());
        assertEquals(2, result.budgetUsage().payloadNodes());
        assertEquals(2, result.budgetUsage().payloadDepth());

        ResolvedCast depthRejected = new WandCastEvaluator(new CastBudget(2048, 128, 8, 1, 2)).evaluate(
            PureWandFixtures.wand(1, false), PureWandFixtures.state(100.0, cards),
            PureWandFixtures.catalog(root, nested, leaf), NoitaDuration.ZERO, 74L
        );
        assertEquals(ResolvedCast.Status.REJECTED, depthRejected.status());
        assertTrue(depthRejected.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals("PAYLOAD_DEPTH_BUDGET")));
        assertEquals("root/0/trigger/0", depthRejected.diagnostics().stream()
            .filter(diagnostic -> diagnostic.code().equals("PAYLOAD_DEPTH_BUDGET"))
            .findFirst().orElseThrow().nodePath());

        ResolvedCast nodeRejected = new WandCastEvaluator(new CastBudget(2048, 128, 1, 4, 2)).evaluate(
            PureWandFixtures.wand(1, false), PureWandFixtures.state(100.0, cards),
            PureWandFixtures.catalog(root, nested, leaf), NoitaDuration.ZERO, 75L
        );
        assertEquals(ResolvedCast.Status.REJECTED, nodeRejected.status());
        assertTrue(nodeRejected.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals("PAYLOAD_NODE_BUDGET")));

        CastBudget legacyShape = new CastBudget(8, 8, 3, 2);
        assertEquals(3, legacyShape.payloadNodes());
        assertEquals(3, legacyShape.payloadDepth());
    }

    @Test
    void rootAuthoritativeEntityBudgetRejectsBeforeWandStateCanCommit() {
        ProjectileDefinition fanOut = new ProjectileDefinition("fan_out", "BOLT", 1.0, 0.0, NoitaDuration.frames(60.0),
            0.0, 0.0, 0, 0.0, 400.0, 0.0, 0.0, 0.99, 0.65, 1.0, 0.0, false, false, 33, 0.0,
            NoitaDuration.ZERO, 0, List.of());
        SpellDefinition spell = spell("fan_out", 0, new AddProjectileAction(fanOut));

        ResolvedCast result = new WandCastEvaluator().evaluate(PureWandFixtures.wand(1, false),
            PureWandFixtures.state(100.0, List.of(PureWandFixtures.card(0, "fan_out"))),
            PureWandFixtures.catalog(spell), NoitaDuration.ZERO, 76L);

        assertEquals(ResolvedCast.Status.REJECTED, result.status());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals("SPAWNED_ENTITY_BUDGET")));
    }

    @Test
    void triggerTreeReservesItsFirstPayloadReleaseBeforeManaAndCardsCommit() {
        ProjectileDefinition rootProjectile = projectile("root", 1.0, 0.0, 0.0, NoitaDuration.ZERO);
        ProjectileDefinition oversizedPayload = new ProjectileDefinition("payload", "BOLT", 1.0, 0.0,
            NoitaDuration.frames(60.0), 0.0, 0.0, 0, 0.0, 400.0, 0.0, 0.0, 0.99, 0.65, 1.0, 0.0,
            false, false, 32, 0.0, NoitaDuration.ZERO, 0, List.of());
        SpellDefinition trigger = spell("trigger", 10, new AddProjectileAction(rootProjectile),
            new BeginTriggerAction(TriggerMode.HIT, 1));
        SpellDefinition payload = spell("payload", 20, new AddProjectileAction(oversizedPayload));
        var initial = PureWandFixtures.state(100.0, List.of(
            PureWandFixtures.card(0, "trigger", 1), PureWandFixtures.card(1, "payload", 1)
        ));

        ResolvedCast result = new WandCastEvaluator().evaluate(PureWandFixtures.wand(1, false), initial,
            PureWandFixtures.catalog(trigger, payload), NoitaDuration.ZERO, 77L);

        assertEquals(ResolvedCast.Status.REJECTED, result.status());
        assertEquals(initial, result.nextState());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals("SPAWNED_ENTITY_BUDGET")));
    }

    @Test
    void payloadShotChildLimitRejectsBeforeAValidPlanCanOutgrowItsNbtList() {
        SpellDefinition trigger = spell("trigger", 10,
            new AddProjectileAction(projectile("trigger", 1.0, 0.0, 0.0, NoitaDuration.ZERO)),
            new BeginTriggerAction(TriggerMode.HIT, 33));
        SpellDefinition payload = spell("payload", 1,
            new AddProjectileAction(projectile("payload", 1.0, 0.0, 0.0, NoitaDuration.ZERO)));
        List<SpellCardState> cards = new ArrayList<>();
        cards.add(PureWandFixtures.card(0, "trigger"));
        for (int slot = 1; slot <= 33; slot++) {
            cards.add(PureWandFixtures.card(slot, "payload"));
        }
        var initial = PureWandFixtures.state(100.0, cards);

        ResolvedCast result = new WandCastEvaluator(new CastBudget(2048, 128, 128, 16, 128, 2)).evaluate(
            PureWandFixtures.wand(1, false), initial, PureWandFixtures.catalog(trigger, payload), NoitaDuration.ZERO, 78L
        );

        assertEquals(ResolvedCast.Status.REJECTED, result.status());
        assertEquals(initial, result.nextState());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals("PAYLOAD_CHILDREN_BUDGET")));
    }

    @Test
    void modifierEffectLimitIsEnforcedBeforeTheAcceptedPlanNeedsPayloadPersistence() {
        SpellDefinition bolt = spell("bolt", 0,
            new AddProjectileAction(projectile("bolt", 1.0, 0.0, 0.0, NoitaDuration.ZERO)));
        var initial = PureWandFixtures.state(100.0, List.of(
            PureWandFixtures.card(0, "modifier"), PureWandFixtures.card(1, "bolt")
        ));

        ResolvedCast atLimit = new WandCastEvaluator().evaluate(PureWandFixtures.wand(1, false), initial,
            PureWandFixtures.catalog(effectModifier("modifier", ProjectilePlan.MAX_MODIFIER_EFFECTS), bolt),
            NoitaDuration.ZERO, 79L);
        assertEquals(ResolvedCast.Status.ACCEPTED, atLimit.status());
        assertEquals(ProjectilePlan.MAX_MODIFIER_EFFECTS, atLimit.effectPlan().projectiles().get(0).effects().size());

        ResolvedCast rejected = new WandCastEvaluator().evaluate(PureWandFixtures.wand(1, false), initial,
            PureWandFixtures.catalog(effectModifier("modifier", ProjectilePlan.MAX_MODIFIER_EFFECTS + 1), bolt),
            NoitaDuration.ZERO, 80L);
        assertEquals(ResolvedCast.Status.REJECTED, rejected.status());
        assertEquals(initial, rejected.nextState());
        assertTrue(rejected.diagnostics().stream().anyMatch(
            diagnostic -> diagnostic.code().equals("MODIFIER_EFFECT_BUDGET")));
    }

    private static SpellDefinition spell(String id, int manaCost, SpellAction... actions) {
        return new SpellDefinition(id, SpellCategory.PROJECTILE, manaCost, false, List.of(actions));
    }

    private static ProjectileDefinition projectile(
        String itemPath, double damage, double castDelayFrames, double rechargeFrames, NoitaDuration triggerDelay
    ) {
        return new ProjectileDefinition(itemPath, "BOLT", damage, 0.0, NoitaDuration.frames(60.0), castDelayFrames,
            rechargeFrames, 0, 0.0, 400.0, 0.0, 0.0, 0.99, 0.65, 1.0, 0.0, false, false, 1, 0.0,
            triggerDelay, 0, List.of());
    }

    private static SpellDefinition effectModifier(String id, int effectCount) {
        List<String> effects = java.util.stream.IntStream.range(0, effectCount)
            .mapToObj(index -> "effect_" + index)
            .toList();
        ShotModifier modifier = new ShotModifier(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0, false, false, false, 0, effects);
        return new SpellDefinition(id, SpellCategory.PROJECTILE_MODIFIER, 0, false,
            List.of(new ModifyShotAction(modifier), new DrawAction(1)));
    }
}
