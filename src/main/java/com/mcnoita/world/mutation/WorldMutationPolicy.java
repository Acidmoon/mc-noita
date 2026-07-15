package com.mcnoita.world.mutation;

import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;

/**
 * Conservative execution-time policy for Noita effects. It intentionally
 * applies mobGriefing to player casts, reflected projectiles, and summons until
 * a server configuration can express a less restrictive documented policy.
 */
public final class WorldMutationPolicy {
    public static final int MAX_QUERY_RESULTS = 128;
    public static final double MAX_QUERY_DIAMETER = 64.0;
    public static final double MAX_QUERY_VOLUME = 262_144.0;
    public static final int MAX_QUERY_CHUNKS = 25;
    /** One DDA ray may visit this many blocks after its 64-block distance guard. */
    public static final int MAX_RAYCAST_BLOCK_CHECKS = 193;
    public static final double MAX_RAYCAST_DISTANCE = 64.0;
    public static final int MAX_EXPLOSION_CANDIDATES = 512;
    public static final int MAX_EXPLOSION_RADIUS = 16;
    public static final float MAX_BREAK_HARDNESS = 20.0f;

    private static final CopyOnWriteArrayList<ProtectionAdapter> PROTECTION_ADAPTERS = new CopyOnWriteArrayList<>();

    public static void registerProtectionAdapter(ProtectionAdapter adapter) {
        PROTECTION_ADAPTERS.addIfAbsent(adapter);
    }

    public static void unregisterProtectionAdapter(ProtectionAdapter adapter) {
        PROTECTION_ADAPTERS.remove(adapter);
    }

    public boolean permitsQuery(WorldMutationContext context, Box area, int resultLimit) {
        if (resultLimit <= 0 || resultLimit > MAX_QUERY_RESULTS || !isBoundedQueryBox(area)
            || !hasExpectedLoadedWorld(context, area)) {
            return false;
        }
        return context.budget().tryReserveIn(WorldMutationKind.ENTITY_QUERY, resultLimit, area);
    }

    /**
     * Gates a block ray before Minecraft can inspect its first chunk. The
     * whole swept envelope must already be loaded, and the DDA upper bound is
     * charged atomically so a fast projectile cannot turn into an unbounded
     * per-tick world read.
     */
    public boolean permitsRaycast(WorldMutationContext context, Vec3d start, Vec3d end) {
        int checks = raycastBlockChecks(start, end);
        if (checks == 0) {
            return false;
        }
        BlockPos startPos = BlockPos.ofFloored(start);
        BlockPos endPos = BlockPos.ofFloored(end);
        Box envelope = new Box(start, end).expand(1.0E-7);
        return context.world().isInBuildLimit(startPos)
            && context.world().isInBuildLimit(endPos)
            && hasExpectedLoadedWorld(context, envelope)
            && context.budget().tryReserveIn(WorldMutationKind.BLOCK_CHECK, checks, envelope);
    }

    public boolean permitsSpawn(WorldMutationContext context, Entity entity) {
        if (entity == null) {
            return false;
        }
        return permitsDestructivePosition(context, entity.getBlockPos(), WorldMutationKind.ENTITY_SPAWN)
            && context.budget().tryReserveAt(WorldMutationKind.ENTITY_SPAWN, 1, entity.getBlockPos());
    }

    public boolean permitsExplosion(WorldMutationContext context, Vec3d center, float radius) {
        // The caller charges the actual bounded entity collection through
        // WorldQueryService. Do not spend a synthetic scan here, otherwise a
        // small frozen explosion could reserve more than its plan estimated.
        return Float.isFinite(radius) && radius > 0.0f && radius <= MAX_EXPLOSION_RADIUS
            && permitsDestructivePosition(context, BlockPos.ofFloored(center), WorldMutationKind.EXPLOSION);
    }

    public boolean permitsBlockCheck(WorldMutationContext context, BlockPos pos, WorldMutationKind kind) {
        return hasExpectedLoadedWorld(context, pos)
            && context.budget().tryReserveAt(WorldMutationKind.BLOCK_CHECK, 1, pos)
            && adaptersAllow(context, pos, kind);
    }

    public boolean permitsBlockMutation(WorldMutationContext context, BlockPos pos, WorldMutationKind kind) {
        return permitsDestructivePosition(context, pos, kind)
            && context.budget().tryReserveAt(WorldMutationKind.BLOCK_MUTATION, 1, pos);
    }

    /**
     * Temporary light never replaces a non-light block and is cleaned up by
     * its owner manager, so it does not require an online player or
     * mobGriefing. Claim adapters and the normal loaded-world/budget guards
     * still apply to avoid treating cosmetics as an unrestricted write path.
     */
    public boolean permitsTemporaryLight(WorldMutationContext context, BlockPos pos) {
        return hasExpectedLoadedWorld(context, pos)
            && adaptersAllow(context, pos, WorldMutationKind.BLOCK_MUTATION)
            && context.budget().tryReserveAt(WorldMutationKind.BLOCK_MUTATION, 1, pos);
    }

    public boolean isDestructible(WorldMutationContext context, BlockPos pos, BlockState state) {
        if (state.isAir() || state.isIn(ModSpellBlockTags.SPELL_UNBREAKABLE)) {
            return false;
        }
        float hardness = state.getHardness(context.world(), pos);
        return Float.isFinite(hardness) && hardness >= 0.0f && hardness <= MAX_BREAK_HARDNESS;
    }

    /** Air may receive a bounded placement; replacing an existing block must obey break rules. */
    public boolean isReplaceable(WorldMutationContext context, BlockPos pos, BlockState state) {
        return state.isAir() || isDestructible(context, pos, state);
    }

