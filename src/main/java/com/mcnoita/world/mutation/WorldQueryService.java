package com.mcnoita.world.mutation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/** Bounded read facade for effects that inspect entities or blocks. */
public final class WorldQueryService {
    private static final WorldMutationPolicy POLICY = new WorldMutationPolicy();
    private static final TypeFilter<Entity, Entity> ANY_ENTITY = TypeFilter.instanceOf(Entity.class);

    private WorldQueryService() {
    }

    public static List<Entity> entities(
        WorldMutationContext context, Entity except, Box area, Predicate<? super Entity> predicate, int requestedLimit
    ) {
        return tryEntities(context, except, area, predicate, requestedLimit).orElse(List.of());
    }

    /**
     * Distinguishes an admitted empty result from a policy/budget rejection so
     * an executor can fail the whole node instead of reporting a successful
     * zero-damage explosion after its authoritative query was denied.
     */
    public static Optional<List<Entity>> tryEntities(
        WorldMutationContext context, Entity except, Box area, Predicate<? super Entity> predicate, int requestedLimit
    ) {
        int limit = boundedResultLimit(requestedLimit);
        if (!POLICY.permitsQuery(context, area, limit)) {
            return Optional.empty();
        }

        // getOtherEntities creates its complete result list before a caller can
        // truncate it. The five-argument collector stops EntityLookup traversal
        // as soon as this limit is reached, so a dense loaded area cannot force
        // an unbounded allocation or scan through this facade.
        List<Entity> candidates = new ArrayList<>(limit);
        context.world().collectEntitiesByType(
            ANY_ENTITY,
            area,
            entity -> entity != except && predicate.test(entity),
            candidates,
            limit
        );
        return Optional.of(List.copyOf(candidates));
    }

    /** Pure limit normalization shared by the runtime facade and regression tests. */
    static int boundedResultLimit(int requestedLimit) {
        return Math.min(WorldMutationPolicy.MAX_QUERY_RESULTS, Math.max(0, requestedLimit));
    }

    public static Optional<BlockState> blockState(WorldMutationContext context, BlockPos pos, WorldMutationKind kind) {
        if (!POLICY.permitsBlockCheck(context, pos, kind)) {
            return Optional.empty();
        }
        return Optional.of(context.world().getBlockState(pos));
    }

    /**
     * Performs the raw Minecraft ray only after the complete swept envelope
     * passes loaded-chunk and central block-check admission. Empty means that
     * policy denied the read; a permitted miss is returned as a MISS result.
     */
    public static Optional<BlockHitResult> raycast(WorldMutationContext context, Entity source, Vec3d start, Vec3d end) {
        if (context == null || source == null || !POLICY.permitsRaycast(context, start, end)) {
            return Optional.empty();
        }
        return Optional.of(context.world().raycast(new RaycastContext(start, end,
            RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, source)));
    }
}
