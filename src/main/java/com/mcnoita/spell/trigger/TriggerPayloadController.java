package com.mcnoita.spell.trigger;

import com.mcnoita.spell.NoitaPayloadPlan;
import com.mcnoita.spell.NoitaSpellTriggerMode;
import com.mcnoita.spell.NoitaTriggerPlan;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Shared server-side trigger state machine. It consumes collision, timer, and
 * termination events but never evaluates cards, charges mana, or queries a
 * SpellCatalog; every payload it returns was frozen during the original cast.
 */
public final class TriggerPayloadController {
    private final NoitaTriggerPlan plan;
    private TriggerRuntimeState state;

    public TriggerPayloadController(NoitaTriggerPlan plan, TriggerRuntimeState state) {
        this.plan = Objects.requireNonNull(plan, "plan");
        this.state = Objects.requireNonNull(state, "state");
    }

    public TriggerRuntimeState state() {
        return state;
    }

    /** Advances persisted Timer progress once per surviving server tick. */
    public void advanceTimer() {
        if (!state.inert() && !state.timerExpired()) {
            state = state.advanceTimer();
        }
    }

    public ReleaseDecision accept(TriggerEvent event) {
        Objects.requireNonNull(event, "event");
        if (state.inert() || !plan.isActive()) {
            return none();
        }

        TriggerReleaseReason reason = switch (event.kind()) {
            case COLLISION -> acceptCollision(event.collision());
            case TIMER_EXPIRED -> acceptTimerExpiry();
            case TERMINATED -> acceptTermination(event.terminationCause());
        };
        if (reason == null) {
            return none();
        }

        int plannedEntities = plan.spawnedEntityCount();
        if (!state.remainingBudget().canReserve(1, plannedEntities)) {
            // A final event is already marked above. Replaying it after a budget
            // failure must not resurrect a partial payload tree on a later tick.
            state = state.markInert();
            return ReleaseDecision.budgetExhausted(plan.nodePath(), state.remainingBudget(),
                TriggerBudgetExhaustion.DIRECT_RELEASE_CAPACITY);
        }
        List<RuntimeRequirement> childRequirements = childRequirements();
        int spawnedEntities = childRequirements.size();

        TriggerRuntimeBudget afterEvent = state.remainingBudget().reserve(1, spawnedEntities);
        List<TriggerRuntimeBudget> allocations = allocateBudgets(afterEvent, childRequirements);
        if (allocations == null) {
            // The evaluator reserves the first full frozen tree before commit.
            // This guard protects old/custom payloads that bypassed that path.
            state = state.markInert();
            return ReleaseDecision.budgetExhausted(plan.nodePath(), state.remainingBudget(),
                TriggerBudgetExhaustion.NESTED_TREE_RESERVATION);
        }
        // The continuing piercing parent and every spawned child receive a
        // disjoint share. Each child first receives enough capacity for one
        // complete nested release; only the remaining capacity is shared for
        // legal repeated Piercing collisions.
        TriggerRuntimeBudget parentBudget = allocations.get(0);
        state = state.reserve(parentBudget);
        return new ReleaseDecision(true, state.releaseSequence(), plan.nodePath(), reason,
            partitionPayloads(allocations.subList(1, allocations.size())), parentBudget, null);
    }

    private TriggerReleaseReason acceptCollision(CollisionKey collision) {
        // Timer's expiry release is its final event. A surviving or Piercing
        // parent may still receive Minecraft collision callbacks afterwards,
        // but those callbacks must not reopen the already-frozen Timer stage.
        if (!plan.releasePolicy().releasesOnCollision()
            || plan.mode() == NoitaSpellTriggerMode.TIMER && state.timerExpired()
            || collision.equals(state.latestCollision())) {
            return null;
        }
        // Store the key before returning a decision, so onEntityHit followed by
        // onCollision cannot release the same frozen tree twice in one tick.
        state = state.withCollision(collision);
        return TriggerReleaseReason.COLLISION;
    }

    private TriggerReleaseReason acceptTimerExpiry() {
        if (plan.mode() != NoitaSpellTriggerMode.TIMER || state.timerExpired()) {
            return null;
        }
        state = state.withTimerExpired();
        return TriggerReleaseReason.TIMER_EXPIRED;
    }

