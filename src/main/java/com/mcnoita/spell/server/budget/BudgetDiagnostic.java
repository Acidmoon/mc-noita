package com.mcnoita.spell.server.budget;

import java.util.Objects;
import java.util.UUID;

/** Structured rejection detail for an all-or-nothing reservation attempt. */
public record BudgetDiagnostic(
    Code code,
    Scope scope,
    BudgetKind kind,
    UUID executionId,
    UUID ownerId,
    String dimensionId,
    ChunkBudgetKey chunk,
    long requested,
    long used,
    long limit,
    String detail
) {
    public BudgetDiagnostic {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(executionId, "executionId");
        dimensionId = Objects.requireNonNull(dimensionId, "dimensionId");
        detail = Objects.requireNonNull(detail, "detail");
        if (requested < 0L || used < 0L || limit < 0L) {
            throw new IllegalArgumentException("budget diagnostic values must not be negative");
        }
    }

    public enum Code {
        LIMIT_EXCEEDED,
        DUPLICATE_EXECUTION
    }

    public enum Scope {
        EXECUTION,
        PER_CAST,
        OWNER_IN_FLIGHT,
        OWNER_TICK,
        OWNER_WINDOW,
        CHUNK_IN_FLIGHT,
        CHUNK_TICK,
        CHUNK_WINDOW,
        DIMENSION_IN_FLIGHT,
        DIMENSION_TICK,
        DIMENSION_WINDOW,
        GLOBAL_IN_FLIGHT,
        GLOBAL_TICK,
        GLOBAL_WINDOW
    }
}
