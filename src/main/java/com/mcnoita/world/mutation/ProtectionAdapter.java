package com.mcnoita.world.mutation;

import net.minecraft.util.math.BlockPos;

/** Optional server integration point for a claims or protection implementation. */
@FunctionalInterface
public interface ProtectionAdapter {
    /** Returns false when the adapter owns this position and denies the operation. */
    boolean allows(WorldMutationContext context, BlockPos pos, WorldMutationKind kind);
}
