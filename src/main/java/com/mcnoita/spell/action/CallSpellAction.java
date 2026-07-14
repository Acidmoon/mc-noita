package com.mcnoita.spell.action;

import java.util.Objects;

/** Calls a target action directly, without normal draw mana/use/hand effects. */
public record CallSpellAction(CallSelection selection) implements SpellAction {
    public CallSpellAction {
        Objects.requireNonNull(selection, "selection");
    }
}
