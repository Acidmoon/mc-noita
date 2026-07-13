# MC Noita Project Agent Instructions

## 1. Mission

This repository ports Noita's wand-building and spell-composition systems to Minecraft Java Edition 1.20.4 on Fabric.

The primary product is not a collection of unrelated magic items. It is a deterministic, composable wand evaluator whose results are executed safely in a multiplayer Minecraft world. Preserve Noita's observable composition rules wherever Minecraft can express them. Where Noita depends on 2D pixel simulation or 60 FPS behavior, implement a documented Minecraft-equivalent effect instead of pretending it is exact.

The detailed roadmap and researched source list are in:

- `docs/Noita法术与法杖移植开发方案-知识搜集.md`
- `Documents/NoitaMechanics/README.md`
- `Documents/NoitaMechanics/00_来源与方法/资料来源索引.md`

## 2. Instruction Priority

Follow instructions in this order:

1. System, platform, safety, and the user's latest request.
2. This repository `AGENTS.md` and any more specific nested `AGENTS.md`.
3. Repository build files, tests, schemas, and documented conventions.
4. Noita Wiki as this project's primary player-observable behavior specification; it is a community reference, not an official engine specification.
5. Local `Documents/NoitaMechanics` research and version-fixed nonofficial data evidence for implementation details not specified by the Wiki.
6. Existing code style and behavior.

Existing code is evidence of the current implementation, not authority for Noita semantics. Do not preserve a known incorrect behavior solely because it already exists.

If the Noita Wiki and datamined scripts appear to conflict:

1. Reproduce or locate the exact player-observable scenario.
2. Record the Wiki page, retrieval date, relevant local source file, and the disagreement.
3. Prefer the Wiki behavior for this project's public contract unless there is strong reproducible evidence that the page is wrong.
4. If adopting a different result, document the deviation and add a regression test.

Do not use Reddit, Steam discussions, videos, or unsourced blogs as the only authority for a mechanic.

## 3. Fixed Technical Baseline

- Minecraft Java Edition: `1.20.4`
- Mod loader: Fabric
- Java: JDK 17
- Mappings: Yarn `1.20.4+build.3`
- Fabric Loader: `0.15.11`
- Fabric Loom: `1.6-SNAPSHOT`
- Fabric API: `0.97.1+1.20.4`
- Mod ID: `mc-noita`
- Common sources: `src/main/java`
- Client-only sources: `src/client/java`
- Generated data: `src/main/generated`

Do not upgrade Minecraft, Java, mappings, networking style, or Loom as an incidental part of a spell change.

Use the Gradle Wrapper from the repository root. On Windows, prefer:

```powershell
.\gradlew.bat build
.\gradlew.bat test
.\gradlew.bat runClient
.\gradlew.bat runServer
.\gradlew.bat runDatagen
```

Use `rtk` for verbose external commands when it is installed and appropriate, but do not make it a project dependency.

## 4. Current Repository Reality

As of 2026-07-13, the working tree contains a substantial in-progress implementation and many uncommitted spell resources. Measure again before relying on these numbers.

- `ModItems.java` declares 315 spell items: 122 Projectile, 45 Static Projectile, 143 Projectile Modifier, and 5 representative Multicast/Utility/Other spells.
- Registration does not mean behavioral completion. Many entries are approximate or share generic behavior.
- `NoitaWandCaster` already has simplified Deck, Hand, Discard, mana, payload, Wand Refresh, Alpha, Gamma, and Duplicate behavior.
- Core behavior is concentrated in large classes and there is currently no established automated test suite.
- The repository contains user-owned modified and untracked assets. Never reset, overwrite, delete, regenerate, or reformat them wholesale without explicit authorization.

Before editing, run a focused `git status --short`, inspect the exact files in scope, and distinguish your changes from existing user changes.

## 5. First-Principles Contract

Reduce every implementation task to these questions before writing code:

