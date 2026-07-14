package com.mcnoita.wand.eval;

import com.mcnoita.spell.action.AddProjectileAction;
import com.mcnoita.spell.action.AddTriggerToNextProjectileAction;
import com.mcnoita.spell.action.BeginTriggerAction;
import com.mcnoita.spell.action.CallSpellAction;
import com.mcnoita.spell.action.DivideAction;
import com.mcnoita.spell.action.DrawAction;
import com.mcnoita.spell.action.DuplicateHandAction;
import com.mcnoita.spell.action.GreekCopyAction;
import com.mcnoita.spell.action.GreekCopyKind;
import com.mcnoita.spell.action.ModifyShotAction;
import com.mcnoita.spell.action.RandomSpellAction;
import com.mcnoita.spell.action.RefreshWandAction;
import com.mcnoita.spell.action.SpellAction;
import com.mcnoita.spell.action.SpellCatalog;
import com.mcnoita.spell.action.SpellCategory;
import com.mcnoita.spell.action.SpellDefinition;
import com.mcnoita.spell.action.TimingAction;
import com.mcnoita.spell.action.TimingOperation;
import com.mcnoita.spell.action.TargetQuery;
import com.mcnoita.spell.action.UseConsumptionPolicy;
import com.mcnoita.spell.plan.BudgetUsage;
import com.mcnoita.spell.plan.CastBudget;
import com.mcnoita.spell.plan.CastDiagnostic;
import com.mcnoita.spell.plan.EffectPlan;
import com.mcnoita.spell.plan.PayloadPlan;
import com.mcnoita.spell.plan.ProjectileDefinition;
import com.mcnoita.spell.plan.ProjectilePlan;
import com.mcnoita.spell.plan.RecoilPlan;
import com.mcnoita.spell.plan.ResolvedCast;
import com.mcnoita.spell.plan.ShotModifier;
import com.mcnoita.spell.plan.ShotState;
import com.mcnoita.spell.plan.SoundPlan;
import com.mcnoita.spell.plan.TriggerPlan;
import com.mcnoita.spell.plan.TriggerMode;
import com.mcnoita.spell.plan.TriggerReleasePolicy;
import com.mcnoita.wand.model.CardRef;
import com.mcnoita.wand.model.DeckState;
import com.mcnoita.wand.model.NoitaDuration;
import com.mcnoita.wand.model.SpellCardState;
import com.mcnoita.wand.model.WandDefinition;
import com.mcnoita.wand.model.WandState;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The mutable, pure workspace for one cast. Draw is intentionally centralized:
 * no SpellAction may move cards between piles or charge mana on its own.
 */
public final class WandCastSession {
    private final WandDefinition wand;
    private final WandState originalState;
    private final SpellCatalog catalog;
    private final NoitaDuration elapsed;
    private final CastRng rng;
    private final CastBudget budget;
    private final ExternalSpellPool externalSpellPool;
    private final Map<CardRef, SpellCardState> cards;
    private final List<CardRef> deck;
    private final List<CardRef> hand;
    private final List<CardRef> discard;
    private final List<MutableProjectile> rootProjectiles = new ArrayList<>();
    private final List<SoundPlan> sounds = new ArrayList<>();
    private final List<RecoilPlan> recoils = new ArrayList<>();
    private final List<CastDiagnostic> diagnostics = new ArrayList<>();
    private final List<DrawOutcome> drawOutcomes = new ArrayList<>();
    private final List<String> actionPath = new ArrayList<>();
    private final Deque<CallFrame> callFrames = new ArrayDeque<>();
    private final EvaluationTrace.Builder trace = new EvaluationTrace.Builder();
    private final TimingAccumulator castDelayTiming;
    private final TimingAccumulator rechargeTiming;

    private List<MutableProjectile> projectileSink = rootProjectiles;
    private String projectilePathPrefix = "root";
    private MutableProjectile lastProjectile;
    private MutableShotScope shotScope = new MutableShotScope(0);
    private double mana;
    private NoitaDuration castDelayRemaining;
    private NoitaDuration rechargeRemaining;
    private boolean rechargePending;
    private boolean reloading;
    private boolean startReload;
    private int refreshCount;
    private boolean inPayload;
    private int drawSuppressionDepth;
    private int nextShotScopeId = 1;
    private int payloadDepth;
    private int maxPayloadDepth;
    private int actionSteps;
    private int projectileNodes;
    private int payloadNodes;
    private int rootSpawnedEntities;
    private int reservedSpawnedEntities;
    private String currentSpellId = "";

    WandCastSession(
        WandDefinition wand,
        WandState originalState,
        SpellCatalog catalog,
        NoitaDuration elapsed,
        CastRng rng,
        CastBudget budget,
        ExternalSpellPool externalSpellPool
    ) {
        this.wand = Objects.requireNonNull(wand, "wand");
        this.originalState = Objects.requireNonNull(originalState, "originalState");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.elapsed = Objects.requireNonNull(elapsed, "elapsed");
        this.rng = Objects.requireNonNull(rng, "rng");
        this.budget = Objects.requireNonNull(budget, "budget");
        this.externalSpellPool = Objects.requireNonNull(externalSpellPool, "externalSpellPool");
        DeckState deckState = originalState.deckState();
        this.cards = new LinkedHashMap<>(deckState.cards());
        this.deck = new ArrayList<>(deckState.deck());
        this.hand = new ArrayList<>(deckState.hand());
        this.discard = new ArrayList<>(deckState.discard());
        this.mana = originalState.mana();
        this.castDelayRemaining = originalState.castDelayRemaining();
        this.rechargeRemaining = originalState.rechargeRemaining();
        this.rechargePending = originalState.rechargePending();
        this.castDelayTiming = new TimingAccumulator(wand.castDelay().frames());
        this.rechargeTiming = new TimingAccumulator(wand.rechargeTime().frames());
    }

