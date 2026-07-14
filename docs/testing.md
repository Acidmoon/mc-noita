# G00-G02 Test Facilities

## Commands

Run the complete G00 gate from the repository root:

```powershell
.\gradlew.bat verifyG00
```

The task runs the catalog check, JUnit suite, isolated GameTest fixture suite,
dedicated-server smoke test, and production build. `generateSpellCatalog` is
deterministic; a second execution must not change the generated JSON or report.

Run the G02 gate from the repository root:

```powershell
.\gradlew.bat verifyG02
```

`verifyG02` additionally runs `runGametest`, which starts Fabric's headless
GameTest server. This is distinct from the isolated `gameTest` JUnit fixture
task: it exercises the test-only Fabric mod inside a real `ServerWorld`.

## JUnit Tags

- `characterization` records observed current behavior and catalog facts.
- `wiki-golden` holds Wiki-defined target behavior. A disabled fixture retains
  its source URL, verification date, exact assertion, and responsible Goal.
- `regression` protects migrations, payload limits, packet codec, sequencing,
  and rate-limit behavior fixed in G00.

Current stable tests cover catalog identity and resources, finite template
values, v0 to v2 NBT migration, future-version rejection, slot and payload
limits, packet round trips, malformed input rejection, replay/order checks,
and independent token buckets.

## GameTest Layout

`src/gametest` is a test-only Fabric mod and is excluded from the release JAR.
It declares eleven fixed-tick, empty-structure scenarios for Starter Wand, Spark
Bolt, Bomb, Double Spell, Damage Plus, Trigger, Wand Refresh, Alpha, Gamma,
payload save/reload, and the G02 two-round wand state machine. The G02 scenario
creates a real server player, equips a three-Spark-Bolt Spells/Cast=2 wand,
casts twice, compares persisted state against pure evaluation, and checks the
actual 2 then 1 projectile spawn result.

Most pre-G02 world fixtures remain narrow lifecycle placeholders. G03 must add
the Trigger/Timer/Piercing/Death collision, persistence, and exactly-once
assertions before those mechanics are described as complete.

## Dedicated Server Smoke

`smokeDedicatedServer` uses `build/smoke-server`, writes test-only EULA and
server properties, waits for the server ready log, sends `stop` through local
RCON, and confirms normal saving and listener shutdown. It fails on client
class loading, Mixin failures, registration conflicts, entrypoint errors, time
limits, or non-normal server shutdown. Full evidence remains in
`build/smoke-server/smoke-server.log` on failure.
