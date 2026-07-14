package com.mcnoita.wand.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.mcnoita.wand.model.CardRef;
import com.mcnoita.wand.model.NoitaDuration;
import com.mcnoita.wand.model.SpellCardState;
import com.mcnoita.wand.model.WandState;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("regression")
class LegacyWandStateAdapterTest {
    @Test
    void legacyAdapterRoundTripPreservesSlotsPilesManaAndRemainingUses() {
        CardRef first = CardRef.forSlot(0);
        CardRef second = CardRef.forSlot(3);
        Map<CardRef, SpellCardState> cards = new LinkedHashMap<>();
        cards.put(first, new SpellCardState(first, 0, "mc-noita:spark_bolt", SpellCardState.UNLIMITED_USES));
        cards.put(second, new SpellCardState(second, 3, "mc-noita:bomb", 2));

        WandState state = LegacyWandStateAdapter.toWandState(cards, List.of(3), List.of(0), 37.5,
            NoitaDuration.frames(6), NoitaDuration.frames(12), true, 9L, 101);
        LegacyWandStateAdapter.LegacyState roundTripped = LegacyWandStateAdapter.fromWandState(state);

        assertEquals(List.of(3), roundTripped.deckSlots());
        assertEquals(List.of(0), roundTripped.discardSlots());
        assertEquals(37.5, roundTripped.mana());
        assertEquals(2, roundTripped.remainingUses().get(3));
        assertEquals(9L, state.revision());
    }

    @Test
    void g01ReloadPileMigrationOrdersAllCardsWithAStableSeed() {
        Map<CardRef, SpellCardState> cards = new LinkedHashMap<>();
        for (int slot = 0; slot < 8; slot++) {
            CardRef ref = CardRef.forSlot(slot);
            cards.put(ref, new SpellCardState(ref, slot, "mc-noita:spark_bolt", SpellCardState.UNLIMITED_USES));
        }
        List<CardRef> g01Discard = List.of(
            CardRef.forSlot(6), CardRef.forSlot(1), CardRef.forSlot(7), CardRef.forSlot(0),
            CardRef.forSlot(5), CardRef.forSlot(2), CardRef.forSlot(4), CardRef.forSlot(3)
        );

        List<CardRef> ordered = MinecraftWandAdapter.migrateG01ReloadDeck(g01Discard, cards, false, 99L);
        List<CardRef> shuffledFirst = MinecraftWandAdapter.migrateG01ReloadDeck(g01Discard, cards, true, 99L);
        List<CardRef> shuffledSecond = MinecraftWandAdapter.migrateG01ReloadDeck(g01Discard, cards, true, 99L);

        assertEquals(List.of(
            CardRef.forSlot(0), CardRef.forSlot(1), CardRef.forSlot(2), CardRef.forSlot(3),
            CardRef.forSlot(4), CardRef.forSlot(5), CardRef.forSlot(6), CardRef.forSlot(7)
        ), ordered);
        assertEquals(shuffledFirst, shuffledSecond);
        assertNotEquals(ordered, shuffledFirst);
        assertEquals(new HashSet<>(cards.keySet()), new HashSet<>(shuffledFirst));
    }
}
