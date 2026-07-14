package com.mcnoita.spell.plan;

import java.util.Objects;

/** Typed root-plan node that materializes one frozen projectile definition. */
public record ProjectileEffectNode(String nodePath, ProjectilePlan projectile) implements EffectNode {
    public ProjectileEffectNode {
        EffectNode.requireNodePath(nodePath);
        projectile = Objects.requireNonNull(projectile, "projectile");
        if (!nodePath.equals(projectile.nodePath())) {
            throw new IllegalArgumentException("projectile effect path must match the frozen projectile path");
        }
    }

    public ProjectileEffectNode(ProjectilePlan projectile) {
        this(Objects.requireNonNull(projectile, "projectile").nodePath(), projectile);
    }
}
