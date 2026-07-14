# NBT Versioning And Migration

## Contract

Every Noita-owned persisted structure has `SchemaVersion`. Missing versions are
interpreted as v0 and current writers always emit v3. The structures covered in
G00-G03 are wand templates, wand slots, cast state, frozen projectile payloads,
and persisted projectile entities.

`NoitaNbtSchema` owns the migration chain. v0 to v1 preserves existing field
names and adds the explicit version; v1 to v2 adds the G02 reload-prepared
marker; v2 to v3 normalizes legacy `DEATH` to `EXPIRATION` and conservatively
marks old `TriggerPayloadReleased=true` entity state inert. Future migrations
must add one named transition at a time; compatibility checks must not be
scattered through individual readers.

Versions above the current version, negative versions, malformed version types,
or missing migration steps are rejected without mutating the original input. A wand with unsupported
Noita data cannot cast. Any invalid payload child rejects the full frozen tree.
Invalid persisted projectile data makes the entity inert/safely removed without
releasing it.

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
- Strings: 512 characters, allowing depth-16 readable node paths.
- Payload structural traversal: depth 128 and 8,192 NBT elements; entity NBT
  traversal: depth 136 and 16,384 elements.
- Frozen payload NBT: 128 KiB; complete entity NBT: 256 KiB.
- Logical projectile count: 128; lifetime and trigger delay: 72,000 ticks.
- Execution-facing payload values are bounded: explosion radius 32, trail-light
  stacks 16, bounce count 32, speed 16, and other damage/physics scalars have
  finite hard ceilings before they reach an entity or world executor.

`NoitaNbtSafety` rejects non-finite floating values and validates recursive NBT
before a payload is decoded. `NoitaProjectilePayload` also shares a decode
context across all nested children, validates every nested schema version, and
rejects duplicate node paths, invalid UUIDs, unknown enums, future schemas and
partial trees. A v3 node requires every descendant to remain v3, and all nodes
in one decoded tree must share the root non-zero server execution UUID and
catalog epoch/hash.
Current v3 payloads require every frozen mechanic, identity, TriggerPlan and
runtime-budget field; current v3 entities also require both the frozen tree and
runtime state instead of falling back to legacy projection data. v3 writers do
not emit a duplicate flat `TriggerPayloads` projection beside `FrozenPayload`,
which prevents a near-limit valid tree from being doubled past the entity cap.
v3 Bomb entities also require an explicit `OwnerUuid`; the zero UUID records an
intentional unowned Bomb, while a missing or malformed owner field is inert.
Numeric reader helpers clamp only finite values to documented storage ranges.

## Extension Procedure

1. Add a named migration transition to `NoitaNbtSchema`.
2. Add a representative vN fixture and a round-trip fixture.
3. Keep the old version's key names in the migrator, not in unrelated readers.
4. Add depth, list, future-version, and malformed-value tests when the schema
   gains a recursive or collection field.
