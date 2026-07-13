# G00 Test Facilities

## Commands

Run the complete G00 gate from the repository root:

```powershell
.\gradlew.bat verifyG00
```

The task runs the catalog check, JUnit suite, isolated GameTest fixture suite,
dedicated-server smoke test, and production build. `generateSpellCatalog` is
deterministic; a second execution must not change the generated JSON or report.

## JUnit Tags

- `characterization` records observed current behavior and catalog facts.
- `wiki-golden` holds Wiki-defined target behavior. A disabled fixture retains
  its source URL, verification date, exact assertion, and responsible Goal.
- `regression` protects migrations, payload limits, packet codec, sequencing,
  and rate-limit behavior fixed in G00.

Current stable tests cover catalog identity and resources, finite template
values, v0 to v1 NBT migration, future-version rejection, slot and payload
limits, packet round trips, malformed input rejection, replay/order checks,
and independent token buckets.

## GameTest Layout

`src/gametest` is a test-only Fabric mod and is excluded from the release JAR.
It declares ten fixed-tick, empty-structure scenarios for Starter Wand, Spark
Bolt, Bomb, Double Spell, Damage Plus, Trigger, Wand Refresh, Alpha, Gamma,
and payload save/reload. Every fixture resets time to tick zero, observes at
tick 20, and removes entities before completion.

The current evaluator remains coupled to live Minecraft player state, so the
ten world scenarios are invariant fixtures rather than claims that incorrect
current spell semantics are golden behavior. G01 must connect the listed
fixtures to the pure evaluator and replace each invariant with entity, damage,
collision, use-consumption, and persistence assertions.

## Dedicated Server Smoke

`smokeDedicatedServer` uses `build/smoke-server`, writes test-only EULA and
server properties, waits for the server ready log, sends `stop` through local
RCON, and confirms normal saving and listener shutdown. It fails on client
class loading, Mixin failures, registration conflicts, entrypoint errors, time
limits, or non-normal server shutdown. Full evidence remains in
`build/smoke-server/smoke-server.log` on failure.
