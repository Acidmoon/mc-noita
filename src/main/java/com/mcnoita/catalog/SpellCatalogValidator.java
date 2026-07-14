package com.mcnoita.catalog;

import com.mcnoita.spell.action.SpellDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Validates complete built-in definitions and reload-time override keys. */
public final class SpellCatalogValidator {
    public ValidationResult validateInitial(Map<String, SpellDefinition> definitions) {
        return validateDefinitions(definitions, false);
    }

    public ValidationResult validateOverrides(Set<String> registeredIds, Map<String, SpellDefinition> overrides) {
        Objects.requireNonNull(registeredIds, "registeredIds");
        ValidationResult shape = validateDefinitions(overrides, true);
        List<String> errors = new ArrayList<>(shape.errors());
        if (overrides != null) {
            for (String id : overrides.keySet()) {
                if (id != null && !registeredIds.contains(id)) {
                    errors.add("reload may not add an unregistered spell id: " + id);
                }
            }
        }
        return new ValidationResult(errors);
    }

    private ValidationResult validateDefinitions(Map<String, SpellDefinition> definitions, boolean allowEmpty) {
        List<String> errors = new ArrayList<>();
        if (definitions == null) {
            return new ValidationResult(List.of("spell definitions must not be null"));
        }
        if (!allowEmpty && definitions.isEmpty()) {
            errors.add("initial spell catalog must not be empty");
        }
        for (Map.Entry<String, SpellDefinition> entry : definitions.entrySet()) {
            String id = entry.getKey();
            SpellDefinition definition = entry.getValue();
            if (id == null || id.isBlank()) {
                errors.add("spell catalog contains a blank id");
            }
            if (definition == null) {
                errors.add("spell catalog contains a null definition for id: " + id);
            } else if (!definition.id().equals(id)) {
                errors.add("spell catalog key does not match definition id: " + id);
            }
        }
        return new ValidationResult(errors);
    }

    public record ValidationResult(List<String> errors) {
        public ValidationResult {
            errors = List.copyOf(Objects.requireNonNull(errors, "errors"));
        }

        public boolean isValid() {
            return errors.isEmpty();
        }
    }
}
