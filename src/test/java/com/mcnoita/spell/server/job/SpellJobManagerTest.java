package com.mcnoita.spell.server.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mcnoita.spell.server.budget.BudgetKind;
import com.mcnoita.spell.server.budget.BudgetLimits;
import com.mcnoita.spell.server.budget.BudgetRequest;
import com.mcnoita.spell.server.budget.ChunkBudgetKey;
import com.mcnoita.spell.server.budget.SpellBudgetManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Pure state-machine tests for persisted jobs and their central re-reservation boundary. */
@Tag("regression")
class SpellJobManagerTest {
    private static final String OVERWORLD = "minecraft:overworld";

    @Test
    void recoveryRetriesOnlyWhenBothFrozenNodeAndHandlerDeclareIdempotence() {
        TestHandler idempotentHandler = new TestHandler("safe", true, SpellJobStepResult.continueAt(null));
        SpellJobHandlerRegistry safeRegistry = registry(idempotentHandler);
        SpellBudgetManager safeBudget = new SpellBudgetManager(BudgetLimits.unlimited());
        SpellJobManager safeManager = new SpellJobManager(safeBudget, safeRegistry);
        SpellJobPersistentState runningSafe = state("safe", true, SpellJobState.RUNNING, 0, 2L);

        SpellJobManager.RecoveryResult safeRecovery = safeManager.recover(List.of(runningSafe)).get(0);
        assertEquals(SpellJobManager.RecoveryOutcome.RETRY_PAUSED, safeRecovery.outcome());
        assertEquals(SpellJobState.PAUSED, safeRecovery.state().state());

        SpellJobManager.TickResult step = safeManager.tick(runningSafe.executionId(), SpellJobGate.allowAll(), 20L);
        assertEquals(SpellJobManager.TickOutcome.STEPPED, step.outcome());
        assertEquals(1, idempotentHandler.calls.get());
        assertEquals(1L, safeBudget.globalTickUsage().get(BudgetKind.CROSS_TICK_JOB_STEPS));

        TestHandler nonIdempotentHandler = new TestHandler("unsafe", false, SpellJobStepResult.continueAt(null));
        SpellJobManager unsafeManager = new SpellJobManager(new SpellBudgetManager(BudgetLimits.unlimited()),
            registry(nonIdempotentHandler));
        SpellJobPersistentState runningUnsafe = state("unsafe", true, SpellJobState.RUNNING, 0, 2L);

        SpellJobManager.RecoveryResult unsafeRecovery = unsafeManager.recover(List.of(runningUnsafe)).get(0);
        assertEquals(SpellJobManager.RecoveryOutcome.INERT_NON_IDEMPOTENT, unsafeRecovery.outcome());
        assertEquals(SpellJobState.INERT, unsafeRecovery.state().state());
        assertEquals(0, nonIdempotentHandler.calls.get());
    }

    @Test
    void nonIdempotentJobsAreRejectedBeforeSideEffectsAndLegacyQueuedRecordsBecomeInert() {
        TestHandler nonIdempotentHandler = new TestHandler("unsafe", false, SpellJobStepResult.complete("side effect"));
        SpellJobPersistentState queued = state("unsafe", true, SpellJobState.QUEUED, 0, 2L);
        SpellJobManager manager = new SpellJobManager(new SpellBudgetManager(BudgetLimits.unlimited()),
            registry(nonIdempotentHandler));

        SpellJobSink.Submission submission = manager.submit(queued, 0L);

        assertFalse(submission.accepted());
        assertEquals(SpellJobState.INERT, submission.state().state());
        assertEquals(0, nonIdempotentHandler.calls.get());
        assertTrue(manager.find(queued.executionId()).isEmpty());

        // This mirrors the old crash window: the store still contains QUEUED
        // because it was written before the handler's side effect. Recovery
        // must not turn that record into a second handler invocation.
        SpellJobManager recoveredManager = new SpellJobManager(new SpellBudgetManager(BudgetLimits.unlimited()),
            registry(nonIdempotentHandler));
        SpellJobManager.RecoveryResult recovered = recoveredManager.recover(List.of(queued), 0L).get(0);

        assertEquals(SpellJobManager.RecoveryOutcome.INERT_NON_IDEMPOTENT, recovered.outcome());
        assertEquals(SpellJobState.INERT, recovered.state().state());
        assertEquals(0, nonIdempotentHandler.calls.get());
        assertEquals(SpellJobManager.TickOutcome.TERMINAL,
            recoveredManager.tick(queued.executionId(), SpellJobGate.allowAll(), 1L).outcome());
        assertEquals(0, nonIdempotentHandler.calls.get());
    }

