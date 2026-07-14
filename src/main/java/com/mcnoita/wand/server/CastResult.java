package com.mcnoita.wand.server;

import com.mcnoita.spell.server.budget.BudgetDiagnostic;
import java.util.Objects;
import java.util.UUID;

/** Structured result for validation, planning, reservation, and commit outcomes. */
public record CastResult(
    Status status,
    String reason,
    CastBinding binding,
    UUID executionId,
    BudgetDiagnostic budgetDiagnostic,
    CommittedCast committedCast
) {
    public enum Status {
        ACCEPTED,
        VALIDATION_REJECTED,
        EVALUATION_REJECTED,
        BUDGET_REJECTED,
        STALE_BINDING,
        COMMIT_REJECTED
    }

    public CastResult {
        status = Objects.requireNonNull(status, "status");
        reason = Objects.requireNonNull(reason, "reason");
        if (status == Status.ACCEPTED && committedCast == null) {
            throw new IllegalArgumentException("accepted result requires a committed cast");
        }
        if (status != Status.ACCEPTED && committedCast != null) {
            throw new IllegalArgumentException("rejected result must not expose a committed cast");
        }
    }

    public static CastResult accepted(CommittedCast committedCast) {
        return new CastResult(Status.ACCEPTED, "accepted", committedCast.prepared().binding(),
            committedCast.prepared().executionId(), null, committedCast);
    }

    public static CastResult rejected(
        Status status, String reason, CastBinding binding, UUID executionId, BudgetDiagnostic budgetDiagnostic
    ) {
        if (status == Status.ACCEPTED) {
            throw new IllegalArgumentException("use accepted() for successful casts");
        }
        return new CastResult(status, reason, binding, executionId, budgetDiagnostic, null);
    }

    public boolean accepted() {
        return status == Status.ACCEPTED;
    }
}
