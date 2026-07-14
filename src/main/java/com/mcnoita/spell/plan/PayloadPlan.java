package com.mcnoita.spell.plan;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * One isolated Trigger ShotState result. It can contain several frozen logical
 * projectile nodes, for example a multicast payload or Double Trigger draw.
 */
public record PayloadPlan(String nodePath, int depth, List<ProjectilePlan> projectiles) {
    /** Matches the persisted PayloadPlan child-list limit. */
    public static final int MAX_PROJECTILES_PER_SHOT = 32;

    public PayloadPlan {
        if (nodePath == null || nodePath.isBlank()) {
            throw new IllegalArgumentException("payload node path must not be blank");
        }
        if (depth < 1) {
            throw new IllegalArgumentException("payload depth must be at least one");
        }
        projectiles = List.copyOf(Objects.requireNonNull(projectiles, "projectiles"));
        if (projectiles.size() > MAX_PROJECTILES_PER_SHOT) {
            throw new IllegalArgumentException("payload shot exceeds the persisted projectile child limit");
        }

        Set<String> nodePaths = new HashSet<>();
        for (ProjectilePlan projectile : projectiles) {
            if (!projectile.nodePath().startsWith(nodePath + "/") || !nodePaths.add(projectile.nodePath())) {
                throw new IllegalArgumentException("payload projectile paths must be unique descendants");
            }
        }
    }
}
