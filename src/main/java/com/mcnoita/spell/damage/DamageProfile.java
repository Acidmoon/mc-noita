package com.mcnoita.spell.damage;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable multi-channel direct damage for one frozen projectile or effect.
 * Healing is deliberately absent: it is a separate server-side operation and
 * must never be represented as a negative DamageSource amount.
 */
public record DamageProfile(Map<DamageChannel, Double> amounts) {
    public static final DamageProfile EMPTY = new DamageProfile(Map.of());

    public DamageProfile {
        Objects.requireNonNull(amounts, "amounts");
        EnumMap<DamageChannel, Double> normalized = new EnumMap<>(DamageChannel.class);
        for (Map.Entry<DamageChannel, Double> entry : amounts.entrySet()) {
            DamageChannel channel = Objects.requireNonNull(entry.getKey(), "damage channel");
            Double value = Objects.requireNonNull(entry.getValue(), "damage amount");
            if (!Double.isFinite(value) || value < 0.0) {
                throw new IllegalArgumentException("damage amounts must be finite and non-negative");
            }
            if (value > 0.0) {
                normalized.put(channel, value);
            }
        }
        amounts = Map.copyOf(normalized);
    }

    /** All historical scalar spell damage remains projectile damage by default. */
    public static DamageProfile legacyProjectile(double amount) {
        return amount <= 0.0 ? EMPTY : new DamageProfile(Map.of(DamageChannel.PROJECTILE, amount));
    }

    public static DamageProfile of(DamageChannel channel, double amount) {
        return amount <= 0.0 ? EMPTY : new DamageProfile(Map.of(Objects.requireNonNull(channel, "channel"), amount));
    }

    public double amount(DamageChannel channel) {
        return amounts.getOrDefault(Objects.requireNonNull(channel, "channel"), 0.0);
    }

    /** Compatibility projection for pre-G05 scalar projectile consumers. */
    public double projectileDamage() {
        return amount(DamageChannel.PROJECTILE);
    }

    /**
     * Replaces the legacy scalar projection without discarding explicitly
     * frozen elemental channels. Runtime modifiers such as ESSENCE_TO_POWER
     * still update that scalar projection while direct hits read this profile.
     */
    public DamageProfile withProjectileDamage(double amount) {
        return withAmount(DamageChannel.PROJECTILE, amount);
    }

    public double totalDamage() {
        double total = 0.0;
        // Map.copyOf does not promise iteration order. Channel order is part of
        // deterministic evaluator output, including the observable total.
        for (DamageChannel channel : DamageChannel.values()) {
            total += amount(channel);
        }
        return total;
    }

    public boolean isEmpty() {
        return amounts.isEmpty();
    }

    public DamageProfile plus(DamageProfile other) {
        Objects.requireNonNull(other, "other");
        if (isEmpty()) {
            return other;
        }
        if (other.isEmpty()) {
            return this;
        }
        EnumMap<DamageChannel, Double> merged = new EnumMap<>(DamageChannel.class);
        merged.putAll(amounts);
        for (Map.Entry<DamageChannel, Double> entry : other.amounts.entrySet()) {
            merged.merge(entry.getKey(), entry.getValue(), Double::sum);
        }
        return new DamageProfile(merged);
    }

    /**
     * Replaces one channel without exposing a mutable map to runtime damage
     * rollers. A zero amount removes the channel so empty profiles retain one
     * canonical representation.
     */
    public DamageProfile withAmount(DamageChannel channel, double amount) {
        Objects.requireNonNull(channel, "channel");
        if (!Double.isFinite(amount) || amount < 0.0) {
            throw new IllegalArgumentException("damage amount must be finite and non-negative");
        }
        EnumMap<DamageChannel, Double> replaced = new EnumMap<>(DamageChannel.class);
        replaced.putAll(amounts);
        if (amount == 0.0) {
            replaced.remove(channel);
        } else {
            replaced.put(channel, amount);
        }
        return replaced.isEmpty() ? EMPTY : new DamageProfile(replaced);
    }

    public DamageProfile scale(double multiplier) {
        if (!Double.isFinite(multiplier) || multiplier < 0.0) {
            throw new IllegalArgumentException("damage multiplier must be finite and non-negative");
        }
        if (multiplier == 0.0 || isEmpty()) {
            return EMPTY;
        }
        if (multiplier == 1.0) {
            return this;
        }
        EnumMap<DamageChannel, Double> scaled = new EnumMap<>(DamageChannel.class);
        for (Map.Entry<DamageChannel, Double> entry : amounts.entrySet()) {
            double amount = entry.getValue() * multiplier;
            if (!Double.isFinite(amount)) {
                throw new IllegalArgumentException("scaled damage overflows a finite profile");
            }
            scaled.put(entry.getKey(), amount);
        }
        return new DamageProfile(scaled);
    }
}
