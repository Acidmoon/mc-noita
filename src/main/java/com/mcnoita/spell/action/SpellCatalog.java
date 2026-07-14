package com.mcnoita.spell.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable catalog snapshot with a stable epoch/hash frozen into every cast. */
public record SpellCatalog(long epoch, String hash, Map<String, SpellDefinition> definitions) {
    public SpellCatalog {
        if (epoch < 0 || hash == null || hash.isBlank()) {
            throw new IllegalArgumentException("catalog epoch and hash must be valid");
        }
        Objects.requireNonNull(definitions, "definitions");
        Map<String, SpellDefinition> copy = new LinkedHashMap<>();
        for (Map.Entry<String, SpellDefinition> entry : definitions.entrySet()) {
            String id = Objects.requireNonNull(entry.getKey(), "catalog id");
            SpellDefinition definition = Objects.requireNonNull(entry.getValue(), "spell definition");
            if (!id.equals(definition.id())) {
                throw new IllegalArgumentException("catalog key must match spell definition id");
            }
            if (copy.put(id, definition) != null) {
                throw new IllegalArgumentException("duplicate catalog id: " + id);
            }
        }
        definitions = Collections.unmodifiableMap(copy);
    }

    public SpellDefinition require(String id) {
        SpellDefinition definition = definitions.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("unknown spell id: " + id);
        }
        return definition;
    }

    public List<SpellDefinition> candidates(SpellCategory category) {
        List<SpellDefinition> candidates = new ArrayList<>();
        for (SpellDefinition definition : definitions.values()) {
            if (definition.category() == category) {
                candidates.add(definition);
            }
        }
        return List.copyOf(candidates);
    }
}