    @Test
    void duplicateAndUnsupportedRecordsNeverBecomeSecondLiveJobs() {
        TestHandler handler = new TestHandler("known", true, SpellJobStepResult.continueAt(null));
        SpellJobManager manager = new SpellJobManager(new SpellBudgetManager(BudgetLimits.unlimited()), registry(handler));
        SpellJobPersistentState duplicate = state("known", true, SpellJobState.QUEUED, 0, 2L);

        List<SpellJobManager.RecoveryResult> recovered = manager.recover(List.of(duplicate, duplicate));
        assertEquals(SpellJobManager.RecoveryOutcome.RESTORED, recovered.get(0).outcome());
        assertEquals(SpellJobManager.RecoveryOutcome.REJECTED_DUPLICATE, recovered.get(1).outcome());
        assertEquals(SpellJobState.INERT, recovered.get(1).state().state());
        assertEquals(1, manager.snapshot().size());

        SpellJobManager unsupportedManager = new SpellJobManager(new SpellBudgetManager(BudgetLimits.unlimited()),
            new SpellJobHandlerRegistry());
        SpellJobPersistentState unsupported = state("missing", false, SpellJobState.QUEUED, 0, 2L);
        SpellJobSink.Submission submission = unsupportedManager.submit(unsupported);
        assertFalse(submission.accepted());
        assertEquals(SpellJobState.INERT, submission.state().state());
        assertTrue(unsupportedManager.find(unsupported.executionId()).isEmpty());
    }

    @Test
    void expiryOwnerAndChunkGatesPauseOrCancelBeforeAHandlerRuns() {
        TestHandler handler = new TestHandler("known", true, SpellJobStepResult.continueAt(null));

        SpellJobManager expiredManager = new SpellJobManager(new SpellBudgetManager(BudgetLimits.unlimited()), registry(handler));
        SpellJobPersistentState expired = state("known", true, SpellJobState.QUEUED, 0, 2L, 0L, 1L);
        assertTrue(expiredManager.submit(expired).accepted());
        assertEquals(SpellJobManager.TickOutcome.CANCELLED_EXPIRED,
            expiredManager.tick(expired.executionId(), SpellJobGate.allowAll(), 1L).outcome());

        SpellJobManager recoveryExpiryManager = new SpellJobManager(new SpellBudgetManager(BudgetLimits.unlimited()),
            registry(handler));
        assertEquals(SpellJobManager.RecoveryOutcome.CANCELLED_EXPIRED,
            recoveryExpiryManager.recover(List.of(expired), 1L).get(0).outcome());
        assertEquals(SpellJobState.CANCELLED, recoveryExpiryManager.find(expired.executionId()).orElseThrow().state());

        SpellJobManager ownerManager = new SpellJobManager(new SpellBudgetManager(BudgetLimits.unlimited()), registry(handler));
        SpellJobPersistentState ownerBlocked = state("known", true, SpellJobState.QUEUED, 0, 2L);
        assertTrue(ownerManager.submit(ownerBlocked).accepted());
        assertEquals(SpellJobManager.TickOutcome.PAUSED_OWNER_INELIGIBLE,
            ownerManager.tick(ownerBlocked.executionId(), gate(false, true), 0L).outcome());

        SpellJobManager chunkManager = new SpellJobManager(new SpellBudgetManager(BudgetLimits.unlimited()), registry(handler));
        SpellJobPersistentState chunkBlocked = state("known", true, SpellJobState.QUEUED, 0, 2L);
        assertTrue(chunkManager.submit(chunkBlocked).accepted());
        assertEquals(SpellJobManager.TickOutcome.PAUSED_CHUNK_UNLOADED,
            chunkManager.tick(chunkBlocked.executionId(), gate(true, false), 0L).outcome());
        assertEquals(0, handler.calls.get());
    }

    @Test
    void rejectedCentralStepBudgetPausesWithoutCallingTheHandler() {
        BudgetLimits noSteps = new BudgetLimits(Map.of(BudgetKind.CROSS_TICK_JOB_STEPS, 0L),
            BudgetLimits.ScopeLimits.unlimited(), BudgetLimits.ScopeLimits.unlimited(),
            BudgetLimits.ScopeLimits.unlimited(), BudgetLimits.ScopeLimits.unlimited(), 20L);
        TestHandler handler = new TestHandler("known", true, SpellJobStepResult.continueAt(null));
        SpellJobManager manager = new SpellJobManager(new SpellBudgetManager(noSteps), registry(handler));
        SpellJobPersistentState job = state("known", true, SpellJobState.QUEUED, 0, 2L);
        assertTrue(manager.submit(job).accepted());

        SpellJobManager.TickResult result = manager.tick(job.executionId(), SpellJobGate.allowAll(), 0L);

        assertEquals(SpellJobManager.TickOutcome.PAUSED_BUDGET, result.outcome());
        assertEquals(SpellJobState.PAUSED, result.state().state());
        assertEquals(0, handler.calls.get());
        assertEquals(BudgetKind.CROSS_TICK_JOB_STEPS, result.budgetDiagnostic().kind());
    }

