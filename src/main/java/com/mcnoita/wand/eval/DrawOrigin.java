package com.mcnoita.wand.eval;

/** Identifies the rule set that requested a normal card draw. */
public enum DrawOrigin {
    /** The wand's Spells/Cast draw. It is never allowed to initiate Wrap. */
    INITIAL,
    /** A spell action such as a modifier or multicast requested more cards. */
    ACTION,
    /** A Trigger/Timer payload resolves its pre-paid cards during the outer cast. */
    PAYLOAD,
    /** A permanent card requested more cards after its special one-draw suppression. */
    PERMANENT
}
