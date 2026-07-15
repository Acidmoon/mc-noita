package com.mcnoita.spell.server.job;

import com.mcnoita.persistence.NoitaNbtLimits;
import com.mcnoita.spell.plan.PersistentJobEffectNode;
import com.mcnoita.spell.server.budget.BudgetKind;
import com.mcnoita.spell.server.budget.ChunkBudgetKey;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Complete durable evidence for one cross-tick task. It intentionally stores
 * frozen mechanics and catalog identity, never a live Item, World, or handler.
 */
public record SpellJobPersistentState(
    UUID executionId,
    UUID ownerId,
    String dimensionId,
    ChunkBudgetKey targetChunk,
    long catalogEpoch,
    String catalogHash,
    FrozenSpellJobNode node,
    int cursor,
    Map<BudgetKind, Long> remainingHardBudget,
    SpellJobState state,
    String stateReason,
    long createdAtTick,
    long expiresAtTick
) {
    public SpellJobPersistentState {
        executionId = requireBoundExecutionId(executionId, "executionId");
        ownerId = requireBoundExecutionId(ownerId, "ownerId");
        dimensionId = FrozenSpellJobNode.requireBoundedNonBlank(dimensionId, "dimensionId");
        targetChunk = Objects.requireNonNull(targetChunk, "targetChunk");
        if (!dimensionId.equals(targetChunk.dimensionId())) {
            throw new IllegalArgumentException("targetChunk must belong to the job dimension");
        }
        if (catalogEpoch < 0L) {
            throw new IllegalArgumentException("catalogEpoch must not be negative");
        }
        catalogHash = requireCatalogHash(catalogHash);
        node = Objects.requireNonNull(node, "node");
        if (cursor < 0 || cursor > node.maximumSteps()) {
            throw new IllegalArgumentException("cursor is outside the frozen job step range");
        }
        remainingHardBudget = FrozenSpellJobNode.copyBudget(remainingHardBudget, "remainingHardBudget", false);
        if (!remainingHardBudget.equals(node.remainingHardBudgetAfterSteps(cursor))) {
            // A persisted job cannot invent work allowance or claim a step was
            // consumed without advancing its cursor. This binds diagnostics and
            // retry budget to one deterministic frozen computation.
            throw new IllegalArgumentException("remainingHardBudget does not match the frozen cursor consumption");
        }
        state = Objects.requireNonNull(state, "state");
        stateReason = FrozenSpellJobNode.requireBoundedText(Objects.requireNonNull(stateReason, "stateReason"), "stateReason");
        if (createdAtTick < 0L || expiresAtTick <= createdAtTick
            || expiresAtTick - createdAtTick > NoitaNbtLimits.MAX_SPELL_JOB_LIFETIME_TICKS) {
            throw new IllegalArgumentException("job lifetime is outside the persisted limit");
        }
    }

    public static SpellJobPersistentState fromEffectNode(
        PersistentJobEffectNode effectNode, UUID executionId, UUID ownerId, String dimensionId, ChunkBudgetKey targetChunk,
        long catalogEpoch, String catalogHash, long createdAtTick
    ) {
        FrozenSpellJobNode frozen = FrozenSpellJobNode.fromEffectNode(effectNode);
        long lifetime = SpellJobTiming.toServerTicks(effectNode.expiresAfter());
        long expiresAtTick;
        try {
            expiresAtTick = Math.addExact(createdAtTick, lifetime);
        } catch (ArithmeticException failure) {
            throw new IllegalArgumentException("persistent job expiry overflows server time", failure);
        }
        return new SpellJobPersistentState(executionId, ownerId, dimensionId, targetChunk, catalogEpoch, catalogHash,
            frozen, 0, frozen.initialRemainingHardBudget(), SpellJobState.QUEUED, "", createdAtTick, expiresAtTick);
    }

    public boolean isTerminal() {
        return state.isTerminal();
    }

    public boolean isExpired(long serverTick) {
        if (serverTick < 0L) {
            throw new IllegalArgumentException("serverTick must not be negative");
        }
        return serverTick >= expiresAtTick;
    }

    public boolean canReserveNextStep() {
        for (Map.Entry<BudgetKind, Long> cost : node.perStepBudget().entrySet()) {
            if (remainingHardBudget.getOrDefault(cost.getKey(), 0L) < cost.getValue()) {
                return false;
            }
        }
        return cursor < node.maximumSteps();
    }

    public SpellJobPersistentState transition(SpellJobState nextState, String reason) {
        Objects.requireNonNull(nextState, "nextState");
        if (!state.canTransitionTo(nextState)) {
            throw new IllegalStateException("terminal spell job cannot transition from " + state + " to " + nextState);
        }
        return new SpellJobPersistentState(executionId, ownerId, dimensionId, targetChunk, catalogEpoch, catalogHash, node,
            cursor, remainingHardBudget, nextState, normalizeReason(reason), createdAtTick, expiresAtTick);
    }

    /** Records one attempted handler invocation after its central reservation committed. */
    public SpellJobPersistentState afterStep(SpellJobState nextState, String reason, ChunkBudgetKey nextChunk) {
        if (state != SpellJobState.RUNNING) {
            throw new IllegalStateException("only RUNNING jobs can consume a step");
        }
        if (!canReserveNextStep()) {
            throw new IllegalStateException("job cannot consume a step after its hard budget is exhausted");
        }
        ChunkBudgetKey resolvedChunk = nextChunk == null ? targetChunk : nextChunk;
        if (!dimensionId.equals(resolvedChunk.dimensionId())) {
            throw new IllegalArgumentException("a spell job cannot move across dimensions");
        }
        EnumMap<BudgetKind, Long> nextBudget = new EnumMap<>(BudgetKind.class);
        nextBudget.putAll(remainingHardBudget);
        for (Map.Entry<BudgetKind, Long> cost : node.perStepBudget().entrySet()) {
            long remaining = nextBudget.getOrDefault(cost.getKey(), 0L) - cost.getValue();
            if (remaining == 0L) {
                nextBudget.remove(cost.getKey());
            } else {
                nextBudget.put(cost.getKey(), remaining);
            }
        }
        int nextCursor = cursor + 1;
        SpellJobState resolvedState = nextState == SpellJobState.QUEUED && nextCursor >= node.maximumSteps()
            ? SpellJobState.COMPLETED : nextState;
        return new SpellJobPersistentState(executionId, ownerId, dimensionId, resolvedChunk, catalogEpoch, catalogHash,
            node, nextCursor, nextBudget, resolvedState, normalizeReason(reason), createdAtTick, expiresAtTick);
    }

    private static UUID requireBoundExecutionId(UUID value, String name) {
        Objects.requireNonNull(value, name);
        if (value.getMostSignificantBits() == 0L && value.getLeastSignificantBits() == 0L) {
            throw new IllegalArgumentException(name + " must not be the unbound zero UUID");
        }
        return value;
    }

    private static String requireCatalogHash(String hash) {
        Objects.requireNonNull(hash, "catalogHash");
        if (hash.length() != 64) {
            throw new IllegalArgumentException("catalogHash must be a SHA-256 hex digest");
        }
        for (int index = 0; index < hash.length(); index++) {
            char value = hash.charAt(index);
            if (!((value >= '0' && value <= '9') || (value >= 'a' && value <= 'f'))) {
                throw new IllegalArgumentException("catalogHash must be lowercase hexadecimal");
            }
        }
        return hash;
    }

    private static String normalizeReason(String reason) {
        return FrozenSpellJobNode.requireBoundedText(reason == null ? "" : reason, "stateReason");
    }
}
