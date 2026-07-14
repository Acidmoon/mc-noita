package com.mcnoita.spell.action;

/** Direct global timing changes used by legacy utility/copy actions. */
public record TimingAction(
    TimingOperation castDelayOperation,
    double castDelayFrames,
    TimingOperation rechargeOperation,
    double rechargeFrames
) implements SpellAction {
    public TimingAction {
        if (castDelayOperation == null || rechargeOperation == null
            || !Double.isFinite(castDelayFrames) || !Double.isFinite(rechargeFrames)) {
            throw new IllegalArgumentException("timing changes must be finite");
        }
    }

    public TimingAction(double castDelayFrames, double rechargeFrames) {
        this(TimingOperation.ADD, castDelayFrames, TimingOperation.ADD, rechargeFrames);
    }
}