1. What player-observable behavior must change?
2. Which state owns the behavior: wand instance, Deck/Hand/Discard, base Shot State, trigger Shot State, global cast state, projectile plan, or world executor?
3. Is this operation Draw, Call, Copy, Discard, Wrap, Reload, or a world effect?
4. When are mana and limited uses checked and consumed?
5. Can the operation recurse, wrap, load chunks, spawn entities, scan entities, or modify blocks?
6. Which invariants must hold after failure, cancellation, disconnect, chunk unload, save/load, and budget exhaustion?
7. What is the smallest deterministic test that proves the behavior?
8. Is the MC result `faithful`, `equivalent`, `approximate`, or `deferred` relative to the Wiki?

Make the smallest change that establishes the correct abstraction. Do not add more spell registrations to compensate for an evaluator that cannot express them correctly.

## 6. Required Architecture Boundaries

### 6.1 Definition and catalog layer

Spell and wand definitions contain source data, classification, action definitions, balance values, and adaptation metadata. A spell's Noita type is not its implementation. Types affect categorization, copying, and use-consumption rules; explicit actions define behavior.

Every completed spell must be traceable by stable Minecraft ID and canonical Noita action ID. Source metadata includes Wiki URL, Wiki revision ID, verification date, target Noita version, and the exact commit/version of any implementation evidence. Java field names are not canonical IDs; aliases such as Wiki `MANA_REDUCE` and Java `ADD_MANA` require explicit mapping. Built-in item IDs are fixed at registry time. Resource reloads may override validated behavior for existing IDs but must not attempt to add new Minecraft Item registry entries after startup.

Avoid adding further multi-thousand-line registration files. Prefer generated/validated catalogs for repetitive data and small, named executors for exceptional behavior. Generated outputs must be reproducible and must not be hand-edited.

### 6.2 Pure evaluator layer

The evaluator owns:

- `WandState` and mana/timing state.
- Deck, Hand, and Discard transitions.
- Draw, Wrap, Reload, Call, Copy, recursion, and limited-use decisions.
- Base and trigger Shot States.
- Deterministic random selection and shuffle.
- Execution budgets and structured diagnostics.
- Production of immutable `ResolvedCast` / `EffectPlan` values that freeze all mechanical parameters and the catalog epoch/hash used to evaluate them.

The evaluator must not import or call any Minecraft `World`, entity, networking, rendering, sound, NBT, Registry, ItemStack, or client classes. NBT/ItemStack conversion belongs to a persistence/adapter layer. The same input state, catalog snapshot, configuration, and RNG seed must produce the same result.

### 6.3 Server execution layer

Server executors translate plans into projectile entities, ray casts, fields, status effects, explosions, summons, and bounded block operations. They do not reinterpret card order or charge mana.

All authoritative state changes happen on the logical server. A cast follows `validate -> evaluate -> reserve budgets -> commit WandState -> execute EffectPlan` on the server thread. Validation binds hand, slot, ItemStack state hash/revision, packet sequence, and catalog epoch so GUI editing, swapping hands, duplicate packets, and reloads cannot race the commit. After commit, executor failure does not roll WandState back; isolate failures per plan node, release unused reservation, emit rate-limited diagnostics, and retry only persisted jobs designed to be idempotent. The client may request a cast but may not submit damage, mana, cooldown completion, random results, payload contents, or block changes.

### 6.4 Client projection layer

Client code owns HUD, screens, key input, particles, sounds, and renderers. Keep all `net.minecraft.client.*` references under `src/client` or client-only mixins/entrypoints. Common/server code must load on a dedicated server without client classes.

Visual aggregation is allowed and encouraged for high-rate wands. Cosmetic effects must not create extra authoritative server projectiles.

## 7. Canonical Wand Semantics

The following are project invariants and require regression tests when touched:

### 7.1 Card piles and Draw

