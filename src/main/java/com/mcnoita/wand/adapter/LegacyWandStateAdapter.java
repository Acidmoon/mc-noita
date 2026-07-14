package com.mcnoita.wand.adapter;

import com.mcnoita.wand.model.CardRef;
import com.mcnoita.wand.model.DeckState;
import com.mcnoita.wand.model.NoitaDuration;
import com.mcnoita.wand.model.SpellCardState;
import com.mcnoita.wand.model.WandState;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure conversion for the pre-G01 slot-index Deck/Discard persistence shape.
 * MinecraftWandAdapter owns NBT I/O and delegates the value conversion here so
 * it can be regression-tested without bootstrapping a world or registry.
 */
public final class LegacyWandStateAdapter {
    private LegacyWandStateAdapter() {
    }

    public static WandState toWandState(
        Map<CardRef, SpellCardState> cards,
        List<Integer> deckSlots,
        List<Integer> discardSlots,
        double mana,
        NoitaDuration castDelayRemaining,
        NoitaDuration rechargeRemaining,
        boolean rechargePending,
        long revision,
        int stateHash
    ) {
        Map<Integer, CardRef> refsBySlot = new LinkedHashMap<>();
        for (SpellCardState card : cards.values()) {
            refsBySlot.put(card.slot(), card.ref());
        }
        List<CardRef> deck = refs(deckSlots, refsBySlot);
        List<CardRef> discard = refs(discardSlots, refsBySlot);
        for (CardRef ref : cards.keySet()) {
            if (!deck.contains(ref) && !discard.contains(ref)) {
                discard.add(ref);
            }
        }
        return new WandState(new DeckState(cards, deck, List.of(), discard), mana, castDelayRemaining,
            rechargeRemaining, rechargePending, revision, stateHash);
    }

    public static LegacyState fromWandState(WandState state) {
        DeckState deckState = state.deckState();
        Map<Integer, Integer> remainingUses = new LinkedHashMap<>();
        for (SpellCardState card : deckState.cards().values()) {
            remainingUses.put(card.slot(), card.remainingUses());
        }
        return new LegacyState(
            deckState.deck().stream().map(ref -> deckState.card(ref).slot()).toList(),
            deckState.discard().stream().map(ref -> deckState.card(ref).slot()).toList(),
            state.mana(),
            remainingUses
        );
    }

    private static List<CardRef> refs(List<Integer> slots, Map<Integer, CardRef> refsBySlot) {
        List<CardRef> refs = new ArrayList<>();
        for (int slot : slots) {
            CardRef ref = refsBySlot.get(slot);
            if (ref == null || refs.contains(ref)) {
                throw new IllegalArgumentException("legacy pile contains an unknown or duplicate slot: " + slot);
            }
            refs.add(ref);
        }
        return refs;
    }

    public record LegacyState(List<Integer> deckSlots, List<Integer> discardSlots, double mana, Map<Integer, Integer> remainingUses) {
        public LegacyState {
            deckSlots = List.copyOf(deckSlots);
            discardSlots = List.copyOf(discardSlots);
            remainingUses = Map.copyOf(remainingUses);
        }
    }
}
