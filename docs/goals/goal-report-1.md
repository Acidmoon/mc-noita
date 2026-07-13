# Goal Report 1: G00 Baseline Facilities

## Baseline

The work began from `db88b41` on `main` with a clean worktree. The previous
core command, `./gradlew.bat test`, succeeded but reported `NO-SOURCE`; it did
not provide behavior coverage.

## Delivered Artifacts

- `docs/baseline/spell-catalog.json` records all 315 registered spells with
  registry ID, Noita ID, category, Wiki source, resource status, executor
  metadata, current conservative implementation level, verification IDs,
  adaptation notes, and follow-up Goal.
- `tools/generate-spell-catalog.ps1` is the single source-to-report generator.
  It generates `docs/实现覆盖率.md`; `resource-allowlist.json` documents the
  three intentional non-spell model exceptions.
- JUnit Jupiter and tag categories are configured in Gradle. Stable G00 tests
  cover catalog integrity, numeric bounds, NBT migration and limits, packet
  codec, replay/order checks, and rate limits. Wiki golden fixtures remain
  disabled with explicit G01 ownership rather than being removed.
- `src/gametest` provides a release-excluded Fabric GameTest mod and ten
  deterministic scenario fixtures covering the requested named spells.
- `smokeDedicatedServer` starts Fabric from `build/smoke-server`, validates the
  real dedicated-server startup path, sends RCON `stop`, and retains evidence.
- Noita persistence now has a central v0-to-v1 migration boundary and bounded
  decoding for template, slots, cast state, payload, and entities.
- Networking now uses versioned minimal intent requests, state-hash staleness
  detection, sequence guards, separate token buckets, finite hover validation,
  and versioned/clamped HUD sync.

## Verification Status

The focused `test`, `gameTest`, `checkSpellCatalog`, static dedicated-server
architecture check, and real `smokeDedicatedServer` tasks pass during this
Goal. `verifyG00` is the repeatable aggregate command.

## How This Guides G01

G01 must move wand evaluation into pure Java state and use the existing Wiki
golden fixtures as executable contracts. The ten GameTest fixtures are the
first integration targets: add actual mocked-player casts, entity ownership,
damage, collisions, trigger release, use consumption, and save/reload checks
without changing their fixed-tick cleanup discipline. Catalog entries may only
move from `approximate` to `verified` when those behavior tests and source
metadata exist.
