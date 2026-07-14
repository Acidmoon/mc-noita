package com.mcnoita.wand.eval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mcnoita.spell.action.AddProjectileAction;
import com.mcnoita.spell.action.AddTriggerToNextProjectileAction;
import com.mcnoita.spell.action.BeginTriggerAction;
import com.mcnoita.spell.action.CallSpellAction;
import com.mcnoita.spell.action.DivideAction;
import com.mcnoita.spell.action.DrawAction;
import com.mcnoita.spell.action.DuplicateHandAction;
import com.mcnoita.spell.action.GreekCopyAction;
import com.mcnoita.spell.action.GreekCopyKind;
import com.mcnoita.spell.action.ModifyShotAction;
import com.mcnoita.spell.action.SpellAction;
import com.mcnoita.spell.action.SpellCategory;
import com.mcnoita.spell.action.SpellDefinition;
import com.mcnoita.spell.action.TargetQuery;
import com.mcnoita.spell.action.TimingAction;
import com.mcnoita.spell.action.UseConsumptionPolicy;
import com.mcnoita.spell.plan.ProjectileDefinition;
import com.mcnoita.spell.plan.ResolvedCast;
import com.mcnoita.spell.plan.ShotModifier;
import com.mcnoita.spell.plan.TriggerMode;
import com.mcnoita.wand.model.CardRef;
import com.mcnoita.wand.model.DeckState;
import com.mcnoita.wand.model.NoitaDuration;
import com.mcnoita.wand.model.SpellCardState;
import com.mcnoita.wand.model.WandState;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class G04InvocationSemanticsTest {
    @Test
    void gammaUsesDeckThenHandAndNeverFallsBackToDiscard() {
        SpellDefinition gamma = new SpellDefinition("gamma", SpellCategory.OTHER, 40, true,
            List.of(new CallSpellAction(TargetQuery.gamma())));
        SpellDefinition bolt = spell("bolt", SpellCategory.PROJECTILE, 80,
            new AddProjectileAction(projectile("bolt", 4.0)));
        SpellDefinition discarded = spell("discarded", SpellCategory.PROJECTILE, 0,
            new AddProjectileAction(projectile("discarded", 99.0)));
        SpellCardState gammaCard = card(0, "gamma");
        SpellCardState boltCard = card(1, "bolt", 3);
        SpellCardState discardedCard = card(2, "discarded");
        WandState state = state(50, List.of(gammaCard, boltCard), List.of(), List.of(discardedCard));

        ResolvedCast result = evaluate(state, List.of(gamma, bolt, discarded), 1L);

        assertEquals(List.of("bolt"), result.effectPlan().projectiles().stream().map(p -> p.itemPath()).toList());
        assertEquals(10.0, result.nextState().mana());
        assertEquals(3, result.remainingUses().get(boltCard.ref()));
        assertFalse(result.nextState().deckState().hand().contains(boltCard.ref()));
        assertTrue(result.trace().entries().stream().anyMatch(entry -> entry.kind() == InvocationKind.CALL
            && entry.targetSpellId().equals("bolt") && entry.pile().equals("DECK")));

        WandState noDeck = state(50, List.of(gammaCard), List.of(), List.of(discardedCard));
        ResolvedCast withoutCandidate = evaluate(noDeck, List.of(gamma, discarded), 2L);
        assertTrue(withoutCandidate.effectPlan().projectiles().isEmpty(), "Gamma must not select Discard");
    }

    @Test
    void recursiveCallsIncrementOnlyForRecursiveTargetsAndStopAfterLevelTwo() {
        TargetQuery modifier = externalCategory(SpellCategory.PROJECTILE_MODIFIER);
        TargetQuery utility = externalCategory(SpellCategory.UTILITY);
        TargetQuery projectile = externalCategory(SpellCategory.PROJECTILE);
        TargetQuery material = externalCategory(SpellCategory.MATERIAL);
        SpellDefinition root = spell("root", SpellCategory.OTHER, 0, new CallSpellAction(modifier));
        SpellDefinition nonRecursive = spell("non_recursive", SpellCategory.PROJECTILE_MODIFIER, 0,
            new CallSpellAction(utility));
        SpellDefinition levelOne = recursive("level_one", SpellCategory.UTILITY, new CallSpellAction(projectile));
        SpellDefinition levelTwo = recursive("level_two", SpellCategory.PROJECTILE, new CallSpellAction(material));
        SpellDefinition blocked = recursive("blocked", SpellCategory.MATERIAL,
            new AddProjectileAction(projectile("blocked", 1.0)));
        ExternalSpellPool pool = new ExternalSpellPool(List.of("non_recursive", "level_one", "level_two", "blocked"));

        ResolvedCast result = evaluate(PureWandFixtures.state(100, List.of(card(0, "root"))),
            List.of(root, nonRecursive, levelOne, levelTwo, blocked), 3L, pool);

        assertTrue(result.effectPlan().projectiles().isEmpty());
        assertTrue(result.trace().entries().stream().anyMatch(entry -> entry.targetSpellId().equals("non_recursive")
            && entry.recursionLevel() == 0));
        assertTrue(result.trace().entries().stream().anyMatch(entry -> entry.targetSpellId().equals("level_one")
            && entry.recursionLevel() == 1));
        assertTrue(result.trace().entries().stream().anyMatch(entry -> entry.targetSpellId().equals("level_two")
            && entry.recursionLevel() == 2));
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals("RECURSIVE_CALL_LIMIT")
            && diagnostic.spellId().equals("blocked")));
    }

    @Test
    void nestedCopyDrawSuppressionDoesNotLeakAfterTheOuterScope() {
        SpellDefinition omega = recursive("omega", SpellCategory.OTHER,
            new GreekCopyAction(GreekCopyKind.OMEGA));
        SpellDefinition tau = recursive("tau", SpellCategory.OTHER,
            new GreekCopyAction(GreekCopyKind.TAU));
        SpellDefinition multicast = spell("multicast", SpellCategory.MULTICAST, 0, new DrawAction(1));
        SpellDefinition bolt = spell("bolt", SpellCategory.PROJECTILE, 0,
            new AddProjectileAction(projectile("bolt", 1.0)));

        ResolvedCast result = evaluate(PureWandFixtures.state(100,
            List.of(card(0, "omega"), card(1, "tau"), card(2, "multicast"), card(3, "bolt"))),
            List.of(omega, tau, multicast, bolt), 4L);

        assertTrue(result.trace().entries().stream().anyMatch(entry -> entry.reason().equals("DRAW_SUPPRESSED")));
        assertTrue(result.status() == ResolvedCast.Status.ACCEPTED);
        assertFalse(result.trace().toJson().contains("@"), "trace JSON must be value-based and stable");
    }

    @Test
    void duplicateSnapshotsHandSkipsItselfAllowsTargetDrawAndAppliesTimingAfterCopy() {
        SpellDefinition bolt = spell("bolt", SpellCategory.PROJECTILE, 0,
            new AddProjectileAction(projectile("bolt", 2.0)));
        SpellDefinition duplicate = recursive("duplicate", SpellCategory.OTHER, new DuplicateHandAction());
        SpellDefinition tail = spell("tail", SpellCategory.PROJECTILE, 0,
            new AddProjectileAction(projectile("tail", 3.0)));

        ResolvedCast result = new WandCastEvaluator().evaluate(PureWandFixtures.wand(2, false),
            PureWandFixtures.state(500, List.of(card(0, "bolt"), card(1, "duplicate"), card(2, "tail"))),
            PureWandFixtures.catalog(bolt, duplicate, tail), NoitaDuration.ZERO, 5L);

        assertEquals(List.of("bolt", "bolt", "tail"),
            result.effectPlan().projectiles().stream().map(p -> p.itemPath()).toList());
        assertEquals(NoitaDuration.frames(20), result.castDelay());
        assertEquals(NoitaDuration.frames(20), result.rechargeTime());
        assertEquals(1, result.trace().entries().stream()
            .filter(entry -> entry.kind() == InvocationKind.COPY && entry.targetSpellId().equals("bolt")).count());
        assertFalse(result.trace().entries().stream().anyMatch(entry -> entry.kind() == InvocationKind.COPY
            && entry.targetSpellId().equals("duplicate")));
    }

    @Test
    void filteredGreekRestoresGlobalStateButRetainsShotChanges() {
        SpellDefinition mu = recursive("mu", SpellCategory.OTHER,
            new TimingAction(50, 0), new GreekCopyAction(GreekCopyKind.MU));
        ShotModifier damage = new ShotModifier(4, 0, 0, 1, 12, 13, 0, 0, 0, 0, 0,
            0, false, false, false, 0, List.of());
        SpellDefinition modifier = spell("modifier", SpellCategory.PROJECTILE_MODIFIER, -50,
            new ModifyShotAction(damage), new DrawAction(1));
        SpellDefinition bolt = spell("bolt", SpellCategory.PROJECTILE, 0,
            new AddProjectileAction(projectile("bolt", 2)));

        ResolvedCast result = evaluate(state(100, List.of(card(0, "mu"), card(2, "bolt")), List.of(),
            List.of(card(1, "modifier"))),
            List.of(mu, modifier, bolt), 6L);

        assertEquals(6.0, result.effectPlan().projectiles().get(0).damage());
        assertEquals(NoitaDuration.frames(50), result.castDelay());
        assertEquals(100.0, result.nextState().mana());
    }

    @Test
    void zetaUsesOnlyServerExternalPoolAndIsDeterministic() {
        SpellDefinition zeta = recursive("zeta", SpellCategory.OTHER, new GreekCopyAction(GreekCopyKind.ZETA));
        SpellDefinition externalA = spell("external_a", SpellCategory.PROJECTILE, 100,
            new AddProjectileAction(projectile("a", 1)));
        SpellDefinition externalB = spell("external_b", SpellCategory.PROJECTILE, 100,
            new AddProjectileAction(projectile("b", 2)));
        SpellDefinition local = spell("local", SpellCategory.PROJECTILE, 0,
            new AddProjectileAction(projectile("local", 3)));
        WandState state = PureWandFixtures.state(100,
            List.of(card(0, "zeta"), card(1, "local")));
        ExternalSpellPool pool = new ExternalSpellPool(List.of("external_a", "external_b"));

        ResolvedCast first = evaluate(state, List.of(zeta, externalA, externalB, local), 99L, pool);
        ResolvedCast second = evaluate(state, List.of(zeta, externalA, externalB, local), 99L, pool);
        assertEquals(first.effectPlan(), second.effectPlan());
        assertEquals(first.trace().toJson(), second.trace().toJson());
        assertEquals(2, first.effectPlan().projectiles().size());
        assertEquals("local", first.effectPlan().projectiles().get(1).itemPath());
        assertEquals(100.0, first.nextState().mana(), "external target mana is never paid");

        ResolvedCast empty = evaluate(state, List.of(zeta, externalA, externalB, local), 99L,
            ExternalSpellPool.EMPTY);
        assertEquals(List.of("local"), empty.effectPlan().projectiles().stream().map(p -> p.itemPath()).toList());
        assertTrue(empty.diagnostics().stream().anyMatch(d -> d.code().equals("EXTERNAL_CALL_TARGET_MISSING")));
    }

    @Test
    void divideUsesDelayedShotFreezeManualUseAndAtomicBudgetRejection() {
        SpellDefinition divide = spell("divide", SpellCategory.OTHER, 0,
            new DivideAction(2, 5, 20, 0, -2, -5, 5));
        SpellDefinition target = new SpellDefinition("target", SpellCategory.PROJECTILE, 100, false,
            List.of(new AddProjectileAction(projectile("target", 10))), UseConsumptionPolicy.WHEN_PROJECTILE_SHOT,
            "data/entities/projectiles/deck/target.xml");
        WandState state = PureWandFixtures.state(100, List.of(card(0, "divide"), card(1, "target", 3)));

        ResolvedCast result = evaluate(state, List.of(divide, target), 7L);
        assertEquals(2, result.effectPlan().projectiles().size());
        assertTrue(result.effectPlan().projectiles().stream().allMatch(plan -> plan.damage() == 8.0));
        assertTrue(result.effectPlan().projectiles().stream().allMatch(plan -> plan.explosionRadius() == 0.0));
        assertTrue(result.effectPlan().projectiles().stream().allMatch(plan -> plan.burstSpreadDegrees() == 5.0));
        assertEquals(2, result.remainingUses().get(CardRef.forSlot(1)));
        assertEquals(100.0, result.nextState().mana());

        ResolvedCast rejected = new WandCastEvaluator(new com.mcnoita.spell.plan.CastBudget(2, 128, 128, 16, 32, 2))
            .evaluate(PureWandFixtures.wand(1, false), state, PureWandFixtures.catalog(divide, target),
                NoitaDuration.ZERO, 7L);
        assertEquals(ResolvedCast.Status.REJECTED, rejected.status());
        assertEquals(state, rejected.nextState());
    }

    @Test
    void addTriggerSearchesModifierAndRelatedProjectileThenPreResolvesPayload() {
        SpellDefinition addTrigger = spell("add_trigger", SpellCategory.OTHER, 0,
            new AddTriggerToNextProjectileAction(TriggerMode.HIT));
        SpellDefinition modifier = spell("modifier", SpellCategory.PROJECTILE_MODIFIER, 100,
            new ModifyShotAction(new ShotModifier(4, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0,
                0, false, false, false, 0, List.of())), new DrawAction(1));
        SpellDefinition target = new SpellDefinition("target", SpellCategory.PROJECTILE, 100, false,
            List.of(new AddProjectileAction(projectile("target", 2))), UseConsumptionPolicy.WHEN_PROJECTILE_SHOT,
            "data/entities/projectiles/deck/target.xml");
        SpellDefinition payload = spell("payload", SpellCategory.PROJECTILE, 10,
            new AddProjectileAction(projectile("payload", 7)));
        WandState state = PureWandFixtures.state(50, List.of(card(0, "add_trigger"), card(1, "modifier"),
            card(2, "target", 2), card(3, "payload")));

        ResolvedCast result = evaluate(state, List.of(addTrigger, modifier, target, payload), 8L);

        assertEquals(1, result.effectPlan().projectiles().size());
        assertEquals(6.0, result.effectPlan().projectiles().get(0).damage());
        assertEquals(TriggerMode.HIT, result.effectPlan().projectiles().get(0).trigger().mode());
        assertEquals("payload", result.effectPlan().projectiles().get(0).trigger().payload().projectiles().get(0).itemPath());
        assertEquals(1, result.remainingUses().get(CardRef.forSlot(2)));
        assertEquals(40.0, result.nextState().mana(), "only the payload Draw pays mana");
    }

    @Test
    void everyGreekCopyKindSelectsRecursiveTargetsInsideTriggerPayloadShot() {
        for (GreekCopyKind kind : GreekCopyKind.values()) {
            SpellCategory targetCategory = switch (kind) {
                case MU -> SpellCategory.PROJECTILE_MODIFIER;
                case SIGMA -> SpellCategory.STATIC_PROJECTILE;
                default -> SpellCategory.PROJECTILE;
            };
            SpellDefinition root = spell("root_" + kind, SpellCategory.PROJECTILE, 0,
                new AddProjectileAction(projectile("root", 1)), new BeginTriggerAction(TriggerMode.HIT, 1));
            SpellDefinition greek = recursive("greek_" + kind, SpellCategory.OTHER, new GreekCopyAction(kind));
            SpellAction targetAction = targetCategory == SpellCategory.PROJECTILE_MODIFIER
                ? new ModifyShotAction(new ShotModifier(1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0,
                    0, false, false, false, 0, List.of()))
                : new AddProjectileAction(projectile("target_" + kind, 2));
            SpellDefinition target = new SpellDefinition("target_" + kind, targetCategory, 0, true,
                targetCategory == SpellCategory.PROJECTILE_MODIFIER
                    ? List.of(targetAction, new DrawAction(1)) : List.of(targetAction));
            SpellDefinition tail = spell("tail_" + kind, SpellCategory.PROJECTILE, 0,
                new AddProjectileAction(projectile("tail_" + kind, 3)));
            List<SpellCardState> cards = List.of(
                card(0, root.id()), card(1, greek.id()), card(2, target.id()), card(3, tail.id()));
            ExternalSpellPool external = kind == GreekCopyKind.ZETA
                ? new ExternalSpellPool(List.of(target.id())) : ExternalSpellPool.EMPTY;

            ResolvedCast result = evaluate(PureWandFixtures.state(500, cards),
                List.of(root, greek, target, tail), 100L + kind.ordinal(), external);

            assertEquals(ResolvedCast.Status.ACCEPTED, result.status(), kind.name());
            assertFalse(result.effectPlan().projectiles().get(0).trigger().payload().projectiles().isEmpty(), kind.name());
            assertTrue(result.trace().entries().stream().anyMatch(entry -> entry.targetSpellId().equals(target.id())
                && entry.recursionLevel() == 1), kind.name());
        }
    }

    @Test
    void alphaAndGammaCallRecursiveTargetsInsideTriggerPayloadShot() {
        for (boolean alpha : List.of(true, false)) {
            String callerId = alpha ? "alpha_payload" : "gamma_payload";
            SpellDefinition root = spell("root_" + callerId, SpellCategory.PROJECTILE, 0,
                new AddProjectileAction(projectile("root", 1)), new BeginTriggerAction(TriggerMode.HIT, 1));
            SpellDefinition caller = new SpellDefinition(callerId, SpellCategory.OTHER, 0, true,
                List.of(new CallSpellAction(alpha ? TargetQuery.alpha() : TargetQuery.gamma())));
            SpellDefinition target = new SpellDefinition("target_" + callerId, SpellCategory.PROJECTILE, 0, true,
                List.of(new AddProjectileAction(projectile("target", 2))));
            WandState state = alpha
                ? state(100, List.of(card(0, root.id()), card(1, caller.id())), List.of(), List.of(card(2, target.id())))
                : PureWandFixtures.state(100,
                    List.of(card(0, root.id()), card(1, caller.id()), card(2, target.id())));

            ResolvedCast result = evaluate(state, List.of(root, caller, target), alpha ? 201L : 202L);

            assertEquals(ResolvedCast.Status.ACCEPTED, result.status(), callerId);
            assertEquals("target", result.effectPlan().projectiles().get(0).trigger().payload().projectiles().get(0).itemPath());
            assertTrue(result.trace().entries().stream().anyMatch(entry -> entry.targetSpellId().equals(target.id())
                && entry.recursionLevel() == 1), callerId);
        }
    }

    @Test
    void addTriggerSupportsAllModesAndFallsBackToDirectCallWithoutPayload() {
        for (TriggerMode mode : List.of(TriggerMode.HIT, TriggerMode.TIMER, TriggerMode.EXPIRATION)) {
            SpellDefinition wrapper = spell("wrapper_" + mode, SpellCategory.OTHER, 0,
                new AddTriggerToNextProjectileAction(mode));
            SpellDefinition target = new SpellDefinition("target_" + mode, SpellCategory.PROJECTILE, 50, false,
                List.of(new AddProjectileAction(projectile("target", 2))), UseConsumptionPolicy.WHEN_PROJECTILE_SHOT,
                "data/entities/projectiles/deck/target.xml");
            SpellDefinition payload = spell("payload_" + mode, SpellCategory.PROJECTILE, 0,
                new AddProjectileAction(projectile("payload", 3)));

            ResolvedCast result = evaluate(PureWandFixtures.state(100,
                List.of(card(0, wrapper.id()), card(1, target.id(), 2), card(2, payload.id()))),
                List.of(wrapper, target, payload), 300L + mode.ordinal());

            assertEquals(mode, result.effectPlan().projectiles().get(0).trigger().mode());
            assertEquals(mode == TriggerMode.TIMER ? NoitaDuration.frames(20) : NoitaDuration.ZERO,
                result.effectPlan().projectiles().get(0).trigger().timerDelay());
        }

        SpellDefinition wrapper = spell("wrapper_no_payload", SpellCategory.OTHER, 0,
            new AddTriggerToNextProjectileAction(TriggerMode.HIT));
        SpellDefinition target = new SpellDefinition("target_no_payload", SpellCategory.PROJECTILE, 90, false,
            List.of(new AddProjectileAction(projectile("direct", 4))), UseConsumptionPolicy.WHEN_PROJECTILE_SHOT,
            "data/entities/projectiles/deck/direct.xml");
        ResolvedCast fallback = evaluate(PureWandFixtures.state(100,
            List.of(card(0, wrapper.id()), card(1, target.id(), 2))), List.of(wrapper, target), 304L);
        assertEquals("direct", fallback.effectPlan().projectiles().get(0).itemPath());
        assertFalse(fallback.effectPlan().projectiles().get(0).hasTrigger());
        assertEquals(100.0, fallback.nextState().mana());
        assertEquals(1, fallback.remainingUses().get(CardRef.forSlot(1)));
    }

    @Test
    void nestedDividePropagatesIterationAndDiscardsOnlyTheConsumedPrefix() {
        SpellDefinition divideTwo = spell("divide_two", SpellCategory.OTHER, 0,
            new DivideAction(2, 5, 20, 0, -0.2, -5, 5));
        SpellDefinition divideThree = spell("divide_three", SpellCategory.OTHER, 0,
            new DivideAction(3, 4, 35, 0, -0.4, -10, 5));
        SpellDefinition target = new SpellDefinition("nested_target", SpellCategory.PROJECTILE, 100, false,
            List.of(new AddProjectileAction(projectile("nested_target", 10))),
            UseConsumptionPolicy.WHEN_PROJECTILE_SHOT, "data/entities/projectiles/deck/nested_target.xml");
        SpellDefinition tail = spell("tail", SpellCategory.PROJECTILE, 0,
            new AddProjectileAction(projectile("tail", 1)));
        WandState state = PureWandFixtures.state(100, List.of(card(0, divideTwo.id()), card(1, divideThree.id()),
            card(2, target.id(), 20), card(3, tail.id())));

        ResolvedCast result = evaluate(state, List.of(divideTwo, divideThree, target, tail), 401L);

        assertEquals(6, result.effectPlan().projectiles().size());
        assertTrue(result.effectPlan().projectiles().stream().allMatch(plan -> plan.damage() == 9.0));
        assertEquals(18, result.remainingUses().get(CardRef.forSlot(2)));
        assertTrue(result.nextState().deckState().deck().contains(CardRef.forSlot(3)));
        assertTrue(result.trace().entries().stream().anyMatch(entry -> entry.kind() == InvocationKind.DIVIDE
            && entry.divideIteration() == 2));
    }

    @Test
    void copyGraphsRemainDeterministicConservativeAndTerminatingAcrossSeeds() {
        SpellDefinition divide = spell("divide", SpellCategory.OTHER, 0,
            new DivideAction(4, 4, 50, 0, -0.6, -20, 5));
        SpellDefinition duplicate = recursive("duplicate", SpellCategory.OTHER, new DuplicateHandAction());
        SpellDefinition bolt = spell("bolt", SpellCategory.PROJECTILE, 0,
            new AddProjectileAction(projectile("bolt", 5)));
        List<SpellCardState> cards = List.of(card(0, divide.id()), card(1, duplicate.id()), card(2, bolt.id()));
        WandState state = PureWandFixtures.state(500, cards);

        for (long seed = 0; seed < 100; seed++) {
            ResolvedCast first = evaluate(state, List.of(divide, duplicate, bolt), seed);
            ResolvedCast second = evaluate(state, List.of(divide, duplicate, bolt), seed);
            assertEquals(first, second, "seed=" + seed);
            DeckState piles = first.nextState().deckState();
            assertEquals(cards.size(), piles.deck().size() + piles.hand().size() + piles.discard().size(),
                "seed=" + seed);
            assertEquals(cards.size(), piles.cards().size(), "seed=" + seed);
        }
    }

    private static TargetQuery externalCategory(SpellCategory category) {
        return new TargetQuery(List.of(TargetQuery.Source.EXTERNAL), TargetQuery.Direction.FIRST, Set.of(category),
            Set.of(), true, false, 1);
    }

    private static ResolvedCast evaluate(WandState state, List<SpellDefinition> definitions, long seed) {
        return evaluate(state, definitions, seed, ExternalSpellPool.EMPTY);
    }

    private static ResolvedCast evaluate(WandState state, List<SpellDefinition> definitions, long seed,
                                         ExternalSpellPool external) {
        return new WandCastEvaluator().evaluate(PureWandFixtures.wand(1, false), state,
            PureWandFixtures.catalog(definitions.toArray(SpellDefinition[]::new)), NoitaDuration.ZERO, seed, external);
    }

    private static SpellDefinition spell(String id, SpellCategory category, int mana, SpellAction... actions) {
        return new SpellDefinition(id, category, mana, false, List.of(actions));
    }

    private static SpellDefinition recursive(String id, SpellCategory category, SpellAction... actions) {
        return new SpellDefinition(id, category, 0, true, List.of(actions));
    }

    private static SpellCardState card(int slot, String id) {
        return card(slot, id, SpellCardState.UNLIMITED_USES);
    }

    private static SpellCardState card(int slot, String id, int uses) {
        return PureWandFixtures.card(slot, id, uses);
    }

    private static WandState state(double mana, List<SpellCardState> deckCards, List<SpellCardState> handCards,
                                   List<SpellCardState> discardCards) {
        Map<CardRef, SpellCardState> cards = new LinkedHashMap<>();
        for (SpellCardState card : List.of(deckCards, handCards, discardCards).stream().flatMap(List::stream).toList()) {
            cards.put(card.ref(), card);
        }
        return new WandState(new DeckState(cards, deckCards.stream().map(SpellCardState::ref).toList(),
            handCards.stream().map(SpellCardState::ref).toList(), discardCards.stream().map(SpellCardState::ref).toList()),
            mana, NoitaDuration.ZERO, NoitaDuration.ZERO, false, 0, 17);
    }

    private static ProjectileDefinition projectile(String id, double damage) {
        return PureWandFixtures.projectile(id, damage);
    }
}
