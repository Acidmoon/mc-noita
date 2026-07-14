package com.mcnoita.spell.trigger;

/** Identifies which runtime reservation prevented a frozen release. */
public enum TriggerBudgetExhaustion {
    DIRECT_RELEASE_CAPACITY,
    NESTED_TREE_RESERVATION
}
