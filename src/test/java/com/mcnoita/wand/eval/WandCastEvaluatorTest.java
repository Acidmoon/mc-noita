package com.mcnoita.wand.eval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mcnoita.spell.action.AddProjectileAction;
import com.mcnoita.spell.action.BeginTriggerAction;
import com.mcnoita.spell.action.CallSelection;
import com.mcnoita.spell.action.CallSpellAction;
import com.mcnoita.spell.action.DrawAction;
import com.mcnoita.spell.action.ModifyShotAction;
import com.mcnoita.spell.action.RandomSpellAction;
import com.mcnoita.spell.action.SpellCategory;
import com.mcnoita.spell.action.SpellDefinition;
import com.mcnoita.spell.plan.CastBudget;
import com.mcnoita.spell.plan.ProjectilePlan;
import com.mcnoita.spell.plan.ResolvedCast;
import com.mcnoita.spell.plan.ShotModifier;
import com.mcnoita.spell.plan.TriggerMode;
import com.mcnoita.wand.model.CardRef;
import com.mcnoita.wand.model.DeckState;
import com.mcnoita.wand.model.NoitaDuration;
import com.mcnoita.wand.model.SpellCardState;
import com.mcnoita.wand.model.WandState;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("regression")
class WandCastEvaluatorTest {
    @Test
    void sameInputsAndSeedProduceEqualResolvedCasts() {
        SpellDefinition bolt = PureWandFixtures.definition("bolt", 5, new AddProjectileAction(PureWandFixtures.projectile("bolt", 3.0)));
        WandState state = PureWandFixtures.state(100.0, List.of(PureWandFixtures.card(0, "bolt")));
        WandCastEvaluator evaluator = new WandCastEvaluator();

        ResolvedCast first = evaluator.evaluate(PureWandFixtures.wand(1, false), state, PureWandFixtures.catalog(bolt), NoitaDuration.ZERO, 42L);
        ResolvedCast second = evaluator.evaluate(PureWandFixtures.wand(1, false), state, PureWandFixtures.catalog(bolt), NoitaDuration.ZERO, 42L);

        assertEquals(first, second);
    }

    @Test
    void differentSeedsShuffleOnlyTheConfiguredCards() {
        SpellDefinition bolt = PureWandFixtures.definition("bolt", 0, new AddProjectileAction(PureWandFixtures.projectile("bolt", 1.0)));
        List<SpellCardState> cards = new ArrayList<>();
        for (int slot = 0; slot < 8; slot++) {
            cards.add(PureWandFixtures.card(slot, "bolt"));
        }
        WandState readyToReload = new WandState(new DeckState(
            PureWandFixtures.state(100.0, cards).deckState().cards(),
            List.of(),
            List.of(),
            cards.stream().map(SpellCardState::ref).toList()
        ), 100.0, NoitaDuration.ZERO, NoitaDuration.ZERO, false, 0L, 17);
        WandCastEvaluator evaluator = new WandCastEvaluator();

        ResolvedCast first = evaluator.evaluate(PureWandFixtures.wand(1, true), readyToReload, PureWandFixtures.catalog(bolt), NoitaDuration.ZERO, 1L);
        ResolvedCast second = evaluator.evaluate(PureWandFixtures.wand(1, true), readyToReload, PureWandFixtures.catalog(bolt), NoitaDuration.ZERO, 2L);

        Set<CardRef> expected = new HashSet<>(readyToReload.deckState().cards().keySet());
        Set<CardRef> firstCards = allCards(first.nextState());
        Set<CardRef> secondCards = allCards(second.nextState());
        assertEquals(expected, firstCards);
        assertEquals(expected, secondCards);
        assertNotEquals(first.nextState().deckState().deck(), second.nextState().deckState().deck());
    }

    @Test
    void insufficientManaAndDepletedCardsProduceNoProjectileAndConserveCards() {
        SpellDefinition expensive = PureWandFixtures.definition("expensive", 50, new AddProjectileAction(PureWandFixtures.projectile("expensive", 9.0)));
        SpellDefinition empty = PureWandFixtures.definition("empty", 0, new AddProjectileAction(PureWandFixtures.projectile("empty", 9.0)));
        WandState state = PureWandFixtures.state(10.0, List.of(PureWandFixtures.card(0, "expensive"), PureWandFixtures.card(1, "empty", 0)));

        ResolvedCast result = new WandCastEvaluator().evaluate(PureWandFixtures.wand(1, false), state,
            PureWandFixtures.catalog(expensive, empty), NoitaDuration.ZERO, 7L);

        assertTrue(result.effectPlan().projectiles().isEmpty());
        assertEquals(allCards(state), allCards(result.nextState()));
        assertEquals(10.0, result.nextState().mana());
    }

