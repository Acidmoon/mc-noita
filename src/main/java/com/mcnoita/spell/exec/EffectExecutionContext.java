package com.mcnoita.spell.exec;

import com.mcnoita.MCNoita;
import com.mcnoita.spell.NoitaExecutionIdentity;
import com.mcnoita.spell.plan.EffectNode;
import com.mcnoita.spell.plan.ProjectileEffectNode;
import com.mcnoita.spell.plan.ResolvedCast;
import com.mcnoita.spell.server.budget.BudgetReservation;
import com.mcnoita.spell.server.budget.ChunkBudgetKey;
import com.mcnoita.spell.trigger.RootTriggerBudgetAllocator;
import com.mcnoita.spell.trigger.TriggerRuntimeBudget;
import com.mcnoita.world.mutation.WorldMutationBudget;
import com.mcnoita.world.mutation.WorldMutationContext;
import java.util.HashMap;
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
    private final Map<String, WorldMutationBudget> immediateWorldBudgets;
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
        this(player, resolvedCast, executionId, reservation, budgetReleaser, Map.of());
    }

    /** The transaction freezes chunks for immediate nodes whose target is known before commit. */
    public EffectExecutionContext(
        ServerPlayerEntity player, ResolvedCast resolvedCast, UUID executionId, BudgetReservation reservation,
        EffectNodeBudgetReleaser budgetReleaser, Map<String, ChunkBudgetKey> synchronousNodeChunks
    ) {
        this(player, resolvedCast, executionId, reservation, budgetReleaser, synchronousNodeChunks, Map.of());
    }

    /** The transaction freezes multi-chunk entity-query coverage for immediate explosions before commit. */
    public EffectExecutionContext(
        ServerPlayerEntity player, ResolvedCast resolvedCast, UUID executionId, BudgetReservation reservation,
        EffectNodeBudgetReleaser budgetReleaser, Map<String, ChunkBudgetKey> synchronousNodeChunks,
        Map<String, java.util.Set<ChunkBudgetKey>> synchronousEntityQueryChunks
    ) {
        this.player = Objects.requireNonNull(player, "player");
        Objects.requireNonNull(resolvedCast, "resolvedCast");
        synchronousNodeChunks = Map.copyOf(Objects.requireNonNull(synchronousNodeChunks, "synchronousNodeChunks"));
        synchronousEntityQueryChunks = Map.copyOf(Objects.requireNonNull(synchronousEntityQueryChunks,
            "synchronousEntityQueryChunks"));
        this.world = player.getServerWorld();
        this.spawnPosition = spellSpawnPosition(player);
        this.direction = player.getRotationVec(1.0f);
        this.executionId = Objects.requireNonNull(executionId, "executionId");
        this.catalogEpoch = resolvedCast.catalogEpoch();
        this.catalogHash = resolvedCast.catalogHash();
        this.reservation = Objects.requireNonNull(reservation, "reservation");
        this.budgetReleaser = EffectNodeBudgetReleaser.require(budgetReleaser);
        this.immediateWorldBudgets = ImmediateWorldBudgetAllocator.allocate(reservation.request(), resolvedCast.effectPlan().nodes(),
            synchronousNodeChunks, synchronousEntityQueryChunks);

        RootTriggerBudgetAllocator.Allocation allocation = RootTriggerBudgetAllocator.allocate(
            resolvedCast.effectPlan().nodes(), reservation.triggerRuntimeBudgetCeiling()
        );
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

    /**
     * Binds a world operation to the committed cast rather than allowing a
     * typed executor to invent ownership, dimension, or catalog identity.
     * Immediate G05 executors receive disjoint local slices of the
     * transaction's already-reserved root request. Cross-tick budget retention
     * belongs to the persistent-job boundary.
     */
    public WorldMutationContext worldMutationContext(EffectNode node) {
        Objects.requireNonNull(node, "node");
        WorldMutationBudget budget = immediateWorldBudgets.getOrDefault(node.nodePath(), ImmediateWorldBudgetAllocator.DENIED);
        return new WorldMutationContext(world, player, player.getUuid(), world.getRegistryKey(),
            new NoitaExecutionIdentity(executionId, node.nodePath(), catalogEpoch, catalogHash),
            budget);
    }

    public BudgetReservation reservation() {
        return reservation;
    }

    public List<TriggerRuntimeBudget> requireRootBudgets(ProjectileEffectNode node) {
        if (!rootBudgetAccepted) {
            throw new IllegalStateException("accepted plan exceeded its preflighted trigger runtime budget");
        }
        List<TriggerRuntimeBudget> budgets = rootProjectileBudgets.get(node.nodePath());
        if (budgets == null || budgets.size() != node.projectile().projectileCount()) {
            throw new IllegalStateException("root projectile budget was not prepared for " + node.nodePath());
        }
        return budgets;
    }

    /**
     * Returns the root projectile's local slice of the already committed
     * reservation. Delayed trigger payloads do not receive this slice because
     * the root reservation closes before they run and must re-reserve safely.
     */
    public WorldMutationBudget rootProjectileSpawnBudget(ProjectileEffectNode node) {
        Objects.requireNonNull(node, "node");
        return immediateWorldBudgets.getOrDefault(node.nodePath(), ImmediateWorldBudgetAllocator.DENIED);
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
        reportDeferred(node, "no safe world executor is registered for " + node.getClass().getSimpleName());
    }

    /** Allows a policy-specific deferred executor to retain its rejection reason. */
    public void reportDeferred(EffectNode node, String reason) {
        Objects.requireNonNull(reason, "reason");
        reportNodeFailure(node, "deferred", new UnsupportedOperationException(
            reason
        ));
    }

    /** Records a deliberate policy rejection without rolling back the committed wand state. */
    public void rejectNode(EffectNode node, String reason) {
        Objects.requireNonNull(reason, "reason");
        reportNodeFailure(node, "rejected", new IllegalArgumentException(reason));
        releaseUnusedBudget(node, "rejected");
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

    public static Vec3d spellSpawnPosition(ServerPlayerEntity player) {
        double yawRadians = Math.toRadians(player.getYaw());
        Vec3d forward = new Vec3d(-Math.sin(yawRadians), 0.0, Math.cos(yawRadians));
        Vec3d right = new Vec3d(-Math.cos(yawRadians), 0.0, -Math.sin(yawRadians));
        return new Vec3d(player.getX(), player.getEyeY(), player.getZ())
            .add(forward.multiply(SPELL_SPAWN_FORWARD_OFFSET))
            .add(right.multiply(SPELL_SPAWN_RIGHT_OFFSET))
            .add(0.0, -SPELL_SPAWN_DOWN_OFFSET, 0.0);
    }

}