- Deck contains drawable cards; Hand contains successfully drawn cards in the current cast; Discard contains used or skipped cards.
- Normal Draw removes the top card, checks remaining uses and mana, deducts mana once, places the card in Hand, and calls its action.
- A card that cannot be drawn because of mana or uses goes directly to Discard and the evaluator may try the next card according to Noita's draw-retry rules.
- Preserve slot/card identity. Except for explicit removal, Always Cast temporary copies, or special actions, each configured card must exist in exactly one of Deck, Hand, or Discard.

### 7.2 Initial Draw, action Draw, Wrap, and Reload

- Wand `spellsPerCast` performs initial Draw and cannot initiate Wrap by itself.
- A spell action's Draw may Wrap when allowed.
- Action Draw on an empty Deck moves Discard to Deck, orders or shuffles it, continues drawing, and marks the cast for Reload.
- If an action Draw has already caused a Wrap, any still-unspent initial Spells/Cast Draw may continue from the newly formed Deck; the initial Draw did not itself initiate the Wrap.
- The first effective Wand Refresh in a cast performs its documented pile rewrite/Wrap without forcing Reload merely because it refreshed. A second call in the same cast does not reset again and forces Recharge.
- Shuffle occurs only at documented transitions; it is not a random card choice on every Draw.

### 7.3 Shot State

- Spells modify the current Shot State, not another spell item.
- All projectiles in the same Shot State receive the state accumulated for that shot.
- A modifier must not be implemented as “modify the next projectile and immediately restore” if this causes later projectiles in the same multicast Shot State to lose the modifier.
- Every trigger payload has a distinct Shot State. Damage, speed, spread, lifetime, and cast-delay effects are isolated to that payload unless the mechanic explicitly uses a global value.
- Mana, recharge modifications, and other documented global cast values remain global even while evaluating a payload.

### 7.4 Timing

- Preserve Noita seconds/frames in canonical definition data and convert through one named utility at the Minecraft boundary.
- Do not scatter magic `/ 60`, `* 20`, or unexplained speed conversion constants across entity classes.
- Cast Delay and Recharge Time count down concurrently. Do not add them together.
- Payload Cast Delay does not delay the outer wand; payload Recharge Time modifications may affect it.
- Minecraft cannot represent all 60 Hz emissions as separate entities. When coalescing, preserve documented mana cost, average damage, timing, and statistical spread as closely as practical.

### 7.5 Triggers

- Hit Trigger releases on valid entity or terrain collision and may release repeatedly when mechanics such as Piercing cause repeated valid hits.
- Timer behaves as a Trigger on collision and, if still alive, releases again at timer expiry and then stops; a Piercing Timer may release on multiple hits before expiry.
- Expiration/Death Trigger releases when the projectile expires or is otherwise destroyed, at most once.
- Trigger payloads are selected, validated, and paid for during wand evaluation, not re-drawn at impact.
- A persisted projectile must carry enough resolved, versioned payload state to resume safely without resolving changed catalog data by ID.
- Collision, timer, death, chunk reload, and duplicate packets must not release the same one-shot payload twice during normal save/unload operation. Do not claim crash-safe exactly-once without a persistent execution ledger and transaction design.

### 7.6 Call, Copy, and recursion

- Draw and Call are distinct. Call invokes a target action without automatically applying normal Draw checks, mana deduction, Hand insertion, or use deduction.
- Each copying spell has its own target-selection, Draw-enabled/disabled, charge, and discard rules. Do not collapse Greek spells, Duplicate, Divide By, and Add Trigger into one generic copy implementation unless their policy differences remain explicit and testable.
- A normally drawn action starts at recursion level 0. Calling a non-recursive action preserves the caller's level; calling an action marked recursive increments it by 1. At vanilla limit 2, an action may not call another recursive action. Enforce this policy rather than applying one generic copy-depth counter to all calls.
- Also enforce independent total-action, payload-node, projectile, entity, and block-operation budgets. Recursion depth alone is not a denial-of-service defense.

### 7.7 Limited uses and Always Cast

