package com.mcnoita.persistence;

/**
 * Conservative decode limits for user-controlled ItemStack and entity NBT.
 * They prevent malformed saves from turning recursive payload parsing into a
 * memory or stack exhaustion path before the evaluator budget layer exists.
 */
public final class NoitaNbtLimits {
    public static final int MAX_WAND_CAPACITY = 64;
    public static final int MAX_ALWAYS_CAST_SPELLS = 16;
    public static final int MAX_WAND_SLOT_ENTRIES = 64;
    public static final int MAX_CAST_STATE_SLOTS = 64;
    public static final int MAX_PAYLOAD_DEPTH = 16;
    public static final int MAX_PAYLOAD_NODES = 128;
    public static final int MAX_PAYLOAD_CHILDREN = 32;
    public static final int MAX_MODIFIER_EFFECTS = 64;
    public static final int MAX_PROJECTILE_COUNT = 128;
    public static final int MAX_PROJECTILE_LIFETIME_TICKS = 72_000;
    public static final int MAX_TRIGGER_DELAY_TICKS = 72_000;
    public static final int MAX_STRING_LENGTH = 128;
    public static final int MAX_MANA_VALUE = 1_000_000;
    public static final int MAX_ENTITY_NBT_DEPTH = 32;
    public static final int MAX_ENTITY_NBT_NODES = 2048;

    private NoitaNbtLimits() {
    }
}
