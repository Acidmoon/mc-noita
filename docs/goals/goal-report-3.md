# Goal Report 3: G02 Basic Cast Semantics

## Objective

G02 required an exact basic Noita wand state machine before additional spells or world effects can build on it. The accepted scope was Draw, Wrap/Reload, mana, normal limited uses, Always Cast, timing, Wand Refresh, persistence migration, JUnit, GameTest, documentation, and a verification task. Trigger lifecycle, Greek/Copy, recursion and complex payload policy remain G03 work.

## Delivered artifacts

- `DrawOrigin`, `DrawRequest`, `DrawOutcome`, `DrawFailure`, and `DrawFailureReason` replace the boolean `drawMany(amount, canWrap, permanent)` contract.
- `WandCastSession` now evaluates normal Draw in the canonical remove/check uses/check mana/charge/Hand/action order, retries failed candidates, prevents a final failed card from immediately wrapping, and preserves per-request diagnostics.
- Initial, Action, Payload, and permanent Draw have distinct Wrap policies. Action Wrap sets `startReload`; `reloading` and `refreshCount` remain independent session state.
- Reload now moves Hand to Discard and rebuilds/order-shuffles Deck in the accepting pure evaluation. The persisted Deck is no longer rebuilt at Recharge completion with a later random seed.
- `SpellDefinition` permits negative mana and declares `UseConsumptionPolicy`; Hand-to-Discard performs automatic use reduction once per eligible normal card.
- Always Cast executes before Initial Draw, ignores positive mana and uses, applies negative mana, suppresses only permanent `DrawAction(1)`, and keeps higher Draw counts real.
- `TimingAction` supports `ADD` and `SET`; Modifier and projectile timing are charged per action execution, payload Cast Delay is isolated, and payload Recharge is global. Chainsaw now maps `c.fire_rate_wait = 0` to `SET`.
- Cast State schema is v2. G01 v1 reload states migrate through `G02ReloadPrepared` and a stable migration seed.
- `G02WandCastSemanticsTest`, an expanded migration test, updated evaluator regression, and a real Fabric GameTest cover the new contract.
- `docs/基础施法语义.md` documents the behavior, evidence, persistence, adaptation level, and G03 boundary. `docs/纯法杖解释器.md` and `docs/testing.md` now describe G02 as verified.
- `verifyG02` runs JUnit, the isolated GameTest source-set fixture, Fabric's headless GameTest server, dedicated-server smoke, and production build.

## Verification

- `./gradlew.bat test`
- `./gradlew.bat gameTest`
- `./gradlew.bat runGametest --no-daemon` (11 Fabric tests passed, including the real two-round wand scenario)

The complete G02 gate is `./gradlew.bat verifyG02`.

## Guidance for G03

Use `DrawRequest` instead of introducing boolean draw options. New copying or Trigger mechanics must state whether they are Draw, Call, Copy, or Payload and must retain a `DrawOutcome`/diagnostic trail. Do not move deck rebuilding or mana charging into world executors or adapters.

G03 must add explicit policies and fixtures for Alpha/Gamma/Divide/Add Trigger selection, target use/mana rules, recursive action metadata, and Timer/Piercing/Death persistence. It must preserve the G02 invariant that payload selection and mana payment happen during pure evaluation, while collision lifecycle executes only frozen plans.
