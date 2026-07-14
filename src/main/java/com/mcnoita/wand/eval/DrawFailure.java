package com.mcnoita.wand.eval;

import com.mcnoita.wand.model.CardRef;
import java.util.Objects;

/** A failed candidate is still a deterministic pile transition, not a retry of the same card. */
public record DrawFailure(CardRef card, DrawFailureReason reason) {
    public DrawFailure {
        Objects.requireNonNull(card, "card");
        Objects.requireNonNull(reason, "reason");
    }
}
