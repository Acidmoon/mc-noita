package com.mcnoita.spell;

import com.mcnoita.persistence.NoitaNbtLimits;
import java.util.Objects;
import java.util.UUID;

/**
 * Stable identity attached to every frozen runtime node. The zero UUID marks a
 * legacy/unbound payload only; newly accepted casts receive a server UUID.
 */
public record NoitaExecutionIdentity(UUID executionId, String nodePath, long catalogEpoch, String catalogHash) {
    public static final UUID UNBOUND_EXECUTION_ID = new UUID(0L, 0L);

    public NoitaExecutionIdentity {
        Objects.requireNonNull(executionId, "executionId");
        nodePath = requireNodePath(nodePath);
        catalogHash = requireBounded(catalogHash, "catalogHash");
    }

    public static NoitaExecutionIdentity unbound(String nodePath) {
        return new NoitaExecutionIdentity(UNBOUND_EXECUTION_ID, nodePath, 0L, "legacy");
    }

    public boolean isBound() {
        return !UNBOUND_EXECUTION_ID.equals(executionId);
    }

    public static String requireNodePath(String value) {
        return requireBounded(value, "nodePath");
    }

    private static String requireBounded(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank() || value.length() > NoitaNbtLimits.MAX_STRING_LENGTH) {
            throw new IllegalArgumentException(name + " must be a nonblank bounded value");
        }
        return value;
    }
}
