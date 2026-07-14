package com.mcnoita.spell.exec;

import com.mcnoita.spell.plan.EffectNode;
import com.mcnoita.spell.server.budget.BudgetReservation;
import java.util.Objects;

/** Transaction-supplied ownership boundary for one failed or deferred effect node. */
@FunctionalInterface
public interface EffectNodeBudgetReleaser {
    void releaseUnused(EffectNode node, String releaseKey);

    static EffectNodeBudgetReleaser forReservation(BudgetReservation reservation) {
        return reservation == null ? (node, releaseKey) -> { } : new ReservationEffectNodeBudgetReleaser(reservation);
    }

    static EffectNodeBudgetReleaser require(EffectNodeBudgetReleaser releaser) {
        return Objects.requireNonNull(releaser, "budgetReleaser");
    }
}
