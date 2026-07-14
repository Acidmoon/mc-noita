package com.mcnoita.wand.eval;

import com.mcnoita.wand.model.CardRef;
import java.util.List;
import java.util.Objects;

/** Immutable audit result for one explicit {@link DrawRequest}. */
public record DrawOutcome(
    DrawOrigin origin,
    int requestedDraws,
    int completedDraws,
    List<CardRef> drawnCards,
    List<DrawFailure> failures,
    boolean deckExhausted,
    boolean wrapped,
    boolean startReload,
    boolean reloading
) {
    public DrawOutcome {
        Objects.requireNonNull(origin, "origin");
        if (requestedDraws < 1 || completedDraws < 0 || completedDraws > requestedDraws) {
            throw new IllegalArgumentException("draw outcome counts are invalid");
        }
        drawnCards = List.copyOf(Objects.requireNonNull(drawnCards, "drawnCards"));
        failures = List.copyOf(Objects.requireNonNull(failures, "failures"));
    }
}
