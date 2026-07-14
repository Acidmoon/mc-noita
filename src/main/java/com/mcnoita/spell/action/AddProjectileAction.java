package com.mcnoita.spell.action;

import com.mcnoita.spell.plan.ProjectileDefinition;
import java.util.Objects;

/** Adds a fully resolved projectile node to the current effect-plan branch. */
public record AddProjectileAction(ProjectileDefinition projectile) implements SpellAction {
    public AddProjectileAction {
        Objects.requireNonNull(projectile, "projectile");
    }
}
