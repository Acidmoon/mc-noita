package com.mcnoita.spell.plan;

/** Frozen explosion intent; world mutation policy decides whether terrain may change. */
public record ExplosionEffectNode(String nodePath, double radius, double damage, boolean terrainRequested) implements EffectNode {
    public ExplosionEffectNode {
        EffectNode.requireNodePath(nodePath);
        if (!Double.isFinite(radius) || radius < 0.0 || !Double.isFinite(damage) || damage < 0.0) {
            throw new IllegalArgumentException("explosion values must be finite and non-negative");
        }
    }
}
