package com.mcnoita.world;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LightBlock;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class NoitaTemporaryLightManager {
    private static final int LIGHT_LEVEL = 8;
    private static final int LIGHT_TICKS = 4;
    private static final int MAX_SAMPLES_PER_TICK = 6;
    private static final double SAMPLE_SPACING = 1.5;
    private static final int BLOCK_UPDATE_FLAGS = Block.NOTIFY_LISTENERS;

    private static final Map<RegistryKey<World>, Map<BlockPos, LightEntry>> LIGHTS = new HashMap<>();

    private NoitaTemporaryLightManager() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(NoitaTemporaryLightManager::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(NoitaTemporaryLightManager::clearAll);
    }

    public static void illuminateTrail(ServerWorld world, Vec3d from, Vec3d to) {
        double distance = from.distanceTo(to);
        int samples = Math.max(1, Math.min(MAX_SAMPLES_PER_TICK, (int) Math.ceil(distance / SAMPLE_SPACING)));
        for (int i = 0; i <= samples; i++) {
            double progress = samples == 0 ? 1.0 : i / (double) samples;
            placeLight(world, BlockPos.ofFloored(from.lerp(to, progress)));
        }
    }

    private static void tick(MinecraftServer server) {
        LIGHTS.entrySet().removeIf(entry -> {
            ServerWorld world = server.getWorld(entry.getKey());
            if (world == null) {
                return true;
            }

            removeExpiredLights(world, entry.getValue());
            return entry.getValue().isEmpty();
        });
    }

    private static void clearAll(MinecraftServer server) {
        for (Map.Entry<RegistryKey<World>, Map<BlockPos, LightEntry>> entry : LIGHTS.entrySet()) {
            ServerWorld world = server.getWorld(entry.getKey());
            if (world == null) {
                continue;
            }

            for (BlockPos pos : entry.getValue().keySet()) {
                removeLight(world, pos);
            }
        }

        LIGHTS.clear();
    }

    private static void placeLight(ServerWorld world, BlockPos rawPos) {
        BlockPos pos = rawPos.toImmutable();
        if (!world.isInBuildLimit(pos) || !world.isChunkLoaded(ChunkPos.toLong(pos))) {
            return;
        }

        Map<BlockPos, LightEntry> worldLights = LIGHTS.computeIfAbsent(world.getRegistryKey(), key -> new HashMap<>());
        BlockState currentState = world.getBlockState(pos);
        LightEntry existingEntry = worldLights.get(pos);
        if (existingEntry == null && !currentState.isAir()) {
            return;
        }

        long expiresAt = world.getTime() + LIGHT_TICKS;
        worldLights.put(pos, new LightEntry(expiresAt));

        if (!currentState.isOf(Blocks.LIGHT) || currentState.get(LightBlock.LEVEL_15) < LIGHT_LEVEL) {
            world.setBlockState(pos, Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, LIGHT_LEVEL), BLOCK_UPDATE_FLAGS);
        }
    }

    private static void removeExpiredLights(ServerWorld world, Map<BlockPos, LightEntry> lights) {
        long now = world.getTime();
        Iterator<Map.Entry<BlockPos, LightEntry>> iterator = lights.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, LightEntry> entry = iterator.next();
            if (entry.getValue().expiresAt() > now) {
                continue;
            }

            removeLight(world, entry.getKey());
            iterator.remove();
        }
    }

    private static void removeLight(ServerWorld world, BlockPos pos) {
        if (world.isInBuildLimit(pos) && world.isChunkLoaded(ChunkPos.toLong(pos)) && world.getBlockState(pos).isOf(Blocks.LIGHT)) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), BLOCK_UPDATE_FLAGS);
        }
    }

    private record LightEntry(long expiresAt) {
    }
}