    /** Pure geometric guard that prevents a hostile Box from driving an unbounded chunk loop. */
    public static boolean isBoundedQueryBox(Box area) {
        double width = area.maxX - area.minX;
        double height = area.maxY - area.minY;
        double depth = area.maxZ - area.minZ;
        return Double.isFinite(width) && Double.isFinite(height) && Double.isFinite(depth)
            && width >= 0.0 && height >= 0.0 && depth >= 0.0
            && width <= MAX_QUERY_DIAMETER && height <= MAX_QUERY_DIAMETER && depth <= MAX_QUERY_DIAMETER
            && width * height * depth <= MAX_QUERY_VOLUME;
    }

    /**
     * Returns the exact loaded-chunk envelope for a bounded half-open Box.
     * Every boundary uses this helper so policy checks, root planning, and
     * runtime quota charging agree on which chunk owns an operation at x/z=16.
     */
    public static Optional<ChunkEnvelope> chunkEnvelope(Box area) {
        if (area == null || !isBoundedQueryBox(area)) {
            return Optional.empty();
        }
        int minChunkX = ((int) Math.floor(area.minX)) >> 4;
        int maxChunkX = coveredMaximumChunk(area.minX, area.maxX);
        int minChunkZ = ((int) Math.floor(area.minZ)) >> 4;
        int maxChunkZ = coveredMaximumChunk(area.minZ, area.maxZ);
        long width = (long) maxChunkX - minChunkX + 1L;
        long depth = (long) maxChunkZ - minChunkZ + 1L;
        if (width < 1L || depth < 1L || width > MAX_QUERY_CHUNKS
            || depth > MAX_QUERY_CHUNKS || width > MAX_QUERY_CHUNKS / depth) {
            return Optional.empty();
        }
        return Optional.of(new ChunkEnvelope(minChunkX, maxChunkX, minChunkZ, maxChunkZ));
    }

    /** Returns zero for malformed or overlong rays before any world lookup. */
    static int raycastBlockChecks(Vec3d start, Vec3d end) {
        if (start == null || end == null || !isFinite(start) || !isFinite(end)) {
            return 0;
        }
        Vec3d delta = end.subtract(start);
        if (delta.lengthSquared() <= 1.0E-8 || delta.lengthSquared() > MAX_RAYCAST_DISTANCE * MAX_RAYCAST_DISTANCE) {
            return 0;
        }
        long checks = (long) Math.ceil(Math.abs(delta.x))
            + (long) Math.ceil(Math.abs(delta.y))
            + (long) Math.ceil(Math.abs(delta.z)) + 1L;
        return checks > MAX_RAYCAST_BLOCK_CHECKS ? 0 : (int) checks;
    }

    private boolean permitsDestructivePosition(WorldMutationContext context, BlockPos pos, WorldMutationKind kind) {
        if (!hasExpectedLoadedWorld(context, pos)
            || !context.world().getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
            return false;
        }
        ServerPlayerEntity owner = context.resolveOnlineOwner();
        if (owner == null) {
            // Offline or non-player ownership never performs destructive work.
            return false;
        }
        MinecraftServer server = context.world().getServer();
        if (server.isSpawnProtected(context.world(), pos, owner)
            || !context.world().canPlayerModifyAt(owner, pos)
            || !adaptersAllow(context, pos, kind)) {
            return false;
        }
        return true;
    }

    private boolean hasExpectedLoadedWorld(WorldMutationContext context, BlockPos pos) {
        return context.world().getRegistryKey().equals(context.expectedDimension())
            && context.world().isInBuildLimit(pos)
            && context.world().getWorldBorder().contains(pos)
            && context.world().isChunkLoaded(ChunkPos.toLong(pos));
    }

    private boolean hasExpectedLoadedWorld(WorldMutationContext context, Box area) {
        if (!isBoundedQueryBox(area) || !context.world().getRegistryKey().equals(context.expectedDimension())
            || !context.world().getWorldBorder().contains(area)) {
            return false;
        }
        ChunkEnvelope envelope = chunkEnvelope(area).orElse(null);
        if (envelope == null) {
            return false;
        }
        for (int chunkX = envelope.minChunkX(); chunkX <= envelope.maxChunkX(); chunkX++) {
            for (int chunkZ = envelope.minChunkZ(); chunkZ <= envelope.maxChunkZ(); chunkZ++) {
                if (!context.world().isChunkLoaded(ChunkPos.toLong(chunkX, chunkZ))) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean adaptersAllow(WorldMutationContext context, BlockPos pos, WorldMutationKind kind) {
        for (ProtectionAdapter adapter : PROTECTION_ADAPTERS) {
            if (!adapter.allows(context, pos, kind)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isFinite(Vec3d value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }

    private static int coveredMaximumChunk(double minimum, double maximum) {
        double coveredMaximum = maximum > minimum ? Math.nextDown(maximum) : maximum;
        return ((int) Math.floor(coveredMaximum)) >> 4;
    }

    /** Immutable x/z chunk envelope shared by all bounded world-query paths. */
    public record ChunkEnvelope(int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ) {
        public ChunkEnvelope {
            if (maxChunkX < minChunkX || maxChunkZ < minChunkZ) {
                throw new IllegalArgumentException("chunk envelope bounds are inverted");
            }
        }

        public boolean contains(int chunkX, int chunkZ) {
            return chunkX >= minChunkX && chunkX <= maxChunkX && chunkZ >= minChunkZ && chunkZ <= maxChunkZ;
        }

        public boolean contains(Box area) {
            return WorldMutationPolicy.chunkEnvelope(area)
                .map(candidate -> candidate.minChunkX >= minChunkX && candidate.maxChunkX <= maxChunkX
                    && candidate.minChunkZ >= minChunkZ && candidate.maxChunkZ <= maxChunkZ)
                .orElse(false);
        }
    }
}