- A depleted card is skipped without spending mana.
- Normal uses are generally reduced during the normal Hand-to-Discard process, not as soon as a card is drawn.
- Utility/Other follow their category rules; other limited-use cards attempt use reduction only when a Projectile, Static Projectile, or Material card entered Hand through normal Draw in the same cast. Merely producing an equivalent world effect through Copy/Call does not satisfy this condition.
- Copy/Call does not automatically run normal Hand-discard use consumption; action-specific manual consumption remains possible.
- Always Cast is outside the normal queue, normally costs no positive mana, and treats limited uses as unlimited.
- Always Cast Trigger/Timer behavior has special cases. Encode them in policy and tests rather than relying on a single `permanent` boolean.

## 8. Minecraft Adaptation Policy

For every non-trivial spell, record an adaptation level and note:

- `faithful`: observable semantics can be reproduced directly.
- `equivalent`: the same gameplay role is produced with Minecraft primitives.
- `approximate`: important behavior differs and the player must be told.
- `deferred`: registered/visible but not behaviorally complete; do not present it as finished.

Preferred mappings:

- Pixel liquids/powders/gases -> bounded block tags, vanilla fluids, area clouds, particles, and status effects.
- Material durability/hardness -> explicit block tags plus hardness/resistance policy; never raw “break every non-air block”.
- Electricity -> bounded conduction through configured fluids/metals; no unbounded graph traversal.
- Fire/freeze/radioactive-toxic/poison -> distinct typed damage plus status effects; keep damage and status application separate when Noita does.
- Healing damage -> server-side healing with correct ownership/friendly-fire collision policy.
- Black/white holes -> bounded attraction/repulsion fields and permission-aware terrain effects.
- Teleport -> collision-safe destination search with border and chunk checks.
- Polymorph -> allowlisted, reversible transformations; bosses and protected entities are immune by default.
- Summons -> owner-tagged entities with count and lifetime limits.
- Large explosions and terrain conversion -> server configuration, permission hooks, protection compatibility, and hard block budgets.

All world mutation goes through a `WorldMutationPolicy` that revalidates owner, dimension, hit position, GameRules, spawn protection, block tags, world border, loaded chunks, and optional claim/protection adapters at execution time. Fabric has no universal claims API; when no adapter exists, use a documented conservative default rather than claiming compatibility. Define explicitly whether `mobGriefing` applies to player casts, summons, reflected projectiles, and offline owners. Destructive behavior must have a server-side disable or scale control.

## 9. Mandatory Safety Budgets

Budget values belong in server configuration with conservative defaults and hard ceilings. A central `SpellBudgetManager` accounts simultaneously at per-cast, per-owner, per-chunk, per-dimension, and global tick/time-window levels. It also covers offline-owner fields/summons, cross-tick jobs, NBT bytes/depth/nodes, network packets/bytes, and client visual events. A per-player limit alone is not a server defense.

Until profiling establishes better values, use these as single-cast planning defaults, not final balance claims:

- Evaluated action nodes: 2048
- Resolved logical projectile plans: 128
- Spawned authoritative entities: 32
- Checked blocks: 4096
- Modified blocks: 512
- Nested payload depth: 16
- Recursive Call depth: 2

These are planning defaults, not permission to consume all of them on every tick. Add separate measured dimension/global ceilings before enabling destructive multiplayer behavior. Prefer amortized bounded jobs and stop work when the relevant chunk unloads or the owner/world becomes invalid.

On budget exhaustion:

1. Reserve budgets for the whole accepted plan before commit. If reservation fails, reject or deterministically truncate during evaluation; never commit and then randomly drop half a tree.
2. Leave Deck/Hand/Discard, uses, mana, and cooldown in a valid, documented state consistent with the accepted plan.
3. Emit rate-limited diagnostics with player, wand, spell path, and budget type.
4. Provide a concise player/admin indication when useful.
5. Never continue unbounded work on another thread or the next tick without a persisted bounded job.

## 10. Persistence and Compatibility

