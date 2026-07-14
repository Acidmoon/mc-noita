package com.mcnoita.spell.action;

import java.util.List;
import java.util.Objects;

/** Immutable catalog definition consumed by the pure evaluator. */
public record SpellDefinition(
    String id,
    SpellCategory category,
    int manaCost,
    boolean recursive,
    List<SpellAction> actions
) {
    public SpellDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("spell id must not be blank");
        }
        Objects.requireNonNull(category, "category");
        if (manaCost < 0) {
            throw new IllegalArgumentException("manaCost must not be negative");
        }
        actions = List.copyOf(Objects.requireNonNull(actions, "actions"));
    }
}
