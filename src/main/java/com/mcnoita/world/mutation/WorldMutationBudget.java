package com.mcnoita.world.mutation;

/**
 * Bridge to the future central SpellBudgetManager. G05-7 keeps the hook at
 * every world boundary so replacing the local default does not require another
 * entity-wide migration.
 */
@FunctionalInterface
public interface WorldMutationBudget {
    WorldMutationBudget UNTRACKED = (kind, amount) -> true;

    boolean tryReserve(WorldMutationKind kind, int amount);
}
