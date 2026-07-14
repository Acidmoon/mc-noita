package com.mcnoita.spell.action;

import com.mcnoita.spell.plan.TriggerMode;
import java.util.Objects;

/** Creates an isolated Trigger ShotState and resolves its payload immediately. */
public record BeginTriggerAction(TriggerMode mode, int drawCount) implements SpellAction {
    public BeginTriggerAction {
        mode = TriggerMode.normalize(Objects.requireNonNull(mode, "mode"));
        if (mode == TriggerMode.NONE || drawCount < 1) {
            throw new IllegalArgumentException("trigger mode and draw count must be valid");
        }
    }
}
