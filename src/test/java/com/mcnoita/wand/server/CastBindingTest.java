package com.mcnoita.wand.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Pure binding checks for stale-stack and catalog-reload rejection. */
@Tag("regression")
class CastBindingTest {
    @Test
    void bindingRequiresEveryObservedStackAndCatalogComponentToMatch() {
        CastBinding baseline = binding(4L, 17, "nbt-a", 7L, "catalog-a", 12L);

        assertTrue(baseline.matches(binding(4L, 17, "nbt-a", 7L, "catalog-a", 12L)));
        assertFalse(baseline.matches(binding(5L, 17, "nbt-a", 7L, "catalog-a", 12L)));
        assertFalse(baseline.matches(binding(4L, 18, "nbt-a", 7L, "catalog-a", 12L)));
        assertFalse(baseline.matches(binding(4L, 17, "nbt-b", 7L, "catalog-a", 12L)));
        assertFalse(baseline.matches(binding(4L, 17, "nbt-a", 8L, "catalog-a", 12L)));
        assertFalse(baseline.matches(binding(4L, 17, "nbt-a", 7L, "catalog-b", 12L)));
        assertFalse(baseline.matches(binding(4L, 17, "nbt-a", 7L, "catalog-a", 13L)));
    }

    @Test
    void intentAndBindingRejectInvalidSlotsAndSequences() {
        assertThrows(IllegalArgumentException.class, () -> new CastBinding(UUID.randomUUID(), "MAIN_HAND", 9,
            "mc-noita:starter_wand", 1, 0L, 0, "nbt", 0L, "hash", 0L));
        assertThrows(IllegalArgumentException.class, () -> new CastIntent(net.minecraft.util.Hand.MAIN_HAND, 9, 0L));
        assertThrows(IllegalArgumentException.class, () -> new CastIntent(net.minecraft.util.Hand.OFF_HAND, 0, 0L));
    }

    private static CastBinding binding(
        long revision, int spellsHash, String nbtHash, long catalogEpoch, String catalogHash, long sequence
    ) {
        return new CastBinding(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"), "MAIN_HAND", 2,
            "mc-noita:starter_wand", 1, revision, spellsHash, nbtHash, catalogEpoch, catalogHash, sequence);
    }
}
