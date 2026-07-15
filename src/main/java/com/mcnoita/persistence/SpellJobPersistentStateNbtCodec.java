package com.mcnoita.persistence;

import com.mcnoita.spell.server.budget.BudgetKind;
import com.mcnoita.spell.server.budget.ChunkBudgetKey;
import com.mcnoita.spell.server.job.FrozenSpellJobNode;
import com.mcnoita.spell.server.job.SpellJobPersistentState;
import com.mcnoita.spell.server.job.SpellJobState;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

/**
 * Strict v5 NBT boundary for frozen cross-tick jobs. A bad or future record is
 * rejected as a whole so callers can persist it as inert/cancelled rather than
 * running a partly decoded world operation.
 */
public final class SpellJobPersistentStateNbtCodec {
    private static final String EXECUTION_ID_KEY = "ExecutionId";
    private static final String OWNER_ID_KEY = "OwnerId";
    private static final String DIMENSION_KEY = "Dimension";
    private static final String CHUNK_X_KEY = "ChunkX";
    private static final String CHUNK_Z_KEY = "ChunkZ";
    private static final String CATALOG_EPOCH_KEY = "CatalogEpoch";
    private static final String CATALOG_HASH_KEY = "CatalogHash";
    private static final String NODE_KEY = "Node";
    private static final String NODE_PATH_KEY = "NodePath";
    private static final String JOB_TYPE_KEY = "JobType";
    private static final String MAXIMUM_STEPS_KEY = "MaximumSteps";
    private static final String RECOVERY_IDEMPOTENT_KEY = "RecoveryIdempotent";
    private static final String PER_STEP_BUDGET_KEY = "PerStepBudget";
    private static final String PARAMETERS_KEY = "Parameters";
    private static final String CURSOR_KEY = "Cursor";
    private static final String REMAINING_HARD_BUDGET_KEY = "RemainingHardBudget";
    private static final String STATE_KEY = "State";
    private static final String STATE_REASON_KEY = "StateReason";
    private static final String CREATED_AT_TICK_KEY = "CreatedAtTick";
    private static final String EXPIRES_AT_TICK_KEY = "ExpiresAtTick";

    private SpellJobPersistentStateNbtCodec() {
    }

    public static NbtCompound toNbt(SpellJobPersistentState state) {
        NbtCompound nbt = new NbtCompound();
        NoitaNbtSchema.writeCurrentVersion(nbt);
        nbt.putString(EXECUTION_ID_KEY, state.executionId().toString());
        nbt.putString(OWNER_ID_KEY, state.ownerId().toString());
        nbt.putString(DIMENSION_KEY, state.dimensionId());
        nbt.putInt(CHUNK_X_KEY, state.targetChunk().chunkX());
        nbt.putInt(CHUNK_Z_KEY, state.targetChunk().chunkZ());
        nbt.putLong(CATALOG_EPOCH_KEY, state.catalogEpoch());
        nbt.putString(CATALOG_HASH_KEY, state.catalogHash());
        nbt.put(NODE_KEY, nodeToNbt(state.node()));
        nbt.putInt(CURSOR_KEY, state.cursor());
        nbt.put(REMAINING_HARD_BUDGET_KEY, budgetToNbt(state.remainingHardBudget()));
        nbt.putString(STATE_KEY, state.state().name());
        nbt.putString(STATE_REASON_KEY, state.stateReason());
        nbt.putLong(CREATED_AT_TICK_KEY, state.createdAtTick());
        nbt.putLong(EXPIRES_AT_TICK_KEY, state.expiresAtTick());
        return nbt;
    }

