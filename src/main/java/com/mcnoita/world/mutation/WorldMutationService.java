package com.mcnoita.world.mutation;

import com.mcnoita.spell.damage.DamageProfile;
import com.mcnoita.spell.damage.SpellDamageService;
import java.util.List;
import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LightBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * The only mutation facade used by spell entities. Explosions apply a bounded
 * and policy-checked block candidate pass. Their entity phase remains deferred
 * until it has a bounded Minecraft-equivalent implementation.
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

    /**
     * Restricted cosmetic write for the light manager. Unlike replaceBlock it
     * only accepts air or an existing light block, so no gameplay block can be
     * overwritten while avoiding the online-owner requirement for a visual
     * trail that must also clean itself up after the caster disconnects.
     */
    public static boolean placeTemporaryLight(WorldMutationContext context, BlockPos pos, int level, int flags) {
        Optional<BlockState> existing = WorldQueryService.blockState(context, pos, WorldMutationKind.BLOCK_CHECK);
        if (existing.isEmpty() || (!existing.get().isAir() && !existing.get().isOf(Blocks.LIGHT))
            || !POLICY.permitsTemporaryLight(context, pos)) {
            return false;
        }
        int boundedLevel = Math.max(0, Math.min(15, level));
        BlockState light = Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, boundedLevel);
        return context.world().setBlockState(pos, light, flags);
    }

    /** Removes only a temporary light block after the same bounded policy check. */
    public static boolean clearTemporaryLight(WorldMutationContext context, BlockPos pos, int flags) {
        Optional<BlockState> existing = WorldQueryService.blockState(context, pos, WorldMutationKind.BLOCK_CHECK);
        if (existing.isEmpty() || !existing.get().isOf(Blocks.LIGHT) || !POLICY.permitsTemporaryLight(context, pos)) {
            return false;
        }
        return context.world().setBlockState(pos, Blocks.AIR.getDefaultState(), flags);
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
        // The source block check and replacement are separately charged above.
        // A falling block is also an authoritative entity, so deny the spawn
        // unless the runtime bridge can charge that allocation to the ledger.
        if (!context.budget().tryReserveAt(WorldMutationKind.ENTITY_SPAWN, 1, pos)) {
            return null;
        }
        return FallingBlockEntity.spawnFromBlock(context.world(), pos, state);
    }

    public static boolean explode(WorldMutationContext context, Entity source, Vec3d center, float requestedRadius, boolean fire) {
        return explode(context, source, context.source(), center, requestedRadius, DamageProfile.EMPTY, false, fire, true);
    }

    /**
     * Applies a bounded, policy-checked Minecraft-equivalent explosion. Vanilla
     * explosion traversal is not used because 1.20.4 exposes no maximum entity
     * result count. The fire flag remains deferred: fire placement must use an
     * explicit bounded block-mutation node rather than an implicit side effect.
     */
    public static boolean explode(
        WorldMutationContext context, Entity source, Vec3d center, float requestedRadius, boolean fire,
        boolean terrainRequested
    ) {
        return explode(context, source, context.source(), center, requestedRadius, DamageProfile.EMPTY, false, fire,
            terrainRequested);
    }

    /**
     * Uses frozen damage and ownership supplied by the executor or projectile;
     * clients cannot select the profile, owner, radius, or target set.
     */
    public static boolean explode(
        WorldMutationContext context, Entity source, Entity owner, Vec3d center, float requestedRadius,
        DamageProfile damageProfile, boolean allowFriendlyFire, boolean fire, boolean terrainRequested
    ) {
        if (source == null || damageProfile == null || !isFinite(center)) {
            return false;
        }
        float radius = Math.min(WorldMutationPolicy.MAX_EXPLOSION_RADIUS, requestedRadius);
        if (!POLICY.permitsExplosion(context, center, radius)) {
            return false;
        }
        if (!applyBoundedExplosionEntityPhase(context, source, owner, center, radius, damageProfile, allowFriendlyFire)) {
            return false;
        }
        BlockPos origin = BlockPos.ofFloored(center);
        if (!terrainRequested) {
            return true;
        }
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

    /**
     * A radius-derived request is charged before collection and caps the
     * EntityLookup traversal. Damage and knockback only happen after the
     * central query, owner/team, and invulnerability policies admit the hit.
     */
    private static boolean applyBoundedExplosionEntityPhase(
        WorldMutationContext context, Entity source, Entity owner, Vec3d center, float radius,
        DamageProfile profile, boolean allowFriendlyFire
    ) {
        if (profile.isEmpty()) {
            return true;
        }
        int limit = boundedExplosionEntityLimit(radius);
        Box area = Box.of(center, radius * 2.0, radius * 2.0, radius * 2.0);
        Optional<List<Entity>> queried = WorldQueryService.tryEntities(context, source, area, entity -> true, limit);
        if (queried.isEmpty()) {
            return false;
        }
        List<Entity> entities = queried.get();
        Entity resolvedOwner = owner == null ? context.resolveOnlineOwner() : owner;
        double radiusSquared = radius * radius;
        for (Entity target : entities) {
            Vec3d offset = target.getPos().subtract(center);
            double distanceSquared = offset.lengthSquared();
            if (distanceSquared >= radiusSquared) {
                continue;
            }
            double distance = Math.sqrt(Math.max(0.0, distanceSquared));
            double scale = Math.max(0.0, 1.0 - distance / radius);
            if (scale == 0.0 || !SpellDamageService.apply(target, source, resolvedOwner, profile.scale(scale),
                allowFriendlyFire)) {
                continue;
            }
            if (distanceSquared > 1.0E-6) {
                Vec3d impulse = offset.normalize().multiply(scale * 0.5);
                target.addVelocity(impulse.x, impulse.y + scale * 0.15, impulse.z);
                target.velocityModified = true;
            }
        }
        return true;
    }

    /** Mirrors PlanBudgetRequestFactory's sphere upper bound while honoring the query hard cap. */
    static int boundedExplosionEntityLimit(float radius) {
        double estimate = Math.ceil((4.0 / 3.0) * Math.PI * radius * radius * radius);
        if (!Double.isFinite(estimate)) {
            return WorldMutationPolicy.MAX_QUERY_RESULTS;
        }
        return Math.max(1, Math.min(WorldMutationPolicy.MAX_QUERY_RESULTS, (int) Math.min(Integer.MAX_VALUE, estimate)));
    }

    /**
     * Teleports only the currently resolved owner. Every candidate is checked
     * through the bounded query path before collision testing, so the method
     * never loads a chunk to honor a spell request.
     */
    public static boolean teleportOwner(WorldMutationContext context, ServerPlayerEntity target, Vec3d requestedDestination) {
        if (target == null || context.resolveOnlineOwner() != target || !isFinite(requestedDestination)) {
            return false;
        }

        BlockPos base = BlockPos.ofFloored(requestedDestination);
        int[] verticalOffsets = {0, 1, -1, 2, -2, 3, -3};
        for (int offset : verticalOffsets) {
            BlockPos feet = base.up(offset);
            if (!isSafeTeleportPosition(context, feet)) {
                continue;
            }
            Vec3d destination = new Vec3d(requestedDestination.x, feet.getY(), requestedDestination.z);
            Vec3d movement = destination.subtract(target.getPos());
            Box destinationBox = target.getBoundingBox().offset(movement);
            if (!context.world().getWorldBorder().contains(destinationBox)
                || !context.world().isSpaceEmpty(target, destinationBox)) {
                continue;
            }
            target.requestTeleport(destination.x, destination.y, destination.z);
            return true;
        }
        return false;
    }

    /** Teleports only the owner re-resolved from the immutable mutation context. */
    public static boolean teleportBoundOwner(WorldMutationContext context, Vec3d requestedDestination) {
        return teleportOwner(context, context.resolveOnlineOwner(), requestedDestination);
    }

    private static boolean isSafeTeleportPosition(WorldMutationContext context, BlockPos feet) {
        Optional<BlockState> floor = WorldQueryService.blockState(context, feet.down(), WorldMutationKind.BLOCK_CHECK);
        Optional<BlockState> foot = WorldQueryService.blockState(context, feet, WorldMutationKind.BLOCK_CHECK);
        Optional<BlockState> head = WorldQueryService.blockState(context, feet.up(), WorldMutationKind.BLOCK_CHECK);
        return floor.isPresent() && foot.isPresent() && head.isPresent()
            && !floor.get().getCollisionShape(context.world(), feet.down()).isEmpty()
            && foot.get().isAir() && head.get().isAir();
    }

    private static boolean isFinite(Vec3d value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }
}