    public ResolvedCast evaluate() {
        try {
            advanceTime();
            if (!isReady()) {
                diagnostics.add(diagnostic("COOLDOWN", "", 0, 0));
                return result(ResolvedCast.Status.REJECTED, currentState(), EffectPlan.empty());
            }

            playAlwaysCastCards();
            draw(DrawRequest.initial(wand.spellsPerCast()));
            finishHand();

            boolean shouldReload = !cards.isEmpty() && (deck.isEmpty() || startReload);
            if (shouldReload) {
                reloadDeck();
            }
            List<ProjectilePlan> frozenProjectiles = freeze(rootProjectiles);
            reserveRuntimeTree(frozenProjectiles);
            NoitaDuration finalCastDelay = castDelayTiming.duration();
            NoitaDuration finalRecharge = shouldReload ? rechargeTiming.duration() : NoitaDuration.ZERO;
            WandState next = new WandState(
                new DeckState(cards, deck, hand, discard),
                settledMana(),
                finalCastDelay,
                finalRecharge,
                !finalRecharge.isZero(),
                originalState.revision() + 1,
                originalState.stateHash()
            );
            return result(ResolvedCast.Status.ACCEPTED, next, new EffectPlan(frozenProjectiles, sounds, recoils));
        } catch (BudgetExceeded exhausted) {
            diagnostics.add(exhausted.diagnostic);
            return result(ResolvedCast.Status.REJECTED, originalState, EffectPlan.empty());
        } catch (RuntimeException failure) {
            diagnostics.add(diagnostic("EVALUATION_ERROR", currentSpellId, 0, 0));
            return result(ResolvedCast.Status.REJECTED, originalState, EffectPlan.empty());
        }
    }

    private void advanceTime() {
        if (!elapsed.isZero()) {
            double restoredMana = elapsed.frames() / NoitaDuration.FRAMES_PER_SECOND * wand.manaChargePerSecond();
            // Mana gain from a card may exceed the cap while preloading, but time-based recovery never does.
            mana = Math.min(wand.manaMax(), Math.max(0.0, mana + restoredMana));
        }
        castDelayRemaining = castDelayRemaining.minusFloorZero(elapsed);
        rechargeRemaining = rechargeRemaining.minusFloorZero(elapsed);
        rechargePending = !rechargeRemaining.isZero();
    }

    private boolean isReady() {
        return castDelayRemaining.isZero() && rechargeRemaining.isZero();
    }

    private void playAlwaysCastCards() {
        for (String alwaysCastId : wand.alwaysCastSpellIds()) {
            SpellDefinition definition = catalog.definitions().get(alwaysCastId);
            if (definition == null) {
                diagnostics.add(diagnostic("UNKNOWN_ALWAYS_CAST", alwaysCastId, 0, 0));
                continue;
            }

            // gun.lua's _play_permanent_card bypasses positive mana but explicitly calls
            // handle_mana_addition, which preserves Add Mana's negative-cost behavior.
            if (definition.manaCost() < 0) {
                mana -= definition.manaCost();
            }
            executeDrawnDefinition(definition, null, ExecutionScope.PERMANENT);
        }
    }

    /**
     * Runs one typed Draw request. Failed cards are discarded and retried while
     * candidates remain. A failure that consumes the last candidate terminates
     * this request before it can Wrap back to the same round of cards.
     */
    private DrawOutcome draw(DrawRequest request) {
        List<CardRef> drawnCards = new ArrayList<>();
        List<DrawFailure> failures = new ArrayList<>();
        boolean wrapped = false;
        boolean deckExhausted = deck.isEmpty();
        int completedDraws = 0;

        for (int requestIndex = 0; requestIndex < request.amount(); requestIndex++) {
            if (reloading) {
                deckExhausted = deck.isEmpty();
                break;
            }
            consumeAction("DRAW");

            if (deck.isEmpty()) {
                if (request.allowsWrap() && !discard.isEmpty()) {
                    wrapDeck();
                    wrapped = true;
                } else {
                    reloading = true;
                    deckExhausted = true;
                    break;
                }
            }

            boolean completed = false;
            while (!deck.isEmpty()) {
                CardRef ref = deck.remove(0);
                SpellCardState card = cards.get(ref);
                if (card == null) {
                    discard.add(ref);
                    failures.add(new DrawFailure(ref, DrawFailureReason.UNKNOWN_SPELL));
                    traceDraw(ref, "UNKNOWN_SPELL");
                    continue;
                }
                if (!card.hasUsesRemaining()) {
                    discard.add(ref);
                    failures.add(new DrawFailure(ref, DrawFailureReason.DEPLETED_USES));
                    traceDraw(ref, "DEPLETED_USES");
                    continue;
                }
                SpellDefinition definition = catalog.definitions().get(card.spellId());
                if (definition == null) {
                    discard.add(ref);
                    failures.add(new DrawFailure(ref, DrawFailureReason.UNKNOWN_SPELL));
                    diagnostics.add(diagnostic("UNKNOWN_SPELL", card.spellId(), 0, 0));
                    traceDraw(ref, "UNKNOWN_SPELL");
                    continue;
                }
                if (definition.manaCost() > mana) {
                    discard.add(ref);
                    failures.add(new DrawFailure(ref, DrawFailureReason.INSUFFICIENT_MANA));
                    diagnostics.add(diagnostic("INSUFFICIENT_MANA", definition.id(),
                        (int) Math.ceil(definition.manaCost()), (int) Math.floor(mana)));
                    traceDraw(ref, "INSUFFICIENT_MANA");
                    continue;
                }

                // This is the only normal-draw charge point. Negative MANA_REDUCE
                // costs therefore add mana before the next candidate is evaluated.
                mana -= definition.manaCost();
                hand.add(ref);
                drawnCards.add(ref);
                completed = true;
                completedDraws++;
                trace.add(InvocationKind.DRAW, currentSpellId, definition.id(), "DECK", 0, 0, true,
                    currentNodePath(), actionSteps, "");
                executeDrawnDefinition(definition, ref, ExecutionScope.NORMAL);
                break;
            }

            deckExhausted = deck.isEmpty();
            if (!completed && deckExhausted) {
                // Do not let a failed final card cause an immediate Wrap in the same request.
                break;
            }
        }

        DrawOutcome outcome = new DrawOutcome(request.origin(), request.amount(), completedDraws, drawnCards, failures,
            deckExhausted, wrapped, startReload, reloading);
        drawOutcomes.add(outcome);
        return outcome;
    }

    private void wrapDeck() {
        deck.addAll(order(discard, "wrap"));
        discard.clear();
        startReload = true;
    }

    private ActionInvocationResult executeDrawnDefinition(SpellDefinition definition, CardRef origin, ExecutionScope scope) {
        CallFrame frame = new CallFrame(InvocationKind.DRAW, currentSpellId, definition.id(), 0, 0,
            drawSuppressionDepth, shotScope.id, currentNodePath(), actionPath);
        callFrames.push(frame);
        try {
            return executeDefinition(definition, origin, scope);
        } finally {
            callFrames.pop();
        }
    }

