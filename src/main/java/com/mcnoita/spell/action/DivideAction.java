package com.mcnoita.spell.action;

/** Parameterized Divide By action values sourced from Noita definitions. */
public record DivideAction(
    int copies,
    int iterationThreshold,
    double castDelayFrames,
    double rechargeFrames,
    double damagePenalty,
    double explosionRadiusPenalty,
    double patternDegrees
) implements SpellAction {
    public DivideAction {
        if (copies < 1 || iterationThreshold < 0 || !Double.isFinite(castDelayFrames)
            || !Double.isFinite(rechargeFrames) || !Double.isFinite(damagePenalty)
            || !Double.isFinite(explosionRadiusPenalty) || !Double.isFinite(patternDegrees)) {
            throw new IllegalArgumentException("divide parameters must be finite and bounded");
        }
    }
}
