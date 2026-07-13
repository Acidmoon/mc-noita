package com.mcnoita.spell;

import java.util.Objects;

public record NoitaProjectileSpellSpec(
    String noitaId,
    String itemPath,
    String englishName,
    String chineseName,
    NoitaProjectileBehavior behavior,
    NoitaSpellTemplate template,
    int projectileCount,
    float burstSpreadDegrees,
    float gravity,
    float drag,
    float bounceDamping,
    float renderScale
) {
    public NoitaProjectileSpellSpec {
        Objects.requireNonNull(noitaId, "noitaId");
        Objects.requireNonNull(itemPath, "itemPath");
        Objects.requireNonNull(englishName, "englishName");
        Objects.requireNonNull(chineseName, "chineseName");
        Objects.requireNonNull(behavior, "behavior");
        Objects.requireNonNull(template, "template");
        if (projectileCount < 1) {
            throw new IllegalArgumentException("projectileCount must be at least 1");
        }
        if (burstSpreadDegrees < 0.0f) {
            throw new IllegalArgumentException("burstSpreadDegrees must not be negative");
        }
        if (gravity < 0.0f) {
            throw new IllegalArgumentException("gravity must not be negative");
        }
        if (drag < 0.0f) {
            throw new IllegalArgumentException("drag must not be negative");
        }
        if (bounceDamping < 0.0f) {
            throw new IllegalArgumentException("bounceDamping must not be negative");
        }
        if (renderScale <= 0.0f) {
            throw new IllegalArgumentException("renderScale must be positive");
        }
    }
}
