package com.mcnoita.wand.eval;

import com.mcnoita.spell.action.AddProjectileAction;
import com.mcnoita.spell.action.BeginTriggerAction;
import com.mcnoita.spell.action.CallSelection;
import com.mcnoita.spell.action.CallSpellAction;
import com.mcnoita.spell.action.DrawAction;
import com.mcnoita.spell.action.DuplicateHandAction;
import com.mcnoita.spell.action.ModifyShotAction;
import com.mcnoita.spell.action.RandomSpellAction;
import com.mcnoita.spell.action.RefreshWandAction;
import com.mcnoita.spell.action.SpellAction;
import com.mcnoita.spell.action.SpellCatalog;
import com.mcnoita.spell.action.SpellCategory;
import com.mcnoita.spell.action.SpellDefinition;
import com.mcnoita.spell.action.TimingAction;
import com.mcnoita.spell.plan.BudgetUsage;
import com.mcnoita.spell.plan.CastBudget;
import com.mcnoita.spell.plan.CastDiagnostic;
import com.mcnoita.spell.plan.EffectPlan;
import com.mcnoita.spell.plan.ProjectileDefinition;
import com.mcnoita.spell.plan.ProjectilePlan;
import com.mcnoita.spell.plan.RecoilPlan;
import com.mcnoita.spell.plan.ResolvedCast;
import com.mcnoita.spell.plan.ShotState;
import com.mcnoita.spell.plan.SoundPlan;
import com.mcnoita.spell.plan.TriggerMode;
import com.mcnoita.wand.model.CardRef;
import com.mcnoita.wand.model.DeckState;
import com.mcnoita.wand.model.NoitaDuration;
import com.mcnoita.wand.model.SpellCardState;
import com.mcnoita.wand.model.WandDefinition;
import com.mcnoita.wand.model.WandState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The only mutable workspace used while evaluating a cast. It owns Draw, Call,
 * Wrap, trigger branches and accounting; no Minecraft object can enter it.
 */
public final class WandCastSession {
    private final WandDefinition wand;
    private final WandState originalState;
    private final SpellCatalog catalog;
    private final NoitaDuration elapsed;
    private final CastRng rng;
    private final CastBudget budget;
    private final Map<CardRef, SpellCardState> cards;
    private final List<CardRef> deck;
    private final List<CardRef> hand;
    private final List<CardRef> discard;
    private final List<MutableProjectile> rootProjectiles = new ArrayList<>();
    private final List<SoundPlan> sounds = new ArrayList<>();
    private final List<RecoilPlan> recoils = new ArrayList<>();
    private final List<CastDiagnostic> diagnostics = new ArrayList<>();
    private final List<String> actionPath = new ArrayList<>();
    private final Set<CardRef> normalProjectileCards = new HashSet<>();

    private List<MutableProjectile> projectileSink = rootProjectiles;
    private MutableProjectile lastProjectile;
    private ShotState shotState = ShotState.EMPTY;
    private double mana;
    private NoitaDuration castDelayRemaining;
    private NoitaDuration rechargeRemaining;
    private boolean rechargePending;
    private boolean actionDrawWrapped;
    private boolean refreshed;
    private boolean inPayload;
    private double globalCastDelayFrames;
    private double globalRechargeFrames;
    private int recursiveCallDepth;
    private int payloadDepth;
    private int actionSteps;
    private int projectileNodes;
    private int payloadNodes;
    private String currentSpellId = "";

    WandCastSession(
        WandDefinition wand,
        WandState originalState,
        SpellCatalog catalog,
        NoitaDuration elapsed,
        CastRng rng,
        CastBudget budget
    ) {
        this.wand = Objects.requireNonNull(wand, "wand");
        this.originalState = Objects.requireNonNull(originalState, "originalState");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.elapsed = Objects.requireNonNull(elapsed, "elapsed");
        this.rng = Objects.requireNonNull(rng, "rng");
        this.budget = Objects.requireNonNull(budget, "budget");
        DeckState deckState = originalState.deckState();
        this.cards = new LinkedHashMap<>(deckState.cards());
        this.deck = new ArrayList<>(deckState.deck());
        this.hand = new ArrayList<>(deckState.hand());
        this.discard = new ArrayList<>(deckState.discard());
        this.mana = originalState.mana();
        this.castDelayRemaining = originalState.castDelayRemaining();
        this.rechargeRemaining = originalState.rechargeRemaining();
        this.rechargePending = originalState.rechargePending();
    }

