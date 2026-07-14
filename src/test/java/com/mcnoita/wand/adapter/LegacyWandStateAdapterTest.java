package com.mcnoita.wand.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mcnoita.wand.model.CardRef;
import com.mcnoita.wand.model.NoitaDuration;
import com.mcnoita.wand.model.SpellCardState;
import com.mcnoita.wand.model.WandState;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
}
