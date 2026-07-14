package com.mcnoita.spell.action;

/** Draws more cards through the evaluator's normal draw path. */
public record DrawAction(int amount) implements SpellAction {
    public DrawAction {
        if (amount < 1) {
            throw new IllegalArgumentException("draw amount must be at least one");
        }
    }
}