    public ResolvedCast evaluate() {
        try {
            advanceTime();
            if (!isReady()) {
                diagnostics.add(diagnostic("COOLDOWN", "", 0, 0));
                return result(ResolvedCast.Status.REJECTED, currentState(), EffectPlan.empty());
            }

            for (String alwaysCastId : wand.alwaysCastSpellIds()) {
                SpellDefinition definition = catalog.definitions().get(alwaysCastId);
                if (definition == null) {
                    diagnostics.add(diagnostic("UNKNOWN_ALWAYS_CAST", alwaysCastId, 0, 0));
                    continue;
                }
                executeDefinition(definition, null, true);
            }
            drawMany(wand.spellsPerCast(), false, false);
            finishHand();

            boolean shouldRecharge = !cards.isEmpty() && (actionDrawWrapped || deck.isEmpty());
            NoitaDuration finalCastDelay = durationWithDelta(wand.castDelay(), globalCastDelayFrames);
            NoitaDuration finalRecharge = durationWithDelta(wand.rechargeTime(), globalRechargeFrames);
            WandState next = new WandState(
                new DeckState(cards, deck, hand, discard),
                clampMana(mana),
                finalCastDelay,
                shouldRecharge ? finalRecharge : NoitaDuration.ZERO,
                shouldRecharge,
                originalState.revision() + 1,
                originalState.stateHash()
            );
            return result(ResolvedCast.Status.ACCEPTED, next, new EffectPlan(freeze(rootProjectiles), sounds, recoils));
        } catch (BudgetExceeded exhausted) {
            diagnostics.add(exhausted.diagnostic);
            return result(ResolvedCast.Status.REJECTED, originalState, EffectPlan.empty());
        } catch (RuntimeException failure) {
            diagnostics.add(diagnostic("EVALUATION_ERROR", currentSpellId, 0, 0));
            return result(ResolvedCast.Status.REJECTED, originalState, EffectPlan.empty());
        }
    }

    private void advanceTime() {
        double restoredMana = elapsed.frames() / NoitaDuration.FRAMES_PER_SECOND * wand.manaChargePerSecond();
        mana = clampMana(mana + restoredMana);
        boolean rechargeCompleted = !rechargeRemaining.isZero() && elapsed.frames() >= rechargeRemaining.frames();
        castDelayRemaining = castDelayRemaining.minusFloorZero(elapsed);
        rechargeRemaining = rechargeRemaining.minusFloorZero(elapsed);
        if (rechargeCompleted && rechargePending) {
            deck.clear();
            deck.addAll(order(cards.keySet(), "reload"));
            hand.clear();
            discard.clear();
            rechargePending = false;
        }
    }

    private boolean isReady() {
        return castDelayRemaining.isZero() && rechargeRemaining.isZero();
    }

    private void drawMany(int amount, boolean canWrap, boolean permanent) {
        if (permanent) {
            return;
        }
        for (int drawn = 0; drawn < amount;) {
            consumeAction("DRAW");
            if (deck.isEmpty()) {
                if (canWrap && !discard.isEmpty()) {
                    deck.addAll(order(discard, "wrap"));
                    discard.clear();
                    actionDrawWrapped = true;
                    continue;
                }
                return;
            }

            CardRef ref = deck.remove(0);
            SpellCardState card = cards.get(ref);
            if (card == null || !card.hasUsesRemaining()) {
                discard.add(ref);
                continue;
            }
            SpellDefinition definition = catalog.definitions().get(card.spellId());
            if (definition == null) {
                discard.add(ref);
                diagnostics.add(diagnostic("UNKNOWN_SPELL", card.spellId(), 0, 0));
                continue;
            }
            if (definition.manaCost() > mana) {
                discard.add(ref);
                diagnostics.add(diagnostic("INSUFFICIENT_MANA", definition.id(), (int) Math.ceil(definition.manaCost()), (int) Math.floor(mana)));
                continue;
            }

            mana -= definition.manaCost();
            hand.add(ref);
            executeDefinition(definition, ref, false);
            drawn++;
        }
    }