    private ActionInvocationResult executeDefinition(SpellDefinition definition, CardRef origin, ExecutionScope scope) {
        String previousSpellId = currentSpellId;
        currentSpellId = definition.id();
        actionPath.add(definition.id());
        ActionInvocationResult result = ActionInvocationResult.SKIPPED;
        try {
            for (SpellAction action : definition.actions()) {
                consumeAction(definition.id());
                result = result.merge(executeAction(action, definition, origin, scope));
            }
            return result;
        } finally {
            actionPath.remove(actionPath.size() - 1);
            currentSpellId = previousSpellId;
        }
    }

    private ActionInvocationResult executeAction(SpellAction action, SpellDefinition definition, CardRef origin,
                                                  ExecutionScope scope) {
        if (action instanceof ModifyShotAction modify) {
            applyModifier(modify.modifier());
        } else if (action instanceof DrawAction drawAction) {
            if (drawSuppressionDepth > 0) {
                trace.add(currentInvocationKind(), definition.id(), "", "", currentRecursionLevel(),
                    currentDivideIteration(), false, currentNodePath(), actionSteps, "DRAW_SUPPRESSED");
                return ActionInvocationResult.SKIPPED;
            }
            // Fixed gun.lua behavior: a permanently attached modifier's single
            // Draw does not add a second card to a one-spell cast. Higher counts
            // remain real action Draw requests and preserve their Wrap behavior.
            if (scope != ExecutionScope.PERMANENT || drawAction.amount() > 1) {
                draw(scope == ExecutionScope.PERMANENT
                    ? DrawRequest.permanent(drawAction.amount())
                    : DrawRequest.action(drawAction.amount()));
            }
        } else if (action instanceof AddProjectileAction add) {
            addProjectile(add.projectile());
        } else if (action instanceof BeginTriggerAction trigger) {
            beginTrigger(trigger);
        } else if (action instanceof CallSpellAction call) {
            return invokeTargets(call.query(), InvocationKind.CALL, InvocationPolicy.CALL, 0);
        } else if (action instanceof DuplicateHandAction) {
            return duplicateHand(definition.id(), scope);
        } else if (action instanceof GreekCopyAction greek) {
            return executeGreek(greek.kind());
        } else if (action instanceof DivideAction divide) {
            return executeDivide(divide);
        } else if (action instanceof AddTriggerToNextProjectileAction addTrigger) {
            return addTriggerToNextProjectile(addTrigger);
        } else if (action instanceof RefreshWandAction) {
            refreshWand();
        } else if (action instanceof RandomSpellAction random) {
            resolveRandomSpell(random.category());
        } else if (action instanceof TimingAction timing) {
            applyTiming(timing);
        }
        return new ActionInvocationResult(true, 0);
    }

    private void traceDraw(CardRef ref, String reason) {
        SpellCardState card = cards.get(ref);
        trace.add(InvocationKind.DRAW, currentSpellId, card == null ? "" : card.spellId(), "DECK",
            currentRecursionLevel(), currentDivideIteration(), drawSuppressionDepth == 0, currentNodePath(),
            actionSteps, reason);
    }

    private void applyModifier(ShotModifier modifier) {
        requireModifierEffectCapacity((long) shotScope.state.effects().size() + modifier.effects().size());
        shotScope.state = shotScope.state.applyModifier(modifier);
        applyModifierTiming(modifier);
    }

    private void applyModifierTiming(ShotModifier modifier) {
        if (!inPayload) {
            castDelayTiming.apply(TimingOperation.ADD, modifier.castDelayFrames());
        }
        rechargeTiming.apply(TimingOperation.ADD, modifier.rechargeFrames());
    }

    private void applyTiming(TimingAction timing) {
        if (!inPayload) {
            castDelayTiming.apply(timing.castDelayOperation(), timing.castDelayFrames());
        }
        rechargeTiming.apply(timing.rechargeOperation(), timing.rechargeFrames());
    }

    private void addProjectile(ProjectileDefinition source) {
        if (inPayload && projectileSink.size() >= PayloadPlan.MAX_PROJECTILES_PER_SHOT) {
            throw budgetExceeded("PAYLOAD_CHILDREN_BUDGET", projectileSink.size() + 1,
                PayloadPlan.MAX_PROJECTILES_PER_SHOT);
        }
        reserveProjectiles(source.projectileCount());
        if (!inPayload) {
            rootSpawnedEntities += source.projectileCount();
            if (rootSpawnedEntities > budget.spawnedEntities()) {
                throw budgetExceeded("SPAWNED_ENTITY_BUDGET", rootSpawnedEntities, budget.spawnedEntities());
            }
        }
        // A projectile action runs once even if its plan expands to many entities.
        if (!inPayload) {
            castDelayTiming.apply(TimingOperation.ADD, source.castDelayFrames());
        }
        // Payload recharge is global in Noita; payload cast delay remains branch-local.
        rechargeTiming.apply(TimingOperation.ADD, source.rechargeFrames());
        double spreadSample = rng.nextDouble("spread") - 0.5;
        requireModifierEffectCapacity((long) source.effects().size() + shotScope.state.effects().size());
        MutableProjectile projectile = new MutableProjectile(
            projectilePathPrefix + "/" + projectileSink.size(), source, shotScope, spreadSample
        );
        projectileSink.add(projectile);
        lastProjectile = projectile;
        if (!inPayload) {
            sounds.add(new SoundPlan(source.behavior().equals("FUSED_EXPLOSIVE") ? SoundPlan.SoundKind.FUSED_EXPLOSIVE_CAST : SoundPlan.SoundKind.PROJECTILE_CAST));
            recoils.add(new RecoilPlan(shotScope.state.recoil()));
        }
    }

    private void beginTrigger(BeginTriggerAction trigger) {
        beginTrigger(trigger, null);
    }

