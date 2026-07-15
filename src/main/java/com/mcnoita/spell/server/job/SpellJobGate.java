package com.mcnoita.spell.server.job;

import com.mcnoita.spell.server.budget.ChunkBudgetKey;
import java.util.UUID;

/**
 * Logical-server probes kept outside the persistent state. A false chunk probe
 * pauses work; this interface must never request or force-load that chunk.
 */
public interface SpellJobGate {
    boolean isOwnerEligible(UUID ownerId, String dimensionId);

    boolean isChunkLoaded(ChunkBudgetKey chunk);

    static SpellJobGate allowAll() {
        return AllowAllGate.INSTANCE;
    }

    enum AllowAllGate implements SpellJobGate {
        INSTANCE;

        @Override
        public boolean isOwnerEligible(UUID ownerId, String dimensionId) {
            return true;
        }

        @Override
        public boolean isChunkLoaded(ChunkBudgetKey chunk) {
            return true;
        }
    }
}
