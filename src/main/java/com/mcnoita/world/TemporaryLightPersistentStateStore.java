package com.mcnoita.world;

import com.mcnoita.MCNoita;
import com.mcnoita.persistence.NoitaNbtSafety;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

/**
 * Overworld-level cleanup ledger for short-lived light blocks. It deliberately
 * records no source, level, or world-effect data: after a restart only the
 * pending removal position and its game-time expiry are meaningful.
 */
final class TemporaryLightPersistentStateStore extends PersistentState {
    static final int FORMAT_VERSION = 2;
    static final int UNKNOWN_LIGHT_LEVEL = -1;
    static final int MAX_RECORDS = 4_096;
    private static final String STORAGE_KEY = "mc_noita_temporary_lights";
    private static final String VERSION_KEY = "Version";
    private static final String ENTRIES_KEY = "Entries";
    private static final String DIMENSION_KEY = "Dimension";
    private static final String X_KEY = "X";
    private static final String Y_KEY = "Y";
    private static final String Z_KEY = "Z";
    private static final String EXPIRES_AT_KEY = "ExpiresAt";
    private static final String LIGHT_LEVEL_KEY = "LightLevel";
    private static final int MAX_STORE_NBT_BYTES = 262_144;
    private static final int MAX_STORE_NBT_NODES = MAX_RECORDS * 8 + 2;
    private static final PersistentState.Type<TemporaryLightPersistentStateStore> TYPE = new PersistentState.Type<>(
        TemporaryLightPersistentStateStore::new, TemporaryLightPersistentStateStore::fromNbt,
        DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private List<PendingLight> lights = List.of();
    private boolean writable = true;

    static TemporaryLightPersistentStateStore get(ServerWorld overworld) {
        return overworld.getPersistentStateManager().getOrCreate(TYPE, STORAGE_KEY);
    }

    static TemporaryLightPersistentStateStore fromNbt(NbtCompound nbt) {
        TemporaryLightPersistentStateStore store = new TemporaryLightPersistentStateStore();
        DecodeResult decoded = decode(nbt);
        if (decoded.status() == DecodeStatus.SUCCESS) {
            store.lights = decoded.lights();
            return store;
        }
        if (decoded.status() == DecodeStatus.FUTURE) {
            // Do not rewrite a newer server's ledger with this build. Cleanup
            // still remains in-memory for this session, but it cannot destroy
            // unknown future records during the next save.
            store.writable = false;
            MCNoita.LOGGER.warn("Refusing future temporary-light persistence format {}", decoded.version());
            return store;
        }
        MCNoita.LOGGER.warn("Discarding malformed temporary-light cleanup ledger before any world mutation");
        store.markDirty();
        return store;
    }

    synchronized List<PendingLight> lights() {
        return lights;
    }

    /** A future-format ledger must never permit a new block without durable cleanup state. */
    synchronized boolean canTrackNewLights() {
        return writable;
    }

    /** Replaces the complete bounded snapshot after a server-thread map update. */
    synchronized void replace(List<PendingLight> nextLights) {
        if (!writable) {
            return;
        }
        List<PendingLight> normalized = normalize(nextLights);
        if (!lights.equals(normalized)) {
            lights = normalized;
            markDirty();
        }
    }

    @Override
    public synchronized NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound encoded = encode(lights);
        for (String key : encoded.getKeys()) {
            nbt.put(key, encoded.get(key));
        }
        return nbt;
    }

    static NbtCompound encode(List<PendingLight> pendingLights) {
        List<PendingLight> normalized = normalize(pendingLights);
        NbtCompound nbt = new NbtCompound();
        nbt.putInt(VERSION_KEY, FORMAT_VERSION);
        NbtList entries = new NbtList();
        for (PendingLight light : normalized) {
            NbtCompound entry = new NbtCompound();
            entry.putString(DIMENSION_KEY, light.dimensionId());
            entry.putInt(X_KEY, light.pos().getX());
            entry.putInt(Y_KEY, light.pos().getY());
            entry.putInt(Z_KEY, light.pos().getZ());
            entry.putLong(EXPIRES_AT_KEY, light.expiresAt());
            entry.putInt(LIGHT_LEVEL_KEY, light.lightLevel());
            entries.add(entry);
        }
        nbt.put(ENTRIES_KEY, entries);
        return nbt;
    }

    static Optional<List<PendingLight>> tryDecode(NbtCompound nbt) {
        DecodeResult decoded = decode(nbt);
        return decoded.status() == DecodeStatus.SUCCESS ? Optional.of(decoded.lights()) : Optional.empty();
    }

