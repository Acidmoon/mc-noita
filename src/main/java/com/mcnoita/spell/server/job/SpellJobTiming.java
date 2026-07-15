package com.mcnoita.spell.server.job;

import com.mcnoita.persistence.NoitaNbtLimits;
import com.mcnoita.wand.model.NoitaDuration;
import java.util.Objects;

/** Single named boundary from Noita's 60 Hz duration to server ticks. */
final class SpellJobTiming {
    private static final double NOITA_FRAMES_PER_SERVER_TICK = NoitaDuration.FRAMES_PER_SECOND / 20.0;

    private SpellJobTiming() {
    }

    static long toServerTicks(NoitaDuration duration) {
        Objects.requireNonNull(duration, "duration");
        double converted = Math.ceil(duration.frames() / NOITA_FRAMES_PER_SERVER_TICK);
        if (!Double.isFinite(converted) || converted < 1.0 || converted > NoitaNbtLimits.MAX_SPELL_JOB_LIFETIME_TICKS) {
            throw new IllegalArgumentException("persistent job duration is outside the server tick limit");
        }
        return (long) converted;
    }
}
