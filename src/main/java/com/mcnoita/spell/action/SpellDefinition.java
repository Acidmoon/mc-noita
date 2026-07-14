package com.mcnoita.spell.action;

import java.util.List;
import java.util.Objects;

/** Immutable catalog definition consumed by the pure evaluator. */
public record SpellDefinition(
    String id,
    SpellCategory category,
    int manaCost,
    boolean recursive,
    List<SpellAction> actions,
    UseConsumptionPolicy useConsumptionPolicy
) {
    public SpellDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("spell id must not be blank");
        }
        Objects.requireNonNull(category, "category");
        actions = List.copyOf(Objects.requireNonNull(actions, "actions"));
        Objects.requireNonNull(useConsumptionPolicy, "useConsumptionPolicy");
    }

    public SpellDefinition(String id, SpellCategory category, int manaCost, boolean recursive, List<SpellAction> actions) {
        this(id, category, manaCost, recursive, actions, defaultUseConsumptionPolicy(category));
    }

    private static UseConsumptionPolicy defaultUseConsumptionPolicy(SpellCategory category) {
        return switch (category) {
            case OTHER, UTILITY -> UseConsumptionPolicy.ALWAYS_ON_HAND_DISCARD;
            case PASSIVE -> UseConsumptionPolicy.NEVER;
            default -> UseConsumptionPolicy.WHEN_PROJECTILE_SHOT;
        };
    }
}
