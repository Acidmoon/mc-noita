package com.mcnoita.spell.trigger;

import com.mcnoita.MCNoita;
import com.mcnoita.spell.NoitaExecutionIdentity;
import com.mcnoita.spell.NoitaSpellTriggerMode;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/**
 * Server-only, bounded diagnostics for accepted frozen trees that later reach
 * a runtime capacity limit. This is intentionally outside the pure controller.
 */
public final class TriggerRuntimeDiagnostics {
    private static final long LOG_INTERVAL_TICKS = 200L;
    private static final int MAX_RATE_LIMIT_KEYS = 1_024;
    private static final Map<String, Long> LAST_LOG_TICK = new LinkedHashMap<>(MAX_RATE_LIMIT_KEYS + 1, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
            return size() > MAX_RATE_LIMIT_KEYS;
        }
    };

    private TriggerRuntimeDiagnostics() {
    }

    public static void reportBudgetExhaustion(
        World world,
        Entity owner,
        NoitaExecutionIdentity identity,
        NoitaSpellTriggerMode mode,
        TriggerRuntimeState state,
        TriggerBudgetExhaustion exhaustion
    ) {
        if (world.isClient || exhaustion == null) {
            return;
        }
        long now = world.getTime();
        String key = identity.executionId() + "|" + identity.nodePath() + "|" + exhaustion;
        synchronized (LAST_LOG_TICK) {
            Long previous = LAST_LOG_TICK.get(key);
            if (previous != null && now - previous < LOG_INTERVAL_TICKS) {
                return;
            }
            LAST_LOG_TICK.put(key, now);
        }
        String ownerId = owner == null ? "none" : owner.getUuidAsString();
        MCNoita.LOGGER.warn(
            "Frozen trigger budget exhausted executionId={} nodePath={} owner={} dimension={} mode={} releaseSequence={} budgetType={}",
            identity.executionId(), identity.nodePath(), ownerId, world.getRegistryKey().getValue(), mode,
            state.releaseSequence(), exhaustion
        );
    }
}
