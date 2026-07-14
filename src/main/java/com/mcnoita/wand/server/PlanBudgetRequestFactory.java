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
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Converts frozen evaluator evidence into the server quota request before any
 * wand state is written. It never inspects item IDs or reinterprets card order.
 */
public final class PlanBudgetRequestFactory {
    private static final long ESTIMATED_NBT_BYTES_PER_NODE = 1_024L;
    private static final long SOUND_PACKET_BYTES = 128L;
    private static final long TELEPORT_DESTINATION_CHECKS = 64L;

    private PlanBudgetRequestFactory() {
    }

    public static BudgetRequest fromResolvedCast(
        UUID executionId, UUID ownerId, String dimensionId, ChunkBudgetKey originChunk, ResolvedCast resolvedCast
    ) {
        Objects.requireNonNull(executionId, "executionId");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(dimensionId, "dimensionId");
        Objects.requireNonNull(originChunk, "originChunk");
        Objects.requireNonNull(resolvedCast, "resolvedCast");
        if (resolvedCast.status() != ResolvedCast.Status.ACCEPTED || !dimensionId.equals(originChunk.dimensionId())) {
            throw new IllegalArgumentException("only an accepted plan in its origin dimension can reserve a cast budget");
        }

        EnumMap<BudgetKind, Long> total = new EnumMap<>(BudgetKind.class);
        EnumMap<BudgetKind, Long> chunk = new EnumMap<>(BudgetKind.class);
        add(total, BudgetKind.ACTION_NODES, resolvedCast.budgetUsage().actionSteps());
        add(total, BudgetKind.LOGICAL_PROJECTILES, resolvedCast.budgetUsage().projectileNodes());

        long staticEntities = 0L;
        long triggerReleases = 0L;
        long visualEvents = 0L;
        long rootLogicalProjectiles = 0L;
        for (EffectNode node : resolvedCast.effectPlan().nodes()) {
            if (node instanceof ProjectileEffectNode projectileNode) {
                // WP5 releases a failed root node with this exact origin-chunk
                // shape, while the total still includes hidden payload nodes.
                add(chunk, BudgetKind.LOGICAL_PROJECTILES, 1L);
                rootLogicalProjectiles = addCapped(rootLogicalProjectiles, 1L);
                staticEntities = addCapped(staticEntities, projectileNode.projectile().staticEntityFootprint());
                triggerReleases = addCapped(triggerReleases, projectileNode.projectile().staticReleaseEventFootprint());
                visualEvents = addCapped(visualEvents, projectileNode.projectile().projectileCount());
                add(chunk, BudgetKind.VISUAL_EVENTS, projectileNode.projectile().projectileCount());
            } else if (node instanceof SoundEffectNode) {
                add(total, BudgetKind.NETWORK_PACKETS, 1L);
                add(total, BudgetKind.NETWORK_BYTES, SOUND_PACKET_BYTES);
                visualEvents = addCapped(visualEvents, 1L);
                add(chunk, BudgetKind.VISUAL_EVENTS, 1L);
            } else if (node instanceof FieldEffectNode field) {
                long scans = estimatedArea(field.radius());
                add(total, BudgetKind.ENTITY_SCANS, scans);
                add(chunk, BudgetKind.ENTITY_SCANS, scans);
            } else if (node instanceof ExplosionEffectNode explosion) {
                long affected = estimatedSphere(explosion.radius());
                add(total, BudgetKind.ENTITY_SCANS, affected);
                add(chunk, BudgetKind.ENTITY_SCANS, affected);
                if (explosion.terrainRequested()) {
                    add(total, BudgetKind.BLOCK_CHECKS, affected);
                    add(total, BudgetKind.BLOCK_MUTATIONS, affected);
                    add(chunk, BudgetKind.BLOCK_CHECKS, affected);
                    add(chunk, BudgetKind.BLOCK_MUTATIONS, affected);
                }
            } else if (node instanceof SummonEffectNode summon) {
                staticEntities = addCapped(staticEntities, summon.count());
            } else if (node instanceof TeleportEffectNode) {
                add(total, BudgetKind.BLOCK_CHECKS, TELEPORT_DESTINATION_CHECKS);
                add(chunk, BudgetKind.BLOCK_CHECKS, TELEPORT_DESTINATION_CHECKS);
            } else if (node instanceof BlockMutationEffectNode mutation) {
                add(total, BudgetKind.BLOCK_CHECKS, mutation.maximumBlocks());
                add(total, BudgetKind.BLOCK_MUTATIONS, mutation.maximumBlocks());
                add(chunk, BudgetKind.BLOCK_CHECKS, mutation.maximumBlocks());
                add(chunk, BudgetKind.BLOCK_MUTATIONS, mutation.maximumBlocks());
            } else if (node instanceof PersistentJobEffectNode job) {
                add(total, BudgetKind.PERSISTENT_JOBS, 1L);
                add(total, BudgetKind.CROSS_TICK_JOB_STEPS, job.maximumSteps());
            }
        }

        ensureAtLeast(total, BudgetKind.LOGICAL_PROJECTILES, rootLogicalProjectiles);
        long authoritativeEntities = Math.max(resolvedCast.budgetUsage().spawnedEntities(), staticEntities);
        add(total, BudgetKind.AUTHORITATIVE_ENTITIES, authoritativeEntities);
        add(chunk, BudgetKind.AUTHORITATIVE_ENTITIES, authoritativeEntities);
        add(total, BudgetKind.TRIGGER_RELEASES, triggerReleases);
        add(chunk, BudgetKind.TRIGGER_RELEASES, triggerReleases);
        add(total, BudgetKind.VISUAL_EVENTS, visualEvents);

        long nbtNodes = addCapped(resolvedCast.budgetUsage().projectileNodes(), resolvedCast.budgetUsage().payloadNodes());
        add(total, BudgetKind.NBT_NODES, nbtNodes);
        add(total, BudgetKind.NBT_BYTES, multiplyCapped(nbtNodes, ESTIMATED_NBT_BYTES_PER_NODE));

        Map<ChunkBudgetKey, Map<BudgetKind, Long>> chunks = chunk.isEmpty() ? Map.of() : Map.of(originChunk, Map.copyOf(chunk));
        return new BudgetRequest(executionId, ownerId, dimensionId, total, chunks);
    }

    private static void add(EnumMap<BudgetKind, Long> values, BudgetKind kind, long amount) {
        if (amount <= 0L) {
            return;
        }
        long current = values.getOrDefault(kind, 0L);
        values.put(kind, addCapped(current, amount));
    }

    private static void ensureAtLeast(EnumMap<BudgetKind, Long> values, BudgetKind kind, long minimum) {
        if (minimum > values.getOrDefault(kind, 0L)) {
            values.put(kind, minimum);
        }
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