- Version every non-trivial wand, cast-state, and projectile-payload NBT structure and maintain an explicit migration chain with fixture files for every supported old version.
- Validate numeric input before allocating large objects: reject or clamp NaN, infinity, negative counts, invalid enum names, excessive list sizes, excessive bytes, excessive tree depth/node count, and unknown IDs. Prefer iterative or explicitly depth-bounded payload decoding.
- Unknown old spell IDs must degrade to a visible invalid/missing spell representation, not silently become a different spell.
- NBT migrations must be idempotent and tested with representative old stacks/entities.
- Keep registry IDs stable. Renaming a Java field does not authorize renaming the item ID.
- Do not store runtime object references, registry raw IDs, or client-calculated authority in persistent payloads.
- Save execution UUID, catalog epoch/hash, resolved mechanical data, released flags, and enough state to prevent duplicate one-shot release after normal world reload. Corrupt data degrades to an inert, diagnosable entity/item rather than recursively allocating until failure.

## 11. Networking and Concurrency

Minecraft 1.20.4 uses the pre-1.20.5 Fabric networking API:

- `ClientPlayNetworking.send(id, buf)`
- `ServerPlayNetworking.registerGlobalReceiver(...)`
- `ServerPlayNetworking.send(player, id, buf)`
- `ClientPlayNetworking.registerGlobalReceiver(...)`

Do not use the 1.20.5+ Custom Payload style in this project.

Every C2S handler must:

1. Validate packet size and enum/range fields before use.
2. Schedule world/player operations on the server thread.
3. Re-read the player's held item, slot/version, cooldown, mana, and permissions.
4. Treat duplicate, stale, reordered, or malicious packets as normal invalid input.
5. Rate-limit actions that can allocate, scan, spawn, or modify blocks.

Do not perform unsafe world/entity operations from asynchronous tasks. Background work may parse immutable data or prepare bounded plans, but world application returns to the logical server thread and revalidates state.

## 12. Data, Resources, and Datagen

- All namespaces and paths are lowercase; use `mc-noita` as the existing namespace.
- Prefer data-driven JSON for repetitive spell specifications, tags, loot, recipes, models, language, and world generation.
- Use validated codecs or explicit schema validation. Error messages must include the resource/spell ID and field path.
- Keep a stable catalog of built-in item IDs; data packs may only override behavior explicitly declared safe to reload.
- Resource reload must build an immutable catalog snapshot and new epoch/hash atomically. If validation fails, keep the last valid catalog or fail startup clearly; never leave half the spells updated. In-flight and persisted EffectPlans retain their frozen mechanics and old epoch instead of re-resolving changed damage or payload behavior by ID.
- Generated data goes to `src/main/generated`; review it before committing.
- Verify every registered spell has the intended model, texture, English key, Chinese key, source metadata, and adaptation status.
- Detect orphan assets and duplicate IDs. Do not delete an orphan automatically when the working tree is dirty.

For Minecraft 1.20.4 datagen recipes, use `generateRecipes(Consumer<RecipeJsonProvider>)`; do not copy 1.21+ examples.

## 13. Testing Requirements

### 13.1 Pure evaluator tests

Use ordinary JUnit tests for card piles, Draw/Call/Copy, Shot State, timing, RNG, budgets, codecs, and migrations. These tests must not start Minecraft when pure Java is sufficient.

Required Wiki-derived golden cases include:

1. Non-shuffle left-to-right order and documented shuffle transitions.
2. Built-in Spells/Cast cannot Wrap.
3. `Double Spell + Damage Plus + Spark Bolt + Spark Bolt` applies damage to both bolts.
4. Insufficient-mana and depleted cards move to Discard and retry correctly, including the last-card case.
5. Action Draw Wrap requests Reload; Wand Refresh Wrap alone does not.
6. Cast Delay and Recharge Time run concurrently.
7. Trigger payload mana is paid during cast evaluation and is not paid again on repeated Piercing Trigger/Timer hits; the no-refund consequence is backed by an explicit game fixture as well as the Wiki-derived model.
8. Trigger Shot State isolation and global Recharge behavior.
9. Gamma/Greek Call does not automatically spend the target's mana or uses.
10. Recursive Calls stop at level 2 and all action trees stop at the total budget.
11. Always Cast zero-mana/unlimited-use behavior and documented trigger exceptions.
12. Limited-use modifiers consume charges only under the documented normal-Draw/Hand-discard conditions.

