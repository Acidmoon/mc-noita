package com.mcnoita.wand.eval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mcnoita.spell.action.AddProjectileAction;
import com.mcnoita.spell.action.BeginTriggerAction;
import com.mcnoita.spell.action.DrawAction;
import com.mcnoita.spell.action.ModifyShotAction;
import com.mcnoita.spell.action.RefreshWandAction;
import com.mcnoita.spell.action.SpellAction;
import com.mcnoita.spell.action.SpellCatalog;
import com.mcnoita.spell.action.SpellCategory;
import com.mcnoita.spell.action.SpellDefinition;
import com.mcnoita.spell.action.TimingAction;
import com.mcnoita.spell.action.TimingOperation;
import com.mcnoita.spell.action.UseConsumptionPolicy;
import com.mcnoita.spell.plan.ProjectileDefinition;
import com.mcnoita.spell.plan.ResolvedCast;
import com.mcnoita.spell.plan.ShotModifier;
import com.mcnoita.spell.plan.TriggerMode;
import com.mcnoita.wand.model.CardRef;
import com.mcnoita.wand.model.DeckState;
import com.mcnoita.wand.model.NoitaDuration;
import com.mcnoita.wand.model.SpellCardState;
import com.mcnoita.wand.model.WandDefinition;
import com.mcnoita.wand.model.WandState;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * G02 regression fixtures for the basic Noita draw state machine.
 *
 * <p>The source contract is the draw/reload path in the fixed local gun.lua
 * evidence and the Noita Wiki pages listed in docs/基础施法语义.md. These tests
 * deliberately avoid Call/Greek and trigger lifecycle policy reserved for G03.
 */
@Tag("wiki-golden")
@Tag("regression")
class G02WandCastSemanticsTest {
    private static final WandCastEvaluator EVALUATOR = new WandCastEvaluator();

    @Test
    void threeBoltsWithTwoSpellsPerCastFireTwoThenOneAndReload() {
        SpellDefinition bolt = projectile("bolt", 0, 1.0);
        WandDefinition wand = wand(2, false, 0, 30, 100, 0.0, List.of());
        WandState initial = state(100.0, List.of(card(0, "bolt"), card(1, "bolt"), card(2, "bolt")));

        ResolvedCast first = evaluate(wand, initial, 4L, bolt);
        ResolvedCast second = evaluate(wand, first.nextState(), 5L, bolt);

        assertEquals(2, first.effectPlan().projectiles().size());
        assertEquals(List.of(CardRef.forSlot(2)), first.nextState().deckState().deck());
        assertEquals(1, second.effectPlan().projectiles().size());
        assertEquals(NoitaDuration.frames(30), second.nextState().rechargeRemaining());
        assertEquals(List.of(CardRef.forSlot(0), CardRef.forSlot(1), CardRef.forSlot(2)), second.nextState().deckState().deck());
        assertTrue(second.drawOutcomes().stream().anyMatch(outcome -> outcome.origin() == DrawOrigin.INITIAL
            && outcome.deckExhausted() && outcome.reloading()));
    }

    @Test
    void actionDrawCanWrapAndRemainingInitialDrawUsesTheNewDeck() {
        SpellDefinition modifier = definition("modifier", SpellCategory.PROJECTILE_MODIFIER, 0,
            new DrawAction(1));
        SpellDefinition bolt = projectile("bolt", 0, 1.0);
        WandDefinition wand = wand(2, false, 0, 20, 100, 0.0, List.of());
        WandState initial = state(
            100.0,
            List.of(card(0, "modifier"), card(1, "bolt"), card(2, "bolt")),
            List.of(0),
            List.of(1, 2)
        );

        ResolvedCast result = evaluate(wand, initial, 7L, modifier, bolt);

        assertEquals(2, result.effectPlan().projectiles().size());
        assertEquals(NoitaDuration.frames(20), result.nextState().rechargeRemaining());
        assertEquals(List.of(CardRef.forSlot(0), CardRef.forSlot(1), CardRef.forSlot(2)), result.nextState().deckState().deck());
        assertTrue(result.drawOutcomes().stream().anyMatch(outcome -> outcome.origin() == DrawOrigin.ACTION
            && outcome.wrapped() && outcome.startReload()));
    }