    private void executeDefinition(SpellDefinition definition, CardRef origin, boolean permanent) {
        String previousSpellId = currentSpellId;
        currentSpellId = definition.id();
        actionPath.add(definition.id());
        try {
            for (SpellAction action : definition.actions()) {
                consumeAction(definition.id());
                executeAction(action, definition, origin, permanent);
            }
        } finally {
            actionPath.remove(actionPath.size() - 1);
            currentSpellId = previousSpellId;
        }
    }

    private void executeAction(SpellAction action, SpellDefinition definition, CardRef origin, boolean permanent) {
        if (action instanceof ModifyShotAction modify) {
            shotState = shotState.applyModifier(modify.modifier());
        } else if (action instanceof DrawAction draw) {
            drawMany(draw.amount(), true, permanent);
        } else if (action instanceof AddProjectileAction add) {
            addProjectile(add.projectile(), origin);
        } else if (action instanceof BeginTriggerAction trigger) {
            beginTrigger(trigger);
        } else if (action instanceof CallSpellAction call) {
            callSpell(call.selection());
        } else if (action instanceof DuplicateHandAction) {
            duplicateHand(definition.id(), permanent);
        } else if (action instanceof RefreshWandAction) {
            refreshWand();
        } else if (action instanceof RandomSpellAction random) {
            resolveRandomSpell(random.category());
        } else if (action instanceof TimingAction timing) {
            if (!inPayload) {
                globalCastDelayFrames += timing.castDelayFrames();
            }
            globalRechargeFrames += timing.rechargeFrames();
        }
    }

    private void addProjectile(ProjectileDefinition source, CardRef origin) {
        reserveProjectiles(source.projectileCount());
        if (!inPayload) {
            globalCastDelayFrames += source.castDelayFrames() + shotState.castDelayFrames();
        }
        // Noita trigger payload cast delay is isolated, but its recharge change stays global.
        globalRechargeFrames += source.rechargeFrames() + shotState.rechargeFrames();
        double divergence = wand.spreadDegrees() + source.spreadDegrees() + shotState.spreadDegrees();
        double spreadOffset = divergence == 0.0 ? 0.0 : (rng.nextDouble("spread") - 0.5) * divergence;
        List<String> effects = new ArrayList<>(source.effects());
        effects.addAll(shotState.effects());
        MutableProjectile projectile = new MutableProjectile(
            source.itemPath(), source.behavior(), Math.max(0.0, source.damage() + shotState.damage()),
            source.criticalChancePercent() + shotState.criticalChancePercent(),
            NoitaDuration.frames(Math.max(1.0, source.lifetime().frames() + shotState.lifetimeFrames())),
            source.trailLightStacks() + shotState.trailLightStacks(), Math.max(0.0, source.explosionRadius() + shotState.explosionRadius()),
            Math.max(0.0, source.speed() * wand.speedMultiplier() * shotState.speedMultiplier()), spreadOffset,
            source.gravity() + shotState.gravity(), source.drag(), source.bounceDamping(), source.renderScale(),
            source.knockbackForce() + shotState.knockbackForce(), source.friendlyFire() || shotState.friendlyFire(),
            source.piercing() || shotState.piercing(), source.projectileCount(), source.burstSpreadDegrees(),
            TriggerMode.NONE, source.triggerDelay(), shotState.bounceCount(), effects
        );
        projectileSink.add(projectile);
        lastProjectile = projectile;
        if (!inPayload) {
            sounds.add(new SoundPlan(source.behavior().equals("FUSED_EXPLOSIVE") ? SoundPlan.SoundKind.FUSED_EXPLOSIVE_CAST : SoundPlan.SoundKind.PROJECTILE_CAST));
            recoils.add(new RecoilPlan(shotState.recoil()));
            if (origin != null) {
                normalProjectileCards.add(origin);
            }
        }
    }

    private void beginTrigger(BeginTriggerAction trigger) {
        if (lastProjectile == null) {
            diagnostics.add(diagnostic("TRIGGER_WITHOUT_PROJECTILE", currentSpellId, 0, 0));
            return;
        }
        payloadNodes++;
        if (payloadNodes > budget.payloadNodes() || payloadDepth >= budget.payloadNodes()) {
            throw budgetExceeded("PAYLOAD_BUDGET", payloadNodes, budget.payloadNodes());
        }

        ShotState previousShot = shotState;
        List<MutableProjectile> previousSink = projectileSink;
        MutableProjectile previousLast = lastProjectile;
        boolean previousInPayload = inPayload;
        List<MutableProjectile> payloads = new ArrayList<>();
        try {
            payloadDepth++;
            inPayload = true;
            shotState = ShotState.EMPTY;
            projectileSink = payloads;
            lastProjectile = null;
            drawMany(trigger.drawCount(), true, false);
            previousLast.triggerMode = trigger.mode();
            previousLast.payloads.addAll(payloads);
        } finally {
            payloadDepth--;
            shotState = previousShot;
            projectileSink = previousSink;
            lastProjectile = previousLast;
            inPayload = previousInPayload;
        }
    }