Add property/fuzz tests for card conservation, mana accounting, deterministic output, serializer round trips, invalid definitions, and guaranteed termination.

### 13.2 Game and integration tests

Use Fabric GameTest or focused integration fixtures for:

- Projectile collision and one-shot trigger release.
- Save/reload, chunk unload, owner disconnect/death, dimension change, corrupt/oversized payload rejection, and schema migration fixtures.
- Damage types, friendly fire, self-healing, piercing, reflection, and repeated collision.
- Block protection, `mobGriefing`, unbreakable tags, and block budgets.
- Dedicated-server class loading and networking validation.

### 13.3 Performance tests

Maintain representative stress wands: rapid-fire, deep nested trigger, Divide By/copy, mass homing, large material conversion, and Nuke/black hole. Record MSPT, server entity count, packet rate, checked/modified blocks, evaluator nodes, and truncation counts.

Do not claim performance is acceptable from a successful compile or a brief single-player test.

## 14. Definition of Done for a Spell

A spell is complete only when all applicable items are true:

1. Stable Minecraft ID and Noita action ID are recorded.
2. Wiki URL and verification date are recorded.
3. Type, mana, uses, timing, damage, tier/tags, Draw/Call behavior, and special rules are verified.
4. Adaptation status and all player-visible differences are documented.
5. Behavior uses the correct evaluator action and server executor; no unrelated generic placeholder remains.
6. Model, texture, English translation, and Chinese translation resolve.
7. A definition validation test exists.
8. At least one behavior test exists; complex modifiers also have single, multicast, and trigger-context tests.
9. Server budgets, permissions, persistence, and disconnect/chunk-unload paths are handled.
10. Dedicated-server compatibility is preserved.

“Item registered”, “texture visible”, “does damage”, or “works in one test wand” alone is not completion.

## 15. Mandatory Coding Workflow

For implementation tasks:

1. Inspect relevant source, tests, definitions, Wiki pages, local research, and `git status` before editing.
2. State the behavior, invariants, failure modes, adaptation level, and smallest verifiable change.
3. Add or update the narrowest failing test first when practical.
4. Implement using existing architecture or improve the minimum necessary abstraction.
5. Add clear comments for non-obvious Noita semantics, scope boundaries, unit conversions, lifecycle guarantees, and safety budgets. Do not add comments that merely restate Java syntax.
6. Update documentation under `docs/` when completing a major evaluator/executor/catalog subsystem or changing a documented adaptation.
7. Review the diff adversarially using the checklist below.
8. Run focused tests, then `build`; run `runServer` or GameTests for server/lifecycle changes.
9. Report changed files, verification commands, known deviations, and remaining risks.

Use `rg` / `rg --files` for search. Read files before editing. Use `apply_patch` for manual changes. Do not overwrite generated or user-owned files with ad hoc scripts.

## 16. Adversarial Review Checklist

Before completion, challenge the change:

