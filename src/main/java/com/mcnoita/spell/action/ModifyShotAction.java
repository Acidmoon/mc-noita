package com.mcnoita.spell.action;

import com.mcnoita.spell.plan.ShotModifier;
import java.util.Objects;

/** Applies a modifier to the current ShotState without restoring an old state. */
public record ModifyShotAction(ShotModifier modifier) implements SpellAction {
    public ModifyShotAction {
        Objects.requireNonNull(modifier, "modifier");
    }
}
