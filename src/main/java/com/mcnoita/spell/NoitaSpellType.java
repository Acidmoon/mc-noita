package com.mcnoita.spell;

public enum NoitaSpellType {
    PROJECTILE("projectile"),
    STATIC_PROJECTILE("static_projectile"),
    PROJECTILE_MODIFIER("projectile_modifier"),
    MULTICAST("multicast"),
    MATERIAL("material"),
    OTHER("other"),
    UTILITY("utility"),
    PASSIVE("passive");

    private final String translationKey;

    NoitaSpellType(String translationKey) {
        this.translationKey = "spell_type.mc-noita." + translationKey;
    }

    public String getTranslationKey() {
        return translationKey;
    }
}
