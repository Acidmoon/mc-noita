package com.mcnoita.world.mutation;

import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * The only mutation facade used by spell entities. Explosions use vanilla for
 * entity knockback/damage with block destruction disabled, then apply a bounded
 * and policy-checked block candidate pass.
 */
public final class WorldMutationService {
    private static final WorldMutationPolicy POLICY = new WorldMutationPolicy();

    private WorldMutationService() {
    }

    public static boolean breakBlock(WorldMutationContext context, BlockPos pos, boolean drop, Entity breaker) {
        Optional<BlockState> state = WorldQueryService.blockState(context, pos, WorldMutationKind.BLOCK_MUTATION);
        if (state.isEmpty() || !POLICY.isDestructible(context, pos, state.get())
            || !POLICY.permitsBlockMutation(context, pos, WorldMutationKind.BLOCK_MUTATION)) {
            return false;
        }
        return context.world().breakBlock(pos, drop, breaker, 16);
    }

    public static boolean replaceBlock(WorldMutationContext context, BlockPos pos, BlockState replacement, int flags) {
        Optional<BlockState> existing = WorldQueryService.blockState(context, pos, WorldMutationKind.BLOCK_MUTATION);
        if (existing.isEmpty() || !POLICY.isReplaceable(context, pos, existing.get())
            || !POLICY.permitsBlockMutation(context, pos, WorldMutationKind.BLOCK_MUTATION)) {
            return false;
        }
        return context.world().setBlockState(pos, replacement, flags);
    }

    public static boolean spawnEntity(WorldMutationContext context, Entity entity) {
        return POLICY.permitsSpawn(context, entity) && context.world().spawnEntity(entity);
    }

    public static FallingBlockEntity spawnFallingBlock(WorldMutationContext context, BlockPos pos, BlockState state) {
        Optional<BlockState> existing = WorldQueryService.blockState(context, pos, WorldMutationKind.ENTITY_SPAWN);
        if (existing.isEmpty() || !POLICY.isReplaceable(context, pos, existing.get())
            || !POLICY.permitsBlockMutation(context, pos, WorldMutationKind.ENTITY_SPAWN)) {
            return null;
        }
        return FallingBlockEntity.spawnFromBlock(context.world(), pos, state);
    }

    public static boolean explode(WorldMutationContext context, Entity source, Vec3d center, float requestedRadius, boolean fire) {
        float radius = Math.min(WorldMutationPolicy.MAX_EXPLOSION_RADIUS, requestedRadius);
        if (!POLICY.permitsExplosion(context, center, radius)) {
            return false;
        }
        // NONE prevents vanilla's unbounded terrain pass. Force fire off too:
        // any spell fire placement must use replaceBlock and receive a per-block
        // policy decision rather than piggybacking on vanilla explosion logic.
        context.world().createExplosion(source, center.x, center.y, center.z, radius, false, World.ExplosionSourceType.NONE);
        BlockPos origin = BlockPos.ofFloored(center);
        int blockRadius = (int) Math.ceil(radius);
        double radiusSquared = radius * radius;
        int candidates = 0;
        for (int x = -blockRadius; x <= blockRadius && candidates < WorldMutationPolicy.MAX_EXPLOSION_CANDIDATES; x++) {
            for (int y = -blockRadius; y <= blockRadius && candidates < WorldMutationPolicy.MAX_EXPLOSION_CANDIDATES; y++) {
                for (int z = -blockRadius; z <= blockRadius && candidates < WorldMutationPolicy.MAX_EXPLOSION_CANDIDATES; z++) {
                    if (x * x + y * y + z * z > radiusSquared) {
                        continue;
                    }
                    BlockPos candidate = origin.add(x, y, z);
                    candidates++;
                    breakBlock(context, candidate, false, source);
                }
            }
        }
        return true;
    }
}
