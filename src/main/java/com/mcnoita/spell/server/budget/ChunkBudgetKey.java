package com.mcnoita.spell.server.budget;

import java.util.Objects;

/** Stable chunk scope that does not require a loaded Minecraft World reference. */
public record ChunkBudgetKey(String dimensionId, int chunkX, int chunkZ) {
    public ChunkBudgetKey {
        dimensionId = Objects.requireNonNull(dimensionId, "dimensionId");
        if (dimensionId.isBlank()) {
            throw new IllegalArgumentException("dimensionId must not be blank");
        }
    }
}