    private void beginTrigger(BeginTriggerAction trigger, NoitaDuration timerDelayOverride) {
        if (lastProjectile == null) {
            diagnostics.add(diagnostic("TRIGGER_WITHOUT_PROJECTILE", currentSpellId, 0, 0));
            return;
        }
        if (lastProjectile.trigger != null) {
            diagnostics.add(diagnostic("TRIGGER_ALREADY_ATTACHED", currentSpellId, 0, 0));
            return;
        }

        int nextPayloadDepth = payloadDepth + 1;
        payloadNodes++;
        if (payloadNodes > budget.payloadNodes()) {
            throw budgetExceeded("PAYLOAD_NODE_BUDGET", payloadNodes, budget.payloadNodes());
        }
        if (nextPayloadDepth > budget.payloadDepth()) {
            throw budgetExceeded("PAYLOAD_DEPTH_BUDGET", nextPayloadDepth, budget.payloadDepth());
        }

        MutableShotScope previousShotScope = shotScope;
        List<MutableProjectile> previousSink = projectileSink;
        MutableProjectile previousLast = lastProjectile;
        boolean previousInPayload = inPayload;
        int previousPayloadDepth = payloadDepth;
        String previousPathPrefix = projectilePathPrefix;
        List<MutableProjectile> payloads = new ArrayList<>();
        String triggerPath = previousLast.nodePath + "/trigger";
        try {
            payloadDepth = nextPayloadDepth;
            maxPayloadDepth = Math.max(maxPayloadDepth, payloadDepth);
            inPayload = true;
            shotScope = new MutableShotScope(nextShotScopeId++);
            projectileSink = payloads;
            projectilePathPrefix = triggerPath;
            lastProjectile = null;
            draw(DrawRequest.payload(trigger.drawCount()));
            previousLast.trigger = new MutableTrigger(
                triggerPath, trigger.mode(), timerDelayOverride == null ? previousLast.source.triggerDelay() : timerDelayOverride,
                payloadDepth, TriggerReleasePolicy.forMode(trigger.mode()), payloads
            );
        } finally {
            payloadDepth = previousPayloadDepth;
            shotScope = previousShotScope;
            projectileSink = previousSink;
            projectilePathPrefix = previousPathPrefix;
            lastProjectile = previousLast;
            inPayload = previousInPayload;
        }
    }

    private ActionInvocationResult invokeTargets(TargetQuery query, InvocationKind kind, InvocationPolicy policy,
                                                 int divideIteration) {
        List<SelectedTarget> targets = selectTargets(query);
        if (targets.isEmpty()) {
            diagnostics.add(diagnostic("CALL_TARGET_MISSING", currentSpellId, 0, 0));
            trace.add(kind, currentSpellId, "", "", currentRecursionLevel(), divideIteration,
                policy.allowTargetDraw(), currentNodePath(), actionSteps, "TARGET_MISSING");
            return ActionInvocationResult.SKIPPED;
        }
        ActionInvocationResult result = ActionInvocationResult.SKIPPED;
        for (SelectedTarget target : targets) {
            result = result.merge(invokeTarget(target, kind, policy, divideIteration));
        }
        return result;
    }

    private ActionInvocationResult invokeTarget(SelectedTarget target, InvocationKind kind, InvocationPolicy policy,
                                                int divideIteration) {
        SpellDefinition definition = target.definition;
        int callerLevel = currentRecursionLevel();
        if (definition.recursive() && callerLevel >= budget.recursiveCallDepth()) {
            diagnostics.add(diagnostic("RECURSIVE_CALL_LIMIT", definition.id(), callerLevel,
                budget.recursiveCallDepth()));
            trace.add(kind, currentSpellId, definition.id(), target.pile, callerLevel, divideIteration,
                policy.allowTargetDraw(), currentNodePath(), actionSteps, "RECURSIVE_CALL_LIMIT");
            return ActionInvocationResult.SKIPPED;
        }
        if (policy.checkTargetUses() && target.ref != null && !cards.get(target.ref).hasUsesRemaining()) {
            trace.add(kind, currentSpellId, definition.id(), target.pile, callerLevel, divideIteration,
                policy.allowTargetDraw(), currentNodePath(), actionSteps, "DEPLETED_USES");
            return ActionInvocationResult.SKIPPED;
        }
        if (policy.checkTargetMana() && definition.manaCost() > mana) {
            trace.add(kind, currentSpellId, definition.id(), target.pile, callerLevel, divideIteration,
                policy.allowTargetDraw(), currentNodePath(), actionSteps, "INSUFFICIENT_MANA");
            return ActionInvocationResult.SKIPPED;
        }

        int targetLevel = definition.recursive() ? callerLevel + 1 : callerLevel;
        CallFrame frame = new CallFrame(kind, currentSpellId, definition.id(), targetLevel, divideIteration,
            drawSuppressionDepth, shotScope.id, currentNodePath(), actionPath);
        double previousMana = mana;
        double previousCastDelay = castDelayTiming.frames;
        double previousRecharge = rechargeTiming.frames;
        trace.add(kind, currentSpellId, definition.id(), target.pile, targetLevel, divideIteration,
            policy.allowTargetDraw(), currentNodePath(), actionSteps, "");
        callFrames.push(frame);
        if (!policy.allowTargetDraw()) {
            drawSuppressionDepth++;
        }
        try {
            if (policy.checkTargetMana()) {
                mana -= definition.manaCost();
            }
            if (policy.enterHand() && target.ref != null && !hand.contains(target.ref)) {
                hand.add(target.ref);
            }
            ActionInvocationResult result = executeDefinition(definition, target.ref, ExecutionScope.CALLED);
            if (policy.automaticUseConsumption() && target.ref != null) {
                consumeManualUse(target.ref);
            }
            return result;
        } finally {
            if (!policy.allowTargetDraw()) {
                drawSuppressionDepth--;
            }
            callFrames.pop();
            if (policy.restoreMana()) {
                mana = previousMana;
            }
            if (policy.restoreCastDelay()) {
                castDelayTiming.frames = previousCastDelay;
            }
            if (policy.restoreRecharge()) {
                rechargeTiming.frames = previousRecharge;
            }
        }
    }

    private List<SelectedTarget> selectTargets(TargetQuery query) {
        List<SelectedTarget> selected = new ArrayList<>();
        for (TargetQuery.Source source : query.sources()) {
            List<SelectedTarget> candidates = source == TargetQuery.Source.EXTERNAL
                ? externalTargets()
                : pileTargets(source);
            if (query.direction() == TargetQuery.Direction.LAST) {
                for (int index = candidates.size() - 1; index >= 0 && selected.size() < query.limit(); index--) {
                    addIfMatching(selected, candidates.get(index), query);
                }
            } else {
                for (SelectedTarget candidate : candidates) {
                    if (selected.size() >= query.limit()) {
                        break;
                    }
                    addIfMatching(selected, candidate, query);
                }
            }
            if (query.direction() != TargetQuery.Direction.ALL && !selected.isEmpty()) {
                break;
            }
        }
        return List.copyOf(selected);
    }

