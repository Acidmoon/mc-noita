package com.mcnoita.wand.eval;

import com.mcnoita.spell.action.SpellCatalog;
import com.mcnoita.spell.action.SpellCategory;
import com.mcnoita.spell.action.SpellDefinition;
import com.mcnoita.spell.plan.ProjectileDefinition;
import com.mcnoita.wand.model.CardRef;
import com.mcnoita.wand.model.DeckState;
import com.mcnoita.wand.model.NoitaDuration;
import com.mcnoita.wand.model.SpellCardState;
import com.mcnoita.wand.model.WandDefinition;
import com.mcnoita.wand.model.WandState;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class PureWandFixtures {
    private PureWandFixtures() {
    }

    static WandDefinition wand(int spellsPerCast, boolean shuffle) {
        return new WandDefinition(shuffle, spellsPerCast, NoitaDuration.ZERO, NoitaDuration.ZERO, 1000, 0.0,
            32, 0.0, 1.0, List.of());
    }

    static WandState state(double mana, List<SpellCardState> cards) {
        Map<CardRef, SpellCardState> map = new LinkedHashMap<>();
        List<CardRef> deck = cards.stream().map(SpellCardState::ref).toList();
        for (SpellCardState card : cards) {
            map.put(card.ref(), card);
        }
        return new WandState(new DeckState(map, deck, List.of(), List.of()), mana, NoitaDuration.ZERO, NoitaDuration.ZERO, false, 0L, 17);
    }

    static SpellCardState card(int slot, String spellId) {
        return card(slot, spellId, SpellCardState.UNLIMITED_USES);
    }

    static SpellCardState card(int slot, String spellId, int uses) {
        return new SpellCardState(CardRef.forSlot(slot), slot, spellId, uses);
    }

    static SpellCatalog catalog(SpellDefinition... definitions) {
        Map<String, SpellDefinition> map = new LinkedHashMap<>();
        for (SpellDefinition definition : definitions) {
            map.put(definition.id(), definition);
        }
        return new SpellCatalog(4L, "fixture-hash", map);
    }

    static SpellDefinition definition(String id, int mana, com.mcnoita.spell.action.SpellAction... actions) {
        return new SpellDefinition(id, SpellCategory.PROJECTILE, mana, false, List.of(actions));
    }

    static ProjectileDefinition projectile(String itemPath, double damage) {
        return projectile(itemPath, damage, 0.0);
    }

    static ProjectileDefinition projectile(String itemPath, double damage, double rechargeFrames) {
        return new ProjectileDefinition(itemPath, "BOLT", damage, 0.0, NoitaDuration.frames(60), 0.0, rechargeFrames,
            0, 0.0, 400.0, 0.0, 0.0, 0.99, 0.65, 1.0, 0.0, false, false, 1, 0.0, NoitaDuration.ZERO, 0, List.of());
    }
}