    @Test
    void failedDrawRetriesLaterCardsButAFailedLastCardDoesNotWrapImmediately() {
        SpellDefinition modifier = definition("modifier", SpellCategory.PROJECTILE_MODIFIER, 0,
            new DrawAction(1));
        SpellDefinition expensive = projectile("expensive", 20, 5.0);
        SpellDefinition fallback = projectile("fallback", 0, 3.0);
        WandDefinition wand = wand(1, false, 0, 10, 100, 0.0, List.of());

        WandState retryState = state(10.0, List.of(card(0, "expensive"), card(1, "fallback")));
        ResolvedCast retry = evaluate(wand, retryState, 8L, expensive, fallback);
        assertEquals(1, retry.effectPlan().projectiles().size());
        assertEquals("fallback", retry.effectPlan().projectiles().get(0).itemPath());
        assertEquals(10.0, retry.nextState().mana());
        assertEquals(1, retry.drawOutcomes().get(0).failures().size());
        assertEquals(DrawFailureReason.INSUFFICIENT_MANA, retry.drawOutcomes().get(0).failures().get(0).reason());

        WandState lastFailure = state(
            10.0,
            List.of(card(0, "modifier"), card(1, "expensive"), card(2, "fallback")),
            List.of(0, 1),
            List.of(2)
        );
        ResolvedCast exhausted = evaluate(wand, lastFailure, 9L, modifier, expensive, fallback);
        DrawOutcome actionOutcome = exhausted.drawOutcomes().stream()
            .filter(outcome -> outcome.origin() == DrawOrigin.ACTION)
            .findFirst()
            .orElseThrow();
        assertTrue(exhausted.effectPlan().projectiles().isEmpty());
        assertTrue(actionOutcome.deckExhausted());
        assertFalse(actionOutcome.wrapped());
        assertFalse(actionOutcome.drawnCards().contains(CardRef.forSlot(2)));
    }

    @Test
    void nonShuffleReloadRestoresSlotsAndShuffleIsSeedDeterministic() {
        SpellDefinition bolt = projectile("bolt", 0, 1.0);
        List<SpellCardState> cards = List.of(card(0, "bolt"), card(1, "bolt"), card(2, "bolt"), card(3, "bolt"));
        WandState emptyDeck = state(100.0, cards, List.of(), List.of(0, 1, 2, 3));

        ResolvedCast ordered = evaluate(wand(1, false, 0, 5, 100, 0.0, List.of()), emptyDeck, 11L, bolt);
        assertEquals(List.of(CardRef.forSlot(0), CardRef.forSlot(1), CardRef.forSlot(2), CardRef.forSlot(3)),
            ordered.nextState().deckState().deck());

        ResolvedCast shuffledA = evaluate(wand(1, true, 0, 5, 100, 0.0, List.of()), emptyDeck, 12L, bolt);
        ResolvedCast shuffledB = evaluate(wand(1, true, 0, 5, 100, 0.0, List.of()), emptyDeck, 12L, bolt);
        assertEquals(shuffledA.nextState().deckState().deck(), shuffledB.nextState().deckState().deck());
        assertEquals(new HashSet<>(cards.stream().map(SpellCardState::ref).toList()),
            new HashSet<>(shuffledA.nextState().deckState().deck()));
    }

    @Test
    void normalAndNegativeManaFollowOneChargePerNormalDraw() {
        SpellDefinition costly = definition("costly", SpellCategory.PROJECTILE_MODIFIER, 30, new DrawAction(1));
        SpellDefinition addMana = definition("add_mana", SpellCategory.PROJECTILE_MODIFIER, -30, new DrawAction(1));
        SpellDefinition bolt = projectile("bolt", 80, 1.0);
        WandDefinition wand = wand(1, false, 0, 0, 100, 0.0, List.of());

        ResolvedCast positive = evaluate(wand, state(100.0, List.of(card(0, "costly"), card(1, "bolt"))), 13L, costly, bolt);
        assertEquals(70.0, positive.nextState().mana());

        ResolvedCast negative = evaluate(wand, state(50.0, List.of(card(0, "add_mana"), card(1, "bolt"))), 14L, addMana, bolt);
        assertEquals(0.0, negative.nextState().mana());
        assertEquals(1, negative.effectPlan().projectiles().size());

        ResolvedCast insufficient = evaluate(wand, state(10.0, List.of(card(0, "bolt"))), 15L, bolt);
        assertEquals(10.0, insufficient.nextState().mana());
        assertTrue(insufficient.effectPlan().projectiles().isEmpty());

        ResolvedCast depleted = evaluate(wand, state(100.0, List.of(card(0, "bolt", 0))), 16L, bolt);
        assertEquals(100.0, depleted.nextState().mana());
        assertTrue(depleted.effectPlan().projectiles().isEmpty());
        assertEquals(DrawFailureReason.DEPLETED_USES, depleted.drawOutcomes().get(0).failures().get(0).reason());
    }

