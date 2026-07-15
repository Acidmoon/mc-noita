package com.mcnoita.wand.server;

import com.mcnoita.spell.plan.BlockMutationEffectNode;
import com.mcnoita.spell.plan.EffectNode;
import com.mcnoita.spell.plan.ExplosionEffectNode;
import com.mcnoita.spell.plan.FieldEffectNode;
import com.mcnoita.spell.plan.PersistentJobEffectNode;
import com.mcnoita.spell.plan.ProjectileEffectNode;
import com.mcnoita.spell.plan.ResolvedCast;
import com.mcnoita.spell.plan.SoundEffectNode;
import com.mcnoita.spell.plan.SummonEffectNode;
import com.mcnoita.spell.plan.TeleportEffectNode;
import com.mcnoita.spell.server.budget.BudgetKind;
import com.mcnoita.spell.server.budget.BudgetRequest;
import com.mcnoita.spell.server.budget.ChunkBudgetKey;
import com.mcnoita.spell.server.job.FrozenSpellJobNode;
import com.mcnoita.world.mutation.WorldMutationPolicy;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Converts frozen evaluator evidence into the server quota request before any
 * wand state is written. It never inspects item IDs or reinterprets card order.
 */
public final class PlanBudgetRequestFactory {
    private static final long ESTIMATED_NBT_BYTES_PER_NODE = 1_024L;
    private static final long SOUND_PACKET_BYTES = 128L;
    private static final long TELEPORT_DESTINATION_CHECKS = 64L;
    private static final long MAX_BREAK_RAY_BLOCK_CHECKS = 64L;
    private static final int MAX_BREAK_RAY_STEPS = 64;

    private PlanBudgetRequestFactory() {
    }

    /**
     * Geometry frozen before commit for immediate nodes. Only entity queries
     * may span the explicit coverage set; point reads and mutations remain
     * bound to the primary chunk until terrain work gains an exact partition.
     */
    public record SynchronousNodeBindings(
        Map<String, ChunkBudgetKey> primaryChunks,
        Map<String, Set<ChunkBudgetKey>> entityQueryChunks
    ) {
        public SynchronousNodeBindings {
            primaryChunks = Map.copyOf(Objects.requireNonNull(primaryChunks, "primaryChunks"));
            Objects.requireNonNull(entityQueryChunks, "entityQueryChunks");
            Map<String, Set<ChunkBudgetKey>> copiedQueries = new LinkedHashMap<>();
            for (Map.Entry<String, Set<ChunkBudgetKey>> entry : entityQueryChunks.entrySet()) {
                copiedQueries.put(Objects.requireNonNull(entry.getKey(), "entity query node path"),
                    Collections.unmodifiableSet(new LinkedHashSet<>(Objects.requireNonNull(entry.getValue(),
                        "entity query chunk coverage"))));
            }
            entityQueryChunks = Collections.unmodifiableMap(copiedQueries);
        }
    }

    public static BudgetRequest fromResolvedCast(
        UUID executionId, UUID ownerId, String dimensionId, ChunkBudgetKey originChunk, ResolvedCast resolvedCast
    ) {
        return fromResolvedCast(executionId, ownerId, dimensionId, originChunk, resolvedCast, Map.of());
    }

    /**
     * Freezes the exact chunk for synchronous effects whose target is known at
     * commit time. Nodes without a deterministic target retain the origin
     * chunk and their executor rejects a later cross-chunk operation.
     */
    public static BudgetRequest fromResolvedCast(
        UUID executionId, UUID ownerId, String dimensionId, ChunkBudgetKey originChunk, ResolvedCast resolvedCast,
        Map<String, ChunkBudgetKey> synchronousNodeChunks
    ) {
        return fromResolvedCast(executionId, ownerId, dimensionId, originChunk, resolvedCast, synchronousNodeChunks, Map.of());
    }

    /** Uses all commit-time primary and cross-chunk entity-query bindings. */
    public static BudgetRequest fromResolvedCast(
        UUID executionId, UUID ownerId, String dimensionId, ChunkBudgetKey originChunk, ResolvedCast resolvedCast,
        SynchronousNodeBindings bindings
    ) {
        Objects.requireNonNull(bindings, "bindings");
        return fromResolvedCast(executionId, ownerId, dimensionId, originChunk, resolvedCast, bindings.primaryChunks(),
            bindings.entityQueryChunks());
    }

