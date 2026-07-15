package com.mcnoita.spell.server.job;

import com.mcnoita.persistence.NoitaNbtLimits;
import com.mcnoita.spell.server.budget.BudgetDiagnostic;
import com.mcnoita.spell.server.budget.BudgetKind;
import com.mcnoita.spell.server.budget.BudgetRequest;
import com.mcnoita.spell.server.budget.BudgetReservation;
import com.mcnoita.spell.server.budget.SpellBudgetManager;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Thread-confined server scheduler for frozen jobs. It holds no World reference:
 * host code supplies owner/chunk gates and each handler owns its bounded world
 * operation. Every handler call gets a fresh central reservation, including
 * after a normal save/reload recovery.
 */
public final class SpellJobManager implements SpellJobSink {
    private final SpellBudgetManager budgetManager;
    private final SpellJobHandlerRegistry handlers;
    private final Map<UUID, SpellJobPersistentState> jobs = new LinkedHashMap<>();
    /** One committed PERSISTENT_JOBS lease per live record, keyed by job execution ID. */
    private final Map<UUID, BudgetReservation> lifetimeLeases = new LinkedHashMap<>();
    private long lastServerTick = -1L;

    public SpellJobManager(SpellBudgetManager budgetManager, SpellJobHandlerRegistry handlers) {
        this.budgetManager = Objects.requireNonNull(budgetManager, "budgetManager");
        this.handlers = Objects.requireNonNull(handlers, "handlers");
    }

    /**
     * Unsupported nodes become inert in the returned result but are not kept as
     * live work. A persistence adapter may retain that returned inert record for
     * diagnostics without giving it another execution path.
     */
    @Override
    public synchronized Submission submit(SpellJobPersistentState job) {
        return submit(job, nextAdmissionTick());
    }

    /** Direct/non-cast admission obtains a fresh lifetime lease before retaining the job. */
    public synchronized Submission submit(SpellJobPersistentState job, long serverTick) {
        Objects.requireNonNull(job, "job");
        requireMonotonicTick(serverTick);
        if (!job.isTerminal() && job.isExpired(serverTick)) {
            return Submission.rejected(job.transition(SpellJobState.CANCELLED, "job expiry reached before admission"),
                "job expiry reached before admission");
        }
        Submission rejected = validateAdmission(job);
        if (rejected != null) {
            return rejected;
        }
        SpellBudgetManager.ReservationAttempt attempt = budgetManager.reserve(lifetimeRequest(job), serverTick);
        if (!attempt.accepted()) {
            return Submission.rejected(job.transition(SpellJobState.PAUSED, "central lifetime lease rejected"),
                "central lifetime lease rejected");
        }
        BudgetReservation lease = attempt.reservation();
        if (!lease.commit()) {
            lease.close();
            return Submission.rejected(toInert(job, "central lifetime lease could not commit"),
                "central lifetime lease could not commit");
        }
        return retainWithLease(job, lease);
    }

    /**
     * Cast-time admission transfers the already committed root PERSISTENT_JOBS
     * slice. It never releases and re-reserves that slice, avoiding a second
     * same-tick charge while keeping the in-flight quota after root close.
     */
    @Override
    public synchronized Submission submit(SpellJobPersistentState job, BudgetReservation rootReservation) {
        Objects.requireNonNull(job, "job");
        if (rootReservation == null) {
            return submit(job, nextAdmissionTick());
        }
        Submission rejected = validateAdmission(job);
        if (rejected != null) {
            return rejected;
        }
        try {
            if (!job.executionId().equals(rootReservation.executionId())) {
                return Submission.rejected(toInert(job, "root reservation execution ID does not match job"),
                    "root reservation execution ID does not match job");
            }
            BudgetReservation lease = rootReservation.transferCommittedSlice(lifetimeLeaseId(job.executionId()),
                rootLifetimeSlice(job, rootReservation));
            return retainWithLease(job, lease);
        } catch (RuntimeException failure) {
            return Submission.rejected(toInert(job, "root lifetime lease transfer was rejected"),
                "root lifetime lease transfer was rejected");
        }
    }

