package com.mcnoita.spell;

public enum NoitaSpellTriggerMode {
    NONE,
    HIT,
    TIMER,
    EXPIRATION,
    /**
     * Legacy v0-v2 serialized spelling. New plans normalize this to
     * EXPIRATION while accepting it during migration so old worlds do not
     * silently become a different trigger type.
     */
    @Deprecated(forRemoval = false)
    DEATH;

    public boolean isExpiration() {
        return this == EXPIRATION || this == DEATH;
    }

    public static NoitaSpellTriggerMode fromPersisted(String value) {
        if ("DEATH".equals(value)) {
            return EXPIRATION;
        }
        return NoitaSpellTriggerMode.valueOf(value);
    }
}