    @Test
    void manaRegenerationClampsAtManaMaxOnlyAfterElapsedTime() {
        SpellDefinition harmless = projectile("harmless", 0, 1.0);
        WandDefinition wand = wand(1, false, 0, 0, 100, 30.0, List.of());
        WandState initial = state(80.0, List.of(card(0, "harmless")));

        ResolvedCast result = EVALUATOR.evaluate(wand, initial, catalog(harmless), NoitaDuration.frames(120), 17L);

        assertEquals(100.0, result.nextState().mana());
    }

    @Test
    void handDiscardUsesFollowCategoryPoliciesExactlyOnce() {
        SpellDefinition modifier = definition("modifier", SpellCategory.PROJECTILE_MODIFIER, 0, new DrawAction(1));
        SpellDefinition bolt = projectile("bolt", 0, 1.0);
        SpellDefinition utility = new SpellDefinition("utility", SpellCategory.UTILITY, 0, false, List.of(),
            UseConsumptionPolicy.ALWAYS_ON_HAND_DISCARD);
        WandDefinition wand = wand(1, false, 0, 0, 100, 0.0, List.of());

        ResolvedCast withProjectile = evaluate(wand, state(100.0, List.of(card(0, "modifier", 2), card(1, "bolt", 2))), 17L,
            modifier, bolt);
        assertEquals(1, withProjectile.remainingUses().get(CardRef.forSlot(0)));
        assertEquals(1, withProjectile.remainingUses().get(CardRef.forSlot(1)));

        ResolvedCast modifierOnly = evaluate(wand, state(100.0, List.of(card(0, "modifier", 2))), 18L, modifier);
        assertEquals(2, modifierOnly.remainingUses().get(CardRef.forSlot(0)));

        ResolvedCast utilityOnly = evaluate(wand, state(100.0, List.of(card(0, "utility", 2))), 19L, utility);
        assertEquals(1, utilityOnly.remainingUses().get(CardRef.forSlot(0)));
    }

    @Test
    void alwaysCastChargesNoPositiveManaHasUnlimitedUsesAndSuppressesOnlySingleDraw() {
        SpellDefinition permanentMana = definition("permanent_mana", SpellCategory.PROJECTILE_MODIFIER, -30, new DrawAction(1));
        SpellDefinition bolt = projectile("bolt", 10, 1.0);
        WandDefinition wand = wand(1, false, 0, 0, 100, 0.0, List.of("permanent_mana"));

        ResolvedCast result = evaluate(wand, state(50.0, List.of(card(0, "bolt", 2))), 20L, permanentMana, bolt);

        assertEquals(70.0, result.nextState().mana());
        assertEquals(1, result.effectPlan().projectiles().size());
        assertEquals(1, result.remainingUses().get(CardRef.forSlot(0)));
        assertTrue(result.drawOutcomes().stream().noneMatch(outcome -> outcome.origin() == DrawOrigin.PERMANENT));
    }

    @Test
    void alwaysCastTriggerResolvesItsPayloadDuringEvaluationWithoutCreatingCardUseChanges() {
        SpellDefinition permanentTrigger = definition("always_trigger", SpellCategory.PROJECTILE, 50,
            new AddProjectileAction(projectileDefinition("trigger", 1.0)), new BeginTriggerAction(TriggerMode.HIT, 1));
        SpellDefinition payload = projectile("payload", 20, 2.0);
        WandDefinition wand = wand(1, false, 0, 0, 100, 0.0, List.of("always_trigger"));

        ResolvedCast result = evaluate(wand, state(40.0, List.of(card(0, "payload", 2))), 21L, permanentTrigger, payload);

        assertEquals(1, result.effectPlan().projectiles().size());
        assertEquals(1, result.effectPlan().projectiles().get(0).payloads().size());
        assertEquals(20.0, result.nextState().mana());
        assertEquals(1, result.remainingUses().get(CardRef.forSlot(0)));
    }

    @Test
    void alwaysCastMultiDrawRemainsARealDrawRequest() {
        SpellDefinition permanentMulticast = definition("permanent_multicast", SpellCategory.PROJECTILE_MODIFIER, 0,
            new DrawAction(2));
        SpellDefinition bolt = projectile("bolt", 0, 1.0);
        WandDefinition wand = wand(1, false, 0, 0, 100, 0.0, List.of("permanent_multicast"));

        ResolvedCast result = evaluate(wand, state(100.0, List.of(card(0, "bolt"), card(1, "bolt"))), 22L,
            permanentMulticast, bolt);

        assertEquals(2, result.effectPlan().projectiles().size());
        assertTrue(result.drawOutcomes().stream().anyMatch(outcome -> outcome.origin() == DrawOrigin.PERMANENT
            && outcome.completedDraws() == 2));
    }