    private void callSpell(CallSelection selection) {
        CardRef target = findCallTarget(selection);
        if (target == null) {
            diagnostics.add(diagnostic("CALL_TARGET_MISSING", currentSpellId, 0, 0));
            return;
        }
        SpellDefinition definition = catalog.definitions().get(cards.get(target).spellId());
        if (definition == null) {
            diagnostics.add(diagnostic("UNKNOWN_CALL_TARGET", cards.get(target).spellId(), 0, 0));
            return;
        }
        int previousDepth = recursiveCallDepth;
        if (definition.recursive()) {
            if (recursiveCallDepth >= budget.recursiveCallDepth()) {
                diagnostics.add(diagnostic("RECURSIVE_CALL_LIMIT", definition.id(), recursiveCallDepth, budget.recursiveCallDepth()));
                return;
            }
            recursiveCallDepth++;
        }
        try {
            executeDefinition(definition, null, false);
        } finally {
            recursiveCallDepth = previousDepth;
        }
    }

    private CardRef findCallTarget(CallSelection selection) {
        if (selection == CallSelection.FIRST_AVAILABLE) {
            return firstCopyable(discard, hand, deck);
        }
        return lastCopyable(deck, hand, discard);
    }

    @SafeVarargs
    private final CardRef firstCopyable(List<CardRef>... piles) {
        for (List<CardRef> pile : piles) {
            for (CardRef ref : pile) {
                if (cards.containsKey(ref)) {
                    return ref;
                }
            }
        }
        return null;
    }

    @SafeVarargs
    private final CardRef lastCopyable(List<CardRef>... piles) {
        for (List<CardRef> pile : piles) {
            for (int index = pile.size() - 1; index >= 0; index--) {
                CardRef ref = pile.get(index);
                if (cards.containsKey(ref)) {
                    return ref;
                }
            }
        }
        return null;
    }

    private void duplicateHand(String duplicateSpellId, boolean permanent) {
        if (permanent) {
            return;
        }
        for (CardRef ref : List.copyOf(hand)) {
            SpellDefinition target = catalog.definitions().get(cards.get(ref).spellId());
            if (target != null && !target.id().equals(duplicateSpellId)) {
                executeDefinition(target, null, false);
            }
        }
        drawMany(1, true, false);
    }

    private void refreshWand() {
        if (refreshed) {
            actionDrawWrapped = true;
            return;
        }
        refreshed = true;
        addUnique(discard, hand);
        addUnique(discard, deck);
        hand.clear();
        deck.clear();
        deck.addAll(order(discard, "wand-refresh"));
        discard.clear();
    }

    private static void addUnique(List<CardRef> target, List<CardRef> source) {
        for (CardRef ref : source) {
            if (!target.contains(ref)) {
                target.add(ref);
            }
        }
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
        executeDefinition(chosen, null, false);
    }

    private void finishHand() {
        for (CardRef ref : hand) {
            if (!discard.contains(ref)) {
                discard.add(ref);
            }
        }
        hand.clear();
        for (CardRef ref : normalProjectileCards) {
            SpellCardState card = cards.get(ref);
            if (card != null && card.remainingUses() > 0) {
                cards.put(ref, card.withRemainingUses(card.remainingUses() - 1));
            }
        }
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
        return new WandState(new DeckState(cards, deck, hand, discard), clampMana(mana), castDelayRemaining,
            rechargeRemaining, rechargePending, originalState.revision(), originalState.stateHash());
    }

    private ResolvedCast result(ResolvedCast.Status status, WandState nextState, EffectPlan plan) {
        Map<CardRef, Integer> remainingUses = new LinkedHashMap<>();
        for (Map.Entry<CardRef, SpellCardState> entry : nextState.deckState().cards().entrySet()) {
            remainingUses.put(entry.getKey(), entry.getValue().remainingUses());
        }
        return new ResolvedCast(status, nextState, plan, remainingUses,
            nextState.castDelayRemaining(), nextState.rechargeRemaining(), rng.rootSeed(), catalog.epoch(), catalog.hash(),
            new BudgetUsage(actionSteps, projectileNodes, payloadNodes), diagnostics);
    }

