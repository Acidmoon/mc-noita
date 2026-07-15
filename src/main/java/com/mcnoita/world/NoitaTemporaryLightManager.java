package com.mcnoita.world;

import com.mcnoita.world.mutation.WorldMutationContext;
import com.mcnoita.world.mutation.WorldMutationKind;
import com.mcnoita.world.mutation.WorldMutationService;
import com.mcnoita.world.mutation.WorldQueryService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.LightBlock;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class NoitaTemporaryLightManager {
    private static final int BASE_LIGHT_LEVEL = 8;
    private static final int LIGHT_LEVEL_PER_EXTRA_STACK = 3;
    private static final int MAX_LIGHT_LEVEL = 15;
    private static final int LIGHT_TICKS = 4;
    private static final int MAX_SAMPLES_PER_TICK = 6;
    private static final double SAMPLE_SPACING = 1.5;
    private static final int BLOCK_UPDATE_FLAGS = Block.NOTIFY_LISTENERS;

    private static final Map<RegistryKey<World>, Map<BlockPos, LightEntry>> LIGHTS = new HashMap<>();
    private static MinecraftServer activeServer;
    private static TemporaryLightPersistentStateStore persistentState;
    private static boolean registered;

    private NoitaTemporaryLightManager() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        ServerLifecycleEvents.SERVER_STARTED.register(NoitaTemporaryLightManager::onServerStarted);
        ServerTickEvents.END_SERVER_TICK.register(NoitaTemporaryLightManager::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(NoitaTemporaryLightManager::onServerStopping);
    }

    public static void illuminateTrail(ServerWorld world, Vec3d from, Vec3d to, int lightStacks) {
        if (lightStacks <= 0) {
            return;
        }

        int lightLevel = Math.min(MAX_LIGHT_LEVEL, BASE_LIGHT_LEVEL + (lightStacks - 1) * LIGHT_LEVEL_PER_EXTRA_STACK);
        double distance = from.distanceTo(to);
        int samples = Math.max(1, Math.min(MAX_SAMPLES_PER_TICK, (int) Math.ceil(distance / SAMPLE_SPACING)));
        for (int i = 0; i <= samples; i++) {
            double progress = samples == 0 ? 1.0 : i / (double) samples;
            placeLight(world, BlockPos.ofFloored(from.lerp(to, progress)), lightLevel);
        }
    }

    private static void tick(MinecraftServer server) {
        if (server != activeServer) {
            return;
        }
        boolean changed = false;
        Iterator<Map.Entry<RegistryKey<World>, Map<BlockPos, LightEntry>>> worlds = LIGHTS.entrySet().iterator();
        while (worlds.hasNext()) {
            Map.Entry<RegistryKey<World>, Map<BlockPos, LightEntry>> entry = worlds.next();
            ServerWorld world = server.getWorld(entry.getKey());
            if (world == null) {
                // A missing dimension cannot be queried safely. Retain its
                // cleanup record until a server exposes that world again.
                continue;
            }

            changed |= removeExpiredLights(world, entry.getValue());
            if (entry.getValue().isEmpty()) {
                worlds.remove();
                changed = true;
            }
        }
        if (changed) {
            persistSnapshot(server);
        }
    }

    private static void onServerStarted(MinecraftServer server) {
        activeServer = server;
        persistentState = TemporaryLightPersistentStateStore.get(server.getOverworld());
        LIGHTS.clear();
        for (TemporaryLightPersistentStateStore.PendingLight light : persistentState.lights()) {
            Identifier dimension = Identifier.tryParse(light.dimensionId());
            if (dimension == null) {
                continue;
            }
            RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, dimension);
            LIGHTS.computeIfAbsent(worldKey, ignored -> new HashMap<>())
                .put(light.pos(), new LightEntry(light.expiresAt(), light.lightLevel()));
        }
    }

    private static void onServerStopping(MinecraftServer server) {
        if (server != activeServer) {
            return;
        }
        // Do not remove blocks while shutdown may unload chunks. The persisted
        // record lets the next server remove a loaded light through the same
        // WorldMutationService policy boundary.
        persistSnapshot(server);
        LIGHTS.clear();
        persistentState = null;
        activeServer = null;
    }

    /**
     * Drops a tracking entry only once its remover confirms that the target
     * state is absent. A false result is retryable, notably for an unloaded
     * chunk that must not be force-loaded during cleanup.
     */
    static <K, V> void removeEntriesAfterSuccessfulRemoval(Map<K, V> entries, Predicate<? super K> remover) {
        Iterator<Map.Entry<K, V>> iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            if (remover.test(iterator.next().getKey())) {
                iterator.remove();
            }
        }
    }

    private static void placeLight(ServerWorld world, BlockPos rawPos, int lightLevel) {
        if (world.getServer() != activeServer || persistentState == null || !persistentState.canTrackNewLights()) {
            // A future-format ledger is intentionally read-only. Creating a
            // light in that state would leave no durable cleanup record after
            // this server stops, so cosmetics must fail closed.
            return;
        }
        BlockPos pos = rawPos.toImmutable();
        WorldMutationContext context = WorldMutationContext.forTemporaryLight(world);
        var currentState = WorldQueryService.blockState(context, pos, WorldMutationKind.BLOCK_CHECK);
        if (currentState.isEmpty()) {
            return;
        }

        Map<BlockPos, LightEntry> worldLights = LIGHTS.get(world.getRegistryKey());
        LightEntry existingEntry = worldLights == null ? null : worldLights.get(pos);
        if (existingEntry == null && !currentState.get().isAir()) {
            return;
        }
        if (existingEntry != null && !currentState.get().isAir() && !currentState.get().isOf(Blocks.LIGHT)) {
            // A player or another system replaced our light before expiry.
            // Never restore the trail by overwriting that authoritative block.
            worldLights.remove(pos);
            removeWorldIfEmpty(world.getRegistryKey(), worldLights);
            persistSnapshot(world.getServer());
            return;
        }
        if (existingEntry == null && trackedLightCount() >= TemporaryLightPersistentStateStore.MAX_RECORDS) {
            // Never create an untracked light block: after a restart it would
            // lack a bounded cleanup record and could remain indefinitely.
            return;
        }

        long expiresAt = world.getTime() + LIGHT_TICKS;
        if (!currentState.get().isOf(Blocks.LIGHT) || currentState.get().get(LightBlock.LEVEL_15) < lightLevel) {
            if (!WorldMutationService.placeTemporaryLight(context, pos, lightLevel, BLOCK_UPDATE_FLAGS)) {
                return;
            }
        }
        LIGHTS.computeIfAbsent(world.getRegistryKey(), key -> new HashMap<>()).put(pos, new LightEntry(expiresAt, lightLevel));
        persistSnapshot(world.getServer());
    }

    private static boolean removeExpiredLights(ServerWorld world, Map<BlockPos, LightEntry> lights) {
        long now = world.getTime();
        boolean changed = false;
        Iterator<Map.Entry<BlockPos, LightEntry>> iterator = lights.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, LightEntry> entry = iterator.next();
            if (entry.getValue().expiresAt() > now) {
                continue;
            }

            if (removeLight(world, entry.getKey(), entry.getValue().lightLevel())) {
                iterator.remove();
                changed = true;
            }
        }
        return changed;
    }

    private static boolean removeLight(ServerWorld world, BlockPos pos, int expectedLightLevel) {
        WorldMutationContext context = WorldMutationContext.forTemporaryLight(world);
        var currentState = WorldQueryService.blockState(context, pos, WorldMutationKind.BLOCK_CHECK);
        if (currentState.isEmpty()) {
            // Keep the entry for a later tick rather than force-loading the
            // chunk or forgetting a light that still exists on disk.
            return false;
        }
        if (!currentState.get().isOf(Blocks.LIGHT)) {
            return true;
        }
        if (expectedLightLevel != TemporaryLightPersistentStateStore.UNKNOWN_LIGHT_LEVEL
            && currentState.get().get(LightBlock.LEVEL_15) != expectedLightLevel) {
            // A later writer changed the light level. Drop only our tracking
            // entry and never erase a block that no longer matches this trail.
            return true;
        }
        return WorldMutationService.clearTemporaryLight(context, pos, BLOCK_UPDATE_FLAGS);
    }

    private static void persistSnapshot(MinecraftServer server) {
        if (server != activeServer || persistentState == null) {
            return;
        }
        List<TemporaryLightPersistentStateStore.PendingLight> pendingLights = new ArrayList<>(trackedLightCount());
        for (Map.Entry<RegistryKey<World>, Map<BlockPos, LightEntry>> worldEntry : LIGHTS.entrySet()) {
            String dimensionId = worldEntry.getKey().getValue().toString();
            for (Map.Entry<BlockPos, LightEntry> lightEntry : worldEntry.getValue().entrySet()) {
                pendingLights.add(new TemporaryLightPersistentStateStore.PendingLight(dimensionId, lightEntry.getKey(),
                    lightEntry.getValue().expiresAt(), lightEntry.getValue().lightLevel()));
            }
        }
        persistentState.replace(pendingLights);
    }

    private static int trackedLightCount() {
        int count = 0;
        for (Map<BlockPos, LightEntry> lights : LIGHTS.values()) {
            count += lights.size();
        }
        return count;
    }

    private static void removeWorldIfEmpty(RegistryKey<World> worldKey, Map<BlockPos, LightEntry> lights) {
        if (lights.isEmpty()) {
            LIGHTS.remove(worldKey);
        }
    }

    private record LightEntry(long expiresAt, int lightLevel) {
    }
}
