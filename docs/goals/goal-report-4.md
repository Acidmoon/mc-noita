# Goal Report 4: G03 Frozen Trigger Payload Trees

## Objective

G03 required Trigger payloads to be selected, charged and resolved during the
wand cast, then released by Minecraft entities from a frozen tree without any
runtime Draw, mana charge or SpellCatalog lookup. The scope includes Hit,
Timer and Expiration lifecycle behavior, persistent runtime state, bounded
repeat release, a shared controller, NBT migration, tests, documentation and a
verification gate. Greek/Copy/Divide/Add Trigger search policies remain out of
scope.

## Delivered Artifacts

- Pure `TriggerPlan`, `PayloadPlan` and `TriggerReleasePolicy` replace the
  scattered Trigger fields on `ProjectilePlan`; every logical node has a
  deterministic `nodePath`.
- `WandCastSession` freezes payload Draw results during cast evaluation, uses a
  fresh payload `ShotState`, preserves global Recharge, and independently
  limits payload node count and payload depth.
- `ResolvedCast.catalogEpoch/hash` now reaches `MinecraftEffectExecutor`.
  The executor creates a server-authoritative execution UUID after WandState
  commit and freezes it with catalog metadata into every entity payload.
- `NoitaProjectilePayload` schema v3 stores the complete adapted trigger tree,
  execution identity, catalog metadata and runtime budget. Decode uses a shared
  tree context and rejects partial/corrupt trees atomically. A current-schema
  tree requires v3 descendants, one non-zero execution UUID, and one catalog
  epoch/hash; legacy runtime constructors are bound and re-pathed at the
  server entity boundary before their first v3 save.
- `TriggerPayloadController`, `TriggerRuntimeState`, `ReleaseDecision` and
  `TriggerPayloadSpawner` provide one shared release state machine for Spark
  Bolt and Bomb. Parent and child entities receive disjoint budget shares after
  every release.
- Spark and Bomb now persist trigger plans, runtime progress and owner data.
  Timer elapsed ticks are persisted separately from entity age so reloads do
  not restart Timer delays. Legacy `DEATH` migrates to `EXPIRATION`; legacy
  `TriggerPayloadReleased=true` becomes inert to avoid replay. v3 writes one
  authoritative `FrozenPayload` instead of duplicating it as a legacy flat
  projection; Bomb writes an explicit owner marker, including a zero UUID for
  intentionally unowned instances.
- New pure/runtime/NBT tests cover payment, Shot State isolation, node paths,
  node/depth/width bounds, modifier-list bounds, Piercing collision behavior,
  Timer collision plus expiry, Expiration one-shot behavior, malformed trees,
  future/downgraded schemas, mixed identities and runtime state round trips.
- The pure evaluator rejects more than 64 modifier effects before WandState
  commit, matching the frozen NBT limit and preventing an accepted cast from
  becoming unloadable after its first entity save/reload.
- Fabric GameTests now execute real block and entity collisions, sequential
  Piercing hits, nested Trigger release, Timer 0/1 tick behavior, same-tick
  Piercing collision plus Timer expiry, natural/KILLED Expiration, pre/post
  Timer-expiry reload, `UNLOADED_TO_CHUNK` lifecycle removal, and legacy
  runtime identity binding.
- `docs/Trigger载荷树与运行时.md` documents the model and lifecycle;
  `docs/NBT版本与迁移.md`, `docs/testing.md`, and `docs/纯法杖解释器.md`
  were updated. `verifyG03` is the new full gate.

## Verification

- `./gradlew.bat test --rerun-tasks --no-daemon --console=plain` passed.
- `./gradlew.bat gameTest --rerun-tasks --no-daemon --console=plain` passed.
- `./gradlew.bat runGametest --no-daemon --console=plain` passed with 25/25
  Fabric GameTests, zero failures, errors and skips.
- `./gradlew.bat smokeDedicatedServer --rerun-tasks --no-daemon --console=plain`
  passed; the dedicated server reached ready state, accepted RCON stop, saved
  all dimensions and stopped normally.
- `./gradlew.bat verifyG03 --rerun-tasks --no-daemon --console=plain` passed,
  including catalog validation, JUnit, both GameTest paths, dedicated-server
  smoke and the production build.
- `git diff --check` passed before final staging.

## Guarantees And Limits

Frozen payload NBT is limited to 128 KiB. Complete entity NBT is limited to
256 KiB; logical payload depth/nodes/direct children are limited to 16/128/32,
modifier effects to 64, and structural traversal to 128 depth/8,192 nodes for
payloads and 136 depth/16,384 nodes for entities. Invalid or partial trees are
rejected atomically and the entity becomes inert or is safely removed without
release.

During normal save/unload/reload operation, persisted `timerExpired` and
`expirationReleased` flags provide at-most-once final Timer/Expiration release;
Hit/Timer Piercing collisions may release repeatedly only for distinct valid
collisions. `UNLOAD`, invalid data and administrative cleanup do not themselves
release Expiration payloads. This is not crash-safe exactly-once: a process
crash after child spawning but before durable parent-state save can replay an
event. A persistent execution ledger and transaction protocol are required to
close that window.

Runtime budgets currently bound one frozen tree's release events and entity
fan-out. Cross-player, chunk, dimension and global time-window reservation,
world-mutation permissions, and all Copy/Call selection policy remain future
work. The GameTest suite exercises Minecraft's `UNLOADED_TO_CHUNK` removal
reason, but full chunk-I/O stress, disconnect recovery and crash recovery stay
in later lifecycle work. New trigger-capable entities should delegate to
`TriggerPayloadController` and must not reopen WandState or SpellCatalog at
impact time.
