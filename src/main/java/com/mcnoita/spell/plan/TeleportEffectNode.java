package com.mcnoita.spell.plan;

/** Frozen teleport intent. The executor must still search a collision-safe destination. */
public record TeleportEffectNode(String nodePath, double maximumDistance, boolean requireSafeDestination) implements EffectNode {
    public TeleportEffectNode {
        EffectNode.requireNodePath(nodePath);
        if (!Double.isFinite(maximumDistance) || maximumDistance < 0.0) {
            throw new IllegalArgumentException("teleport distance must be finite and non-negative");
        }
    }
}
