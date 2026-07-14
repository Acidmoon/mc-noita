package com.mcnoita.spell.action;

/** Direct global timing changes used by legacy utility/copy actions. */
public record TimingAction(double castDelayFrames, double rechargeFrames) implements SpellAction {
    public TimingAction {
        if (!Double.isFinite(castDelayFrames) || !Double.isFinite(rechargeFrames)) {
            throw new IllegalArgumentException("timing changes must be finite");
        }
    }
}