    private void addIfMatching(List<SelectedTarget> selected, SelectedTarget candidate, TargetQuery query) {
        SpellDefinition definition = candidate.definition;
        if ((!query.categories().isEmpty() && !query.categories().contains(definition.category()))
            || query.excludedSpellIds().contains(definition.id())
            || (!query.recursiveTargetsAllowed() && definition.recursive())
            || (query.requireRelatedProjectile() && definition.relatedProjectile().isBlank())) {
            return;
        }
        selected.add(candidate);
    }

    private List<SelectedTarget> pileTargets(TargetQuery.Source source) {
        List<CardRef> pile = switch (source) {
            case DECK -> deck;
            case HAND -> hand;
            case DISCARD -> discard;
            case EXTERNAL -> throw new IllegalArgumentException("external targets do not have a wand pile");
        };
        List<SelectedTarget> targets = new ArrayList<>();
        for (CardRef ref : List.copyOf(pile)) {
            SpellCardState card = cards.get(ref);
            SpellDefinition definition = card == null ? null : catalog.definitions().get(card.spellId());
            if (definition != null) {
                targets.add(new SelectedTarget(ref, definition, source.name()));
            }
        }
        return targets;
    }

    private List<SelectedTarget> externalTargets() {
        List<SelectedTarget> targets = new ArrayList<>();
        for (String spellId : externalSpellPool.spellIds()) {
            SpellDefinition definition = catalog.definitions().get(spellId);
            if (definition != null) {
                targets.add(new SelectedTarget(null, definition, TargetQuery.Source.EXTERNAL.name()));
            }
        }
        return targets;
    }

    private ActionInvocationResult duplicateHand(String duplicateSpellId, ExecutionScope scope) {
        int snapshotSize = hand.size();
        ActionInvocationResult result = ActionInvocationResult.SKIPPED;
        for (int index = 0; index < snapshotSize; index++) {
            CardRef ref = hand.get(index);
            SpellCardState card = cards.get(ref);
            SpellDefinition target = card == null ? null : catalog.definitions().get(card.spellId());
            if (target != null && !target.id().equals(duplicateSpellId)) {
                result = result.merge(invokeTarget(new SelectedTarget(ref, target, "HAND"), InvocationKind.COPY,
                    InvocationPolicy.CALL, 0));
            }
        }
        castDelayTiming.apply(TimingOperation.ADD, 20.0);
        rechargeTiming.apply(TimingOperation.ADD, 20.0);
        // Permanent Duplicate executes its copy body, but its final single Draw
        // follows the permanent-card suppression rule.
        if (scope != ExecutionScope.PERMANENT && drawSuppressionDepth == 0) {
            draw(DrawRequest.action(1));
        } else if (scope != ExecutionScope.PERMANENT) {
            trace.add(InvocationKind.COPY, duplicateSpellId, "", "", currentRecursionLevel(),
                currentDivideIteration(), false, currentNodePath(), actionSteps, "DRAW_SUPPRESSED");
        }
        return result;
    }

    private ActionInvocationResult executeGreek(GreekCopyKind kind) {
        return switch (kind) {
            case TAU -> invokeTargets(TargetQuery.tau(), InvocationKind.CALL, InvocationPolicy.CALL, 0);
            case OMEGA -> executeOmega();
            case MU -> executeFilteredGreek(SpellCategory.PROJECTILE_MODIFIER, true);
            case PHI -> executeFilteredGreek(SpellCategory.PROJECTILE, false);
            case SIGMA -> executeFilteredGreek(SpellCategory.STATIC_PROJECTILE, true);
            case ZETA -> executeZeta();
        };
    }

    private ActionInvocationResult executeOmega() {
        List<SelectedTarget> targets = new ArrayList<>();
        for (SelectedTarget target : pileTargets(TargetQuery.Source.DISCARD)) {
            if (!containsRefresh(target.definition)) {
                targets.add(target);
            }
        }
        for (SelectedTarget target : pileTargets(TargetQuery.Source.HAND)) {
            if (!target.definition.recursive()) {
                targets.add(target);
            }
        }
        for (SelectedTarget target : pileTargets(TargetQuery.Source.DECK)) {
            if (!containsRefresh(target.definition)) {
                targets.add(target);
            }
        }
        ActionInvocationResult result = ActionInvocationResult.SKIPPED;
        for (SelectedTarget target : targets) {
            result = result.merge(invokeTarget(target, InvocationKind.COPY, InvocationPolicy.COPY_NO_DRAW, 0));
        }
        return result;
    }

    private boolean containsRefresh(SpellDefinition definition) {
        return definition.actions().stream().anyMatch(RefreshWandAction.class::isInstance);
    }

    private ActionInvocationResult executeFilteredGreek(SpellCategory category, boolean drawAfter) {
        TargetQuery query = TargetQuery.allWand(Set.of(category));
        ActionInvocationResult result = invokeTargets(query, InvocationKind.COPY, InvocationPolicy.FILTERED_COPY, 0);
        if (drawAfter && drawSuppressionDepth == 0) {
            draw(DrawRequest.action(1));
        }
        return result;
    }

    private ActionInvocationResult executeZeta() {
        List<SelectedTarget> candidates = selectTargets(TargetQuery.externalRandom());
        ActionInvocationResult result = ActionInvocationResult.SKIPPED;
        if (candidates.isEmpty()) {
            diagnostics.add(diagnostic("EXTERNAL_CALL_TARGET_MISSING", currentSpellId, 0, 0));
            trace.add(InvocationKind.COPY, currentSpellId, "", "EXTERNAL", currentRecursionLevel(), 0,
                false, currentNodePath(), actionSteps, "TARGET_MISSING");
        } else {
            SelectedTarget selected = candidates.get(rng.nextInt("zeta-external", candidates.size()));
            result = invokeTarget(selected, InvocationKind.COPY, InvocationPolicy.COPY_NO_DRAW, 0);
        }
        if (drawSuppressionDepth == 0) {
            draw(DrawRequest.action(1));
        }
        return result;
    }

