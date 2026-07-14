package com.mcnoita.wand.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable Deck, Hand and Discard state. A configured card is represented in
 * exactly one pile; callers cannot mutate the collections after construction.
 */
public record DeckState(
    Map<CardRef, SpellCardState> cards,
    List<CardRef> deck,
    List<CardRef> hand,
    List<CardRef> discard
) {
    public DeckState {
        Objects.requireNonNull(cards, "cards");
        Objects.requireNonNull(deck, "deck");
        Objects.requireNonNull(hand, "hand");
        Objects.requireNonNull(discard, "discard");

        Map<CardRef, SpellCardState> copiedCards = new LinkedHashMap<>();
        for (Map.Entry<CardRef, SpellCardState> entry : cards.entrySet()) {
            CardRef ref = Objects.requireNonNull(entry.getKey(), "card reference");
            SpellCardState card = Objects.requireNonNull(entry.getValue(), "card state");
            if (!ref.equals(card.ref())) {
                throw new IllegalArgumentException("card map key must match card state reference");
            }
            copiedCards.put(ref, card);
        }
        cards = Collections.unmodifiableMap(copiedCards);
        deck = List.copyOf(deck);
        hand = List.copyOf(hand);
        discard = List.copyOf(discard);

        Set<CardRef> seen = new LinkedHashSet<>();
        validatePile("deck", deck, copiedCards, seen);
        validatePile("hand", hand, copiedCards, seen);
        validatePile("discard", discard, copiedCards, seen);
        if (!seen.equals(copiedCards.keySet())) {
            throw new IllegalArgumentException("every configured card must belong to exactly one pile");
        }
    }

    private static void validatePile(String name, List<CardRef> pile, Map<CardRef, SpellCardState> cards, Set<CardRef> seen) {
        for (CardRef ref : pile) {
            if (!cards.containsKey(ref)) {
                throw new IllegalArgumentException(name + " contains an unknown card reference");
            }
            if (!seen.add(ref)) {
                throw new IllegalArgumentException("card appears in more than one pile: " + ref.value());
            }
        }
    }

    public SpellCardState card(CardRef ref) {
        SpellCardState card = cards.get(ref);
        if (card == null) {
            throw new IllegalArgumentException("unknown card reference: " + ref.value());
        }
        return card;
    }

    public DeckState withPiles(List<CardRef> nextDeck, List<CardRef> nextHand, List<CardRef> nextDiscard) {
        return new DeckState(cards, nextDeck, nextHand, nextDiscard);
    }

    public DeckState withCards(Map<CardRef, SpellCardState> nextCards) {
        return new DeckState(nextCards, deck, hand, discard);
    }

    public DeckState resetToDeck(List<CardRef> orderedDeck) {
        return new DeckState(cards, orderedDeck, List.of(), List.of());
    }

    public List<CardRef> allCardsBySlot() {
        List<CardRef> refs = new ArrayList<>(cards.keySet());
        refs.sort((left, right) -> Integer.compare(card(left).slot(), card(right).slot()));
        return List.copyOf(refs);
    }
}
