package com.mcnoita.world.mutation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/** Bounded read facade for effects that inspect entities or blocks. */
public final class WorldQueryService {
    private static final WorldMutationPolicy POLICY = new WorldMutationPolicy();

    private WorldQueryService() {
    }

    public static List<Entity> entities(
        WorldMutationContext context, Entity except, Box area, Predicate<? super Entity> predicate, int requestedLimit
    ) {
        int limit = Math.min(WorldMutationPolicy.MAX_QUERY_RESULTS, Math.max(0, requestedLimit));
        if (!POLICY.permitsQuery(context, area, limit)) {
            return List.of();
        }
        List<Entity> candidates = new ArrayList<>(limit);
        for (Entity entity : context.world().getOtherEntities(except, area, predicate)) {
            candidates.add(entity);
            if (candidates.size() == limit) {
                break;
            }
        }
        return List.copyOf(candidates);
    }

    public static Optional<BlockState> blockState(WorldMutationContext context, BlockPos pos, WorldMutationKind kind) {
        if (!POLICY.permitsBlockCheck(context, pos, kind)) {
            return Optional.empty();
        }
        return Optional.of(context.world().getBlockState(pos));
    }
}
