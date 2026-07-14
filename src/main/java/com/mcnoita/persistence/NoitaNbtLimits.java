package com.mcnoita.persistence;

import com.mcnoita.spell.plan.ProjectilePlan;

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
    public static final int MAX_PAYLOAD_NBT_DEPTH = 128;
    public static final int MAX_PAYLOAD_NODES = 128;
    public static final int MAX_PAYLOAD_NBT_NODES = 8_192;
    public static final int MAX_PAYLOAD_CHILDREN = 32;
    /** Must match the pure plan cap so accepted casts remain persistable. */
    public static final int MAX_MODIFIER_EFFECTS = ProjectilePlan.MAX_MODIFIER_EFFECTS;
    /** Structural traversal must admit the largest individually validated NBT list. */
    public static final int MAX_NBT_LIST_ENTRIES = Math.max(MAX_PAYLOAD_CHILDREN, MAX_MODIFIER_EFFECTS);
    public static final int MAX_PROJECTILE_COUNT = 128;
    public static final int MAX_PROJECTILE_LIFETIME_TICKS = 72_000;
    public static final int MAX_TRIGGER_DELAY_TICKS = 72_000;
    // These are execution-safety limits, not balance values. They keep a
    // malformed persisted payload from turning into an unbounded explosion,
    // lighting loop, physics loop, or damage value after a world reload.
    public static final float MAX_ABSOLUTE_PROJECTILE_DAMAGE = 100_000.0f;
    public static final float MAX_CRITICAL_CHANCE_PERCENT = 10_000.0f;
    public static final float MAX_EXPLOSION_RADIUS = 32.0f;
    public static final float MAX_PROJECTILE_SPEED = 16.0f;
    public static final float MAX_DIVERGENCE_DEGREES = 360.0f;
    public static final float MAX_ABSOLUTE_GRAVITY = 16.0f;
    public static final float MAX_DRAG = 4.0f;
    public static final float MAX_BOUNCE_DAMPING = 4.0f;
    public static final float MAX_RENDER_SCALE = 16.0f;
    public static final float MAX_ABSOLUTE_KNOCKBACK_FORCE = 64.0f;
    public static final int MAX_TRAIL_LIGHT_STACKS = 16;
    public static final int MAX_BOUNCE_COUNT = 32;
    // Node paths gain one bounded segment per nested payload/release. 512 keeps
    // a depth-16 runtime tree readable while still rejecting pathological NBT.
    public static final int MAX_STRING_LENGTH = 512;
    public static final int MAX_MANA_VALUE = 1_000_000;
    // A semantic depth-16 Trigger tree contains several compound/list layers
    // per level, so entity wrapping must not reject the valid payload tree.
    public static final int MAX_ENTITY_NBT_DEPTH = MAX_PAYLOAD_NBT_DEPTH + 8;
    public static final int MAX_ENTITY_NBT_NODES = 16_384;
    // A semantic depth-16 tree serializes its frozen mechanics at each level;
    // the validated worst-case fixture is about 89 KiB before entity wrapping.
    public static final int MAX_PAYLOAD_NBT_BYTES = 131_072;
    public static final int MAX_ENTITY_NBT_BYTES = 262_144;

    private NoitaNbtLimits() {
    }
}
