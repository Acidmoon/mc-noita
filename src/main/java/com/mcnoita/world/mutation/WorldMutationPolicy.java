package com.mcnoita.world.mutation;

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
        return context.budget().tryReserve(WorldMutationKind.ENTITY_QUERY, resultLimit);
    }

    public boolean permitsSpawn(WorldMutationContext context, Entity entity) {
        if (entity == null) {
            return false;
        }
        return permitsDestructivePosition(context, entity.getBlockPos(), WorldMutationKind.ENTITY_SPAWN)
            && context.budget().tryReserve(WorldMutationKind.ENTITY_SPAWN, 1);
    }

    public boolean permitsExplosion(WorldMutationContext context, Vec3d center, float radius) {
        return Float.isFinite(radius) && radius > 0.0f && radius <= MAX_EXPLOSION_RADIUS
            && permitsDestructivePosition(context, BlockPos.ofFloored(center), WorldMutationKind.EXPLOSION);
    }

    public boolean permitsBlockCheck(WorldMutationContext context, BlockPos pos, WorldMutationKind kind) {
        return hasExpectedLoadedWorld(context, pos)
            && context.budget().tryReserve(WorldMutationKind.BLOCK_CHECK, 1)
            && adaptersAllow(context, pos, kind);
    }

    public boolean permitsBlockMutation(WorldMutationContext context, BlockPos pos, WorldMutationKind kind) {
        return permitsDestructivePosition(context, pos, kind)
            && context.budget().tryReserve(WorldMutationKind.BLOCK_MUTATION, 1);
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
        int minChunkX = ((int) Math.floor(area.minX)) >> 4;
        int maxChunkX = ((int) Math.floor(area.maxX)) >> 4;
        int minChunkZ = ((int) Math.floor(area.minZ)) >> 4;
        int maxChunkZ = ((int) Math.floor(area.maxZ)) >> 4;
        long chunkCount = (long) (maxChunkX - minChunkX + 1) * (long) (maxChunkZ - minChunkZ + 1);
        if (chunkCount > MAX_QUERY_CHUNKS) {
            return false;
        }
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
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
}
