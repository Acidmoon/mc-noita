package com.mcnoita.wand.model;

/**
 * A duration expressed in Noita's 60 Hz simulation frames. Keeping this unit
 * in the evaluator prevents server tick conversion from leaking into spell
 * composition rules.
 */
public record NoitaDuration(double frames) {
    public static final double FRAMES_PER_SECOND = 60.0;
    public static final NoitaDuration ZERO = new NoitaDuration(0.0);

    public NoitaDuration {
        if (!Double.isFinite(frames) || frames < 0.0) {
            throw new IllegalArgumentException("frames must be finite and non-negative");
        }
    }

    public static NoitaDuration frames(double frames) {
        return frames == 0.0 ? ZERO : new NoitaDuration(frames);
    }

    public static NoitaDuration seconds(double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0.0) {
            throw new IllegalArgumentException("seconds must be finite and non-negative");
        }
        return frames(seconds * FRAMES_PER_SECOND);
    }

    public NoitaDuration plus(NoitaDuration other) {
        return frames(frames + other.frames);
    }

    public NoitaDuration minusFloorZero(NoitaDuration other) {
        return frames(Math.max(0.0, frames - other.frames));
    }

    public boolean isZero() {
        return frames == 0.0;
    }
}
