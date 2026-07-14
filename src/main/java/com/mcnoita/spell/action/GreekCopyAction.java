package com.mcnoita.spell.action;

import java.util.Objects;

/** Executes one named Greek spell policy rather than a generic pile shortcut. */
public record GreekCopyAction(GreekCopyKind kind) implements SpellAction {
    public GreekCopyAction {
        Objects.requireNonNull(kind, "kind");
    }
}
