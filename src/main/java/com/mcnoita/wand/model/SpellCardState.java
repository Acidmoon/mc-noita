package com.mcnoita.wand.model;

import java.util.Objects;

/**
 * Persistent state for one configured spell card. Several cards may point to
 * the same spell definition, but their slots and remaining uses stay distinct.
 */
public record SpellCardState(CardRef ref, int slot, String spellId, int remainingUses) {
    public static final int UNLIMITED_USES = -1;

    public SpellCardState {
        Objects.requireNonNull(ref, "ref");
        if (slot < 0) {
            throw new IllegalArgumentException("slot must not be negative");
        }
        if (spellId == null || spellId.isBlank()) {
            throw new IllegalArgumentException("spellId must not be blank");
        }
        if (remainingUses < UNLIMITED_USES) {
            throw new IllegalArgumentException("remainingUses must be unlimited or non-negative");
        }
    }

    public boolean hasUsesRemaining() {
        return remainingUses != 0;
    }

    public SpellCardState withRemainingUses(int nextRemainingUses) {
        return new SpellCardState(ref, slot, spellId, nextRemainingUses);
    }
}
