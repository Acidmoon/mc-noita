package com.mcnoita.wand.adapter;

import com.mcnoita.wand.model.NoitaDuration;

/** The sole conversion boundary between pure Noita frames and Minecraft ticks. */
public final class MinecraftTimeAdapter {
    private static final double NOITA_FRAMES_PER_MINECRAFT_TICK = 3.0;

    private MinecraftTimeAdapter() {
    }

    public static NoitaDuration fromMinecraftTicks(long ticks) {
        return NoitaDuration.frames(Math.max(0L, ticks) * NOITA_FRAMES_PER_MINECRAFT_TICK);
    }

    public static int toMinecraftTicks(NoitaDuration duration, int minimumTicks) {
        if (minimumTicks < 0) {
            throw new IllegalArgumentException("minimumTicks must not be negative");
        }
        double ticks = Math.ceil(duration.frames() / NOITA_FRAMES_PER_MINECRAFT_TICK);
        return (int) Math.min(Integer.MAX_VALUE, Math.max(minimumTicks, ticks));
    }
}