    /**
     * Restores persisted records without executing them. A crash may leave a
     * RUNNING record after its external side effect; only an explicitly
     * idempotent node and handler are allowed to return to PAUSED for retry.
     */
    public synchronized List<RecoveryResult> recover(Collection<SpellJobPersistentState> records) {
        return recover(records, nextAdmissionTick());
    }

    /** Restores state and immediately reacquires a fresh in-flight lifetime lease for every live record. */
    public synchronized List<RecoveryResult> recover(Collection<SpellJobPersistentState> records, long serverTick) {
        Objects.requireNonNull(records, "records");
        requireMonotonicTick(serverTick);
        List<RecoveryResult> results = new ArrayList<>();
        for (SpellJobPersistentState record : records) {
            Objects.requireNonNull(record, "spell job record");
            if (jobs.containsKey(record.executionId())) {
                results.add(new RecoveryResult(toInert(record, "duplicate execution ID during recovery"),
                    RecoveryOutcome.REJECTED_DUPLICATE));
                continue;
            }
            if (jobs.size() >= NoitaNbtLimits.MAX_SPELL_JOB_RECORDS) {
                results.add(new RecoveryResult(toInert(record, "persistent job record limit reached during recovery"),
                    RecoveryOutcome.REJECTED_CAPACITY));
                continue;
            }
            RecoveryResult normalized = normalizeRecovered(record);
            SpellJobPersistentState normalizedState = normalized.state();
            if (normalizedState.isTerminal()) {
                jobs.put(record.executionId(), normalizedState);
                results.add(normalized);
                continue;
            }
            if (normalizedState.isExpired(serverTick)) {
                SpellJobPersistentState cancelled = normalizedState.transition(SpellJobState.CANCELLED,
                    "job expiry reached during recovery");
                jobs.put(record.executionId(), cancelled);
                results.add(new RecoveryResult(cancelled, RecoveryOutcome.CANCELLED_EXPIRED));
                continue;
            }
            SpellBudgetManager.ReservationAttempt attempt = budgetManager.reserve(lifetimeRequest(normalizedState), serverTick);
            if (!attempt.accepted()) {
                SpellJobPersistentState paused = normalizedState.transition(SpellJobState.PAUSED,
                    "central lifetime lease rejected during recovery");
                jobs.put(record.executionId(), paused);
                results.add(new RecoveryResult(paused, RecoveryOutcome.PAUSED_LIFETIME_BUDGET));
                continue;
            }
            BudgetReservation lease = attempt.reservation();
            if (!lease.commit()) {
                lease.close();
                SpellJobPersistentState inert = toInert(normalizedState,
                    "central lifetime lease could not commit during recovery");
                jobs.put(record.executionId(), inert);
                results.add(new RecoveryResult(inert, RecoveryOutcome.INERT_INTERNAL));
                continue;
            }
            retainRecoveredLease(normalizedState, lease);
            results.add(normalized);
        }
        return List.copyOf(results);
    }

    public synchronized Optional<SpellJobPersistentState> find(UUID executionId) {
        return Optional.ofNullable(jobs.get(executionId));
    }

    /** Snapshot only; a server persistence adapter decides when durable storage is flushed. */
    public synchronized List<SpellJobPersistentState> snapshot() {
        return List.copyOf(jobs.values());
    }

    /** Terminal records can be removed after their inert/completed evidence was persisted or inspected. */
    public synchronized boolean removeTerminal(UUID executionId) {
        SpellJobPersistentState state = jobs.get(executionId);
        if (state == null || !state.isTerminal()) {
            return false;
        }
        jobs.remove(executionId);
        closeLifetimeLease(executionId);
        return true;
    }

    /** Releases only runtime leases during server stop; persistent records stay available to the storage adapter. */
    public synchronized void closeLifetimeLeasesForShutdown() {
        for (BudgetReservation lease : List.copyOf(lifetimeLeases.values())) {
            lease.close();
        }
        lifetimeLeases.clear();
    }

