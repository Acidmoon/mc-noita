package com.mcnoita.spell.trigger;

import com.mcnoita.persistence.NoitaNbtLimits;
import java.util.Objects;

/**
 * A bounded identity for one physical collision. The controller retains only
 * the most recent key, which de-duplicates Minecraft's entity-hit/collision
 * callback pair without retaining an unbounded history for piercing shots.
 */
public record CollisionKey(long serverTick, String target, String face, String nodePath) {
    public CollisionKey {
        target = requireBounded(target, "target");
        face = requireBounded(face, "face");
        nodePath = requireBounded(nodePath, "nodePath");
    }

    private static String requireBounded(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank() || value.length() > NoitaNbtLimits.MAX_STRING_LENGTH) {
            throw new IllegalArgumentException(name + " must be a nonblank bounded value");
        }
        return value;
    }
}
