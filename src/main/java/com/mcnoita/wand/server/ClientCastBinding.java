package com.mcnoita.wand.server;

import java.util.Objects;

/**
 * Values echoed by a v2 client intent from the latest server HUD projection.
 * They are assertions, never client authority: CastTransaction re-reads every
 * value from the held stack and catalog before it commits.
 */
public record ClientCastBinding(int stateHash, long wandRevision, long catalogEpoch, String catalogHash) {
    public ClientCastBinding {
        catalogHash = Objects.requireNonNull(catalogHash, "catalogHash");
        if (wandRevision < 0L || catalogEpoch < 0L || catalogHash.isBlank()) {
            throw new IllegalArgumentException("client cast binding is invalid");
        }
    }
}
