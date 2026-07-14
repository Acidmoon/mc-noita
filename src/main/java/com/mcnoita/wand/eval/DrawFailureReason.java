package com.mcnoita.wand.eval;

/** Reason why a removed Deck card moved directly to Discard instead of Hand. */
public enum DrawFailureReason {
    DEPLETED_USES,
    INSUFFICIENT_MANA,
    UNKNOWN_SPELL
}