    private ActionInvocationResult executeDivide(DivideAction divide) {
        int iteration = Math.max(1, currentDivideIteration());
        if (iteration > deck.size()) {
            diagnostics.add(diagnostic("DIVIDE_TARGET_MISSING", currentSpellId, iteration, deck.size()));
            applyDivideEffects(divide);
            return ActionInvocationResult.SKIPPED;
        }
        CardRef targetRef = deck.get(iteration - 1);
        SpellCardState card = cards.get(targetRef);
        SpellDefinition target = card == null ? null : catalog.definitions().get(card.spellId());
        if (target == null) {
            diagnostics.add(diagnostic("UNKNOWN_CALL_TARGET", card == null ? "" : card.spellId(), 0, 0));
            applyDivideEffects(divide);
            return ActionInvocationResult.SKIPPED;
        }
        if (!card.hasUsesRemaining()) {
            diagnostics.add(diagnostic("DIVIDE_TARGET_DEPLETED", target.id(), 0, 0));
            applyDivideEffects(divide);
            return ActionInvocationResult.SKIPPED;
        }

        boolean outermost = callFrames.stream().noneMatch(frame -> frame.kind() == InvocationKind.DIVIDE);
        double divideBaseCastDelay = castDelayTiming.frames;
        double divideBaseRecharge = rechargeTiming.frames;
        int copies = iteration >= divide.iterationThreshold() ? 1 : divide.copies();
        ActionInvocationResult result = ActionInvocationResult.SKIPPED;
        for (int copy = 0; copy < copies; copy++) {
            int nextIteration = iteration + 1;
            InvocationPolicy policy = InvocationPolicy.CALL.withTargetDraw(copy > 0);
            result = result.merge(invokeTarget(new SelectedTarget(targetRef, target, "DECK"),
                InvocationKind.DIVIDE, policy, nextIteration));
        }
        if (result.invoked()) {
            consumeManualUse(targetRef);
        }
        if (outermost) {
            // Noita restores timing produced by copied targets at the outer
            // Divide boundary while retaining the Divide card's own penalty.
            castDelayTiming.frames = divideBaseCastDelay;
            rechargeTiming.frames = divideBaseRecharge;
        }
        applyDivideEffects(divide);

        int maxIteration = Math.max(iteration, result.maxDivideIteration());
        if (outermost && result.invoked()) {
            int consumed = Math.min(maxIteration, deck.size());
            for (int index = 0; index < consumed; index++) {
                discard.add(deck.remove(0));
            }
        }
        return new ActionInvocationResult(result.invoked(), maxIteration);
    }

    private void applyDivideEffects(DivideAction divide) {
        castDelayTiming.apply(TimingOperation.ADD, divide.castDelayFrames());
        rechargeTiming.apply(TimingOperation.ADD, divide.rechargeFrames());
        shotScope.state = shotScope.state.applyDividePenalty(divide.damagePenalty(),
            divide.explosionRadiusPenalty(), divide.patternDegrees());
    }

    private ActionInvocationResult addTriggerToNextProjectile(AddTriggerToNextProjectileAction action) {
        List<CardRef> prefix = new ArrayList<>();
        CardRef targetRef = null;
        SpellDefinition target = null;
        for (CardRef ref : List.copyOf(deck)) {
            SpellCardState card = cards.get(ref);
            SpellDefinition candidate = card == null ? null : catalog.definitions().get(card.spellId());
            if (candidate == null) {
                prefix.add(ref);
                continue;
            }
            if (candidate.actions().stream().anyMatch(AddTriggerToNextProjectileAction.class::isInstance)) {
                prefix.add(ref);
                continue;
            }
            if (candidate.category() == SpellCategory.PROJECTILE_MODIFIER) {
                if (card.hasUsesRemaining()) {
                    invokeTarget(new SelectedTarget(ref, candidate, "DECK"), InvocationKind.ADD_TRIGGER,
                        InvocationPolicy.COPY_NO_DRAW, 0);
                }
                prefix.add(ref);
                continue;
            }
            if (candidate.category() == SpellCategory.PASSIVE || candidate.category() == SpellCategory.OTHER
                || candidate.category() == SpellCategory.MULTICAST) {
                prefix.add(ref);
                continue;
            }
            if (!candidate.relatedProjectile().isBlank() && card.hasUsesRemaining()) {
                targetRef = ref;
                target = candidate;
            }
            break;
        }
        if (targetRef == null || target == null) {
            diagnostics.add(diagnostic("ADD_TRIGGER_TARGET_MISSING", currentSpellId, 0, 0));
            return ActionInvocationResult.SKIPPED;
        }

        for (CardRef ref : prefix) {
            if (deck.remove(ref)) {
                discard.add(ref);
            }
        }
        deck.remove(targetRef);
        discard.add(targetRef);
        consumeManualUse(targetRef);

        boolean hasPayload = deck.stream().map(cards::get).filter(Objects::nonNull)
            .filter(SpellCardState::hasUsesRemaining)
            .map(card -> catalog.definitions().get(card.spellId())).filter(Objects::nonNull)
            .map(SpellDefinition::category)
            .anyMatch(category -> category == SpellCategory.PROJECTILE
                || category == SpellCategory.STATIC_PROJECTILE || category == SpellCategory.MATERIAL
                || category == SpellCategory.UTILITY);
        ActionInvocationResult result;
        if (hasPayload) {
            AddProjectileAction projectileAction = target.actions().stream()
                .filter(AddProjectileAction.class::isInstance)
                .map(AddProjectileAction.class::cast)
                .findFirst()
                .orElse(null);
            if (projectileAction == null) {
                diagnostics.add(diagnostic("ADD_TRIGGER_RELATED_PROJECTILE_MISSING", target.id(), 0, 0));
                return ActionInvocationResult.SKIPPED;
            }
            double previousCastDelay = castDelayTiming.frames;
            double previousRecharge = rechargeTiming.frames;
            addProjectile(projectileAction.projectile());
            castDelayTiming.frames = previousCastDelay;
            rechargeTiming.frames = previousRecharge;
            trace.add(InvocationKind.ADD_TRIGGER, currentSpellId, target.id(), "DECK", currentRecursionLevel(),
                0, false, currentNodePath(), actionSteps, "RELATED_PROJECTILE");
            beginTrigger(new BeginTriggerAction(action.mode(), 1), action.timerDelay());
            result = new ActionInvocationResult(true, 0);
        } else {
            result = invokeTarget(new SelectedTarget(targetRef, target, "DECK"),
                InvocationKind.ADD_TRIGGER, InvocationPolicy.COPY_NO_DRAW, 0);
        }
        return result;
    }

    private void consumeManualUse(CardRef ref) {
        SpellCardState card = cards.get(ref);
        if (card != null && card.remainingUses() > 0) {
            cards.put(ref, card.withRemainingUses(card.remainingUses() - 1));
        }
    }

