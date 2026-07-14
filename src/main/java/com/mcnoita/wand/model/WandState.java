package com.mcnoita.wand.model;

import java.util.Objects;

/**
 * Immutable persistent evaluator state. Cooldown values are remaining Noita
 * frames, never absolute Minecraft world ticks.
 */
public record WandState(
    DeckState deckState,
    double mana,
    NoitaDuration castDelayRemaining,
    NoitaDuration rechargeRemaining,
    boolean rechargePending,
    long revision,
    int stateHash
) {
    public WandState {
        Objects.requireNonNull(deckState, "deckState");
        Objects.requireNonNull(castDelayRemaining, "castDelayRemaining");
        Objects.requireNonNull(rechargeRemaining, "rechargeRemaining");
        if (!Double.isFinite(mana) || mana < 0.0 || revision < 0) {
            throw new IllegalArgumentException("wand state values must be finite and non-negative");
        }
    }

    public boolean isReady() {
        return castDelayRemaining.isZero() && rechargeRemaining.isZero();
    }
}
