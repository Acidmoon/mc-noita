# NBT Versioning And Migration

## Contract

Every Noita-owned persisted structure has `SchemaVersion`. Missing versions are
interpreted as v0 and current writers always emit v5. The covered structures are
wand templates, wand slots, cast state, frozen projectile payloads, persisted
projectile entities, and G05 `SpellJobPersistentState` records.

`NoitaNbtSchema` owns the migration chain. v0 to v1 preserves existing field
names and adds the explicit version; v1 to v2 adds the G02 reload-prepared
marker; v2 to v3 normalizes legacy `DEATH` to `EXPIRATION` and conservatively
marks old `TriggerPayloadReleased=true` entity state inert; v3 to v4 replaces
frozen scalar projectile damage with a validated `DamageProfile` compound. v4
to v5 adds the durable job boundary. Because v4 had no executable job grammar,
an attempted legacy `SPELL_JOB` record is explicitly marked `INERT` rather than
being guessed into a world operation. For non-job structures, v4 to v5 advances
the common schema marker without reinterpreting frozen mechanics.

`DamageProfile` is the v4 compatibility bridge, not a second damage source.
Historic scalar `Damage` migrates only to the `PROJECTILE` channel. Current v5
payload writers retain a scalar `Damage` mirror for legacy readers and
diagnostics, but the profile is mechanically authoritative and the v5 decoder
rejects a missing or mismatching mirror. A migration must add one named
transition at a time; compatibility checks must not be scattered through
individual readers.

Versions above the current version, negative versions, malformed version types,
or missing migration steps are rejected without mutating the original input. A wand with unsupported
Noita data cannot cast. Any invalid payload child rejects the full frozen tree.
Invalid persisted projectile data makes the entity inert/safely removed without
releasing it.

Temporary visual cleanup uses a separate overworld `PersistentState` ledger rather than the spell schema. Current v2 records contain dimension, block position, expiry tick, and the expected light level for a pending `minecraft:light` removal. v1 records decode with an unknown level for one compatibility cleanup pass; v2 avoids deleting a later writer's different-level light. Future ledgers are read-only and block new temporary-light placement, while malformed trees and more than 4,096 records are rejected; recovery never force-loads a chunk.

## Bounds

`NoitaNbtLimits` currently enforces these decode limits:

- Wand capacity and configured slots: 64.
- Always-cast IDs: 16.
- Cast-state slots: 64, unique and in range.
- Payload depth: 16; total payload nodes: 128; immediate children: 32.
  The pure `PayloadPlan` applies the same 32-node limit before a cast commits,
  so an accepted trigger shot can always be serialized into one bounded NBT list.
- Modifier effects: 64. Structural NBT traversal permits lists up to 64, then
  field-specific decoders retain the stricter 32-child payload-shot limit.
- Persisted jobs: 64 records, 4,096 steps per record, 32 frozen parameters,
  depth 16, 512 NBT nodes, 16 KiB per record, and a 72,000-tick maximum
  lifetime. The overworld store rejects an oversized record list before any
  record is scheduled.
- Strings: 512 characters, allowing depth-16 readable node paths.
- Payload structural traversal: depth 128 and 8,192 NBT elements; entity NBT
  traversal: depth 136 and 16,384 elements.
- Frozen payload NBT: 128 KiB; complete entity NBT: 256 KiB.
- Logical projectile count: 128; lifetime and trigger delay: 72,000 ticks.
- Frozen `TriggerRuntimeBudget`: 0 to 128 release events and spawned entities.
  The legacy bootstrap default remains 32/32, but current v5 readers preserve
  a higher centrally configured per-cast ceiling instead of clamping it back
  to that default after save/load.
- Execution-facing payload values are bounded: every `DamageProfile` channel is
  finite, non-negative, and no greater than 100,000; explosion radius 32, trail-light
  stacks 16, bounce count 32, speed 16, and other damage/physics scalars have
  finite hard ceilings before they reach an entity or world executor.

`NoitaNbtSafety` rejects non-finite floating values and validates recursive NBT
before a payload is decoded. `NoitaProjectilePayload` also shares a decode
context across all nested children, validates every nested schema version, and
rejects duplicate node paths, invalid UUIDs, unknown enums, future schemas and
partial trees. A v3+ frozen node requires every descendant to remain frozen; a
current v5 root additionally requires every descendant to have been written as
v5, so a newly authored tree cannot silently contain an older frozen child. All
nodes in one decoded tree must share the root non-zero server execution UUID and
catalog epoch/hash.

Current v5 payloads require every frozen mechanic, identity, TriggerPlan,
runtime-budget field, `DamageProfile`, and a matching scalar `Damage` mirror;
current v3+ entities also require both the frozen tree and runtime state instead
of falling back to legacy projection data. v4 introduced the single-authority
`FrozenPayload` layout and explicit Bomb `OwnerUuid`; both rules remain in v5.
The zero owner UUID records an intentional unowned Bomb, while a missing or
malformed owner field is inert. Numeric reader helpers clamp only finite values
to documented storage ranges.

## Persistent Jobs

`SpellJobPersistentStateNbtCodec` is the strict v5 grammar for cross-tick
work. It records execution and owner UUIDs, dimension and target chunk, catalog
epoch/hash, a frozen node definition, cursor, remaining hard budget, lifecycle
state/reason, and creation/expiry ticks. The cursor must exactly match the
persisted remaining budget; a record cannot forge extra steps after reload.

The codec decodes a private NBT copy and rejects future, corrupt, oversized, or
unknown job records as a whole. The overworld `SpellJobPersistentStateStore`
retains a structurally identifiable bad record only as an inert diagnostic
state; unidentifiable records are omitted before the scheduler sees them. A
recovered `RUNNING` job is retried only when both its frozen node and live
handler declare recovery idempotence. Otherwise it remains inert. This is a
bounded recovery policy, not a claim of crash-safe exactly-once world effects.

## Extension Procedure

1. Add a named migration transition to `NoitaNbtSchema`.
2. Add a representative vN fixture and a round-trip fixture.
3. Keep the old version's key names in the migrator, not in unrelated readers.
4. Add depth, list, future-version, and malformed-value tests when the schema
   gains a recursive or collection field.
5. For a durable job field, add codec, recovery, owner/chunk gate, hard-budget,
   and non-idempotent `RUNNING` recovery fixtures before registering a handler.
