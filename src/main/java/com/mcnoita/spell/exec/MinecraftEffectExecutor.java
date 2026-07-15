package com.mcnoita.spell.exec;

import com.mcnoita.spell.plan.EffectNode;
import com.mcnoita.spell.plan.ResolvedCast;
import com.mcnoita.spell.server.budget.BudgetReservation;
import com.mcnoita.spell.server.budget.ChunkBudgetKey;
import com.mcnoita.spell.server.job.SpellJobServerService;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.server.network.ServerPlayerEntity;

/** Executes committed typed EffectPlan nodes without reinterpreting wand mechanics. */
public final class MinecraftEffectExecutor {
    private static final EffectExecutorRegistry EXECUTORS = EffectExecutorRegistry.createDefault(SpellJobServerService.getInstance());

    private MinecraftEffectExecutor() {
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

    /** Preserves the transaction's pre-reserved target chunks for immediate nodes. */
    public static void execute(
        ServerPlayerEntity player, ResolvedCast resolvedCast, UUID executionId, BudgetReservation reservation,
        Map<String, ChunkBudgetKey> synchronousNodeChunks
    ) {
        execute(player, resolvedCast, executionId, reservation, EffectNodeBudgetReleaser.forReservation(reservation),
            synchronousNodeChunks);
    }

    /** Preserves explicit cross-chunk entity-query coverage frozen before the transaction commits. */
    public static void execute(
        ServerPlayerEntity player, ResolvedCast resolvedCast, UUID executionId, BudgetReservation reservation,
        Map<String, ChunkBudgetKey> synchronousNodeChunks,
        Map<String, java.util.Set<ChunkBudgetKey>> synchronousEntityQueryChunks
    ) {
        execute(player, resolvedCast, executionId, reservation, EffectNodeBudgetReleaser.forReservation(reservation),
            synchronousNodeChunks, synchronousEntityQueryChunks);
    }

    /** Transaction extension point for exact per-node unused-budget slices. */
    public static void execute(
        ServerPlayerEntity player, ResolvedCast resolvedCast, UUID executionId, BudgetReservation reservation,
        EffectNodeBudgetReleaser budgetReleaser
    ) {
        execute(player, resolvedCast, executionId, reservation, budgetReleaser, Map.of());
    }

    /** Uses the commit-time node chunk mapping for synchronous policy operations. */
    public static void execute(
        ServerPlayerEntity player, ResolvedCast resolvedCast, UUID executionId, BudgetReservation reservation,
        EffectNodeBudgetReleaser budgetReleaser, Map<String, ChunkBudgetKey> synchronousNodeChunks
    ) {
        execute(player, resolvedCast, executionId, reservation, budgetReleaser, synchronousNodeChunks, Map.of());
    }

    /** Executes against the complete target-chunk binding captured by the transaction. */
    public static void execute(
        ServerPlayerEntity player, ResolvedCast resolvedCast, UUID executionId, BudgetReservation reservation,
        EffectNodeBudgetReleaser budgetReleaser, Map<String, ChunkBudgetKey> synchronousNodeChunks,
        Map<String, java.util.Set<ChunkBudgetKey>> synchronousEntityQueryChunks
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(resolvedCast, "resolvedCast");
        Objects.requireNonNull(executionId, "executionId");
        Objects.requireNonNull(reservation, "reservation");
        EffectNodeBudgetReleaser.require(budgetReleaser);
        if (!executionId.equals(reservation.executionId())) {
            throw new IllegalArgumentException("execution UUID must match the supplied budget reservation");
        }

        EffectExecutionContext context = new EffectExecutionContext(player, resolvedCast, executionId, reservation, budgetReleaser,
            synchronousNodeChunks, synchronousEntityQueryChunks);
        for (EffectNode node : resolvedCast.effectPlan().nodes()) {
            EXECUTORS.execute(node, context);
        }
    }
}