    @Test
    void castDelayAndRechargeRunConcurrentlyAndTimingOperationsClampAtZero() {
        SpellDefinition bolt = projectile("bolt", 0, 1.0);
        WandState oneBolt = state(100.0, List.of(card(0, "bolt")));

        ResolvedCast rechargeLonger = evaluate(wand(1, false, 10, 30, 100, 0.0, List.of()), oneBolt, 23L, bolt);
        assertFalse(EVALUATOR.evaluate(wand(1, false, 10, 30, 100, 0.0, List.of()), rechargeLonger.nextState(), catalog(bolt),
            NoitaDuration.frames(10), 24L).status() == ResolvedCast.Status.ACCEPTED);
        assertEquals(ResolvedCast.Status.ACCEPTED, EVALUATOR.evaluate(wand(1, false, 10, 30, 100, 0.0, List.of()), rechargeLonger.nextState(),
            catalog(bolt), NoitaDuration.frames(30), 25L).status());

        ResolvedCast delayLonger = evaluate(wand(1, false, 30, 10, 100, 0.0, List.of()), oneBolt, 26L, bolt);
        assertFalse(EVALUATOR.evaluate(wand(1, false, 30, 10, 100, 0.0, List.of()), delayLonger.nextState(), catalog(bolt),
            NoitaDuration.frames(10), 27L).status() == ResolvedCast.Status.ACCEPTED);
        assertEquals(ResolvedCast.Status.ACCEPTED, EVALUATOR.evaluate(wand(1, false, 30, 10, 100, 0.0, List.of()), delayLonger.nextState(),
            catalog(bolt), NoitaDuration.frames(30), 28L).status());

        ResolvedCast castDelayOnly = evaluate(wand(1, false, 12, 30, 100, 0.0, List.of()),
            state(100.0, List.of(card(0, "bolt"), card(1, "bolt"))), 29L, bolt);
        assertEquals(NoitaDuration.frames(12), castDelayOnly.nextState().castDelayRemaining());
        assertEquals(NoitaDuration.ZERO, castDelayOnly.nextState().rechargeRemaining());

        SpellDefinition timeReset = definition("time_reset", SpellCategory.UTILITY, 0,
            new TimingAction(TimingOperation.SET, 0.0, TimingOperation.ADD, -20.0));
        ResolvedCast reset = evaluate(wand(1, false, 10, 0, 100, 0.0, List.of()), state(100.0, List.of(card(0, "time_reset"), card(1, "bolt"))),
            30L, timeReset, bolt);
        assertEquals(NoitaDuration.ZERO, reset.nextState().castDelayRemaining());
        assertEquals(NoitaDuration.ZERO, reset.nextState().rechargeRemaining());

        SpellDefinition timedModifier = definition("timed_modifier", SpellCategory.PROJECTILE_MODIFIER, 0,
            new ModifyShotAction(new ShotModifier(0.0, 0.0, 0.0, 1.0, 10.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0, false, false, false, 0, List.of())), new DrawAction(2));
        ResolvedCast multicast = evaluate(wand(1, false, 0, 0, 100, 0.0, List.of()),
            state(100.0, List.of(card(0, "timed_modifier"), card(1, "bolt"), card(2, "bolt"))), 31L, timedModifier, bolt);
        assertEquals(2, multicast.effectPlan().projectiles().size());
        assertEquals(NoitaDuration.frames(10), multicast.nextState().castDelayRemaining());
    }

    @Test
    void firstWandRefreshReordersWithoutReloadButSecondForcesRecharge() {
        SpellDefinition refresh = definition("refresh", SpellCategory.UTILITY, 0, new RefreshWandAction());
        SpellDefinition bolt = projectile("bolt", 0, 1.0);
        WandState initial = state(100.0, List.of(card(0, "refresh"), card(1, "bolt")));

        ResolvedCast first = evaluate(wand(1, false, 0, 20, 100, 0.0, List.of()), initial, 32L, refresh, bolt);
        assertEquals(NoitaDuration.ZERO, first.nextState().rechargeRemaining());
        assertEquals(List.of(CardRef.forSlot(0), CardRef.forSlot(1)), first.nextState().deckState().deck());

        ResolvedCast second = evaluate(wand(2, false, 0, 20, 100, 0.0, List.of()), initial, 33L, refresh, bolt);
        assertEquals(NoitaDuration.frames(20), second.nextState().rechargeRemaining());
        assertEquals(List.of(CardRef.forSlot(0), CardRef.forSlot(1)), second.nextState().deckState().deck());
    }

