# G00-G03 Test Facilities

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

Run the G03 gate from the repository root:

```powershell
.\gradlew.bat verifyG03
```

`verifyG03` runs catalog validation, the complete JUnit suite, isolated
GameTest fixtures, the headless Fabric GameTest server, dedicated-server smoke,
and the production build.

## JUnit Tags

- `characterization` records observed current behavior and catalog facts.
- `wiki-golden` holds Wiki-defined target behavior. A disabled fixture retains
  its source URL, verification date, exact assertion, and responsible Goal.
- `regression` protects migrations, payload limits, packet codec, sequencing,
  and rate-limit behavior fixed in G00.

Current stable tests cover catalog identity and resources, finite template
values, v0 to v3 NBT migration, future-version rejection, slot and frozen-tree
limits, packet round trips, malformed input rejection, replay/order checks,
independent token buckets, Trigger prepayment, Shot State isolation, runtime
release decisions, and frozen payload serialization.

## GameTest Layout

`src/gametest` is a test-only Fabric mod and is excluded from the release JAR.
It declares twenty-five fixed-tick, empty-structure scenarios for Starter Wand,
Spark Bolt, Bomb, Double Spell, Damage Plus, Spark/Bomb Trigger block/entity
hits, sequential Piercing entity Hits, nested Trigger release, landed MINE
proximity Hit, 0/1-tick Timer release, Piercing collision plus same-tick Timer
expiry, Timer save/reload both before and after expiry, natural and `KILLED`
Expiration, simulated `UNLOADED_TO_CHUNK` lifecycle removal, legacy-runtime
identity binding, Wand Refresh, Alpha, Gamma, and the G02 two-round wand state
machine. The G02 scenario
creates a real server player, equips a three-Spark-Bolt Spells/Cast=2 wand,
casts twice, compares persisted state against pure evaluation, and checks the
actual 2 then 1 projectile spawn result.

G03 replaces the Trigger placeholder with real entity spawning and collision
fixtures. Unit tests additionally cover collision-key de-duplication, Timer
collision plus expiry, Expiration one-shot behavior, nested runtime budget
reservation, v3 identity/schema corruption, 64-entry modifier lists, state
persistence, and corrupt-tree rejection. The Fabric fixture exercises
Minecraft's `UNLOADED_TO_CHUNK` removal reason and restores the saved entity
before a later real termination.
Full crash recovery still requires a persistent execution ledger and transaction;
normal reload semantics are bounded by persisted runtime flags, not claimed as
crash-safe exactly-once.

## Dedicated Server Smoke

`smokeDedicatedServer` uses `build/smoke-server`, writes test-only EULA and
server properties, waits for the server ready log, sends `stop` through local
RCON, and confirms normal saving and listener shutdown. It fails on client
class loading, Mixin failures, registration conflicts, entrypoint errors, time
limits, or non-normal server shutdown. Full evidence remains in
`build/smoke-server/smoke-server.log` on failure.