    private static DecodeResult decode(NbtCompound nbt) {
        if (nbt == null || nbt.getSizeInBytes() > MAX_STORE_NBT_BYTES
            || !NoitaNbtSafety.validateTree(nbt, 4, MAX_STORE_NBT_NODES, MAX_RECORDS)
            || !nbt.contains(VERSION_KEY, NbtElement.INT_TYPE)) {
            return DecodeResult.invalid();
        }
        int version = nbt.getInt(VERSION_KEY);
        if (version > FORMAT_VERSION) {
            return DecodeResult.future(version);
        }
        if ((version != 1 && version != FORMAT_VERSION) || !nbt.contains(ENTRIES_KEY, NbtElement.LIST_TYPE)) {
            return DecodeResult.invalid();
        }
        NbtList entries = nbt.getList(ENTRIES_KEY, NbtElement.COMPOUND_TYPE);
        if (entries.size() > MAX_RECORDS) {
            return DecodeResult.invalid();
        }
        try {
            List<PendingLight> decoded = new ArrayList<>(entries.size());
            for (int index = 0; index < entries.size(); index++) {
                NbtCompound entry = entries.getCompound(index);
                require(entry, DIMENSION_KEY, NbtElement.STRING_TYPE);
                require(entry, X_KEY, NbtElement.INT_TYPE);
                require(entry, Y_KEY, NbtElement.INT_TYPE);
                require(entry, Z_KEY, NbtElement.INT_TYPE);
                require(entry, EXPIRES_AT_KEY, NbtElement.LONG_TYPE);
                int lightLevel = UNKNOWN_LIGHT_LEVEL;
                if (version >= 2) {
                    require(entry, LIGHT_LEVEL_KEY, NbtElement.INT_TYPE);
                    lightLevel = entry.getInt(LIGHT_LEVEL_KEY);
                }
                decoded.add(new PendingLight(entry.getString(DIMENSION_KEY),
                    new BlockPos(entry.getInt(X_KEY), entry.getInt(Y_KEY), entry.getInt(Z_KEY)),
                    entry.getLong(EXPIRES_AT_KEY), lightLevel));
            }
            return DecodeResult.success(normalize(decoded));
        } catch (IllegalArgumentException ignored) {
            return DecodeResult.invalid();
        }
    }

    private static List<PendingLight> normalize(List<PendingLight> pendingLights) {
        Objects.requireNonNull(pendingLights, "pendingLights");
        if (pendingLights.size() > MAX_RECORDS) {
            throw new IllegalArgumentException("too many pending temporary lights");
        }
        Map<LightLocation, PendingLight> byLocation = new LinkedHashMap<>();
        for (PendingLight light : pendingLights) {
            PendingLight nonNullLight = Objects.requireNonNull(light, "pending light");
            LightLocation location = new LightLocation(nonNullLight.dimensionId(), nonNullLight.pos());
            PendingLight existing = byLocation.get(location);
            if (existing == null || nonNullLight.expiresAt() > existing.expiresAt()) {
                byLocation.put(location, nonNullLight);
            }
        }
        return List.copyOf(byLocation.values());
    }

    private static void require(NbtCompound nbt, String key, byte type) {
        if (!nbt.contains(key, type)) {
            throw new IllegalArgumentException("missing or malformed temporary-light field " + key);
        }
    }

    /** Immutable, bounded data kept after the original projectile has gone away. */
    record PendingLight(String dimensionId, BlockPos pos, long expiresAt, int lightLevel) {
        PendingLight {
            if (dimensionId == null || Identifier.tryParse(dimensionId) == null) {
                throw new IllegalArgumentException("invalid temporary-light dimension");
            }
            pos = Objects.requireNonNull(pos, "pos").toImmutable();
            if (lightLevel < UNKNOWN_LIGHT_LEVEL || lightLevel > 15) {
                throw new IllegalArgumentException("invalid temporary-light level");
            }
        }

        /** v1 records had no level fingerprint and retain their legacy cleanup behavior once. */
        PendingLight(String dimensionId, BlockPos pos, long expiresAt) {
            this(dimensionId, pos, expiresAt, UNKNOWN_LIGHT_LEVEL);
        }
    }

    private record LightLocation(String dimensionId, BlockPos pos) {
    }

    private record DecodeResult(DecodeStatus status, int version, List<PendingLight> lights) {
        private static DecodeResult success(List<PendingLight> lights) {
            return new DecodeResult(DecodeStatus.SUCCESS, FORMAT_VERSION, lights);
        }

        private static DecodeResult future(int version) {
            return new DecodeResult(DecodeStatus.FUTURE, version, List.of());
        }

        private static DecodeResult invalid() {
            return new DecodeResult(DecodeStatus.INVALID, -1, List.of());
        }
    }

    private enum DecodeStatus {
        SUCCESS,
        FUTURE,
        INVALID
    }
}
