package com.mcnoita.spell.action;

import java.util.Objects;

/** Calls a queried target directly, without normal draw mana/use/hand effects. */
public record CallSpellAction(TargetQuery query) implements SpellAction {
    public CallSpellAction {
        Objects.requireNonNull(query, "query");
    }

    /** Compatibility bridge for G02/G03 fixtures; new code declares a TargetQuery. */
    public CallSpellAction(CallSelection selection) {
        this(selection == CallSelection.FIRST_AVAILABLE ? TargetQuery.alpha() : TargetQuery.gamma());
    }
}
