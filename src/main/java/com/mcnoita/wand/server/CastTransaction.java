package com.mcnoita.wand.server;

import com.mcnoita.MCNoita;
import com.mcnoita.catalog.CatalogSnapshot;
import com.mcnoita.catalog.SpellCatalogService;
import com.mcnoita.item.NoitaWandItem;
import com.mcnoita.network.NoitaNetworkProtocol;
import com.mcnoita.spell.exec.EffectExecutionContext;
import com.mcnoita.spell.exec.MinecraftEffectExecutor;
import com.mcnoita.spell.plan.ResolvedCast;
import com.mcnoita.spell.server.budget.BudgetDiagnostic;
import com.mcnoita.spell.server.budget.BudgetKind;
import com.mcnoita.spell.server.budget.BudgetRequest;
import com.mcnoita.spell.server.budget.BudgetReservation;
import com.mcnoita.spell.server.budget.ChunkBudgetKey;
import com.mcnoita.spell.server.budget.SpellBudgetManager;
import com.mcnoita.spell.server.job.SpellJobServerService;
import com.mcnoita.spell.trigger.RootTriggerBudgetAllocator;
import com.mcnoita.wand.adapter.MinecraftExternalSpellPoolAdapter;
import com.mcnoita.wand.adapter.MinecraftWandAdapter;
import com.mcnoita.wand.eval.WandCastEvaluator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

/**
 * The only server-side bridge from a cast intent to a committed EffectPlan. All
 * parsing and evaluation run on an ItemStack copy, so every rejection leaves the
 * player's actual wand and every external hotbar wand byte-for-byte untouched.
 */
public final class CastTransaction {
    private static final Runnable NO_POST_RESERVATION_HOOK = () -> {
    };

    private final WandCastEvaluator evaluator;
    private final SpellCatalogService catalogService;
    private final SpellBudgetManager budgetManager;
    private final ExecutionSink executionSink;
    private final Runnable postReservationHook;

    public CastTransaction(
        WandCastEvaluator evaluator,
        SpellCatalogService catalogService,
        SpellBudgetManager budgetManager,
        ExecutionSink executionSink
    ) {
        this(evaluator, catalogService, budgetManager, executionSink, NO_POST_RESERVATION_HOOK);
    }

    private CastTransaction(
        WandCastEvaluator evaluator,
        SpellCatalogService catalogService,
        SpellBudgetManager budgetManager,
        ExecutionSink executionSink,
        Runnable postReservationHook
    ) {
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
        this.catalogService = Objects.requireNonNull(catalogService, "catalogService");
        this.budgetManager = Objects.requireNonNull(budgetManager, "budgetManager");
        this.executionSink = Objects.requireNonNull(executionSink, "executionSink");
        this.postReservationHook = Objects.requireNonNull(postReservationHook, "postReservationHook");
    }

    public static CastTransaction createProduction() {
        return new CastTransaction(new WandCastEvaluator(), SpellCatalogService.getInstance(),
            SpellJobServerService.getInstance().budgetManager(), new ExecutionSink() {
                @Override
                public void execute(
                    ServerPlayerEntity player, ResolvedCast resolvedCast, UUID executionId, BudgetReservation reservation
                ) {
                    MinecraftEffectExecutor.execute(player, resolvedCast, executionId, reservation);
                }

                @Override
                public void execute(
                    ServerPlayerEntity player, ResolvedCast resolvedCast, UUID executionId, BudgetReservation reservation,
                    Map<String, ChunkBudgetKey> synchronousNodeChunks
                ) {
                    MinecraftEffectExecutor.execute(player, resolvedCast, executionId, reservation, synchronousNodeChunks);
                }

                @Override
                public void execute(
                    ServerPlayerEntity player, ResolvedCast resolvedCast, UUID executionId, BudgetReservation reservation,
                    Map<String, ChunkBudgetKey> synchronousNodeChunks,
                    Map<String, java.util.Set<ChunkBudgetKey>> synchronousEntityQueryChunks
                ) {
                    MinecraftEffectExecutor.execute(player, resolvedCast, executionId, reservation, synchronousNodeChunks,
                        synchronousEntityQueryChunks);
                }
            });
    }

    /**
     * Creates an integration-test transaction with a deterministic pause after
     * a successful reservation and before the first binding revalidation.
     * Production construction always installs the no-op hook above.
     */
    public static CastTransaction withPostReservationHookForTesting(
        WandCastEvaluator evaluator,
        SpellCatalogService catalogService,
        SpellBudgetManager budgetManager,
        ExecutionSink executionSink,
        Runnable postReservationHook
    ) {
        return new CastTransaction(evaluator, catalogService, budgetManager, executionSink, postReservationHook);
    }

