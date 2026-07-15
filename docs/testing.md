# G00-G05 Test Facilities

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

Run the G04 gate from the repository root:

```powershell
.\gradlew.bat verifyG04
```

`verifyG04` adds pure invocation/trace fixtures and real Add Trigger/Divide
server scenarios while retaining every G03 lifecycle and persistence check.

Run the G05 gate from the repository root:

```powershell
.\gradlew.bat verifyG05
```

`verifyG05` runs catalog validation, all JUnit tests, the isolated GameTest
fixture suite, the headless Fabric GameTest server, the common-server
architecture check, dedicated-server smoke, and the production build.

## JUnit Tags

- `characterization` records observed current behavior and catalog facts.
- `wiki-golden` holds Wiki-defined target behavior. A disabled fixture retains
  its source URL, verification date, exact assertion, and responsible Goal.
- `regression` protects migrations, payload limits, packet codec, sequencing,
  and rate-limit behavior fixed in G00.

Current stable tests cover catalog identity and resources, finite template
values, v0 to v5 NBT migration, future-version rejection, slot and frozen-tree
limits, centrally configured Trigger-runtime ceilings, v4 `DamageProfile` compatibility, strict v5 persistent-job codecs,
packet round trips, malformed input rejection, replay/order checks, transaction
binding and budget rejection, independent token buckets, Trigger prepayment,
Shot State isolation, runtime release decisions, and frozen payload
serialization. `SpellJobManagerTest` additionally covers lifetime-lease
transfer, fresh per-step reservation, owner/chunk gates, expiry, hard-budget
exhaustion, duplicate/unsupported records, and idempotent versus non-idempotent
recovery.

## GameTest Layout

`src/gametest` is a test-only Fabric mod and is excluded from the release JAR.
It declares thirty-three fixed-tick, empty-structure scenarios for Starter Wand,
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

G04 adds a production-wand Add Trigger collision test that releases a frozen
G03 payload after a real block hit, plus a nested Divide fixture that requires
the server tick to advance and the authoritative entity ceiling to hold.

G03 replaces the Trigger placeholder with real entity spawning and collision
fixtures. Unit tests additionally cover collision-key de-duplication, Timer
collision plus expiry, Expiration one-shot behavior, nested runtime budget
reservation, v3/v4/v5 identity/schema corruption, v4 damage-profile
compatibility, 64-entry modifier lists, state persistence, and corrupt-tree
rejection. The Fabric fixture exercises Minecraft's `UNLOADED_TO_CHUNK` removal
reason and restores the saved entity before a later real termination.

G05 adds six real-server scenarios, bringing the suite to 33: a root plus
frozen Trigger child rejected by the configured per-cast entity ceiling proves
complete wand NBT stays byte-identical and execution does not
start; an open editor rejects the packet; stale client wand revision and stale
catalog epoch both reject before commit; and two real `HEAL` collisions prove
that a wounded teammate is healed while an unrelated hostile remains unchanged
when `friendlyFire=false`. The healing scenarios additionally assert projectile
termination, so an unchanged health value cannot pass merely because a target
was missed. A post-reservation stack-swap fixture proves the second binding
check rejects an externally edited held wand without overwriting either stack.
The Bomb entity-Hit fixture deliberately crosses a real chunk seam, exercising
the live multi-chunk query reservation rather than relying on the GameTest
grid's incidental alignment.

Persistent-job save/recovery behavior is currently covered by pure codec and
manager fixtures. A concrete job handler must add an end-to-end GameTest for its
own world effect; no generic background job is treated as implemented, and
non-idempotent interrupted work is intentionally not retried as exactly-once.

## Dedicated Server Smoke

`smokeDedicatedServer` uses `build/smoke-server`, writes test-only EULA and
server properties, waits for the server ready log, sends `stop` through local
RCON, and confirms normal saving and listener shutdown. It fails on client
class loading, Mixin failures, registration conflicts, entrypoint errors, time
limits, or non-normal server shutdown. Full evidence remains in
`build/smoke-server/smoke-server.log` on failure.
