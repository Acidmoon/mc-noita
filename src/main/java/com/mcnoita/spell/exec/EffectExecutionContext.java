package com.mcnoita.spell.exec;

import com.mcnoita.MCNoita;
import com.mcnoita.spell.plan.EffectNode;
import com.mcnoita.spell.plan.ProjectileEffectNode;
import com.mcnoita.spell.plan.ResolvedCast;
import com.mcnoita.spell.server.budget.BudgetReservation;
import com.mcnoita.spell.trigger.TriggerRuntimeBudget;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/**
 * Server-only execution data shared by typed node executors. It receives the
 * transaction's execution identity rather than inventing one after commit.
 */
public final class EffectExecutionContext {
    private static final double SPELL_SPAWN_FORWARD_OFFSET = 0.65;
    private static final double SPELL_SPAWN_RIGHT_OFFSET = 0.35;
    private static final double SPELL_SPAWN_DOWN_OFFSET = 0.25;
    private static final long FAILURE_LOG_INTERVAL_TICKS = 200L;
    private static final Map<String, Long> LAST_FAILURE_LOG_TICK = new HashMap<>();

    private final ServerPlayerEntity player;
    private final ServerWorld world;
    private final Vec3d spawnPosition;
    private final Vec3d direction;
    private final UUID executionId;
    private final long catalogEpoch;
    private final String catalogHash;
    private final BudgetReservation reservation;
    private final EffectNodeBudgetReleaser budgetReleaser;
    private final boolean rootBudgetAccepted;
    private final Map<String, List<TriggerRuntimeBudget>> rootProjectileBudgets;

    public EffectExecutionContext(
        ServerPlayerEntity player, ResolvedCast resolvedCast, UUID executionId, BudgetReservation reservation
    ) {
        this(player, resolvedCast, executionId, reservation, EffectNodeBudgetReleaser.forReservation(reservation));
    }

    /** A transaction may supply exact per-node slices instead of the conservative default mapper. */
    public EffectExecutionContext(
        ServerPlayerEntity player, ResolvedCast resolvedCast, UUID executionId, BudgetReservation reservation,
        EffectNodeBudgetReleaser budgetReleaser
    ) {
        this.player = Objects.requireNonNull(player, "player");
        Objects.requireNonNull(resolvedCast, "resolvedCast");
        this.world = player.getServerWorld();
        this.spawnPosition = spellSpawnPosition(player);
        this.direction = player.getRotationVec(1.0f);
        this.executionId = Objects.requireNonNull(executionId, "executionId");
        this.catalogEpoch = resolvedCast.catalogEpoch();
        this.catalogHash = resolvedCast.catalogHash();
        this.reservation = reservation;
        this.budgetReleaser = EffectNodeBudgetReleaser.require(budgetReleaser);

        RootBudgetAllocation allocation = allocateRootBudgets(resolvedCast.effectPlan().nodes());
        this.rootBudgetAccepted = allocation.accepted();
        this.rootProjectileBudgets = allocation.budgets();
    }

    public ServerPlayerEntity player() {
        return player;
    }

    public ServerWorld world() {
        return world;
    }

    public Vec3d spawnPosition() {
        return spawnPosition;
    }

    public Vec3d direction() {
        return direction;
    }

    public UUID executionId() {
        return executionId;
    }

    public long catalogEpoch() {
        return catalogEpoch;
    }

    public String catalogHash() {
        return catalogHash;
    }

    /** May be null only for the deprecated legacy/test execution overload. */
    public BudgetReservation reservation() {
        return reservation;
    }

    public List<TriggerRuntimeBudget> requireRootBudgets(ProjectileEffectNode node) {
        if (!rootBudgetAccepted) {
            throw new IllegalStateException("accepted plan exceeded the legacy trigger runtime root budget");
        }
        List<TriggerRuntimeBudget> budgets = rootProjectileBudgets.get(node.nodePath());
        if (budgets == null || budgets.size() != node.projectile().projectileCount()) {
            throw new IllegalStateException("root projectile budget was not prepared for " + node.nodePath());
        }
        return budgets;
    }

