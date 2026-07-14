package com.mcnoita.spell.plan;

import com.mcnoita.wand.model.NoitaDuration;
import java.util.Objects;

/** Bounded persistent-area intent, kept pure until a field executor is supplied. */
public record FieldEffectNode(String nodePath, FieldKind kind, double radius, NoitaDuration duration) implements EffectNode {
    public enum FieldKind {
        GENERIC,
        DAMAGE,
        STATUS,
        ATTRACT,
        REPEL
    }

    public FieldEffectNode {
        EffectNode.requireNodePath(nodePath);
        kind = Objects.requireNonNull(kind, "kind");
        duration = Objects.requireNonNull(duration, "duration");
        if (!Double.isFinite(radius) || radius < 0.0) {
            throw new IllegalArgumentException("field radius must be finite and non-negative");
        }
    }
}
