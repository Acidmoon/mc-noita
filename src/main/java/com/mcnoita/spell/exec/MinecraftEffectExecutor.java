package com.mcnoita.spell.exec;

import com.mcnoita.spell.plan.EffectNode;
import com.mcnoita.spell.plan.ResolvedCast;
import com.mcnoita.spell.server.budget.BudgetReservation;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.server.network.ServerPlayerEntity;

/** Executes committed typed EffectPlan nodes without reinterpreting wand mechanics. */
public final class MinecraftEffectExecutor {
    private static final EffectExecutorRegistry EXECUTORS = EffectExecutorRegistry.createDefault();

    private MinecraftEffectExecutor() {
    }

    /**
     * Legacy/test-only compatibility entry point. Production transactions must
     * create the execution UUID before reservation and call the four-argument
     * overload instead.
     */
    @Deprecated(forRemoval = false)
    public static void execute(ServerPlayerEntity player, ResolvedCast resolvedCast) {
        execute(player, resolvedCast, UUID.randomUUID(), null);
    }

    /** Convenience overload for committed plans that do not own a central reservation yet. */
    public static void execute(ServerPlayerEntity player, ResolvedCast resolvedCast, UUID executionId) {
        execute(player, resolvedCast, executionId, null);
    }

    /**
     * The authoritative execution entry point. Its identity is supplied by the
     * transaction before reservation/commit and is copied into every persisted
     * payload node produced by the projectile executor.
     */
    public static void execute(
        ServerPlayerEntity player, ResolvedCast resolvedCast, UUID executionId, BudgetReservation reservation
    ) {
        execute(player, resolvedCast, executionId, reservation, EffectNodeBudgetReleaser.forReservation(reservation));
    }

    /** Transaction extension point for exact per-node unused-budget slices. */
    public static void execute(
        ServerPlayerEntity player, ResolvedCast resolvedCast, UUID executionId, BudgetReservation reservation,
        EffectNodeBudgetReleaser budgetReleaser
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(resolvedCast, "resolvedCast");
        Objects.requireNonNull(executionId, "executionId");
        EffectNodeBudgetReleaser.require(budgetReleaser);
        if (reservation != null && !executionId.equals(reservation.executionId())) {
            throw new IllegalArgumentException("execution UUID must match the supplied budget reservation");
        }

        EffectExecutionContext context = new EffectExecutionContext(player, resolvedCast, executionId, reservation, budgetReleaser);
        for (EffectNode node : resolvedCast.effectPlan().nodes()) {
            EXECUTORS.execute(node, context);
        }
    }
}
