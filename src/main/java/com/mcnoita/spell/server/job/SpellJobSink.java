package com.mcnoita.spell.server.job;

import com.mcnoita.spell.server.budget.BudgetReservation;
import java.util.Objects;

/** Boundary used by typed effect execution to offer a frozen job to server storage. */
@FunctionalInterface
public interface SpellJobSink {
    Submission submit(SpellJobPersistentState job);

    /**
     * A committed root cast may transfer its admitted PERSISTENT_JOBS slice to
     * the durable sink. Implementations without a central ledger fall back to
     * ordinary admission instead of silently assuming a transfer happened.
     */
    default Submission submit(SpellJobPersistentState job, BudgetReservation rootReservation) {
        return submit(job);
    }

    static SpellJobSink rejecting() {
        return job -> {
            SpellJobPersistentState inert = job.isTerminal() ? job : job.transition(SpellJobState.INERT,
                "no persistent-job service is configured");
            return Submission.rejected(inert, "no persistent-job service is configured");
        };
    }

    record Submission(boolean accepted, SpellJobPersistentState state, String reason) {
        public Submission {
            state = Objects.requireNonNull(state, "state");
            reason = FrozenSpellJobNode.requireBoundedText(reason == null ? "" : reason, "submission reason");
        }

        public static Submission accepted(SpellJobPersistentState state) {
            return new Submission(true, state, "");
        }

        public static Submission rejected(SpellJobPersistentState state, String reason) {
            return new Submission(false, state, reason);
        }
    }
}