    @Test
    void rootReservationTransfersItsLifetimeSliceAndTerminalJobClosesItExactlyOnce() {
        SpellBudgetManager budget = new SpellBudgetManager(BudgetLimits.unlimited());
        TestHandler handler = new TestHandler("known", true, SpellJobStepResult.complete("done"));
        SpellJobManager manager = new SpellJobManager(budget, registry(handler));
        SpellJobPersistentState job = state("known", true, SpellJobState.QUEUED, 0, 2L);
        BudgetRequest rootRequest = BudgetRequest.builder(job.executionId(), OVERWORLD)
            .owner(job.ownerId())
            .addInChunk(job.targetChunk(), BudgetKind.PERSISTENT_JOBS, 1L)
            .build();
        SpellBudgetManager.ReservationAttempt rootAttempt = budget.reserve(rootRequest, 0L);
        assertTrue(rootAttempt.accepted());
        assertTrue(rootAttempt.reservation().commit());

        assertTrue(manager.submit(job, rootAttempt.reservation()).accepted());
        assertTrue(rootAttempt.reservation().close());
        assertEquals(Map.of(BudgetKind.PERSISTENT_JOBS, 1L), budget.globalInFlightUsage());

        SpellJobManager.TickResult completed = manager.tick(job.executionId(), SpellJobGate.allowAll(), 1L);
        assertEquals(SpellJobManager.TickOutcome.COMPLETED, completed.outcome());
        assertEquals(SpellJobState.COMPLETED, completed.state().state());
        assertEquals(Map.of(), budget.globalInFlightUsage());
        assertFalse(manager.removeTerminal(UUID.randomUUID()));
        assertTrue(manager.removeTerminal(job.executionId()));
        assertEquals(Map.of(), budget.globalInFlightUsage());
    }

    private static SpellJobHandlerRegistry registry(SpellJobHandler handler) {
        SpellJobHandlerRegistry registry = new SpellJobHandlerRegistry();
        registry.register(handler);
        return registry;
    }

    private static SpellJobPersistentState state(
        String type, boolean nodeIdempotent, SpellJobState state, int cursor, long remainingSteps
    ) {
        return state(type, nodeIdempotent, state, cursor, remainingSteps, 0L, 80L);
    }

    private static SpellJobPersistentState state(
        String type, boolean nodeIdempotent, SpellJobState state, int cursor, long remainingSteps,
        long createdAt, long expiresAt
    ) {
        FrozenSpellJobNode node = new FrozenSpellJobNode("root/" + type, type, 2, nodeIdempotent,
            Map.of(BudgetKind.CROSS_TICK_JOB_STEPS, 1L), Map.of());
        return new SpellJobPersistentState(UUID.randomUUID(), UUID.randomUUID(), OVERWORLD,
            new ChunkBudgetKey(OVERWORLD, 3, -2), 5L, "b".repeat(64), node, cursor,
            Map.of(BudgetKind.CROSS_TICK_JOB_STEPS, remainingSteps), state, "", createdAt, expiresAt);
    }

    private static SpellJobGate gate(boolean ownerEligible, boolean chunkLoaded) {
        return new SpellJobGate() {
            @Override
            public boolean isOwnerEligible(UUID ownerId, String dimensionId) {
                return ownerEligible;
            }

            @Override
            public boolean isChunkLoaded(ChunkBudgetKey chunk) {
                return chunkLoaded;
            }
        };
    }

    private static final class TestHandler implements SpellJobHandler {
        private final String jobType;
        private final boolean recoveryIdempotent;
        private final SpellJobStepResult result;
        private final AtomicInteger calls = new AtomicInteger();

        private TestHandler(String jobType, boolean recoveryIdempotent, SpellJobStepResult result) {
            this.jobType = jobType;
            this.recoveryIdempotent = recoveryIdempotent;
            this.result = result;
        }

        @Override
        public String jobType() {
            return jobType;
        }

        @Override
        public boolean isRecoveryIdempotent() {
            return recoveryIdempotent;
        }

        @Override
        public SpellJobStepResult execute(SpellJobExecutionContext context) {
            calls.incrementAndGet();
            return result;
        }
    }
}
