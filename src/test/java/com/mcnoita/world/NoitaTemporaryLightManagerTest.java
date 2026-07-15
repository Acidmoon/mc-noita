package com.mcnoita.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Regression coverage for retryable temporary-light cleanup bookkeeping. */
@Tag("regression")
class NoitaTemporaryLightManagerTest {
    @Test
    void cleanupRetainsEntriesWhoseRemovalCannotRunYet() {
        Map<String, Integer> entries = new LinkedHashMap<>(Map.of(
            "loaded", 1,
            "unloaded", 2,
            "denied", 3
        ));

        NoitaTemporaryLightManager.removeEntriesAfterSuccessfulRemoval(
            entries, key -> key.equals("loaded")
        );

        assertEquals(Map.of("unloaded", 2, "denied", 3), entries);
    }

    @Test
    void cleanupRemovesEveryEntryConfirmedAbsent() {
        Map<String, Integer> entries = new LinkedHashMap<>(Map.of("first", 1, "second", 2));

        NoitaTemporaryLightManager.removeEntriesAfterSuccessfulRemoval(entries, key -> true);

        assertEquals(Map.of(), entries);
    }

    @Test
    void pendingCleanupCodecRoundTripsOnlyPositionDimensionAndExpiry() {
        TemporaryLightPersistentStateStore.PendingLight pending = new TemporaryLightPersistentStateStore.PendingLight(
            "minecraft:the_nether", new BlockPos(12, 64, -8), 4_200L, 11
        );

        NbtCompound encoded = TemporaryLightPersistentStateStore.encode(List.of(pending));
        NbtCompound entry = encoded.getList("Entries", NbtElement.COMPOUND_TYPE).getCompound(0);

        assertEquals(TemporaryLightPersistentStateStore.FORMAT_VERSION, encoded.getInt("Version"));
        assertEquals(List.of("Dimension", "ExpiresAt", "LightLevel", "X", "Y", "Z"),
            entry.getKeys().stream().sorted().toList());
        assertEquals(List.of(pending), TemporaryLightPersistentStateStore.tryDecode(encoded).orElseThrow());
    }

    @Test
    void v1CleanupLedgerRemainsDecodableButMarksItsLightLevelUnknown() {
        NbtCompound legacy = TemporaryLightPersistentStateStore.encode(List.of(
            new TemporaryLightPersistentStateStore.PendingLight("minecraft:overworld", new BlockPos(0, 64, 0), 20L, 8)
        ));
        legacy.putInt("Version", 1);
        legacy.getList("Entries", NbtElement.COMPOUND_TYPE).getCompound(0).remove("LightLevel");

        TemporaryLightPersistentStateStore.PendingLight decoded = TemporaryLightPersistentStateStore.tryDecode(legacy)
            .orElseThrow().get(0);

        assertEquals(TemporaryLightPersistentStateStore.UNKNOWN_LIGHT_LEVEL, decoded.lightLevel());
    }

    @Test
    void pendingCleanupCodecRejectsFutureAndOversizedLedgers() {
        NbtCompound future = TemporaryLightPersistentStateStore.encode(List.of());
        future.putInt("Version", TemporaryLightPersistentStateStore.FORMAT_VERSION + 1);
        assertTrue(TemporaryLightPersistentStateStore.tryDecode(future).isEmpty());
        assertFalse(TemporaryLightPersistentStateStore.fromNbt(future).canTrackNewLights());

        NbtCompound oversized = TemporaryLightPersistentStateStore.encode(List.of());
        NbtList entries = new NbtList();
        for (int index = 0; index <= TemporaryLightPersistentStateStore.MAX_RECORDS; index++) {
            NbtCompound entry = new NbtCompound();
            entry.putString("Dimension", "minecraft:overworld");
            entry.putInt("X", index);
            entry.putInt("Y", 64);
            entry.putInt("Z", 0);
            entry.putLong("ExpiresAt", index);
            entries.add(entry);
        }
        oversized.put("Entries", entries);

        assertFalse(TemporaryLightPersistentStateStore.tryDecode(oversized).isPresent());
    }
}
