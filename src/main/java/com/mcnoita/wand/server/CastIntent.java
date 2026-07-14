package com.mcnoita.wand.server;

import java.util.Objects;
import net.minecraft.util.Hand;

/** Server-side cast request after packet parsing but before stack validation. */
public record CastIntent(Hand hand, int slot, long sequence, ClientCastBinding clientBinding) {
    /** Direct server calls have no client packet sequence but still bind one stable sentinel. */
    public static final long SERVER_INITIATED_SEQUENCE = -1L;

    public CastIntent {
        hand = Objects.requireNonNull(hand, "hand");
        if (sequence < SERVER_INITIATED_SEQUENCE) {
            throw new IllegalArgumentException("cast sequence must be non-negative or server initiated");
        }
        if (hand == Hand.MAIN_HAND && (slot < 0 || slot >= 9)) {
            throw new IllegalArgumentException("main-hand casts require a hotbar slot");
        }
        if (hand == Hand.OFF_HAND && slot != -1) {
            throw new IllegalArgumentException("off-hand casts must use slot -1");
        }
    }

    /** Compatibility constructor for direct server calls that have no C2S binding. */
    public CastIntent(Hand hand, int slot, long sequence) {
        this(hand, slot, sequence, null);
    }

    public static CastIntent mainHand(int slot) {
        return new CastIntent(Hand.MAIN_HAND, slot, SERVER_INITIATED_SEQUENCE);
    }

    public static CastIntent clientMainHand(int slot, long sequence, ClientCastBinding clientBinding) {
        return new CastIntent(Hand.MAIN_HAND, slot, sequence, clientBinding);
    }
}
