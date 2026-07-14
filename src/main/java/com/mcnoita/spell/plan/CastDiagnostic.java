package com.mcnoita.spell.plan;

import java.util.List;

/** Structured diagnostic safe to surface to logs, tests and a later UI. */
public record CastDiagnostic(String code, String spellId, List<String> actionPath, int used, int limit, String nodePath) {
    public CastDiagnostic {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("diagnostic code must not be blank");
        }
        spellId = spellId == null ? "" : spellId;
        actionPath = List.copyOf(actionPath);
        if (nodePath == null || nodePath.isBlank()) {
            throw new IllegalArgumentException("diagnostic node path must not be blank");
        }
    }

    /** Compatibility constructor for diagnostics created before plan node paths existed. */
    public CastDiagnostic(String code, String spellId, List<String> actionPath, int used, int limit) {
        this(code, spellId, actionPath, used, limit, "root");
    }
}
