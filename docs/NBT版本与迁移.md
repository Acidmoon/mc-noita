# NBT Versioning And Migration

## Contract

Every Noita-owned persisted structure has `SchemaVersion`. Missing versions are
interpreted as v0 and current writers always emit v1. The structures covered in
G00 are wand templates, wand slots, cast state, projectile payloads, and
persisted projectile entities.

`NoitaNbtSchema` owns the migration chain. Its v0 to v1 migration preserves
existing field names and adds the explicit version. Future migrations must add
one named transition at a time, for example v1 to v2; compatibility checks must
not be scattered through individual readers.

Versions above the current version, negative versions, or missing migration
steps are rejected without mutating the original input. A wand with unsupported
Noita data cannot cast. Invalid payloads are omitted and invalid persisted
projectile data makes the entity inert instead of recursively decoding it.

## Bounds

`NoitaNbtLimits` currently enforces these decode limits:

- Wand capacity and configured slots: 64.
- Always-cast IDs: 16.
- Cast-state slots: 64, unique and in range.
- Payload depth: 16; total payload nodes: 128; immediate children: 32.
- Modifier effects: 64.
- Strings: 128 characters.
- Entity NBT traversal: depth 32 and 2048 nodes.
- Logical projectile count: 128; lifetime and trigger delay: 72,000 ticks.

`NoitaNbtSafety` rejects non-finite floating values and validates recursive NBT
before a payload is decoded. Numeric reader helpers clamp only finite values to
documented storage ranges.

## Extension Procedure

1. Add a named migration transition to `NoitaNbtSchema`.
2. Add a representative vN fixture and a round-trip fixture.
3. Keep the old version's key names in the migrator, not in unrelated readers.
4. Add depth, list, future-version, and malformed-value tests when the schema
   gains a recursive or collection field.
