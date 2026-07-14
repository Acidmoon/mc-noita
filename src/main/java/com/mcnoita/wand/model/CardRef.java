package com.mcnoita.wand.model;

import java.util.Objects;

/** Stable identity for one configured card, independent of its spell definition. */
public record CardRef(String value) {
    public CardRef {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("card reference must not be blank");
        }
    }

    public static CardRef forSlot(int slot) {
        if (slot < 0) {
            throw new IllegalArgumentException("slot must not be negative");
        }
        return new CardRef("slot:" + slot);
    }
}