    /**
     * Reserves a full entity-query cap in every frozen query chunk. Minecraft
     * may return all bounded results from any one covered chunk, so splitting
     * the cap proportionally would undercharge an adversarial dense edge.
     */
    public static BudgetRequest fromResolvedCast(
        UUID executionId, UUID ownerId, String dimensionId, ChunkBudgetKey originChunk, ResolvedCast resolvedCast,
        Map<String, ChunkBudgetKey> synchronousNodeChunks, Map<String, Set<ChunkBudgetKey>> synchronousEntityQueryChunks
    ) {
        Objects.requireNonNull(executionId, "executionId");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(dimensionId, "dimensionId");
        Objects.requireNonNull(originChunk, "originChunk");
        Objects.requireNonNull(resolvedCast, "resolvedCast");
        synchronousNodeChunks = Map.copyOf(Objects.requireNonNull(synchronousNodeChunks, "synchronousNodeChunks"));
        synchronousEntityQueryChunks = Map.copyOf(Objects.requireNonNull(synchronousEntityQueryChunks,
            "synchronousEntityQueryChunks"));
        if (resolvedCast.status() != ResolvedCast.Status.ACCEPTED || !dimensionId.equals(originChunk.dimensionId())) {
            throw new IllegalArgumentException("only an accepted plan in its origin dimension can reserve a cast budget");
        }

        EnumMap<BudgetKind, Long> total = new EnumMap<>(BudgetKind.class);
        Map<ChunkBudgetKey, EnumMap<BudgetKind, Long>> chunks = new LinkedHashMap<>();
        add(total, BudgetKind.ACTION_NODES, resolvedCast.budgetUsage().actionSteps());
        add(total, BudgetKind.LOGICAL_PROJECTILES, resolvedCast.budgetUsage().projectileNodes());

        int persistentJobNodes = 0;
        for (EffectNode node : resolvedCast.effectPlan().nodes()) {
            ChunkBudgetKey nodeChunk = nodeChunk(node, originChunk, synchronousNodeChunks);
            if (node instanceof ProjectileEffectNode projectileNode) {
                long rootCount = projectileNode.projectile().projectileCount();
                addInChunk(chunks, nodeChunk, BudgetKind.LOGICAL_PROJECTILES, rootCount);
                // Trigger descendants remain constrained by the pure evaluator
                // tree budget, but their actual entity admission happens at
                // release time and at the target chunk. Counting them here as
                // root entities would spend the same owner/global tick quota
                // twice and can reject an already-paid first Trigger release.
                add(total, BudgetKind.AUTHORITATIVE_ENTITIES, rootCount);
                addInChunk(chunks, nodeChunk, BudgetKind.AUTHORITATIVE_ENTITIES, rootCount);
                long triggerReleases = projectileNode.projectile().staticReleaseEventFootprint();
                add(total, BudgetKind.TRIGGER_RELEASES, triggerReleases);
                addInChunk(chunks, nodeChunk, BudgetKind.TRIGGER_RELEASES, triggerReleases);
                add(total, BudgetKind.VISUAL_EVENTS, rootCount);
                addInChunk(chunks, nodeChunk, BudgetKind.VISUAL_EVENTS, rootCount);
            } else if (node instanceof SoundEffectNode) {
                add(total, BudgetKind.NETWORK_PACKETS, 1L);
                add(total, BudgetKind.NETWORK_BYTES, SOUND_PACKET_BYTES);
                add(total, BudgetKind.VISUAL_EVENTS, 1L);
                addInChunk(chunks, nodeChunk, BudgetKind.VISUAL_EVENTS, 1L);
            } else if (node instanceof FieldEffectNode field) {
                long scans = estimatedArea(field.radius());
                add(total, BudgetKind.ENTITY_SCANS, scans);
                addInChunk(chunks, nodeChunk, BudgetKind.ENTITY_SCANS, scans);
            } else if (node instanceof ExplosionEffectNode explosion) {
                addExplosionCosts(total, chunks, nodeChunk, immediateWorldCosts(explosion),
                    synchronousEntityQueryChunks.get(node.nodePath()));
            } else if (node instanceof SummonEffectNode summon) {
                add(total, BudgetKind.AUTHORITATIVE_ENTITIES, summon.count());
                addInChunk(chunks, nodeChunk, BudgetKind.AUTHORITATIVE_ENTITIES, summon.count());
            } else if (node instanceof TeleportEffectNode) {
                addAll(total, immediateWorldCosts(node));
                addAllInChunk(chunks, nodeChunk, immediateWorldCosts(node));
            } else if (node instanceof BlockMutationEffectNode mutation) {
                addAll(total, immediateWorldCosts(mutation));
                addAllInChunk(chunks, nodeChunk, immediateWorldCosts(mutation));
            } else if (node instanceof PersistentJobEffectNode) {
                // Validate the complete frozen per-step hard-budget product
                // before reserve/commit. Deferring this to the executor would
                // let an invalid long-lived job consume wand state first.
                FrozenSpellJobNode.fromEffectNode((PersistentJobEffectNode) node);
                if (++persistentJobNodes > 1) {
                    // Current G05 job identity is one root execution UUID plus
                    // one retained lease. Reject before WandState commit until
                    // a future schema introduces distinct per-node job IDs.
                    throw new IllegalArgumentException("an accepted cast may contain at most one persistent job node");
                }
                add(total, BudgetKind.PERSISTENT_JOBS, 1L);
                addInChunk(chunks, nodeChunk, BudgetKind.PERSISTENT_JOBS, 1L);
            }
        }

        long nbtNodes = addCapped(resolvedCast.budgetUsage().projectileNodes(), resolvedCast.budgetUsage().payloadNodes());
        add(total, BudgetKind.NBT_NODES, nbtNodes);
        add(total, BudgetKind.NBT_BYTES, multiplyCapped(nbtNodes, ESTIMATED_NBT_BYTES_PER_NODE));

        return new BudgetRequest(executionId, ownerId, dimensionId, total, immutableChunks(chunks));
    }

