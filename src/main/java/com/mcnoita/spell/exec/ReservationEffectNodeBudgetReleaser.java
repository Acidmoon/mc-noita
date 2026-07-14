package com.mcnoita.spell.exec;

import com.mcnoita.spell.plan.BlockMutationEffectNode;
import com.mcnoita.spell.plan.EffectNode;
import com.mcnoita.spell.plan.FieldEffectNode;
import com.mcnoita.spell.plan.PersistentJobEffectNode;
import com.mcnoita.spell.plan.ProjectileEffectNode;
import com.mcnoita.spell.plan.SoundEffectNode;
import com.mcnoita.spell.plan.SummonEffectNode;
import com.mcnoita.spell.server.budget.BudgetKind;
import com.mcnoita.spell.server.budget.BudgetRequest;
import com.mcnoita.spell.server.budget.BudgetReservation;
import java.util.Map;
import java.util.Objects;

/**
 * Conservative default until the transaction can supply exact per-node slices.
 * It releases only costs with no chunk-local allocation, so it can never free a
 * global total while retaining an incompatible chunk reservation.
 */
final class ReservationEffectNodeBudgetReleaser implements EffectNodeBudgetReleaser {
    private final BudgetReservation reservation;

    ReservationEffectNodeBudgetReleaser(BudgetReservation reservation) {
        this.reservation = Objects.requireNonNull(reservation, "reservation");
    }

    @Override
    public void releaseUnused(EffectNode node, String releaseKey) {
        BudgetRequest unused = EffectNodeBudgetSlices.forFailedNode(reservation.request(), node);
        if (!unused.isEmpty()) {
            reservation.releaseUnused(releaseKey, unused);
        }
    }

    static final class EffectNodeBudgetSlices {
        private EffectNodeBudgetSlices() {
        }

        static BudgetRequest forFailedNode(BudgetRequest original, EffectNode node) {
            Objects.requireNonNull(original, "original");
            Objects.requireNonNull(node, "node");
            BudgetRequest.Builder builder = BudgetRequest.builder(original.executionId(), original.dimensionId())
                .owner(original.ownerId());
            if (node instanceof ProjectileEffectNode projectile) {
                addUnchunked(builder, original, BudgetKind.AUTHORITATIVE_ENTITIES, projectile.projectile().projectileCount());
            } else if (node instanceof SummonEffectNode summon) {
                addUnchunked(builder, original, BudgetKind.AUTHORITATIVE_ENTITIES, summon.count());
            } else if (node instanceof SoundEffectNode) {
                addUnchunked(builder, original, BudgetKind.VISUAL_EVENTS, 1L);
            } else if (node instanceof FieldEffectNode) {
                addUnchunked(builder, original, BudgetKind.PERSISTENT_JOBS, 1L);
            } else if (node instanceof BlockMutationEffectNode mutation) {
                addUnchunked(builder, original, BudgetKind.BLOCK_MUTATIONS, mutation.maximumBlocks());
            } else if (node instanceof PersistentJobEffectNode job) {
                addUnchunked(builder, original, BudgetKind.PERSISTENT_JOBS, 1L);
                addUnchunked(builder, original, BudgetKind.CROSS_TICK_JOB_STEPS, job.maximumSteps());
            }
            return builder.build();
        }

        private static void addUnchunked(
            BudgetRequest.Builder builder, BudgetRequest original, BudgetKind kind, long requested
        ) {
            if (requested <= 0L || hasChunkAllocation(original, kind)) {
                return;
            }
            long available = original.cost(kind);
            if (available > 0L) {
                builder.add(kind, Math.min(available, requested));
            }
        }

        private static boolean hasChunkAllocation(BudgetRequest request, BudgetKind kind) {
            for (Map<BudgetKind, Long> costs : request.chunkCosts().values()) {
                if (costs.getOrDefault(kind, 0L) > 0L) {
                    return true;
                }
            }
            return false;
        }
    }
}
