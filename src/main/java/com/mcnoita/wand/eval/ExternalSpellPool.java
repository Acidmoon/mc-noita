package com.mcnoita.wand.eval;

import java.util.List;

/** Server-authored, immutable Zeta candidates from other hotbar wands. */
public record ExternalSpellPool(List<String> spellIds) {
    public static final ExternalSpellPool EMPTY = new ExternalSpellPool(List.of());

    public ExternalSpellPool {
        spellIds = List.copyOf(spellIds);
        if (spellIds.stream().anyMatch(id -> id == null || id.isBlank())) {
            throw new IllegalArgumentException("external spell IDs must not be blank");
        }
    }
}