    public void reportNodeFailure(EffectNode node, String stage, RuntimeException failure) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(failure, "failure");
        String key = stage + "|" + node.getClass().getName();
        long worldTime = world.getTime();
        synchronized (LAST_FAILURE_LOG_TICK) {
            Long previous = LAST_FAILURE_LOG_TICK.get(key);
            if (previous != null && worldTime - previous < FAILURE_LOG_INTERVAL_TICKS) {
                return;
            }
            LAST_FAILURE_LOG_TICK.put(key, worldTime);
        }
        MCNoita.LOGGER.warn(
            "Effect node {} ({}) failed after WandState commit; executionId={} catalog={}/{} owner={}",
            node.nodePath(), node.getClass().getSimpleName(), executionId, catalogEpoch, catalogHash, player.getUuid(), failure
        );
    }

    public void reportDeferred(EffectNode node) {
        reportNodeFailure(node, "deferred", new UnsupportedOperationException(
            "no safe world executor is registered for " + node.getClass().getSimpleName()
        ));
    }

    /** Releases a failed node's unused reservation slice once, after its diagnostic is recorded. */
    public void releaseUnusedBudget(EffectNode node, String stage) {
        if (reservation == null) {
            return;
        }
        String releaseKey = "effect-node/" + stage + "/" + node.nodePath();
        try {
            budgetReleaser.releaseUnused(node, releaseKey);
        } catch (RuntimeException failure) {
            reportNodeFailure(node, "budget-release", failure);
        }
    }

    private static RootBudgetAllocation allocateRootBudgets(List<EffectNode> nodes) {
        List<RootSlot> slots = new ArrayList<>();
        int rootEntityLimit = TriggerRuntimeBudget.DEFAULT.remainingSpawnedEntities();
        for (EffectNode node : nodes) {
            if (!(node instanceof ProjectileEffectNode projectileNode)) {
                continue;
            }
            int projectileCount = projectileNode.projectile().projectileCount();
            if (projectileCount < 1 || projectileCount > rootEntityLimit - slots.size()) {
                return RootBudgetAllocation.rejected();
            }
            int futureEntities = cappedInt(projectileNode.projectile().futureEntityFootprintPerInstance());
            int releaseEvents = cappedInt(projectileNode.projectile().staticReleaseEventFootprintPerInstance());
            for (int index = 0; index < projectileCount; index++) {
                slots.add(new RootSlot(projectileNode.nodePath(), new RootRequirement(releaseEvents, futureEntities)));
            }
        }
        if (slots.isEmpty()) {
            return RootBudgetAllocation.accepted(Map.of());
        }

        int requiredEvents = 0;
        int requiredEntities = 0;
        for (RootSlot slot : slots) {
            requiredEvents = addCapped(requiredEvents, slot.requirement().releaseEvents());
            requiredEntities = addCapped(requiredEntities, slot.requirement().futureEntities());
        }
        int availableEntities = rootEntityLimit - slots.size();
        int availableEvents = TriggerRuntimeBudget.DEFAULT.remainingReleaseEvents();
        if (requiredEntities > availableEntities || requiredEvents > availableEvents) {
            return RootBudgetAllocation.rejected();
        }

        int[] events = new int[slots.size()];
        int[] entities = new int[slots.size()];
        for (int index = 0; index < slots.size(); index++) {
            RootRequirement requirement = slots.get(index).requirement();
            events[index] = requirement.releaseEvents();
            entities[index] = requirement.futureEntities();
        }
        distribute(events, availableEvents - requiredEvents);
        distribute(entities, availableEntities - requiredEntities);

        Map<String, List<TriggerRuntimeBudget>> budgets = new LinkedHashMap<>();
        for (int index = 0; index < slots.size(); index++) {
            budgets.computeIfAbsent(slots.get(index).nodePath(), ignored -> new ArrayList<>())
                .add(new TriggerRuntimeBudget(events[index], entities[index]));
        }
        Map<String, List<TriggerRuntimeBudget>> immutable = new LinkedHashMap<>();
        for (Map.Entry<String, List<TriggerRuntimeBudget>> entry : budgets.entrySet()) {
            immutable.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return RootBudgetAllocation.accepted(Collections.unmodifiableMap(immutable));
    }

    private static Vec3d spellSpawnPosition(ServerPlayerEntity player) {
        double yawRadians = Math.toRadians(player.getYaw());
        Vec3d forward = new Vec3d(-Math.sin(yawRadians), 0.0, Math.cos(yawRadians));
        Vec3d right = new Vec3d(-Math.cos(yawRadians), 0.0, -Math.sin(yawRadians));
        return new Vec3d(player.getX(), player.getEyeY(), player.getZ())
            .add(forward.multiply(SPELL_SPAWN_FORWARD_OFFSET))
            .add(right.multiply(SPELL_SPAWN_RIGHT_OFFSET))
            .add(0.0, -SPELL_SPAWN_DOWN_OFFSET, 0.0);
    }

    private static void distribute(int[] values, int extras) {
        for (int index = 0; values.length > 0 && index < extras; index++) {
            values[index % values.length]++;
        }
    }

    private static int addCapped(int left, int right) {
        return right > Integer.MAX_VALUE - left ? Integer.MAX_VALUE : left + right;
    }

    private static int cappedInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private record RootRequirement(int releaseEvents, int futureEntities) {
    }

    private record RootSlot(String nodePath, RootRequirement requirement) {
    }

    private record RootBudgetAllocation(boolean accepted, Map<String, List<TriggerRuntimeBudget>> budgets) {
        private static RootBudgetAllocation accepted(Map<String, List<TriggerRuntimeBudget>> budgets) {
            return new RootBudgetAllocation(true, budgets);
        }

        private static RootBudgetAllocation rejected() {
            return new RootBudgetAllocation(false, Map.of());
        }
    }
}