    private void refreshWand() {
        if (refreshCount++ > 0) {
            // gun.lua only performs the first pile reset; later calls request a recharge.
            startReload = true;
            return;
        }
        List<CardRef> allCards = new ArrayList<>(hand.size() + deck.size() + discard.size());
        allCards.addAll(hand);
        allCards.addAll(deck);
        allCards.addAll(discard);
        hand.clear();
        deck.clear();
        discard.clear();
        deck.addAll(order(allCards, "wand-refresh"));
    }

    private void resolveRandomSpell(SpellCategory category) {
        List<SpellDefinition> candidates = new ArrayList<>();
        for (SpellDefinition candidate : catalog.candidates(category)) {
            boolean randomOnly = candidate.actions().stream().allMatch(action -> action instanceof RandomSpellAction);
            if (!randomOnly) {
                candidates.add(candidate);
            }
        }
        if (candidates.isEmpty()) {
            diagnostics.add(diagnostic("RANDOM_CANDIDATE_MISSING", currentSpellId, 0, 0));
            return;
        }
        SpellDefinition chosen = candidates.get(rng.nextInt("random-spell:" + category.name(), candidates.size()));
        invokeTarget(new SelectedTarget(null, chosen, "CATALOG"), InvocationKind.CALL, InvocationPolicy.CALL, 0);
    }

    private void finishHand() {
        boolean projectileShot = hand.stream().map(cards::get).filter(Objects::nonNull)
            .map(card -> catalog.definitions().get(card.spellId())).filter(Objects::nonNull)
            .map(SpellDefinition::category).anyMatch(this::isProjectileCategory);

        for (CardRef ref : List.copyOf(hand)) {
            SpellCardState card = cards.get(ref);
            SpellDefinition definition = card == null ? null : catalog.definitions().get(card.spellId());
            if (card != null && definition != null && card.remainingUses() > 0
                && shouldConsumeUse(definition.useConsumptionPolicy(), projectileShot)) {
                cards.put(ref, card.withRemainingUses(card.remainingUses() - 1));
            }
            discard.add(ref);
        }
        hand.clear();
    }

    private boolean isProjectileCategory(SpellCategory category) {
        return category == SpellCategory.PROJECTILE || category == SpellCategory.STATIC_PROJECTILE || category == SpellCategory.MATERIAL;
    }

    private static boolean shouldConsumeUse(UseConsumptionPolicy policy, boolean projectileShot) {
        return switch (policy) {
            case WHEN_PROJECTILE_SHOT -> projectileShot;
            case ALWAYS_ON_HAND_DISCARD -> true;
            case NEVER -> false;
        };
    }

    /** Reload is evaluated now so the persisted Deck order comes from this cast's seed. */
    private void reloadDeck() {
        List<CardRef> nextDeck = new ArrayList<>(deck.size() + discard.size());
        nextDeck.addAll(deck);
        nextDeck.addAll(discard);
        deck.clear();
        discard.clear();
        deck.addAll(order(nextDeck, "reload"));
    }

    private void reserveProjectiles(int count) {
        projectileNodes += count;
        if (projectileNodes > budget.projectileNodes()) {
            throw budgetExceeded("PROJECTILE_BUDGET", projectileNodes, budget.projectileNodes());
        }
    }

    private void consumeAction(String spellId) {
        actionSteps++;
        if (actionSteps > budget.actionSteps()) {
            throw budgetExceeded("ACTION_BUDGET", actionSteps, budget.actionSteps());
        }
        currentSpellId = spellId;
    }

    private List<CardRef> order(Iterable<CardRef> refs, String domain) {
        List<CardRef> ordered = new ArrayList<>();
        for (CardRef ref : refs) {
            ordered.add(ref);
        }
        ordered.sort((left, right) -> Integer.compare(cards.get(left).slot(), cards.get(right).slot()));
        if (wand.shuffle()) {
            for (int index = ordered.size() - 1; index > 0; index--) {
                int other = rng.nextInt("shuffle:" + domain, index + 1);
                CardRef value = ordered.get(index);
                ordered.set(index, ordered.get(other));
                ordered.set(other, value);
            }
        }
        return ordered;
    }

    private WandState currentState() {
        return new WandState(new DeckState(cards, deck, hand, discard), settledMana(), castDelayRemaining,
            rechargeRemaining, rechargePending, originalState.revision(), originalState.stateHash());
    }

    private ResolvedCast result(ResolvedCast.Status status, WandState nextState, EffectPlan plan) {
        Map<CardRef, Integer> remainingUses = new LinkedHashMap<>();
        for (Map.Entry<CardRef, SpellCardState> entry : nextState.deckState().cards().entrySet()) {
            remainingUses.put(entry.getKey(), entry.getValue().remainingUses());
        }
        return new ResolvedCast(status, nextState, plan, remainingUses, drawOutcomes,
            nextState.castDelayRemaining(), nextState.rechargeRemaining(), rng.rootSeed(), catalog.epoch(), catalog.hash(),
            new BudgetUsage(actionSteps, projectileNodes, payloadNodes, maxPayloadDepth,
                reservedSpawnedEntities == 0 ? rootSpawnedEntities : reservedSpawnedEntities), diagnostics, trace.build());
    }

    private CastDiagnostic diagnostic(String code, String spellId, int used, int limit) {
        String nodePath = currentNodePath();
        return new CastDiagnostic(code, spellId, actionPath, used, limit, nodePath);
    }

    private String currentNodePath() {
        return lastProjectile == null ? projectilePathPrefix : lastProjectile.nodePath;
    }

    private int currentRecursionLevel() {
        return callFrames.isEmpty() ? 0 : callFrames.peek().recursionLevel();
    }

    private int currentDivideIteration() {
        return callFrames.isEmpty() ? 0 : callFrames.peek().divideIteration();
    }

    private InvocationKind currentInvocationKind() {
        return callFrames.isEmpty() ? InvocationKind.DRAW : callFrames.peek().kind();
    }

    private BudgetExceeded budgetExceeded(String code, int used, int limit) {
        return new BudgetExceeded(diagnostic(code, currentSpellId, used, limit));
    }

    /**
     * Modifier effects are stored in every frozen projectile NBT node. Reject
     * before WandState commit instead of creating a cast that reload decoding
     * must later discard as malformed.
     */
    private void requireModifierEffectCapacity(long effectCount) {
        if (effectCount > ProjectilePlan.MAX_MODIFIER_EFFECTS) {
            throw budgetExceeded("MODIFIER_EFFECT_BUDGET", cappedInt(effectCount), ProjectilePlan.MAX_MODIFIER_EFFECTS);
        }
    }