    public synchronized TickResult tick(UUID executionId, SpellJobGate gate, long serverTick) {
        Objects.requireNonNull(executionId, "executionId");
        Objects.requireNonNull(gate, "gate");
        requireMonotonicTick(serverTick);
        SpellJobPersistentState current = jobs.get(executionId);
        if (current == null) {
            return new TickResult(executionId, TickOutcome.NOT_FOUND, null, null);
        }
        if (current.state() == SpellJobState.RUNNING) {
            RecoveryResult recovered = normalizeRecovered(current);
            current = recovered.state();
            jobs.put(executionId, current);
            if (current.state() == SpellJobState.INERT) {
                closeLifetimeLease(executionId);
                return new TickResult(executionId, TickOutcome.INERT_RECOVERY, current, null);
            }
        }
        if (current.isTerminal()) {
            closeLifetimeLease(executionId);
            return new TickResult(executionId, TickOutcome.TERMINAL, current, null);
        }
        if (current.isExpired(serverTick)) {
            SpellJobPersistentState cancelled = current.transition(SpellJobState.CANCELLED, "job expiry reached");
            jobs.put(executionId, cancelled);
            closeLifetimeLease(executionId);
            return new TickResult(executionId, TickOutcome.CANCELLED_EXPIRED, cancelled, null);
        }
        BudgetDiagnostic lifetimeDiagnostic = ensureLifetimeLease(current, serverTick);
        if (lifetimeDiagnostic != null) {
            SpellJobPersistentState paused = current.transition(SpellJobState.PAUSED, "central lifetime lease rejected");
            jobs.put(executionId, paused);
            return new TickResult(executionId, TickOutcome.PAUSED_LIFETIME_BUDGET, paused, lifetimeDiagnostic);
        }
        if (!gate.isOwnerEligible(current.ownerId(), current.dimensionId())) {
            SpellJobPersistentState paused = current.transition(SpellJobState.PAUSED, "owner is offline, invalid, or in another dimension");
            jobs.put(executionId, paused);
            return new TickResult(executionId, TickOutcome.PAUSED_OWNER_INELIGIBLE, paused, null);
        }
        if (!gate.isChunkLoaded(current.targetChunk())) {
            SpellJobPersistentState paused = current.transition(SpellJobState.PAUSED, "target chunk is not loaded");
            jobs.put(executionId, paused);
            return new TickResult(executionId, TickOutcome.PAUSED_CHUNK_UNLOADED, paused, null);
        }
        Optional<SpellJobHandler> handler = handlers.find(current.node().jobType());
        if (handler.isEmpty()) {
            SpellJobPersistentState inert = toInert(current, "handler disappeared after job was queued");
            jobs.put(executionId, inert);
            closeLifetimeLease(executionId);
            return new TickResult(executionId, TickOutcome.INERT_UNSUPPORTED, inert, null);
        }
        if (!isRecoveryIdempotent(current, handler.get())) {
            // Admission and recovery reject these records, but retain this
            // guard so a future manager path cannot execute an unsafe handler
            // before a durable pre-effect RUNNING checkpoint exists.
            SpellJobPersistentState inert = toInert(current,
                "persistent job is not recovery-idempotent without a durable pre-effect checkpoint");
            jobs.put(executionId, inert);
            closeLifetimeLease(executionId);
            return new TickResult(executionId, TickOutcome.INERT_RECOVERY, inert, null);
        }
        if (!current.canReserveNextStep()) {
            SpellJobPersistentState cancelled = current.transition(SpellJobState.CANCELLED, "remaining hard job budget is exhausted");
            jobs.put(executionId, cancelled);
            closeLifetimeLease(executionId);
            return new TickResult(executionId, TickOutcome.CANCELLED_HARD_BUDGET, cancelled, null);
        }

        BudgetRequest request = stepRequest(current);
        SpellBudgetManager.ReservationAttempt attempt = budgetManager.reserve(request, serverTick);
        if (!attempt.accepted()) {
            SpellJobPersistentState paused = current.transition(SpellJobState.PAUSED, "central step budget rejected");
            jobs.put(executionId, paused);
            return new TickResult(executionId, TickOutcome.PAUSED_BUDGET, paused, attempt.diagnostic());
        }

        BudgetReservation reservation = attempt.reservation();
        try {
            if (!reservation.commit()) {
                SpellJobPersistentState inert = toInert(current, "central step reservation could not commit");
                jobs.put(executionId, inert);
                closeLifetimeLease(executionId);
                return new TickResult(executionId, TickOutcome.INERT_INTERNAL, inert, null);
            }

            // This is an in-memory diagnostic transition. Only recovery-idempotent
            // jobs reach this point until the persistence layer has a synchronous
            // durable pre-effect checkpoint protocol.
            SpellJobPersistentState running = current.transition(SpellJobState.RUNNING, "step reservation committed");
            jobs.put(executionId, running);
            SpellJobStepResult result = Objects.requireNonNull(handler.get().execute(new SpellJobExecutionContext(running, serverTick)),
                "spell job handler result");
            SpellJobPersistentState next = applyHandlerResult(running, result);
            jobs.put(executionId, next);
            if (next.isTerminal()) {
                closeLifetimeLease(executionId);
            }
            return new TickResult(executionId, outcomeFor(next), next, null);
        } catch (RuntimeException failure) {
            SpellJobPersistentState runningOrCurrent = jobs.getOrDefault(executionId, current);
            SpellJobPersistentState inert = toInert(runningOrCurrent, "handler failed after committed step reservation");
            jobs.put(executionId, inert);
            closeLifetimeLease(executionId);
            return new TickResult(executionId, TickOutcome.INERT_HANDLER_FAILURE, inert, null);
        } finally {
            // A committed close preserves this tick/window spend while releasing
            // in-flight capacity. The next step must reserve again.
            reservation.close();
        }
    }

