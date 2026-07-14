package com.mcnoita.wand.server;

import com.mcnoita.spell.server.budget.BudgetReservation;
import java.util.Objects;

/** Durable wand replacement plus the reservation that authorized its execution. */
public record CommittedCast(PreparedCast prepared, BudgetReservation reservation) {
    public CommittedCast {
        prepared = Objects.requireNonNull(prepared, "prepared");
        reservation = Objects.requireNonNull(reservation, "reservation");
        if (!prepared.executionId().equals(reservation.executionId())
            || reservation.state() != BudgetReservation.State.COMMITTED) {
            throw new IllegalArgumentException("committed cast must own a committed matching reservation");
        }
    }
}
