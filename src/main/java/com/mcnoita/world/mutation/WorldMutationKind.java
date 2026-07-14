package com.mcnoita.world.mutation;

/** Operations that may allocate, inspect, or mutate authoritative world state. */
public enum WorldMutationKind {
    BLOCK_CHECK,
    BLOCK_MUTATION,
    EXPLOSION,
    ENTITY_QUERY,
    ENTITY_SPAWN
}
