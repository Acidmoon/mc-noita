package com.mcnoita.wand.model;

import java.util.List;
import java.util.Objects;

/** Immutable wand properties that do not change while evaluating a cast. */
public record WandDefinition(
    boolean shuffle,
    int spellsPerCast,
    NoitaDuration castDelay,
    NoitaDuration rechargeTime,
    int manaMax,
    double manaChargePerSecond,
    int capacity,
    double spreadDegrees,
    double speedMultiplier,
    List<String> alwaysCastSpellIds
) {
    public WandDefinition {
        Objects.requireNonNull(castDelay, "castDelay");
        Objects.requireNonNull(rechargeTime, "rechargeTime");
        Objects.requireNonNull(alwaysCastSpellIds, "alwaysCastSpellIds");
        if (spellsPerCast < 1) {
            throw new IllegalArgumentException("spellsPerCast must be at least one");
        }
        if (manaMax < 0 || capacity < 1) {
            throw new IllegalArgumentException("manaMax and capacity must be non-negative and positive respectively");
        }
        if (!Double.isFinite(manaChargePerSecond) || manaChargePerSecond < 0.0
            || !Double.isFinite(spreadDegrees) || !Double.isFinite(speedMultiplier) || speedMultiplier <= 0.0) {
            throw new IllegalArgumentException("wand numeric properties must be finite and valid");
        }
        alwaysCastSpellIds = List.copyOf(alwaysCastSpellIds);
    }
}
