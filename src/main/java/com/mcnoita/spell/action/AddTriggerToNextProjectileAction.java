package com.mcnoita.spell.action;

import com.mcnoita.spell.plan.TriggerMode;
import com.mcnoita.wand.model.NoitaDuration;
import java.util.Objects;

/** Searches and wraps the next related projectile using the frozen G03 plan. */
public record AddTriggerToNextProjectileAction(TriggerMode mode, NoitaDuration timerDelay) implements SpellAction {
    public AddTriggerToNextProjectileAction {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(timerDelay, "timerDelay");
        if (mode == TriggerMode.NONE) {
            throw new IllegalArgumentException("Add Trigger mode must release a payload");
        }
        if (mode != TriggerMode.TIMER && !timerDelay.isZero()) {
            throw new IllegalArgumentException("only Add Timer may define a timer delay");
        }
    }

    public AddTriggerToNextProjectileAction(TriggerMode mode) {
        this(mode, mode == TriggerMode.TIMER ? NoitaDuration.frames(20) : NoitaDuration.ZERO);
    }
}