    /**
     * Reserve the static lower bound of the frozen tree before cards, mana and
     * cooldown become visible. Runtime retains capacity for legal Piercing
     * repeats, but an accepted first release must never be impossible solely
     * because roots consumed all entity slots.
     */
    private void reserveRuntimeTree(List<ProjectilePlan> projectiles) {
        long entities = 0L;
        long releaseEvents = 0L;
        for (ProjectilePlan projectile : projectiles) {
            entities = addCapped(entities, projectile.staticEntityFootprint());
            releaseEvents = addCapped(releaseEvents, projectile.staticReleaseEventFootprint());
        }
        int entityLimit = Math.min(budget.spawnedEntities(), CastBudget.DEFAULT_SPAWNED_ENTITIES);
        if (entities > entityLimit) {
            throw budgetExceeded("SPAWNED_ENTITY_BUDGET", cappedInt(entities), entityLimit);
        }
        if (releaseEvents > CastBudget.DEFAULT_RUNTIME_RELEASE_EVENTS) {
            throw budgetExceeded("TRIGGER_RELEASE_EVENT_BUDGET", cappedInt(releaseEvents),
                CastBudget.DEFAULT_RUNTIME_RELEASE_EVENTS);
        }
        reservedSpawnedEntities = (int) entities;
    }

    private static long addCapped(long left, long right) {
        return right > Long.MAX_VALUE - left ? Long.MAX_VALUE : left + right;
    }

    private static int cappedInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private double settledMana() {
        // The local Wiki record states that preloading may exceed manaMax, but the
        // overflow is removed when preloading completes. Keep it available only
        // while this session evaluates later cards.
        return Math.max(0.0, Math.min(wand.manaMax(), mana));
    }

    private List<ProjectilePlan> freeze(List<MutableProjectile> mutableProjectiles) {
        List<ProjectilePlan> plans = new ArrayList<>(mutableProjectiles.size());
        for (MutableProjectile projectile : mutableProjectiles) {
            ProjectileDefinition source = projectile.source;
            ShotState finalShot = projectile.scope.state;
            TriggerPlan trigger = null;
            if (projectile.trigger != null) {
                MutableTrigger mutableTrigger = projectile.trigger;
                PayloadPlan payload = new PayloadPlan(mutableTrigger.nodePath, mutableTrigger.payloadDepth, freeze(mutableTrigger.payloads));
                trigger = new TriggerPlan(mutableTrigger.nodePath, mutableTrigger.mode, mutableTrigger.timerDelay,
                    mutableTrigger.payloadDepth, mutableTrigger.releasePolicy, payload);
            }
            double divergence = wand.spreadDegrees() + source.spreadDegrees() + finalShot.spreadDegrees();
            double spreadOffset = divergence == 0.0 ? 0.0 : projectile.spreadSample * divergence;
            List<String> effects = new ArrayList<>(source.effects());
            effects.addAll(finalShot.effects());
            requireModifierEffectCapacity(effects.size());
            plans.add(new ProjectilePlan(projectile.nodePath, source.itemPath(), source.behavior(),
                Math.max(0.0, source.damage() + finalShot.damage()),
                source.criticalChancePercent() + finalShot.criticalChancePercent(),
                NoitaDuration.frames(Math.max(1.0, source.lifetime().frames() + finalShot.lifetimeFrames())),
                source.trailLightStacks() + finalShot.trailLightStacks(),
                Math.max(0.0, source.explosionRadius() + finalShot.explosionRadius()),
                Math.max(0.0, source.speed() * wand.speedMultiplier() * finalShot.speedMultiplier()), spreadOffset,
                source.gravity() + finalShot.gravity(), source.drag(), source.bounceDamping(), source.renderScale(),
                source.knockbackForce() + finalShot.knockbackForce(), source.friendlyFire() || finalShot.friendlyFire(),
                source.piercing() || finalShot.piercing(), source.projectileCount(),
                source.burstSpreadDegrees() + finalShot.patternDegrees(), trigger,
                finalShot.bounceCount(), effects));
        }
        return List.copyOf(plans);
    }

    private enum ExecutionScope {
        NORMAL,
        CALLED,
        PERMANENT
    }

    private static final class TimingAccumulator {
        private double frames;

        private TimingAccumulator(double frames) {
            this.frames = frames;
        }

        private void apply(TimingOperation operation, double value) {
            frames = operation == TimingOperation.SET ? value : frames + value;
        }

        private NoitaDuration duration() {
            return NoitaDuration.frames(Math.max(0.0, frames));
        }
    }

    private static final class MutableProjectile {
        private final String nodePath;
        private final ProjectileDefinition source;
        private final MutableShotScope scope;
        private final double spreadSample;
        private MutableTrigger trigger;

        private MutableProjectile(String nodePath, ProjectileDefinition source, MutableShotScope scope,
                                  double spreadSample) {
            this.nodePath = nodePath;
            this.source = source;
            this.scope = scope;
            this.spreadSample = spreadSample;
        }
    }

    private static final class MutableShotScope {
        private final int id;
        private ShotState state = ShotState.EMPTY;

        private MutableShotScope(int id) {
            this.id = id;
        }
    }

    private record SelectedTarget(CardRef ref, SpellDefinition definition, String pile) {
        private SelectedTarget {
            Objects.requireNonNull(definition, "definition");
            pile = pile == null ? "" : pile;
        }
    }

    private static final class MutableTrigger {
        private final String nodePath;
        private final TriggerMode mode;
        private final NoitaDuration timerDelay;
        private final int payloadDepth;
        private final TriggerReleasePolicy releasePolicy;
        private final List<MutableProjectile> payloads;

        private MutableTrigger(
            String nodePath,
            TriggerMode mode,
            NoitaDuration timerDelay,
            int payloadDepth,
            TriggerReleasePolicy releasePolicy,
            List<MutableProjectile> payloads
        ) {
            this.nodePath = nodePath;
            this.mode = mode;
            this.timerDelay = timerDelay;
            this.payloadDepth = payloadDepth;
            this.releasePolicy = releasePolicy;
            this.payloads = payloads;
        }
    }

    private static final class BudgetExceeded extends RuntimeException {
        private final CastDiagnostic diagnostic;

        private BudgetExceeded(CastDiagnostic diagnostic) {
            this.diagnostic = diagnostic;
        }
    }
}
