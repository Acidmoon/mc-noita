package com.mcnoita.wand.server;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable server-observed identity for the exact stack and catalog evaluated
 * by a cast. It deliberately contains values only, so binding comparisons are
 * unit-testable without a Minecraft world.
 */
public record CastBinding(
    UUID playerId,
    String hand,
    int slot,
    String itemId,
    int itemCount,
    long stateRevision,
    int spellsHash,
    String nbtStateHash,
    long catalogEpoch,
    String catalogHash,
    long sequence
) {
    public CastBinding {
        Objects.requireNonNull(playerId, "playerId");
        hand = requireNonBlank(hand, "hand");
        if (slot < -1 || slot >= 9) {
            throw new IllegalArgumentException("slot must be an off-hand sentinel or a hotbar slot");
        }
        itemId = requireNonBlank(itemId, "itemId");
        if (itemCount < 1 || stateRevision < 0L || sequence < CastIntent.SERVER_INITIATED_SEQUENCE) {
            throw new IllegalArgumentException("binding count, revision, and sequence are invalid");
        }
        nbtStateHash = requireNonBlank(nbtStateHash, "nbtStateHash");
        catalogHash = requireNonBlank(catalogHash, "catalogHash");
    }

    /** A re-read must match every server-observed component, not only NBT hash. */
    public boolean matches(CastBinding candidate) {
        return equals(candidate);
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
