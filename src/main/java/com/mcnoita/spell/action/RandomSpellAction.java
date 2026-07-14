package com.mcnoita.spell.action;

import java.util.Objects;

/** Resolves one catalog candidate through a deterministic RNG substream. */
public record RandomSpellAction(SpellCategory category) implements SpellAction {
    public RandomSpellAction {
        Objects.requireNonNull(category, "category");
    }
}
