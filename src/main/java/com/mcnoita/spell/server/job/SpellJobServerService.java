package com.mcnoita.spell.server.job;

import com.mcnoita.MCNoita;
import com.mcnoita.spell.server.budget.BudgetLimits;
import com.mcnoita.spell.server.budget.BudgetReservation;
import com.mcnoita.spell.server.budget.ChunkBudgetKey;
import com.mcnoita.spell.server.budget.SpellBudgetManager;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Production bridge from server lifecycle events to frozen persistent jobs.
 * It is intentionally a stable sink object so the static effect registry can
 * be constructed before a Minecraft server exists, then safely reject work
 * until SERVER_STARTED has restored its PersistentState.
 */
public final class SpellJobServerService implements SpellJobSink {
    private static final SpellJobServerService INSTANCE = new SpellJobServerService();

    private final SpellBudgetManager budgetManager = new SpellBudgetManager(BudgetLimits.DEFAULT);
    private final SpellJobHandlerRegistry handlers = new SpellJobHandlerRegistry();
    private boolean registered;
    private Runtime runtime;

    private SpellJobServerService() {
    }

    public static SpellJobServerService getInstance() {
        return INSTANCE;
    }

    public static void register() {
        INSTANCE.registerEvents();
    }

    /** CastTransaction and durable jobs intentionally share this one central ledger. */
    public SpellBudgetManager budgetManager() {
        return budgetManager;
    }

    /** Future bounded world executors must register their exact frozen type before server startup. */
    public void registerHandler(SpellJobHandler handler) {
        handlers.register(handler);
    }

    @Override
    public synchronized Submission submit(SpellJobPersistentState job) {
        Runtime active = runtime;
        if (active == null) {
            return SpellJobSink.rejecting().submit(job);
        }
        return active.manager().submit(job, active.server().getTicks());
    }

    @Override
    public synchronized Submission submit(SpellJobPersistentState job, BudgetReservation rootReservation) {
        Runtime active = runtime;
        if (active == null) {
            return SpellJobSink.rejecting().submit(job, rootReservation);
        }
        return active.manager().submit(job, rootReservation);
    }

    private synchronized void registerEvents() {
        if (registered) {
            return;
        }
        registered = true;
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
    }

    private synchronized void onServerStarted(MinecraftServer server) {
        try {
            budgetManager.resetForServerLifecycle();
        } catch (IllegalStateException failure) {
            MCNoita.LOGGER.error("Spell-job budget ledger was still active at server start; persistent jobs remain disabled", failure);
            runtime = null;
            return;
        }
        SpellJobPersistentStateStore store = SpellJobPersistentStateStore.get(server.getOverworld());
        SpellJobManager manager = new SpellJobManager(budgetManager, handlers);
        runtime = new Runtime(server, store, manager);
        manager.recover(store.jobs(), server.getTicks());
        store.replace(manager.snapshot());
    }

    private synchronized void onServerTick(MinecraftServer server) {
        Runtime active = runtime;
        if (active == null || active.server() != server) {
            return;
        }
        SpellJobGate gate = new MinecraftServerJobGate(server);
        long serverTick = server.getTicks();
        for (SpellJobPersistentState job : active.manager().snapshot()) {
            if (!job.isTerminal()) {
                active.manager().tick(job.executionId(), gate, serverTick);
            }
        }
        active.store().replace(active.manager().snapshot());
    }

    private synchronized void onServerStopping(MinecraftServer server) {
        Runtime active = runtime;
        if (active == null || active.server() != server) {
            return;
        }
        active.store().replace(active.manager().snapshot());
        active.manager().closeLifetimeLeasesForShutdown();
        runtime = null;
    }

    private record Runtime(MinecraftServer server, SpellJobPersistentStateStore store, SpellJobManager manager) {
        private Runtime {
            server = Objects.requireNonNull(server, "server");
            store = Objects.requireNonNull(store, "store");
            manager = Objects.requireNonNull(manager, "manager");
        }
    }

    /** Owner and chunk checks only query existing server state; they never load a target chunk. */
    private static final class MinecraftServerJobGate implements SpellJobGate {
        private final MinecraftServer server;

        private MinecraftServerJobGate(MinecraftServer server) {
            this.server = server;
        }

        @Override
        public boolean isOwnerEligible(UUID ownerId, String dimensionId) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(ownerId);
            return player != null && player.isAlive() && !player.isRemoved() && !player.isSpectator()
                && player.getServerWorld().getRegistryKey().getValue().toString().equals(dimensionId);
        }

        @Override
        public boolean isChunkLoaded(ChunkBudgetKey chunk) {
            for (ServerWorld world : server.getWorlds()) {
                if (world.getRegistryKey().getValue().toString().equals(chunk.dimensionId())) {
                    return world.getChunkManager().isChunkLoaded(chunk.chunkX(), chunk.chunkZ());
                }
            }
            return false;
        }
    }
}