    private TriggerReleaseReason acceptTermination(ProjectileTerminationCause cause) {
        if (plan.mode() != NoitaSpellTriggerMode.EXPIRATION || state.expirationReleased()
            || !cause.releasesExpirationPayload()) {
            return null;
        }
        state = state.withExpirationReleased();
        return TriggerReleaseReason.EXPIRATION;
    }

    private List<PayloadRelease> partitionPayloads(List<TriggerRuntimeBudget> childBudgets) {
        List<PayloadRelease> releases = new ArrayList<>(plan.payloads().size());
        int offset = 0;
        for (NoitaPayloadPlan payload : plan.payloads()) {
            int childCount = payload.spawnedEntityCount();
            releases.add(new PayloadRelease(payload, childBudgets.subList(offset, offset + childCount)));
            offset += childCount;
        }
        return List.copyOf(releases);
    }

    private List<RuntimeRequirement> childRequirements() {
        List<RuntimeRequirement> requirements = new ArrayList<>(plan.spawnedEntityCount());
        for (NoitaPayloadPlan payload : plan.payloads()) {
            for (var projectile : payload.projectiles()) {
                RuntimeRequirement requirement = requirementForOneEntity(projectile);
                for (int index = 0; index < projectile.projectileCount(); index++) {
                    requirements.add(requirement);
                }
            }
        }
        return List.copyOf(requirements);
    }

    private static RuntimeRequirement requirementForOneEntity(com.mcnoita.spell.NoitaProjectilePayload payload) {
        NoitaTriggerPlan childPlan = payload.triggerPlan();
        if (!childPlan.isActive()) {
            return RuntimeRequirement.NONE;
        }
        long releaseEvents = 1L;
        long spawnedEntities = 0L;
        for (NoitaPayloadPlan childPayload : childPlan.payloads()) {
            for (var child : childPayload.projectiles()) {
                RuntimeRequirement nested = requirementForOneEntity(child);
                int count = child.projectileCount();
                releaseEvents = addCapped(releaseEvents, multiplyCapped(count, nested.releaseEvents()));
                spawnedEntities = addCapped(spawnedEntities,
                    multiplyCapped(count, addCapped(1L, nested.spawnedEntities())));
            }
        }
        return new RuntimeRequirement(cappedInt(releaseEvents), cappedInt(spawnedEntities));
    }

    private static List<TriggerRuntimeBudget> allocateBudgets(
        TriggerRuntimeBudget available, List<RuntimeRequirement> childRequirements
    ) {
        int childCount = childRequirements.size();
        int requiredEvents = 0;
        int requiredEntities = 0;
        for (RuntimeRequirement requirement : childRequirements) {
            requiredEvents = addCapped(requiredEvents, requirement.releaseEvents());
            requiredEntities = addCapped(requiredEntities, requirement.spawnedEntities());
        }
        if (!available.canReserve(requiredEvents, requiredEntities)) {
            return null;
        }

        int parts = childCount + 1;
        int[] events = new int[parts];
        int[] entities = new int[parts];
        for (int index = 0; index < childCount; index++) {
            events[index + 1] = childRequirements.get(index).releaseEvents();
            entities[index + 1] = childRequirements.get(index).spawnedEntities();
        }
        distribute(events, available.remainingReleaseEvents() - requiredEvents);
        distribute(entities, available.remainingSpawnedEntities() - requiredEntities);

        List<TriggerRuntimeBudget> allocations = new ArrayList<>(parts);
        for (int index = 0; index < parts; index++) {
            allocations.add(new TriggerRuntimeBudget(events[index], entities[index]));
        }
        return List.copyOf(allocations);
    }

    private static void distribute(int[] values, int extras) {
        for (int index = 0; index < extras; index++) {
            values[index % values.length]++;
        }
    }

    private static int addCapped(int left, int right) {
        return right > Integer.MAX_VALUE - left ? Integer.MAX_VALUE : left + right;
    }

    private static long addCapped(long left, long right) {
        return right > Long.MAX_VALUE - left ? Long.MAX_VALUE : left + right;
    }

    private static long multiplyCapped(long left, long right) {
        return left == 0L || right == 0L ? 0L : left > Long.MAX_VALUE / right ? Long.MAX_VALUE : left * right;
    }

    private static int cappedInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private record RuntimeRequirement(int releaseEvents, int spawnedEntities) {
        private static final RuntimeRequirement NONE = new RuntimeRequirement(0, 0);
    }

    private ReleaseDecision none() {
        return ReleaseDecision.none(plan.nodePath(), state.remainingBudget());
    }
}