    private static void add(EnumMap<BudgetKind, Long> values, BudgetKind kind, long amount) {
        if (amount <= 0L) {
            return;
        }
        long current = values.getOrDefault(kind, 0L);
        values.put(kind, addCapped(current, amount));
    }

    private static void addAll(EnumMap<BudgetKind, Long> values, Map<BudgetKind, Long> costs) {
        for (Map.Entry<BudgetKind, Long> entry : costs.entrySet()) {
            add(values, entry.getKey(), entry.getValue());
        }
    }

    private static void addInChunk(
        Map<ChunkBudgetKey, EnumMap<BudgetKind, Long>> chunks, ChunkBudgetKey chunk, BudgetKind kind, long amount
    ) {
        if (amount <= 0L) {
            return;
        }
        add(chunks.computeIfAbsent(chunk, ignored -> new EnumMap<>(BudgetKind.class)), kind, amount);
    }

    private static void addAllInChunk(
        Map<ChunkBudgetKey, EnumMap<BudgetKind, Long>> chunks, ChunkBudgetKey chunk, Map<BudgetKind, Long> costs
    ) {
        for (Map.Entry<BudgetKind, Long> entry : costs.entrySet()) {
            addInChunk(chunks, chunk, entry.getKey(), entry.getValue());
        }
    }

    private static void addExplosionCosts(
        EnumMap<BudgetKind, Long> total, Map<ChunkBudgetKey, EnumMap<BudgetKind, Long>> chunks,
        ChunkBudgetKey primaryChunk, Map<BudgetKind, Long> costs, Set<ChunkBudgetKey> requestedQueryChunks
    ) {
        Set<ChunkBudgetKey> queryChunks = validatedExplosionQueryChunks(primaryChunk, requestedQueryChunks);
        for (Map.Entry<BudgetKind, Long> entry : costs.entrySet()) {
            if (entry.getKey() == BudgetKind.ENTITY_SCANS) {
                for (ChunkBudgetKey queryChunk : queryChunks) {
                    add(total, entry.getKey(), entry.getValue());
                    addInChunk(chunks, queryChunk, entry.getKey(), entry.getValue());
                }
            } else {
                add(total, entry.getKey(), entry.getValue());
                addInChunk(chunks, primaryChunk, entry.getKey(), entry.getValue());
            }
        }
    }