    private RecoveryResult normalizeRecovered(SpellJobPersistentState record) {
        Optional<SpellJobHandler> handler = handlers.find(record.node().jobType());
        if (!record.isTerminal() && handler.isEmpty()) {
            return new RecoveryResult(toInert(record, "no handler for recovered frozen job type"), RecoveryOutcome.INERT_UNSUPPORTED);
        }
        if (!record.isTerminal() && !isRecoveryIdempotent(record, handler.orElseThrow())) {
            return new RecoveryResult(toInert(record,
                "persistent job is not recovery-idempotent without a durable pre-effect checkpoint"),
                RecoveryOutcome.INERT_NON_IDEMPOTENT);
        }
        if (record.state() == SpellJobState.RUNNING) {
            return new RecoveryResult(record.transition(SpellJobState.PAUSED,
                "recovered interrupted idempotent step"), RecoveryOutcome.RETRY_PAUSED);
        }
        return new RecoveryResult(record, RecoveryOutcome.RESTORED);
    }

    private static SpellJobPersistentState applyHandlerResult(SpellJobPersistentState running, SpellJobStepResult result) {
        return switch (result.outcome()) {
            case CONTINUE -> running.afterStep(SpellJobState.QUEUED, result.reason(), result.nextChunk());
            case PAUSE -> running.afterStep(SpellJobState.PAUSED, result.reason(), result.nextChunk());
            case COMPLETE -> running.afterStep(SpellJobState.COMPLETED, result.reason(), result.nextChunk());
            case CANCEL -> running.afterStep(SpellJobState.CANCELLED, result.reason(), result.nextChunk());
            case INERT -> running.afterStep(SpellJobState.INERT, result.reason(), result.nextChunk());
        };
    }

    private static TickOutcome outcomeFor(SpellJobPersistentState state) {
        return switch (state.state()) {
            case QUEUED -> TickOutcome.STEPPED;
            case PAUSED -> TickOutcome.PAUSED_HANDLER;
            case COMPLETED -> TickOutcome.COMPLETED;
            case CANCELLED -> TickOutcome.CANCELLED_HANDLER;
            case INERT -> TickOutcome.INERT_HANDLER;
            case RUNNING -> throw new IllegalStateException("handler result cannot leave a job RUNNING");
        };
    }