    public CastResult cast(ServerPlayerEntity player, CastIntent intent) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(intent, "intent");
        if (!isEligiblePlayer(player)) {
            return rejected(CastResult.Status.VALIDATION_REJECTED, "player is not eligible to cast", null, null, null);
        }

        // Wand cooldowns intentionally use world time, while the shared budget
        // ledger must use the monotonic server tick also used by jobs/runtime
        // world operations. A `/time set` must never move that ledger backward.
        long now = player.getServerWorld().getTime();
        long budgetTick = player.getServer().getTicks();
        ItemStack source = sourceStack(player, intent);
        if (source == null || source.isEmpty() || !(source.getItem() instanceof NoitaWandItem wandItem)) {
            return rejected(CastResult.Status.VALIDATION_REJECTED, "held slot no longer contains a castable wand", null, null, null);
        }

        // Both validation helpers migrate legacy NBT when necessary. They must
        // only ever receive this private copy before a cast has committed.
        ItemStack replacement = source.copy();
        if (!wandItem.hasSupportedNbt(replacement) || !MinecraftWandAdapter.canCast(replacement, now)) {
            return rejected(CastResult.Status.VALIDATION_REJECTED, "wand is malformed or cooling down", null, null, null);
        }

        CatalogSnapshot snapshot;
        try {
            snapshot = catalogService.current();
        } catch (IllegalStateException unavailable) {
            return rejected(CastResult.Status.VALIDATION_REJECTED, "catalog snapshot is unavailable", null, null, null);
        }
        if (!matchesClientBinding(intent, source, snapshot)) {
            return rejected(CastResult.Status.STALE_BINDING, "client observed an obsolete wand or catalog binding", null, null, null);
        }

        // Do not advance an entity RNG for a stale client packet. The seed is
        // sampled only after every pre-evaluation binding assertion succeeds.
        long randomSeed = player.getRandom().nextLong();
        MinecraftWandAdapter.LoadedWand loaded = MinecraftWandAdapter.read(replacement, wandItem, now, randomSeed);
        if (loaded == null) {
            return rejected(CastResult.Status.VALIDATION_REJECTED, "wand state could not be decoded", null, null, null);
        }

        UUID executionId = UUID.randomUUID();
        CastBinding binding = captureBinding(player, intent, source, loaded, snapshot);
        ResolvedCast resolved = evaluator.evaluate(loaded.definition(), loaded.state(), snapshot.catalog(), loaded.elapsed(),
            randomSeed, MinecraftExternalSpellPoolAdapter.fromOtherHotbarWands(player, source));
        if (resolved.status() != ResolvedCast.Status.ACCEPTED) {
            return rejected(CastResult.Status.EVALUATION_REJECTED, "pure evaluator rejected the cast", binding, executionId, null);
        }

        // The root executor must never discover a configured Trigger-tree
        // ceiling after WandState commit. Descendant world admission remains
        // runtime-charged, but their frozen local partition is preflighted
        // against the same immutable per-cast limits as the reservation.
        RootTriggerBudgetAllocator.Allocation rootBudgetAllocation = RootTriggerBudgetAllocator.allocate(
            resolved.effectPlan().nodes(), budgetManager.triggerRuntimeBudgetCeiling()
        );
        if (!rootBudgetAllocation.accepted()) {
            return rejected(CastResult.Status.BUDGET_REJECTED, "trigger runtime budget preflight was rejected", binding,
                executionId, rootBudgetDiagnostic(player, executionId, rootBudgetAllocation));
        }

        ChunkBudgetKey originChunk = new ChunkBudgetKey(snapshotDimension(player), player.getChunkPos().x, player.getChunkPos().z);
        Vec3d spawnPosition = EffectExecutionContext.spellSpawnPosition(player);
        PlanBudgetRequestFactory.SynchronousNodeBindings synchronousNodeBindings;
        BudgetRequest planBudget;
        try {
            synchronousNodeBindings = PlanBudgetRequestFactory.synchronousNodeBindings(originChunk.dimensionId(), originChunk,
                resolved, player.getPos(), spawnPosition, player.getRotationVec(1.0f));
            planBudget = PlanBudgetRequestFactory.fromResolvedCast(executionId, player.getUuid(), originChunk.dimensionId(),
                originChunk, resolved, synchronousNodeBindings);
        } catch (IllegalArgumentException invalidPlan) {
            // This boundary remains before reservation, replacement-NBT write,
            // or WandState commit. A plan with multiple retained job nodes is
            // unsupported until it has distinct durable job identities.
            return rejected(CastResult.Status.EVALUATION_REJECTED, "effect plan cannot be committed safely", binding,
                executionId, null);
        }
        PreparedCast prepared = new PreparedCast(intent, binding, snapshot, loaded, resolved, replacement, executionId,
            planBudget, budgetTick);
        SpellBudgetManager.ReservationAttempt attempt = budgetManager.reserve(prepared.budgetRequest(), budgetTick);
        if (!attempt.accepted()) {
            return rejected(CastResult.Status.BUDGET_REJECTED, "central budget reservation was rejected", binding, executionId,
                attempt.diagnostic());
        }