    private CastDiagnostic diagnostic(String code, String spellId, int used, int limit) {
        return new CastDiagnostic(code, spellId, actionPath, used, limit);
    }

    private BudgetExceeded budgetExceeded(String code, int used, int limit) {
        return new BudgetExceeded(diagnostic(code, currentSpellId, used, limit));
    }

    private double clampMana(double value) {
        return Math.max(0.0, Math.min(wand.manaMax(), value));
    }

    private static NoitaDuration durationWithDelta(NoitaDuration base, double deltaFrames) {
        return NoitaDuration.frames(Math.max(0.0, base.frames() + deltaFrames));
    }

    private static List<ProjectilePlan> freeze(List<MutableProjectile> mutableProjectiles) {
        List<ProjectilePlan> plans = new ArrayList<>(mutableProjectiles.size());
        for (MutableProjectile projectile : mutableProjectiles) {
            plans.add(new ProjectilePlan(projectile.itemPath, projectile.behavior, projectile.damage, projectile.criticalChancePercent,
                projectile.lifetime, projectile.trailLightStacks, projectile.explosionRadius, projectile.speed,
                projectile.spreadOffsetDegrees, projectile.gravity, projectile.drag, projectile.bounceDamping,
                projectile.renderScale, projectile.knockbackForce, projectile.friendlyFire, projectile.piercing,
                projectile.projectileCount, projectile.burstSpreadDegrees, projectile.triggerMode, projectile.triggerDelay,
                projectile.bounceCount, projectile.effects, freeze(projectile.payloads)));
        }
        return List.copyOf(plans);
    }

    private static final class MutableProjectile {
        private final String itemPath;
        private final String behavior;
        private final double damage;
        private final double criticalChancePercent;
        private final NoitaDuration lifetime;
        private final int trailLightStacks;
        private final double explosionRadius;
        private final double speed;
        private final double spreadOffsetDegrees;
        private final double gravity;
        private final double drag;
        private final double bounceDamping;
        private final double renderScale;
        private final double knockbackForce;
        private final boolean friendlyFire;
        private final boolean piercing;
        private final int projectileCount;
        private final double burstSpreadDegrees;
        private TriggerMode triggerMode;
        private final NoitaDuration triggerDelay;
        private final int bounceCount;
        private final List<String> effects;
        private final List<MutableProjectile> payloads = new ArrayList<>();

        private MutableProjectile(
            String itemPath, String behavior, double damage, double criticalChancePercent, NoitaDuration lifetime,
            int trailLightStacks, double explosionRadius, double speed, double spreadOffsetDegrees, double gravity,
            double drag, double bounceDamping, double renderScale, double knockbackForce, boolean friendlyFire,
            boolean piercing, int projectileCount, double burstSpreadDegrees, TriggerMode triggerMode,
            NoitaDuration triggerDelay, int bounceCount, List<String> effects
        ) {
            this.itemPath = itemPath;
            this.behavior = behavior;
            this.damage = damage;
            this.criticalChancePercent = criticalChancePercent;
            this.lifetime = lifetime;
            this.trailLightStacks = trailLightStacks;
            this.explosionRadius = explosionRadius;
            this.speed = speed;
            this.spreadOffsetDegrees = spreadOffsetDegrees;
            this.gravity = gravity;
            this.drag = drag;
            this.bounceDamping = bounceDamping;
            this.renderScale = renderScale;
            this.knockbackForce = knockbackForce;
            this.friendlyFire = friendlyFire;
            this.piercing = piercing;
            this.projectileCount = projectileCount;
            this.burstSpreadDegrees = burstSpreadDegrees;
            this.triggerMode = triggerMode;
            this.triggerDelay = triggerDelay;
            this.bounceCount = bounceCount;
            this.effects = List.copyOf(effects);
        }
    }

    private static final class BudgetExceeded extends RuntimeException {
        private final CastDiagnostic diagnostic;

        private BudgetExceeded(CastDiagnostic diagnostic) {
            this.diagnostic = diagnostic;
        }
    }
}