- Does a modifier leak into or disappear from the wrong Shot State?
- Can initial Draw Wrap when it must not, or fail to Reload when it must?
- Is mana or a limited use charged zero times or twice?
- Does a copied action incorrectly enter Hand or pay target mana?
- Can a recursive/copy/multicast/trigger graph exceed a hard budget?
- Can collision, timer, expiry, save/load, or packet replay release a payload twice?
- Can a world scan load chunks or grow without a radius/count bound?
- Can a destructive spell bypass permissions, claims, `mobGriefing`, or unbreakable blocks?
- Can a malicious C2S packet choose its damage, mana, target, seed, or payload?
- Can changing hands, editing the wand, replaying a sequence number, or reloading the catalog race `validate/evaluate/reserve/commit/execute`?
- Can common code load a client class on a dedicated server?
- What happens when the owner logs out, dies, changes dimension, or loses the wand?
- What happens with malformed/old NBT, missing definitions, unknown IDs, NaN, infinity, and excessive list sizes?
- Are RNG seeds stable enough for tests and authoritative enough for multiplayer?
- Does the accepted plan reserve per-cast, owner, chunk, dimension, global, NBT, and network budgets before state commit?
- Can an in-flight or loaded projectile accidentally resolve new catalog data under an old payload ID?
- Did a registry ID, NBT key, resource path, or generated output change without migration?
- Did the change overwrite or assume ownership of existing dirty-worktree files?
- Is the implementation labeled more faithfully than evidence supports?

Fix in-scope findings before reporting completion. Add the test most likely to prevent recurrence.

## 17. Mixins and Fabric Conventions

- Prefer Fabric API events over Mixins.
- When Mixins are necessary, prefer `@Inject`, accessor/invoker Mixins, or MixinExtras operations.
- Do not use `@Overwrite` without an exceptional documented reason and focused compatibility tests.
- Keep client-only Mixins in the client Mixin config.
- Register items, entities, screen handlers, particles, events, and network receivers during the correct initialization phase.
- Keep common logic server-loadable and avoid static initialization that reads a world, player, or client singleton.

## 18. Documentation and Comments

Comments should explain why a Noita rule is surprising, what state scope it belongs to, why a unit conversion exists, or which lifecycle/security invariant is protected. Prefer a link to the relevant local design section and Wiki page over copying large Wiki passages into source comments.

When a major part of the evaluator, plan model, executor framework, material mapping, damage system, or catalog is completed, add or update a document under `docs/` that covers:

- Responsibility and boundaries.
- Data flow and important types.
- Noita semantics and MC adaptations.
- Persistence/network format if applicable.
- Budgets and failure behavior.
- Tests and extension procedure.

Keep documents aligned with code; remove stale completion claims.

## 19. Private Non-Commercial Resource Policy

This project is for private, non-commercial play with friends. Under the user's chosen project policy, resource ownership and license compatibility are not development gates.

- Agents may directly download and use icons, textures, screenshots, wand images, reference text, and other required resources from the official Noita website, the official Noita modding pages, or the Noita Wiki.
- Do not pause implementation to investigate copyright ownership, license compatibility, attribution proof, commercial-use rights, or redistribution permission.
- Do not require `THIRD_PARTY_NOTICES`, a license manifest, written permission, or replacement original art before building or sharing the mod with the user's friends.
- Keep only lightweight provenance: source URL, original filename, and download date. This is for maintenance, deduplication, and re-downloading, not a rights audit.
- Prefer official Noita or Noita Wiki resources over third-party mirrors when the same asset is available from both.
- Download scripts must use explicit official/Wiki URLs, validate output paths, avoid path traversal, and never overwrite a locally modified resource without explicit authorization.
- If the user later changes the project to public distribution or commercial use, wait for the user to define a new resource policy; do not reintroduce a licensing workflow on your own.

## 20. Private Build and Verification

Minimum verification for a normal code change:

```powershell
.\gradlew.bat test
.\gradlew.bat build
```

Also run as relevant:

```powershell
.\gradlew.bat runDatagen
.\gradlew.bat runServer
.\gradlew.bat runClient
```

When sharing the mod with the user's friends, use only the remapped JAR from `build/libs/`, never a `*-dev.jar`. Keep `fabric.mod.json` constraints aligned with Minecraft 1.20.4, Fabric Loader 0.15.11, Java 17, and Fabric API.

If verification cannot run, state exactly why and what static/manual checks replaced it. Never claim a test passed unless it was executed successfully.