    @Test
    void triggerUsesIndependentShotStateButPayloadRechargeRemainsGlobal() {
        SpellDefinition trigger = PureWandFixtures.definition("trigger", 0,
            new AddProjectileAction(PureWandFixtures.projectile("trigger", 1.0)), new BeginTriggerAction(TriggerMode.HIT, 1));
        SpellDefinition modifier = new SpellDefinition("modifier", SpellCategory.PROJECTILE_MODIFIER, 0, false,
            List.of(new ModifyShotAction(new ShotModifier(4.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0, false, false, false, 0, List.of())), new DrawAction(1)));
        SpellDefinition payload = PureWandFixtures.definition("payload", 0,
            new AddProjectileAction(PureWandFixtures.projectile("payload", 2.0, 20.0)));
        WandState state = PureWandFixtures.state(100.0, List.of(PureWandFixtures.card(0, "trigger"),
            PureWandFixtures.card(1, "modifier"), PureWandFixtures.card(2, "payload")));

        ResolvedCast result = new WandCastEvaluator().evaluate(PureWandFixtures.wand(1, false), state,
            PureWandFixtures.catalog(trigger, modifier, payload), NoitaDuration.ZERO, 1L);

        ProjectilePlan root = result.effectPlan().projectiles().get(0);
        assertEquals(1.0, root.damage());
        assertEquals(1, root.payloads().size());
        assertEquals(6.0, root.payloads().get(0).damage());
        assertEquals(20.0, result.rechargeTime().frames());
    }

    @Test
    void randomSpellOnlyUsesItsAllowedCandidateCategory() {
        SpellDefinition random = PureWandFixtures.definition("random", 0, new RandomSpellAction(SpellCategory.PROJECTILE));
        SpellDefinition bolt = PureWandFixtures.definition("bolt", 0, new AddProjectileAction(PureWandFixtures.projectile("bolt", 1.0)));
        SpellDefinition utility = new SpellDefinition("utility", SpellCategory.UTILITY, 0, false,
            List.of(new AddProjectileAction(PureWandFixtures.projectile("utility", 1.0))));

        ResolvedCast result = new WandCastEvaluator().evaluate(PureWandFixtures.wand(1, false),
            PureWandFixtures.state(100.0, List.of(PureWandFixtures.card(0, "random"))),
            PureWandFixtures.catalog(random, bolt, utility), NoitaDuration.ZERO, 22L);

        assertEquals("bolt", result.effectPlan().projectiles().get(0).itemPath());
    }

    @Test
    void recursiveCallsAndActionBudgetsTerminateDeterministically() {
        SpellDefinition recursive = new SpellDefinition("recursive", SpellCategory.OTHER, 0, true,
            List.of(new CallSpellAction(CallSelection.FIRST_AVAILABLE)));
        WandState state = PureWandFixtures.state(100.0, List.of(PureWandFixtures.card(0, "recursive")));
        ResolvedCast recursionResult = new WandCastEvaluator().evaluate(PureWandFixtures.wand(1, false), state,
            PureWandFixtures.catalog(recursive), NoitaDuration.ZERO, 3L);
        assertTrue(recursionResult.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals("RECURSIVE_CALL_LIMIT")));

        SpellDefinition unbounded = new SpellDefinition("unbounded", SpellCategory.OTHER, 0, false,
            List.of(new CallSpellAction(CallSelection.FIRST_AVAILABLE)));
        ResolvedCast budgetResult = new WandCastEvaluator(new CastBudget(3, 128, 16, 2)).evaluate(PureWandFixtures.wand(1, false),
            PureWandFixtures.state(100.0, List.of(PureWandFixtures.card(0, "unbounded"))), PureWandFixtures.catalog(unbounded), NoitaDuration.ZERO, 4L);
        assertEquals(ResolvedCast.Status.REJECTED, budgetResult.status());
        assertTrue(budgetResult.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals("ACTION_BUDGET")));
    }

    @Test
    void returnedModelsAndCollectionsCannotBeMutatedExternally() {
        List<SpellCardState> mutableCards = new ArrayList<>(List.of(PureWandFixtures.card(0, "bolt")));
        WandState state = PureWandFixtures.state(100.0, mutableCards);
        mutableCards.clear();
        SpellDefinition bolt = PureWandFixtures.definition("bolt", 0, new AddProjectileAction(PureWandFixtures.projectile("bolt", 1.0)));
        ResolvedCast result = new WandCastEvaluator().evaluate(PureWandFixtures.wand(1, false), state,
            PureWandFixtures.catalog(bolt), NoitaDuration.ZERO, 5L);

        assertEquals(1, state.deckState().cards().size());
        assertThrows(UnsupportedOperationException.class, () -> result.effectPlan().projectiles().add(null));
        assertThrows(UnsupportedOperationException.class, () -> result.remainingUses().clear());
    }

    private static Set<CardRef> allCards(WandState state) {
        Set<CardRef> refs = new HashSet<>(state.deckState().deck());
        refs.addAll(state.deckState().hand());
        refs.addAll(state.deckState().discard());
        return refs;
    }
}
