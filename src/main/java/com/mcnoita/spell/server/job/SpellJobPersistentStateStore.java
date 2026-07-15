package com.mcnoita.spell.server.job;

import com.mcnoita.MCNoita;
import com.mcnoita.persistence.NoitaNbtLimits;
import com.mcnoita.persistence.NoitaNbtSafety;
import com.mcnoita.persistence.SpellJobPersistentStateNbtCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

/**
 * Overworld-level storage for frozen jobs. Invalid records are omitted before
 * scheduling, never repaired into a guessed world action; the server log keeps
 * the diagnostic evidence for an administrator.
 */
public final class SpellJobPersistentStateStore extends PersistentState {
    private static final String STORAGE_KEY = "mc_noita_spell_jobs";
    private static final String JOBS_KEY = "Jobs";
    private static final int MAX_STORE_NBT_BYTES = NoitaNbtLimits.MAX_SPELL_JOB_NBT_BYTES
        * NoitaNbtLimits.MAX_SPELL_JOB_RECORDS;
    private static final int MAX_STORE_NBT_NODES = NoitaNbtLimits.MAX_SPELL_JOB_NBT_NODES
        * NoitaNbtLimits.MAX_SPELL_JOB_RECORDS;

    private static final PersistentState.Type<SpellJobPersistentStateStore> TYPE = new PersistentState.Type<>(
        SpellJobPersistentStateStore::new, SpellJobPersistentStateStore::fromNbt,
        DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private List<SpellJobPersistentState> jobs = List.of();

    public static SpellJobPersistentStateStore get(ServerWorld overworld) {
        return overworld.getPersistentStateManager().getOrCreate(TYPE, STORAGE_KEY);
    }

    private static SpellJobPersistentStateStore fromNbt(NbtCompound nbt) {
        SpellJobPersistentStateStore store = new SpellJobPersistentStateStore();
        if (nbt.getSizeInBytes() > MAX_STORE_NBT_BYTES
            || !NoitaNbtSafety.validateTree(nbt, NoitaNbtLimits.MAX_SPELL_JOB_NBT_DEPTH + 2,
                MAX_STORE_NBT_NODES, NoitaNbtLimits.MAX_SPELL_JOB_RECORDS)
            || !nbt.contains(JOBS_KEY, NbtElement.LIST_TYPE)) {
            MCNoita.LOGGER.warn("Discarding malformed persisted spell-job store before scheduling any job");
            store.markDirty();
            return store;
        }
        NbtList entries = nbt.getList(JOBS_KEY, NbtElement.COMPOUND_TYPE);
        if (entries.size() > NoitaNbtLimits.MAX_SPELL_JOB_RECORDS) {
            MCNoita.LOGGER.warn("Discarding oversized persisted spell-job store with {} records", entries.size());
            store.markDirty();
            return store;
        }
        List<SpellJobPersistentState> decoded = new ArrayList<>(entries.size());
        for (int index = 0; index < entries.size(); index++) {
            NbtCompound rawRecord = entries.getCompound(index);
            Optional<SpellJobPersistentState> valid = SpellJobPersistentStateNbtCodec.tryFromNbt(rawRecord);
            if (valid.isPresent()) {
                decoded.add(valid.get());
                continue;
            }
            Optional<SpellJobPersistentState> inert = SpellJobPersistentStateNbtCodec.tryInertFromMalformedNbt(rawRecord);
            if (inert.isPresent()) {
                decoded.add(inert.get());
                store.markDirty();
                MCNoita.LOGGER.warn("Converted corrupt or future spell-job record {} to inert diagnostic state", index);
            } else {
                store.markDirty();
                MCNoita.LOGGER.warn("Ignoring unidentifiable corrupt or future spell-job record {}", index);
            }
        }
        store.jobs = List.copyOf(decoded);
        return store;
    }

    public synchronized List<SpellJobPersistentState> jobs() {
        return jobs;
    }

    /** Writes normalized manager state after recovery and every server tick that changes a job. */
    public synchronized void replace(List<SpellJobPersistentState> nextJobs) {
        List<SpellJobPersistentState> copied = List.copyOf(nextJobs);
        if (!jobs.equals(copied)) {
            jobs = copied;
            markDirty();
        }
    }

    @Override
    public synchronized NbtCompound writeNbt(NbtCompound nbt) {
        NbtList entries = new NbtList();
        for (SpellJobPersistentState job : jobs) {
            entries.add(SpellJobPersistentStateNbtCodec.toNbt(job));
        }
        nbt.put(JOBS_KEY, entries);
        return nbt;
    }
}
