package com.mcnoita.catalog;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mcnoita.spell.action.SpellDefinition;
import com.mcnoita.spell.action.UseConsumptionPolicy;
import java.math.BigDecimal;
import java.util.Set;

/**
 * Parses the intentionally small G05 datapack override surface. Actions and
 * category stay frozen from the built-in definition until they have a complete,
 * versioned action codec; accepting partial action JSON would make a reload
 * silently change evaluator semantics without a validation contract.
 */
final class SpellCatalogOverrideDecoder {
    private static final Set<String> ALLOWED_FIELDS = Set.of(
        "id", "mana_cost", "recursive", "use_consumption_policy", "related_projectile"
    );

    private SpellCatalogOverrideDecoder() {
    }

    static String readId(JsonObject value, String source) {
        return requiredString(value, "id", source);
    }

    static SpellDefinition decode(JsonObject value, String source, SpellDefinition base) {
        if (value == null) {
            throw new IllegalArgumentException(source + " must be a JSON object");
        }
        for (String key : value.keySet()) {
            if (!ALLOWED_FIELDS.contains(key)) {
                throw new IllegalArgumentException(source + " contains unsupported field '" + key + "'");
            }
        }

        String id = readId(value, source);
        if (!base.id().equals(id)) {
            throw new IllegalArgumentException(source + " id does not match its built-in definition: " + id);
        }
        int manaCost = value.has("mana_cost") ? integer(value.get("mana_cost"), "mana_cost", source) : base.manaCost();
        boolean recursive = value.has("recursive") ? bool(value.get("recursive"), "recursive", source) : base.recursive();
        UseConsumptionPolicy usePolicy = value.has("use_consumption_policy")
            ? enumValue(value.get("use_consumption_policy"), UseConsumptionPolicy.class, "use_consumption_policy", source)
            : base.useConsumptionPolicy();
        String relatedProjectile = value.has("related_projectile")
            ? optionalString(value.get("related_projectile"), "related_projectile", source)
            : base.relatedProjectile();

        return new SpellDefinition(id, base.category(), manaCost, recursive, base.actions(), usePolicy, relatedProjectile);
    }

    private static String requiredString(JsonObject value, String field, String source) {
        if (!value.has(field)) {
            throw new IllegalArgumentException(source + " is missing required field '" + field + "'");
        }
        String result = optionalString(value.get(field), field, source);
        if (result.isBlank()) {
            throw new IllegalArgumentException(source + " field '" + field + "' must not be blank");
        }
        return result;
    }

    private static String optionalString(JsonElement value, String field, String source) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException(source + " field '" + field + "' must be a string");
        }
        String result = value.getAsString();
        if (result.length() > 256) {
            throw new IllegalArgumentException(source + " field '" + field + "' is too long");
        }
        return result;
    }

    private static int integer(JsonElement value, String field, String source) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(source + " field '" + field + "' must be an integer");
        }
        try {
            return new BigDecimal(value.getAsString()).intValueExact();
        } catch (ArithmeticException | NumberFormatException failure) {
            throw new IllegalArgumentException(source + " field '" + field + "' must be an integer", failure);
        }
    }

    private static boolean bool(JsonElement value, String field, String source) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException(source + " field '" + field + "' must be a boolean");
        }
        return value.getAsBoolean();
    }

    private static <E extends Enum<E>> E enumValue(
        JsonElement value, Class<E> type, String field, String source
    ) {
        String name = optionalString(value, field, source);
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException(source + " field '" + field + "' has invalid value '" + name + "'", failure);
        }
    }
}