        BudgetReservation reservation = attempt.reservation();
        try {
            postReservationHook.run();
        } catch (RuntimeException | Error hookFailure) {
            // Test-only hooks must not leak an accepted reservation when a
            // fixture assertion fails before the normal stale-binding cleanup.
            reservation.close();
            throw hookFailure;
        }
        if (!matchesCurrentBinding(player, intent, binding, now)) {
            reservation.close();
            return rejected(CastResult.Status.STALE_BINDING, "wand or catalog changed before commit", binding, executionId, null);
        }

        try {
            MinecraftWandAdapter.write(prepared.replacementStack(), prepared.loadedWand(), prepared.resolvedCast().nextState(), now);
        } catch (RuntimeException writeFailure) {
            reservation.close();
            MCNoita.LOGGER.warn("Cast {} failed while writing replacement wand state", executionId, writeFailure);
            return rejected(CastResult.Status.COMMIT_REJECTED, "replacement wand state could not be written", binding, executionId, null);
        }

        // Re-read after staging the replacement so GUI edits, swaps, and catalog
        // reloads cannot race the actual inventory replacement.
        if (!matchesCurrentBinding(player, intent, binding, now)) {
            reservation.close();
            return rejected(CastResult.Status.STALE_BINDING, "wand or catalog changed during staged commit", binding, executionId, null);
        }
        if (!reservation.commit()) {
            reservation.close();
            return rejected(CastResult.Status.COMMIT_REJECTED, "budget reservation could not be committed", binding, executionId, null);
        }
        if (!replaceBoundStack(player, intent, prepared.replacementStack())) {
            reservation.close();
            return rejected(CastResult.Status.COMMIT_REJECTED, "held slot changed before replacement", binding, executionId, null);
        }