    @Test
    void cardConservationManaConservationDeterminismAndNoDuplicatesHoldAcrossSeeds() {
        SpellDefinition modifier = definition("modifier", SpellCategory.PROJECTILE_MODIFIER, 4,
            new ModifyShotAction(ShotModifier.EMPTY), new DrawAction(1));
        SpellDefinition bolt = projectile("bolt", 3, 1.0);
        WandDefinition wand = wand(2, true, 5, 15, 100, 0.0, List.of());
        WandState initial = state(100.0, List.of(card(0, "modifier", 5), card(1, "bolt", 5), card(2, "bolt", 5)));

        for (long seed = 0; seed < 48; seed++) {
            ResolvedCast first = evaluate(wand, initial, seed, modifier, bolt);
            ResolvedCast second = evaluate(wand, initial, seed, modifier, bolt);
            Set<CardRef> pileCards = allCards(first.nextState());

            assertEquals(first, second);
            assertEquals(initial.deckState().cards().keySet(), pileCards);
            assertEquals(first.nextState().deckState().cards().size(), pileCards.size());
            assertTrue(first.nextState().mana() <= initial.mana(), "no negative-mana action may create mana");
        }
    }

    private static ResolvedCast evaluate(WandDefinition wand, WandState state, long seed, SpellDefinition... definitions) {
        return EVALUATOR.evaluate(wand, state, catalog(definitions), NoitaDuration.ZERO, seed);
    }

    private static SpellCatalog catalog(SpellDefinition... definitions) {
        Map<String, SpellDefinition> values = new LinkedHashMap<>();
        for (SpellDefinition definition : definitions) {
            values.put(definition.id(), definition);
        }
        return new SpellCatalog(2L, "g02-fixture", values);
    }

    private static WandDefinition wand(
        int spellsPerCast,
        boolean shuffle,
        double castDelayFrames,
        double rechargeFrames,
        int manaMax,
        double manaChargePerSecond,
        List<String> alwaysCast
    ) {
        return new WandDefinition(shuffle, spellsPerCast, NoitaDuration.frames(castDelayFrames), NoitaDuration.frames(rechargeFrames),
            manaMax, manaChargePerSecond, 16, 0.0, 1.0, alwaysCast);
    }

    private static WandState state(double mana, List<SpellCardState> cards) {
        return state(mana, cards, cards.stream().map(SpellCardState::slot).toList(), List.of());
    }

    private static WandState state(double mana, List<SpellCardState> cards, List<Integer> deckSlots, List<Integer> discardSlots) {
        Map<CardRef, SpellCardState> values = new LinkedHashMap<>();
        for (SpellCardState card : cards) {
            values.put(card.ref(), card);
        }
        return new WandState(new DeckState(values, refs(deckSlots), List.of(), refs(discardSlots)), mana,
            NoitaDuration.ZERO, NoitaDuration.ZERO, false, 0L, 1);
    }

    private static List<CardRef> refs(List<Integer> slots) {
        return slots.stream().map(CardRef::forSlot).toList();
    }

    private static SpellCardState card(int slot, String id) {
        return card(slot, id, SpellCardState.UNLIMITED_USES);
    }

    private static SpellCardState card(int slot, String id, int uses) {
        return new SpellCardState(CardRef.forSlot(slot), slot, id, uses);
    }

    private static SpellDefinition projectile(String id, int manaCost, double damage) {
        return definition(id, SpellCategory.PROJECTILE, manaCost, new AddProjectileAction(projectileDefinition(id, damage)));
    }

    private static ProjectileDefinition projectileDefinition(String id, double damage) {
        return new ProjectileDefinition(id, "BOLT", damage, 0.0, NoitaDuration.frames(60), 0.0, 0.0,
            0, 0.0, 400.0, 0.0, 0.0, 0.99, 0.65, 1.0, 0.0, false, false, 1, 0.0,
            NoitaDuration.ZERO, 0, List.of());
    }

    private static SpellDefinition definition(String id, SpellCategory category, int manaCost, SpellAction... actions) {
        return new SpellDefinition(id, category, manaCost, false, List.of(actions));
    }

    private static Set<CardRef> allCards(WandState state) {
        Set<CardRef> refs = new HashSet<>(state.deckState().deck());
        refs.addAll(state.deckState().hand());
        refs.addAll(state.deckState().discard());
        return refs;
    }
}
