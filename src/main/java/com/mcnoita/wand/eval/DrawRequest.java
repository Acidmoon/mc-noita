package com.mcnoita.wand.eval;

import java.util.Objects;

/**
 * Explicit draw command consumed by {@link WandCastSession}. The origin, not a
 * loosely related boolean, owns the Wrap policy so new draw sites are forced to
 * choose their Noita semantics deliberately.
 */
public record DrawRequest(DrawOrigin origin, int amount) {
    public DrawRequest {
        Objects.requireNonNull(origin, "origin");
        if (amount < 1) {
            throw new IllegalArgumentException("draw amount must be at least one");
        }
    }

    public static DrawRequest initial(int amount) {
        return new DrawRequest(DrawOrigin.INITIAL, amount);
    }

    public static DrawRequest action(int amount) {
        return new DrawRequest(DrawOrigin.ACTION, amount);
    }

    public static DrawRequest payload(int amount) {
        return new DrawRequest(DrawOrigin.PAYLOAD, amount);
    }

    public static DrawRequest permanent(int amount) {
        return new DrawRequest(DrawOrigin.PERMANENT, amount);
    }

    public boolean allowsWrap() {
        return origin != DrawOrigin.INITIAL;
    }
}