        CommittedCast committed = new CommittedCast(prepared, reservation);
        try {
            executionSink.execute(player, prepared.resolvedCast(), executionId, reservation, synchronousNodeBindings.primaryChunks(),
                synchronousNodeBindings.entityQueryChunks());
        } catch (RuntimeException executionFailure) {
            // Wand state stays committed after any executor failure. Node-level
            // executors already isolate failures; this covers setup failures too.
            MCNoita.LOGGER.warn("Committed cast {} failed while entering execution", executionId, executionFailure);
        } finally {
            reservation.close();
        }
        return CastResult.accepted(committed);
    }

    /** Read-only eligibility probe for HUD/network prechecks. */
    public boolean canCast(ServerPlayerEntity player, CastIntent intent) {
        if (player == null || intent == null || !isEligiblePlayer(player)) {
            return false;
        }
        ItemStack source = sourceStack(player, intent);
        if (source == null || source.isEmpty() || !(source.getItem() instanceof NoitaWandItem)) {
            return false;
        }
        return MinecraftWandAdapter.canCastReadOnly(source, player.getServerWorld().getTime());
    }

    private boolean matchesCurrentBinding(ServerPlayerEntity player, CastIntent intent, CastBinding expected, long now) {
        if (!isEligiblePlayer(player) || !player.getUuid().equals(expected.playerId())) {
            return false;
        }
        ItemStack source = sourceStack(player, intent);
        if (source == null || source.isEmpty() || !(source.getItem() instanceof NoitaWandItem wandItem)) {
            return false;
        }
        ItemStack copy = source.copy();
        if (!wandItem.hasSupportedNbt(copy) || !MinecraftWandAdapter.canCast(copy, now)) {
            return false;
        }
        MinecraftWandAdapter.LoadedWand loaded = MinecraftWandAdapter.read(copy, wandItem, now, 0L);
        if (loaded == null) {
            return false;
        }
        CatalogSnapshot current;
        try {
            current = catalogService.current();
        } catch (IllegalStateException unavailable) {
            return false;
        }
        return matchesClientBinding(intent, source, current)
            && expected.matches(captureBinding(player, intent, source, loaded, current));
    }

    private static boolean isEligiblePlayer(ServerPlayerEntity player) {
        // A cast packet received while an editor/container is open can race a
        // spell-slot update. The logical server owns both actions, so reject
        // the packet instead of accepting a cast against a half-edited wand.
        return player.isAlive()
            && !player.isSpectator()
            && !player.isRemoved()
            && player.currentScreenHandler == player.playerScreenHandler;
    }

    private static ItemStack sourceStack(ServerPlayerEntity player, CastIntent intent) {
        if (intent.hand() != Hand.MAIN_HAND || intent.slot() != player.getInventory().selectedSlot) {
            return null;
        }
        return player.getInventory().getStack(intent.slot());
    }

    private static boolean replaceBoundStack(ServerPlayerEntity player, CastIntent intent, ItemStack replacement) {
        if (intent.hand() != Hand.MAIN_HAND || intent.slot() != player.getInventory().selectedSlot) {
            return false;
        }
        player.getInventory().setStack(intent.slot(), replacement);
        return true;
    }

    private static boolean matchesClientBinding(CastIntent intent, ItemStack stack, CatalogSnapshot snapshot) {
        ClientCastBinding expected = intent.clientBinding();
        return expected == null
            || (expected.stateHash() == NoitaNetworkProtocol.wandStateHash(stack)
                && expected.wandRevision() == MinecraftWandAdapter.stateRevisionReadOnly(stack)
                && expected.catalogEpoch() == snapshot.epoch()
                && expected.catalogHash().equals(snapshot.hash()));
    }

    private static CastBinding captureBinding(
        ServerPlayerEntity player, CastIntent intent, ItemStack stack, MinecraftWandAdapter.LoadedWand loaded,
        CatalogSnapshot snapshot
    ) {
        return new CastBinding(player.getUuid(), intent.hand().name(), intent.slot(),
            Registries.ITEM.getId(stack.getItem()).toString(), stack.getCount(), loaded.state().revision(), loaded.spellsHash(),
            MinecraftWandAdapter.canonicalNbtStateHash(stack.getNbt()), snapshot.epoch(), snapshot.hash(), intent.sequence());
    }

    private static String snapshotDimension(ServerPlayerEntity player) {
        return player.getServerWorld().getRegistryKey().getValue().toString();
    }

    private static BudgetDiagnostic rootBudgetDiagnostic(
        ServerPlayerEntity player, UUID executionId, RootTriggerBudgetAllocator.Allocation allocation
    ) {
        BudgetKind kind = allocation.rejection() == RootTriggerBudgetAllocator.Rejection.TRIGGER_RELEASES
            ? BudgetKind.TRIGGER_RELEASES : BudgetKind.AUTHORITATIVE_ENTITIES;
        return new BudgetDiagnostic(BudgetDiagnostic.Code.LIMIT_EXCEEDED, BudgetDiagnostic.Scope.PER_CAST, kind,
            executionId, player.getUuid(), snapshotDimension(player), null, allocation.requested(), 0L, allocation.limit(),
            "frozen root Trigger tree exceeds the configured per-cast " + kind + " ceiling");
    }

    private static CastResult rejected(
        CastResult.Status status, String reason, CastBinding binding, UUID executionId,
        com.mcnoita.spell.server.budget.BudgetDiagnostic budgetDiagnostic
    ) {
        return CastResult.rejected(status, reason, binding, executionId, budgetDiagnostic);
    }

    @FunctionalInterface
    public interface ExecutionSink {
        void execute(ServerPlayerEntity player, ResolvedCast resolvedCast, UUID executionId, BudgetReservation reservation);

        /** Production execution receives commit-time target chunks; test sinks may ignore them. */
        default void execute(
            ServerPlayerEntity player, ResolvedCast resolvedCast, UUID executionId, BudgetReservation reservation,
            Map<String, ChunkBudgetKey> synchronousNodeChunks
        ) {
            execute(player, resolvedCast, executionId, reservation);
        }

        /** Test sinks may ignore geometry, while production must preserve the pre-commit query coverage. */
        default void execute(
            ServerPlayerEntity player, ResolvedCast resolvedCast, UUID executionId, BudgetReservation reservation,
            Map<String, ChunkBudgetKey> synchronousNodeChunks,
            Map<String, java.util.Set<ChunkBudgetKey>> synchronousEntityQueryChunks
        ) {
            execute(player, resolvedCast, executionId, reservation, synchronousNodeChunks);
        }
    }
}