    /** Decodes a private copy so rejecting a future record never rewrites caller-owned NBT. */
    public static Optional<SpellJobPersistentState> tryFromNbt(NbtCompound rawNbt) {
        if (rawNbt == null) {
            return Optional.empty();
        }
        NbtCompound nbt = rawNbt.copy();
        if (nbt.getSizeInBytes() > NoitaNbtLimits.MAX_SPELL_JOB_NBT_BYTES
            || !NoitaNbtSafety.validateTree(nbt, NoitaNbtLimits.MAX_SPELL_JOB_NBT_DEPTH,
                NoitaNbtLimits.MAX_SPELL_JOB_NBT_NODES, NoitaNbtLimits.MAX_SPELL_JOB_PARAMETERS)) {
            return Optional.empty();
        }
        try {
            if (!NoitaNbtSchema.migrateToCurrent(nbt, NoitaNbtSchema.Kind.SPELL_JOB)
                || NoitaNbtSchema.readStoredVersion(nbt) != NoitaNbtSchema.CURRENT_VERSION) {
                return Optional.empty();
            }
            return Optional.of(readCurrent(nbt));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    /**
     * Keeps a structurally identifiable bad record as inert diagnostic evidence.
     * The replacement deliberately discards every untrusted frozen mechanic, so
     * it can never reach a handler even when a later server adds that job type.
     */
    public static Optional<SpellJobPersistentState> tryInertFromMalformedNbt(NbtCompound rawNbt) {
        if (rawNbt == null || rawNbt.getSizeInBytes() > NoitaNbtLimits.MAX_SPELL_JOB_NBT_BYTES
            || !NoitaNbtSafety.validateTree(rawNbt, NoitaNbtLimits.MAX_SPELL_JOB_NBT_DEPTH,
                NoitaNbtLimits.MAX_SPELL_JOB_NBT_NODES, NoitaNbtLimits.MAX_SPELL_JOB_PARAMETERS)) {
            return Optional.empty();
        }
        try {
            require(rawNbt, EXECUTION_ID_KEY, NbtElement.STRING_TYPE);
            require(rawNbt, OWNER_ID_KEY, NbtElement.STRING_TYPE);
            require(rawNbt, DIMENSION_KEY, NbtElement.STRING_TYPE);
            require(rawNbt, CHUNK_X_KEY, NbtElement.INT_TYPE);
            require(rawNbt, CHUNK_Z_KEY, NbtElement.INT_TYPE);
            require(rawNbt, CATALOG_EPOCH_KEY, NbtElement.LONG_TYPE);
            require(rawNbt, CATALOG_HASH_KEY, NbtElement.STRING_TYPE);
            FrozenSpellJobNode inertNode = new FrozenSpellJobNode("inert/persisted-job", "invalid_persisted_job", 1,
                false, Map.of(BudgetKind.CROSS_TICK_JOB_STEPS, 1L), Map.of());
            return Optional.of(new SpellJobPersistentState(
                parseUuid(rawNbt.getString(EXECUTION_ID_KEY), EXECUTION_ID_KEY),
                parseUuid(rawNbt.getString(OWNER_ID_KEY), OWNER_ID_KEY), rawNbt.getString(DIMENSION_KEY),
                new ChunkBudgetKey(rawNbt.getString(DIMENSION_KEY), rawNbt.getInt(CHUNK_X_KEY), rawNbt.getInt(CHUNK_Z_KEY)),
                rawNbt.getLong(CATALOG_EPOCH_KEY), rawNbt.getString(CATALOG_HASH_KEY), inertNode, 0,
                inertNode.initialRemainingHardBudget(), SpellJobState.INERT,
                "corrupt or future persisted job record", 0L, 1L
            ));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private static SpellJobPersistentState readCurrent(NbtCompound nbt) {
        require(nbt, EXECUTION_ID_KEY, NbtElement.STRING_TYPE);
        require(nbt, OWNER_ID_KEY, NbtElement.STRING_TYPE);
        require(nbt, DIMENSION_KEY, NbtElement.STRING_TYPE);
        require(nbt, CHUNK_X_KEY, NbtElement.INT_TYPE);
        require(nbt, CHUNK_Z_KEY, NbtElement.INT_TYPE);
        require(nbt, CATALOG_EPOCH_KEY, NbtElement.LONG_TYPE);
        require(nbt, CATALOG_HASH_KEY, NbtElement.STRING_TYPE);
        require(nbt, NODE_KEY, NbtElement.COMPOUND_TYPE);
        require(nbt, CURSOR_KEY, NbtElement.INT_TYPE);
        require(nbt, REMAINING_HARD_BUDGET_KEY, NbtElement.COMPOUND_TYPE);
        require(nbt, STATE_KEY, NbtElement.STRING_TYPE);
        require(nbt, STATE_REASON_KEY, NbtElement.STRING_TYPE);
        require(nbt, CREATED_AT_TICK_KEY, NbtElement.LONG_TYPE);
        require(nbt, EXPIRES_AT_TICK_KEY, NbtElement.LONG_TYPE);
        SpellJobState state = enumValue(SpellJobState.class, nbt.getString(STATE_KEY), STATE_KEY);
        return new SpellJobPersistentState(
            parseUuid(nbt.getString(EXECUTION_ID_KEY), EXECUTION_ID_KEY),
            parseUuid(nbt.getString(OWNER_ID_KEY), OWNER_ID_KEY),
            nbt.getString(DIMENSION_KEY),
            new ChunkBudgetKey(nbt.getString(DIMENSION_KEY), nbt.getInt(CHUNK_X_KEY), nbt.getInt(CHUNK_Z_KEY)),
            nbt.getLong(CATALOG_EPOCH_KEY), nbt.getString(CATALOG_HASH_KEY), readNode(nbt.getCompound(NODE_KEY)),
            nbt.getInt(CURSOR_KEY), readBudget(nbt.getCompound(REMAINING_HARD_BUDGET_KEY), REMAINING_HARD_BUDGET_KEY),
            state, nbt.getString(STATE_REASON_KEY), nbt.getLong(CREATED_AT_TICK_KEY), nbt.getLong(EXPIRES_AT_TICK_KEY)
        );
    }

    private static NbtCompound nodeToNbt(FrozenSpellJobNode node) {
        NbtCompound nbt = new NbtCompound();
        nbt.putString(NODE_PATH_KEY, node.nodePath());
        nbt.putString(JOB_TYPE_KEY, node.jobType());
        nbt.putInt(MAXIMUM_STEPS_KEY, node.maximumSteps());
        nbt.putBoolean(RECOVERY_IDEMPOTENT_KEY, node.recoveryIdempotent());
        nbt.put(PER_STEP_BUDGET_KEY, budgetToNbt(node.perStepBudget()));
        NbtCompound parameters = new NbtCompound();
        for (Map.Entry<String, String> entry : node.parameters().entrySet()) {
            parameters.putString(entry.getKey(), entry.getValue());
        }
        nbt.put(PARAMETERS_KEY, parameters);
        return nbt;
    }

    private static FrozenSpellJobNode readNode(NbtCompound nbt) {
        require(nbt, NODE_PATH_KEY, NbtElement.STRING_TYPE);
        require(nbt, JOB_TYPE_KEY, NbtElement.STRING_TYPE);
        require(nbt, MAXIMUM_STEPS_KEY, NbtElement.INT_TYPE);
        require(nbt, RECOVERY_IDEMPOTENT_KEY, NbtElement.BYTE_TYPE);
        require(nbt, PER_STEP_BUDGET_KEY, NbtElement.COMPOUND_TYPE);
        require(nbt, PARAMETERS_KEY, NbtElement.COMPOUND_TYPE);
        return new FrozenSpellJobNode(nbt.getString(NODE_PATH_KEY), nbt.getString(JOB_TYPE_KEY),
            nbt.getInt(MAXIMUM_STEPS_KEY), nbt.getBoolean(RECOVERY_IDEMPOTENT_KEY),
            readBudget(nbt.getCompound(PER_STEP_BUDGET_KEY), PER_STEP_BUDGET_KEY),
            readParameters(nbt.getCompound(PARAMETERS_KEY)));
    }

    private static NbtCompound budgetToNbt(Map<BudgetKind, Long> budget) {
        NbtCompound nbt = new NbtCompound();
        for (BudgetKind kind : BudgetKind.values()) {
            Long amount = budget.get(kind);
            if (amount != null) {
                nbt.putLong(kind.name(), amount);
            }
        }
        return nbt;
    }

    private static Map<BudgetKind, Long> readBudget(NbtCompound nbt, String field) {
        if (nbt.getKeys().size() > BudgetKind.values().length) {
            throw new IllegalArgumentException(field + " has too many budget kinds");
        }
        EnumMap<BudgetKind, Long> values = new EnumMap<>(BudgetKind.class);
        for (String key : nbt.getKeys()) {
            BudgetKind kind = enumValue(BudgetKind.class, key, field + " key");
            require(nbt, key, NbtElement.LONG_TYPE);
            long amount = nbt.getLong(key);
            if (amount < 1L) {
                throw new IllegalArgumentException(field + " contains a nonpositive amount");
            }
            values.put(kind, amount);
        }
        return Map.copyOf(values);
    }

    private static Map<String, String> readParameters(NbtCompound nbt) {
        if (nbt.getKeys().size() > NoitaNbtLimits.MAX_SPELL_JOB_PARAMETERS) {
            throw new IllegalArgumentException("too many frozen job parameters");
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (String key : nbt.getKeys()) {
            require(nbt, key, NbtElement.STRING_TYPE);
            values.put(key, nbt.getString(key));
        }
        return Map.copyOf(values);
    }

    private static UUID parseUuid(String value, String field) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("invalid " + field, failure);
        }
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, String field) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("invalid " + field, failure);
        }
    }

    private static void require(NbtCompound nbt, String key, byte type) {
        if (!nbt.contains(key, type)) {
            throw new IllegalArgumentException("missing or malformed current spell job field " + key);
        }
    }
}