    private static SpellJobPersistentState toInert(SpellJobPersistentState state, String reason) {
        return state.isTerminal() ? state : state.transition(SpellJobState.INERT, reason);
    }

    private Submission validateAdmission(SpellJobPersistentState job) {
        Objects.requireNonNull(job, "job");
        if (job.state() != SpellJobState.QUEUED) {
            return Submission.rejected(job, "job is not queued");
        }
        if (jobs.containsKey(job.executionId()) || lifetimeLeases.containsKey(job.executionId())) {
            return Submission.rejected(toInert(job, "duplicate execution ID"), "duplicate execution ID");
        }
        if (jobs.size() >= NoitaNbtLimits.MAX_SPELL_JOB_RECORDS) {
            return Submission.rejected(toInert(job, "persistent job record limit reached"), "persistent job record limit reached");
        }
        Optional<SpellJobHandler> handler = handlers.find(job.node().jobType());
        if (handler.isEmpty()) {
            return Submission.rejected(toInert(job, "no handler for frozen job type"), "unsupported job type");
        }
        if (!isRecoveryIdempotent(job, handler.get())) {
            // PersistentState only becomes durably written later in the server
            // save cycle. Executing this job first could leave a QUEUED record
            // after a crash and replay a non-idempotent world side effect.
            return Submission.rejected(toInert(job,
                "persistent job requires node and handler recovery idempotence without a durable pre-effect checkpoint"),
                "persistent job is not recovery-idempotent");
        }
        return null;
    }

    private static boolean isRecoveryIdempotent(SpellJobPersistentState job, SpellJobHandler handler) {
        return job.node().recoveryIdempotent() && handler.isRecoveryIdempotent();
    }

    private Submission retainWithLease(SpellJobPersistentState job, BudgetReservation lease) {
        if (lifetimeLeases.putIfAbsent(job.executionId(), lease) != null) {
            // A lease is never intentionally replaced. Close the just-created
            // handle so a failed duplicate admission cannot retain capacity.
            lease.close();
            return Submission.rejected(toInert(job, "duplicate lifetime lease"), "duplicate lifetime lease");
        }
        jobs.put(job.executionId(), job);
        return Submission.accepted(job);
    }

    private void retainRecoveredLease(SpellJobPersistentState job, BudgetReservation lease) {
        if (lifetimeLeases.putIfAbsent(job.executionId(), lease) != null) {
            lease.close();
            throw new IllegalStateException("duplicate lifetime lease during recovery");
        }
        jobs.put(job.executionId(), job);
    }

    private BudgetDiagnostic ensureLifetimeLease(SpellJobPersistentState job, long serverTick) {
        BudgetReservation existing = lifetimeLeases.get(job.executionId());
        if (existing != null && existing.state() == BudgetReservation.State.COMMITTED) {
            return null;
        }
        if (existing != null) {
            lifetimeLeases.remove(job.executionId());
        }
        SpellBudgetManager.ReservationAttempt attempt = budgetManager.reserve(lifetimeRequest(job), serverTick);
        if (!attempt.accepted()) {
            return attempt.diagnostic();
        }
        BudgetReservation lease = attempt.reservation();
        if (!lease.commit()) {
            lease.close();
            return new BudgetDiagnostic(BudgetDiagnostic.Code.LIMIT_EXCEEDED, BudgetDiagnostic.Scope.EXECUTION,
                BudgetKind.PERSISTENT_JOBS, lifetimeLeaseId(job.executionId()), job.ownerId(), job.dimensionId(),
                job.targetChunk(), 1L, 0L, 0L, "lifetime lease could not commit");
        }
        lifetimeLeases.put(job.executionId(), lease);
        return null;
    }

