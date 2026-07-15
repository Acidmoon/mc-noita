package com.mcnoita.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mcnoita.spell.server.budget.BudgetKind;
import com.mcnoita.spell.server.budget.ChunkBudgetKey;
import com.mcnoita.spell.server.job.FrozenSpellJobNode;
import com.mcnoita.spell.server.job.SpellJobPersistentState;
import com.mcnoita.spell.server.job.SpellJobState;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Regression coverage for strict v5 frozen-job persistence boundaries. */
@Tag("regression")
class SpellJobPersistentStateNbtCodecTest {
    @Test
    void currentJobRoundTripsWithoutReinterpretingFrozenMechanics() {
        SpellJobPersistentState original = sample(SpellJobState.PAUSED, 1, Map.of(BudgetKind.CROSS_TICK_JOB_STEPS, 1L));

        NbtCompound encoded = SpellJobPersistentStateNbtCodec.toNbt(original);

        assertEquals(NoitaNbtSchema.CURRENT_VERSION, encoded.getInt(NoitaNbtSchema.VERSION_KEY));
        assertEquals(original, SpellJobPersistentStateNbtCodec.tryFromNbt(encoded).orElseThrow());
    }

    @Test
    void futureAndCorruptRecordsAreRejectedWithoutMutatingSourceNbt() {
        NbtCompound future = SpellJobPersistentStateNbtCodec.toNbt(sample(SpellJobState.QUEUED, 0,
            Map.of(BudgetKind.CROSS_TICK_JOB_STEPS, 2L)));
        future.putInt(NoitaNbtSchema.VERSION_KEY, NoitaNbtSchema.CURRENT_VERSION + 1);

        assertTrue(SpellJobPersistentStateNbtCodec.tryFromNbt(future).isEmpty());
        assertEquals(NoitaNbtSchema.CURRENT_VERSION + 1, future.getInt(NoitaNbtSchema.VERSION_KEY));
        assertEquals(SpellJobState.INERT,
            SpellJobPersistentStateNbtCodec.tryInertFromMalformedNbt(future).orElseThrow().state());

        NbtCompound corrupt = SpellJobPersistentStateNbtCodec.toNbt(sample(SpellJobState.QUEUED, 0,
            Map.of(BudgetKind.CROSS_TICK_JOB_STEPS, 2L)));
        corrupt.getCompound("Node").getCompound("PerStepBudget").putLong("UNKNOWN_BUDGET", 1L);

        assertFalse(SpellJobPersistentStateNbtCodec.tryFromNbt(corrupt).isPresent());

        NbtCompound forgedRemaining = SpellJobPersistentStateNbtCodec.toNbt(sample(SpellJobState.QUEUED, 0,
            Map.of(BudgetKind.CROSS_TICK_JOB_STEPS, 2L)));
        forgedRemaining.getCompound("RemainingHardBudget").putLong(BudgetKind.CROSS_TICK_JOB_STEPS.name(), 1L);

        assertTrue(SpellJobPersistentStateNbtCodec.tryFromNbt(forgedRemaining).isEmpty());
    }

    @Test
    void legacyV4JobMigrationIsExplicitlyInertAndNeverDecodesAsExecutable() {
        NbtCompound legacy = new NbtCompound();
        legacy.putInt(NoitaNbtSchema.VERSION_KEY, 4);

        assertTrue(NoitaNbtSchema.migrateToCurrent(legacy, NoitaNbtSchema.Kind.SPELL_JOB));
        assertEquals(NoitaNbtSchema.CURRENT_VERSION, legacy.getInt(NoitaNbtSchema.VERSION_KEY));
        assertEquals("INERT", legacy.getString("State"));
        assertTrue(SpellJobPersistentStateNbtCodec.tryFromNbt(legacy).isEmpty());
    }

    private static SpellJobPersistentState sample(SpellJobState state, int cursor, Map<BudgetKind, Long> remaining) {
        FrozenSpellJobNode node = new FrozenSpellJobNode("root/job", "bounded_test", 2, true,
            Map.of(BudgetKind.CROSS_TICK_JOB_STEPS, 1L), Map.of("radius", "4"));
        return new SpellJobPersistentState(UUID.fromString("00000000-0000-0000-0000-000000000001"),
            UUID.fromString("00000000-0000-0000-0000-000000000002"), "minecraft:overworld",
            new ChunkBudgetKey("minecraft:overworld", 2, -3), 7L, "a".repeat(64), node, cursor, remaining,
            state, "saved test state", 100L, 140L);
    }
}