    private static Set<ChunkBudgetKey> validatedExplosionQueryChunks(
        ChunkBudgetKey primaryChunk, Set<ChunkBudgetKey> requestedQueryChunks
    ) {
        Set<ChunkBudgetKey> queryChunks = requestedQueryChunks == null || requestedQueryChunks.isEmpty()
            ? Set.of(primaryChunk) : requestedQueryChunks;
        LinkedHashSet<ChunkBudgetKey> checked = new LinkedHashSet<>();
        for (ChunkBudgetKey chunk : queryChunks) {
            if (chunk == null || !primaryChunk.dimensionId().equals(chunk.dimensionId())) {
                throw new IllegalArgumentException("synchronous explosion query chunks must stay in the cast dimension");
            }
            checked.add(chunk);
        }
        if (!checked.contains(primaryChunk) || checked.size() > WorldMutationPolicy.MAX_QUERY_CHUNKS) {
            throw new IllegalArgumentException("synchronous explosion query coverage is invalid");
        }
        return Collections.unmodifiableSet(checked);
    }

    private static ChunkBudgetKey nodeChunk(
        EffectNode node, ChunkBudgetKey originChunk, Map<String, ChunkBudgetKey> synchronousNodeChunks
    ) {
        ChunkBudgetKey chunk = synchronousNodeChunks.getOrDefault(node.nodePath(), originChunk);
        if (!originChunk.dimensionId().equals(chunk.dimensionId())) {
            throw new IllegalArgumentException("synchronous node chunk must stay in the cast dimension: " + node.nodePath());
        }
        return chunk;
    }

