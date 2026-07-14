package com.mcnoita.spell.server.budget;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Package-private arithmetic helpers that keep every quota map canonical. */
final class BudgetValues {
    private BudgetValues() {
    }

    static Map<BudgetKind, Long> copyCosts(Map<BudgetKind, Long> values, String name) {
        Objects.requireNonNull(values, name);
        EnumMap<BudgetKind, Long> copy = new EnumMap<>(BudgetKind.class);
        for (Map.Entry<BudgetKind, Long> entry : values.entrySet()) {
            BudgetKind kind = Objects.requireNonNull(entry.getKey(), name + " kind");
            Long value = Objects.requireNonNull(entry.getValue(), name + " value");
            if (value < 0L) {
                throw new IllegalArgumentException(name + " values must not be negative");
            }
            if (value > 0L) {
                copy.put(kind, value);
            }
        }
        return Collections.unmodifiableMap(copy);
    }

    /** Limits preserve explicit zero values; an omitted kind is unlimited. */
    static Map<BudgetKind, Long> copyLimits(Map<BudgetKind, Long> values, String name) {
        Objects.requireNonNull(values, name);
        EnumMap<BudgetKind, Long> copy = new EnumMap<>(BudgetKind.class);
        for (Map.Entry<BudgetKind, Long> entry : values.entrySet()) {
            BudgetKind kind = Objects.requireNonNull(entry.getKey(), name + " kind");
            Long value = Objects.requireNonNull(entry.getValue(), name + " value");
            if (value < 0L) {
                throw new IllegalArgumentException(name + " limits must not be negative");
            }
            copy.put(kind, value);
        }
        return Collections.unmodifiableMap(copy);
    }

    static long amount(Map<BudgetKind, Long> values, BudgetKind kind) {
        return values.getOrDefault(kind, 0L);
    }

    static boolean isEmpty(Map<BudgetKind, Long> values) {
        return values.isEmpty();
    }

    static boolean fitsWithin(Map<BudgetKind, Long> candidate, Map<BudgetKind, Long> capacity) {
        for (BudgetKind kind : BudgetKind.values()) {
            if (amount(candidate, kind) > amount(capacity, kind)) {
                return false;
            }
        }
        return true;
    }

    static Map<BudgetKind, Long> subtract(Map<BudgetKind, Long> left, Map<BudgetKind, Long> right) {
        EnumMap<BudgetKind, Long> result = new EnumMap<>(BudgetKind.class);
        for (BudgetKind kind : BudgetKind.values()) {
            long remaining = amount(left, kind) - amount(right, kind);
            if (remaining < 0L) {
                throw new IllegalArgumentException("budget release exceeds the reserved amount for " + kind);
            }
            if (remaining > 0L) {
                result.put(kind, remaining);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    static void addInto(EnumMap<BudgetKind, Long> target, Map<BudgetKind, Long> addition) {
        for (Map.Entry<BudgetKind, Long> entry : addition.entrySet()) {
            BudgetKind kind = entry.getKey();
            long current = target.getOrDefault(kind, 0L);
            long value = entry.getValue();
            if (current > Long.MAX_VALUE - value) {
                throw new IllegalStateException("budget accounting overflow for " + kind);
            }
            target.put(kind, current + value);
        }
    }

    static void subtractFrom(EnumMap<BudgetKind, Long> target, Map<BudgetKind, Long> subtraction) {
        for (Map.Entry<BudgetKind, Long> entry : subtraction.entrySet()) {
            BudgetKind kind = entry.getKey();
            long current = target.getOrDefault(kind, 0L);
            long next = current - entry.getValue();
            if (next < 0L) {
                throw new IllegalStateException("budget accounting underflow for " + kind);
            }
            if (next == 0L) {
                target.remove(kind);
            } else {
                target.put(kind, next);
            }
        }
    }

    static Map<BudgetKind, Long> snapshot(Map<BudgetKind, Long> values) {
        return copyCosts(values, "budget usage");
    }
}
