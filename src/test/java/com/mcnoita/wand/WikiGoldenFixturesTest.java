package com.mcnoita.wand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.mcnoita.spell.action.AddProjectileAction;
import com.mcnoita.spell.action.CallSelection;
import com.mcnoita.spell.action.CallSpellAction;
import com.mcnoita.spell.action.DrawAction;
import com.mcnoita.spell.action.ModifyShotAction;
import com.mcnoita.spell.action.SpellCatalog;
import com.mcnoita.spell.action.SpellCategory;
import com.mcnoita.spell.action.SpellDefinition;
import com.mcnoita.spell.plan.ProjectileDefinition;
import com.mcnoita.spell.plan.ResolvedCast;
import com.mcnoita.spell.plan.ShotModifier;
import com.mcnoita.wand.eval.WandCastEvaluator;
import com.mcnoita.wand.model.CardRef;
import com.mcnoita.wand.model.DeckState;
import com.mcnoita.wand.model.NoitaDuration;
import com.mcnoita.wand.model.SpellCardState;
import com.mcnoita.wand.model.WandDefinition;
import com.mcnoita.wand.model.WandState;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("wiki-golden")
class WikiGoldenFixturesTest {
    @Test
    void damagePlusAppliesToBothProjectilesInTheSameMulticastShotState() {
        SpellDefinition doubleSpell = definition("double", SpellCategory.MULTICAST, 0, new DrawAction(2));
        SpellDefinition damagePlus = definition("damage", SpellCategory.PROJECTILE_MODIFIER, 0,
            new ModifyShotAction(new ShotModifier(4.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0, false, false, false, 0, List.of())), new DrawAction(1));
        SpellDefinition bolt = definition("bolt", SpellCategory.PROJECTILE, 0, new AddProjectileAction(projectile("bolt", 3.0)));

        ResolvedCast result = evaluate(List.of(card(0, "double"), card(1, "damage"), card(2, "bolt"), card(3, "bolt")),
            doubleSpell, damagePlus, bolt);

        assertEquals(2, result.effectPlan().projectiles().size());
        assertEquals(7.0, result.effectPlan().projectiles().get(0).damage());
        assertEquals(7.0, result.effectPlan().projectiles().get(1).damage());
    }

    @Test
    void initialSpellsPerCastDoesNotInitiateWrap() {
        SpellDefinition bolt = definition("bolt", SpellCategory.PROJECTILE, 0, new AddProjectileAction(projectile("bolt", 1.0)));
        ResolvedCast result = evaluate(2, List.of(card(0, "bolt"), card(1, "bolt"), card(2, "bolt")), bolt);

        assertEquals(2, result.effectPlan().projectiles().size());
        assertEquals(1, result.nextState().deckState().deck().size());
        assertFalse(result.nextState().rechargePending(), "Wiki: https://noita.wiki.gg/wiki/Expert_Guide:_Draw, checked 2026-07-13");
    }

    @Test
    void gammaCallDoesNotSpendTheTargetsManaOrUses() {
        SpellDefinition gamma = definition("gamma", SpellCategory.OTHER, 40, new CallSpellAction(CallSelection.LAST_AVAILABLE));
        SpellDefinition target = definition("target", SpellCategory.PROJECTILE, 80, new AddProjectileAction(projectile("target", 2.0)));
        ResolvedCast result = evaluate(List.of(card(0, "gamma"), card(1, "target", 3)), gamma, target);

        assertEquals(1, result.effectPlan().projectiles().size());
        assertEquals(10.0, result.nextState().mana());
        assertEquals(3, result.remainingUses().get(CardRef.forSlot(1)));
    }

    private static ResolvedCast evaluate(List<SpellCardState> cards, SpellDefinition... definitions) {
        return evaluate(1, cards, definitions);
    }

    private static ResolvedCast evaluate(int spellsPerCast, List<SpellCardState> cards, SpellDefinition... definitions) {
        Map<CardRef, SpellCardState> cardMap = new LinkedHashMap<>();
        for (SpellCardState card : cards) {
            cardMap.put(card.ref(), card);
        }
        WandState state = new WandState(new DeckState(cardMap, cards.stream().map(SpellCardState::ref).toList(), List.of(), List.of()),
            50.0, NoitaDuration.ZERO, NoitaDuration.ZERO, false, 0L, 11);
        Map<String, SpellDefinition> definitionMap = new LinkedHashMap<>();
        for (SpellDefinition definition : definitions) {
            definitionMap.put(definition.id(), definition);
        }
        WandDefinition wand = new WandDefinition(false, spellsPerCast, NoitaDuration.ZERO, NoitaDuration.ZERO,
            100, 0.0, 16, 0.0, 1.0, List.of());
        return new WandCastEvaluator().evaluate(wand, state, new SpellCatalog(1L, "wiki-fixture", definitionMap), NoitaDuration.ZERO, 9L);
    }

    private static SpellCardState card(int slot, String spellId) {
        return card(slot, spellId, SpellCardState.UNLIMITED_USES);
    }

    private static SpellCardState card(int slot, String spellId, int uses) {
        CardRef ref = CardRef.forSlot(slot);
        return new SpellCardState(ref, slot, spellId, uses);
    }

    private static SpellDefinition definition(String id, SpellCategory category, int manaCost, com.mcnoita.spell.action.SpellAction... actions) {
        return new SpellDefinition(id, category, manaCost, false, List.of(actions));
    }

    private static ProjectileDefinition projectile(String itemPath, double damage) {
        return new ProjectileDefinition(itemPath, "BOLT", damage, 0.0, NoitaDuration.frames(60), 0.0, 0.0,
            0, 0.0, 400.0, 0.0, 0.0, 0.99, 0.65, 1.0, 0.0, false, false, 1, 0.0,
            NoitaDuration.ZERO, 0, List.of());
    }
}
