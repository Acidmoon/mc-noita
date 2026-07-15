package com.mcnoita.spell.server.job;

import com.mcnoita.spell.server.budget.ChunkBudgetKey;
import java.util.Objects;

/** A handler outcome after one attempted, centrally reserved step. */
public record SpellJobStepResult(Outcome outcome, ChunkBudgetKey nextChunk, String reason) {
    public enum Outcome {
        CONTINUE,
        PAUSE,
        COMPLETE,
        CANCEL,
        INERT
    }

    public SpellJobStepResult {
        outcome = Objects.requireNonNull(outcome, "outcome");
        reason = FrozenSpellJobNode.requireBoundedText(reason == null ? "" : reason, "step reason");
    }

    public static SpellJobStepResult continueAt(ChunkBudgetKey nextChunk) {
        return new SpellJobStepResult(Outcome.CONTINUE, nextChunk, "");
    }

    public static SpellJobStepResult pause(String reason) {
        return new SpellJobStepResult(Outcome.PAUSE, null, reason);
    }

    public static SpellJobStepResult complete(String reason) {
        return new SpellJobStepResult(Outcome.COMPLETE, null, reason);
    }

    public static SpellJobStepResult cancel(String reason) {
        return new SpellJobStepResult(Outcome.CANCEL, null, reason);
    }

    public static SpellJobStepResult inert(String reason) {
        return new SpellJobStepResult(Outcome.INERT, null, reason);
    }
}
