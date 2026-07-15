package com.mcnoita.spell.damage;

/**
 * Noita-facing direct-damage channels frozen into an EffectPlan. This is pure
 * domain data: Minecraft damage types are selected only by SpellDamageService.
 */
public enum DamageChannel {
    PROJECTILE("projectile"),
    EXPLOSION("explosion"),
    FIRE("fire"),
    ELECTRICITY("electricity"),
    DRILL("drill"),
    SLICE("slice"),
    ICE("ice"),
    TOXIC("toxic"),
    POISON("poison"),
    CURSE("curse"),
    HOLY("holy");

    private final String damageTypePath;

    DamageChannel(String damageTypePath) {
        this.damageTypePath = damageTypePath;
    }

    /** Stable data-pack path under {@code data/mc-noita/damage_type}. */
    public String damageTypePath() {
        return damageTypePath;
    }
}
