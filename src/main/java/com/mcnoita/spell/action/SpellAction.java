package com.mcnoita.spell.action;

/** Marker for explicit pure evaluator actions. */
public sealed interface SpellAction permits AddProjectileAction, AddTriggerToNextProjectileAction, BeginTriggerAction,
    CallSpellAction, DivideAction, DrawAction, DuplicateHandAction, GreekCopyAction, ModifyShotAction,
    RandomSpellAction, RefreshWandAction, TimingAction {
}
