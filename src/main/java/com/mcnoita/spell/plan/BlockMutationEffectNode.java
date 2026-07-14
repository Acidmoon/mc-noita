package com.mcnoita.spell.plan;

import java.util.Objects;

/** Frozen terrain intent; the later executor must route it through WorldMutationPolicy. */
public record BlockMutationEffectNode(String nodePath, MutationKind kind, int maximumBlocks, double radius) implements EffectNode {
    public enum MutationKind {
        BREAK,
        PLACE,
        REPLACE
    }

    public BlockMutationEffectNode {
        EffectNode.requireNodePath(nodePath);
        kind = Objects.requireNonNull(kind, "kind");
        if (maximumBlocks < 1 || !Double.isFinite(radius) || radius < 0.0) {
            throw new IllegalArgumentException("block mutation limits must be finite and positive");
        }
    }
}