    private void closeLifetimeLease(UUID executionId) {
        BudgetReservation lease = lifetimeLeases.remove(executionId);
        if (lease != null) {
            lease.close();
        }
    }

    private static BudgetRequest lifetimeRequest(SpellJobPersistentState job) {
        return BudgetRequest.builder(lifetimeLeaseId(job.executionId()), job.dimensionId())
            .owner(job.ownerId())
            .addInChunk(job.targetChunk(), BudgetKind.PERSISTENT_JOBS, 1L)
            .build();
    }

    private static BudgetRequest rootLifetimeSlice(SpellJobPersistentState job, BudgetReservation rootReservation) {
        return BudgetRequest.builder(rootReservation.executionId(), job.dimensionId())
            .owner(job.ownerId())
            .addInChunk(job.targetChunk(), BudgetKind.PERSISTENT_JOBS, 1L)
            .build();
    }

    static UUID lifetimeLeaseId(UUID executionId) {
        UUID leaseId = UUID.nameUUIDFromBytes(("mc-noita/spell-job-lease/v1/" + executionId)
            .getBytes(StandardCharsets.UTF_8));
        if (!leaseId.equals(executionId)) {
            return leaseId;
        }
        return UUID.nameUUIDFromBytes(("mc-noita/spell-job-lease/v1/fallback/" + executionId)
            .getBytes(StandardCharsets.UTF_8));
    }

    private static BudgetRequest stepRequest(SpellJobPersistentState job) {
        BudgetRequest.Builder builder = BudgetRequest.builder(job.executionId(), job.dimensionId()).owner(job.ownerId());
        for (Map.Entry<BudgetKind, Long> entry : job.node().perStepBudget().entrySet()) {
            // Chunk accounting is deliberately attached to the currently loaded
            // target. The manager never asks Minecraft to load it for a job.
            builder.addInChunk(job.targetChunk(), entry.getKey(), entry.getValue());
        }
        return builder.build();
    }

    private void requireMonotonicTick(long serverTick) {
        if (serverTick < 0L || serverTick < lastServerTick) {
            throw new IllegalArgumentException("spell job ticks must be non-negative and monotonic");
        }
        lastServerTick = serverTick;
    }

    private long nextAdmissionTick() {
        return Math.max(0L, lastServerTick);
    }

    public enum RecoveryOutcome {
        RESTORED,
        RETRY_PAUSED,
        INERT_NON_IDEMPOTENT,
        INERT_UNSUPPORTED,
        CANCELLED_EXPIRED,
        PAUSED_LIFETIME_BUDGET,
        REJECTED_DUPLICATE,
        REJECTED_CAPACITY,
        INERT_INTERNAL
    }

    public record RecoveryResult(SpellJobPersistentState state, RecoveryOutcome outcome) {
        public RecoveryResult {
            state = Objects.requireNonNull(state, "state");
            outcome = Objects.requireNonNull(outcome, "outcome");
        }
    }

    public enum TickOutcome {
        NOT_FOUND,
        TERMINAL,
        PAUSED_OWNER_INELIGIBLE,
        PAUSED_CHUNK_UNLOADED,
        PAUSED_BUDGET,
        PAUSED_LIFETIME_BUDGET,
        PAUSED_HANDLER,
        STEPPED,
        COMPLETED,
        CANCELLED_EXPIRED,
        CANCELLED_HARD_BUDGET,
        CANCELLED_HANDLER,
        INERT_UNSUPPORTED,
        INERT_RECOVERY,
        INERT_HANDLER,
        INERT_HANDLER_FAILURE,
        INERT_INTERNAL
    }

    public record TickResult(UUID executionId, TickOutcome outcome, SpellJobPersistentState state,
                             BudgetDiagnostic budgetDiagnostic) {
        public TickResult {
            executionId = Objects.requireNonNull(executionId, "executionId");
            outcome = Objects.requireNonNull(outcome, "outcome");
            if (outcome != TickOutcome.NOT_FOUND) {
                state = Objects.requireNonNull(state, "state");
            }
        }
    }
}