    private static Map<ChunkBudgetKey, Map<BudgetKind, Long>> immutableChunks(
        Map<ChunkBudgetKey, EnumMap<BudgetKind, Long>> chunks
    ) {
        Map<ChunkBudgetKey, Map<BudgetKind, Long>> immutable = new LinkedHashMap<>();
        for (Map.Entry<ChunkBudgetKey, EnumMap<BudgetKind, Long>> entry : chunks.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                immutable.put(entry.getKey(), Map.copyOf(entry.getValue()));
            }
        }
        return Map.copyOf(immutable);
    }

    /** Computes only target chunks that are deterministic before the transaction commits. */
    public static Map<String, ChunkBudgetKey> synchronousNodeChunks(
        String dimensionId, ChunkBudgetKey originChunk, ResolvedCast resolvedCast, Vec3d playerPosition,
        Vec3d spawnPosition, Vec3d direction
    ) {
        return synchronousNodeBindings(dimensionId, originChunk, resolvedCast, playerPosition, spawnPosition, direction)
            .primaryChunks();
    }

    /** Computes primary point chunks and exact bounded entity-query envelopes before commit. */
    public static SynchronousNodeBindings synchronousNodeBindings(
        String dimensionId, ChunkBudgetKey originChunk, ResolvedCast resolvedCast, Vec3d playerPosition,
        Vec3d spawnPosition, Vec3d direction
    ) {
        Objects.requireNonNull(dimensionId, "dimensionId");
        Objects.requireNonNull(originChunk, "originChunk");
        Objects.requireNonNull(resolvedCast, "resolvedCast");
        Objects.requireNonNull(playerPosition, "playerPosition");
        Objects.requireNonNull(spawnPosition, "spawnPosition");
        Objects.requireNonNull(direction, "direction");
        if (!dimensionId.equals(originChunk.dimensionId()) || !isFinite(playerPosition) || !isFinite(spawnPosition)
            || !isFinite(direction)) {
            throw new IllegalArgumentException("synchronous node geometry must be finite and remain in the origin dimension");
        }
        Vec3d normalizedDirection = direction.lengthSquared() > 1.0E-6 ? direction.normalize() : Vec3d.ZERO;
        Map<String, ChunkBudgetKey> chunks = new LinkedHashMap<>();
        Map<String, Set<ChunkBudgetKey>> entityQueryChunks = new LinkedHashMap<>();
        for (EffectNode node : resolvedCast.effectPlan().nodes()) {
            Vec3d position = null;
            if (node instanceof ProjectileEffectNode || node instanceof ExplosionEffectNode || node instanceof SummonEffectNode) {
                position = spawnPosition;
            } else if (node instanceof TeleportEffectNode teleport) {
                position = playerPosition.add(normalizedDirection.multiply(teleport.maximumDistance()));
            }
            if (position != null) {
                chunks.put(node.nodePath(), chunkAt(dimensionId, position));
            }
            if (node instanceof ExplosionEffectNode explosion) {
                if (explosion.radius() <= 0.0 || explosion.radius() > WorldMutationPolicy.MAX_EXPLOSION_RADIUS) {
                    throw new IllegalArgumentException("immediate explosion radius is outside the execution policy");
                }
                double radius = explosion.radius();
                Box queryArea = Box.of(spawnPosition, radius * 2.0, radius * 2.0, radius * 2.0);
                entityQueryChunks.put(node.nodePath(), chunksForEnvelope(dimensionId, queryArea));
                if (explosion.terrainRequested()) {
                    bindSingleTerrainChunk(chunks, node.nodePath(),
                        chunksForExplosionTerrain(dimensionId, spawnPosition, radius), "explosion terrain");
                }
            } else if (node instanceof BlockMutationEffectNode mutation
                && mutation.kind() == BlockMutationEffectNode.MutationKind.BREAK
                && mutation.maximumBlocks() == 1
                && mutation.radius() > 0.0
                && mutation.radius() <= WorldMutationPolicy.MAX_QUERY_DIAMETER) {
                bindSingleTerrainChunk(chunks, node.nodePath(),
                    chunksForBreakRay(dimensionId, spawnPosition, normalizedDirection, mutation.radius()), "line-of-sight break");
            }
        }
        return new SynchronousNodeBindings(chunks, entityQueryChunks);
    }

    /**
     * G05 has no per-chunk terrain work partition yet. Do not let an accepted
     * cast discover that fact after WandState commit: an immediate terrain
     * effect must be wholly owned by one frozen chunk or be rejected here.
     */
    private static void bindSingleTerrainChunk(
        Map<String, ChunkBudgetKey> chunks, String nodePath, Set<ChunkBudgetKey> terrainChunks, String operation
    ) {
        if (terrainChunks.size() != 1) {
            throw new IllegalArgumentException(operation + " crosses chunks and requires a partitioned terrain plan");
        }
        chunks.put(nodePath, terrainChunks.iterator().next());
    }

    /** Mirrors WorldMutationService's inclusive integer sphere candidate loop. */
    private static Set<ChunkBudgetKey> chunksForExplosionTerrain(String dimensionId, Vec3d center, double radius) {
        BlockPos origin = BlockPos.ofFloored(center);
        int blockRadius = Math.max(1, (int) Math.ceil(radius));
        LinkedHashSet<ChunkBudgetKey> chunks = new LinkedHashSet<>();
        for (int chunkX = (origin.getX() - blockRadius) >> 4; chunkX <= (origin.getX() + blockRadius) >> 4; chunkX++) {
            for (int chunkZ = (origin.getZ() - blockRadius) >> 4; chunkZ <= (origin.getZ() + blockRadius) >> 4; chunkZ++) {
                chunks.add(new ChunkBudgetKey(dimensionId, chunkX, chunkZ));
            }
        }
        return Collections.unmodifiableSet(chunks);
    }

    /** Mirrors the representative executor's finite per-block line-of-sight probe. */
    private static Set<ChunkBudgetKey> chunksForBreakRay(String dimensionId, Vec3d origin, Vec3d direction, double reach) {
        int steps = Math.min(MAX_BREAK_RAY_STEPS, Math.max(1, (int) Math.ceil(reach)));
        Vec3d step = direction.lengthSquared() > 1.0E-6 ? direction.normalize() : Vec3d.ZERO;
        LinkedHashSet<ChunkBudgetKey> chunks = new LinkedHashSet<>();
        for (int index = 1; index <= steps; index++) {
            double distance = Math.min(index, reach);
            chunks.add(chunkAt(dimensionId, origin.add(step.multiply(distance))));
        }
        return Collections.unmodifiableSet(chunks);
    }

    private static Set<ChunkBudgetKey> chunksForEnvelope(String dimensionId, Box area) {
        WorldMutationPolicy.ChunkEnvelope envelope = WorldMutationPolicy.chunkEnvelope(area)
            .orElseThrow(() -> new IllegalArgumentException("immediate world query envelope exceeds the server safety bounds"));
        LinkedHashSet<ChunkBudgetKey> chunks = new LinkedHashSet<>();
        for (int chunkX = envelope.minChunkX(); chunkX <= envelope.maxChunkX(); chunkX++) {
            for (int chunkZ = envelope.minChunkZ(); chunkZ <= envelope.maxChunkZ(); chunkZ++) {
                chunks.add(new ChunkBudgetKey(dimensionId, chunkX, chunkZ));
            }
        }
        return Collections.unmodifiableSet(chunks);
    }

    private static ChunkBudgetKey chunkAt(String dimensionId, Vec3d position) {
        BlockPos block = BlockPos.ofFloored(position);
        return new ChunkBudgetKey(dimensionId, block.getX() >> 4, block.getZ() >> 4);
    }

    private static boolean isFinite(Vec3d value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }

    /** Frozen immediate world-operation footprint shared with EffectExecutionContext's local slices. */
    public static Map<BudgetKind, Long> immediateWorldCosts(EffectNode node) {
        Objects.requireNonNull(node, "node");
        EnumMap<BudgetKind, Long> costs = new EnumMap<>(BudgetKind.class);
        if (node instanceof ProjectileEffectNode projectile) {
            // Root projectiles are spawned synchronously during the committed
            // transaction, so they consume the plan's existing reservation.
            // Trigger descendants are delayed and must acquire fresh runtime
            // capacity after that root reservation closes.
            add(costs, BudgetKind.AUTHORITATIVE_ENTITIES, projectile.projectile().projectileCount());
        } else if (node instanceof ExplosionEffectNode explosion) {
            add(costs, BudgetKind.ENTITY_SCANS, boundedExplosionEntityScans(explosion.radius()));
            if (explosion.terrainRequested()) {
                // WorldMutationService walks an integer cube and caps its
                // candidate loop. Reserve that actual upper bound rather than
                // the smaller continuous-sphere estimate used for entities.
                long terrainCandidates = estimatedExplosionTerrainCandidates(explosion.radius());
                add(costs, BudgetKind.BLOCK_CHECKS, terrainCandidates);
                add(costs, BudgetKind.BLOCK_MUTATIONS, terrainCandidates);
            }
        } else if (node instanceof TeleportEffectNode) {
            add(costs, BudgetKind.BLOCK_CHECKS, TELEPORT_DESTINATION_CHECKS);
        } else if (node instanceof BlockMutationEffectNode mutation) {
            long checks = mutation.kind() == BlockMutationEffectNode.MutationKind.BREAK
                ? boundedBreakRayChecks(mutation.radius())
                : mutation.maximumBlocks();
            add(costs, BudgetKind.BLOCK_CHECKS, checks);
            add(costs, BudgetKind.BLOCK_MUTATIONS, mutation.maximumBlocks());
        }
        return Map.copyOf(costs);
    }

    private static long estimatedArea(double radius) {
        if (!Double.isFinite(radius) || radius <= 0.0) {
            return 1L;
        }
        return cappedDouble(Math.PI * radius * radius);
    }

    private static long estimatedSphere(double radius) {
        if (!Double.isFinite(radius) || radius <= 0.0) {
            return 1L;
        }
        return cappedDouble((4.0 / 3.0) * Math.PI * radius * radius * radius);
    }

    private static long estimatedExplosionTerrainCandidates(double radius) {
        if (!Double.isFinite(radius) || radius <= 0.0) {
            return 1L;
        }
        long blockRadius = Math.max(1L, (long) Math.ceil(radius));
        long edge = blockRadius > (Long.MAX_VALUE - 1L) / 2L ? Long.MAX_VALUE : blockRadius * 2L + 1L;
        long cubic = edge > 0L && edge > Long.MAX_VALUE / edge / edge ? Long.MAX_VALUE : edge * edge * edge;
        return Math.min(WorldMutationPolicy.MAX_EXPLOSION_CANDIDATES, cubic);
    }

    /** Must match WorldMutationService.boundedExplosionEntityLimit exactly. */
    private static long boundedExplosionEntityScans(double radius) {
        return Math.min(WorldMutationPolicy.MAX_QUERY_RESULTS, estimatedSphere(radius));
    }

    /** Mirrors the line-of-sight search plus WorldMutationService.breakBlock's final state read. */
    private static long boundedBreakRayChecks(double radius) {
        if (!Double.isFinite(radius) || radius <= 0.0) {
            return 1L;
        }
        long rayChecks = Math.min(MAX_BREAK_RAY_BLOCK_CHECKS, Math.max(1L, (long) Math.ceil(radius)));
        return rayChecks + 1L;
    }

    private static long cappedDouble(double value) {
        return !Double.isFinite(value) || value >= Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(1L, (long) Math.ceil(value));
    }

    private static long addCapped(long left, long right) {
        return right > Long.MAX_VALUE - left ? Long.MAX_VALUE : left + right;
    }

    private static long multiplyCapped(long left, long right) {
        return left == 0L || right == 0L ? 0L : left > Long.MAX_VALUE / right ? Long.MAX_VALUE : left * right;
    }
}
