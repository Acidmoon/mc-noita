package com.mcnoita.spell.action;

/** Marker for explicit pure evaluator actions. */
public sealed interface SpellAction permits AddProjectileAction, BeginTriggerAction, CallSpellAction, DrawAction,
    DuplicateHandAction, ModifyShotAction, RandomSpellAction, RefreshWandAction, TimingAction {
}
