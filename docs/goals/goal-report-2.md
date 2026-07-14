# Goal Report 2: G01 Pure Wand Evaluator

## Objective

G01 required a deterministic, world-independent cast calculation that accepts a wand definition, wand state, spell catalog, elapsed time and root seed, then returns the complete next state and a frozen execution plan.

## Delivered artifacts

- New pure model packages: `wand/model`, `wand/eval`, `spell/action` and `spell/plan`.
- Immutable `WandDefinition`, per-card `SpellCardState`, conserving `DeckState`, persistent `WandState`, Noita-frame `NoitaDuration`, immutable `ShotState`, deterministic `CastRng`, `WandCastSession`, budgets, diagnostics, `ResolvedCast` and `EffectPlan`.
- Explicit action catalog bridge in `LegacySpellCatalogAdapter`; it maps existing registered spell templates without changing registry IDs.
- `MinecraftWandAdapter` and `LegacyWandStateAdapter` retain existing wand NBT keys while translating to and from pure values.
- `MinecraftEffectExecutor` consumes only frozen `EffectPlan` nodes after state commit and isolates/limits failure logging per node.
- `NoitaWandCaster` is now the server sequence `validate -> adapt -> evaluate -> commit -> execute` rather than a mixed evaluator/world service.
- `docs/纯法杖解释器.md` documents boundaries, flow, time, random strategy, budgets, failure behavior, compatibility and extension procedure.

## Verification

- `test` runs real Wiki golden fixtures instead of G01-disabled placeholder assertions.
- Pure tests cover deterministic output, independent shuffle substreams, card conservation, mana/use failure, Trigger Shot isolation, payload Recharge globality, random candidate filtering, recursion/action budget termination and immutable outputs.
- `PureEvaluatorArchitectureTest` blocks forbidden Minecraft/Fabric/execution imports in pure packages.
- `LegacyWandStateAdapterTest` round-trips legacy slot Deck/Discard data, mana and remaining uses without bootstrapping a Minecraft world.

## Guidance for the next Goals

G02 should replace the temporary basic Draw/Wrap/Reload and limited-use policies with the exact Wiki fixtures while retaining the new model boundaries. In particular, action retry, end-of-deck behavior, initial Draw versus action Draw, Always Cast exceptions, hand-discard consumption and concurrent cooldown presentation need focused tests.

G03 should use `CallSpellAction`, `BeginTriggerAction` and `ProjectilePlan` as the stable foundation for full Greek/Copy/Divide/Add Trigger policy, recursive action metadata and robust Timer/Piercing/Death persistence semantics. It must not move Minecraft classes back into the pure packages.

Later executor work must add server-wide budget reservation, world-mutation policy, protected-block checks and persistent one-shot execution records before destructive multiplayer effects are enabled.
