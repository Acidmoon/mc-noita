package com.mcnoita.spell.server.budget;

import com.mcnoita.spell.trigger.TriggerRuntimeBudget;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configurable caps with non-bypassable hard ceilings. Missing entries inherit
 * the corresponding hard maximum, while an explicit zero disables that resource
 * for the corresponding scope and period.
 */
public record BudgetLimits(
    Map<BudgetKind, Long> perCast,
    ScopeLimits owner,
    ScopeLimits chunk,
    ScopeLimits dimension,
    ScopeLimits global,
    long windowTicks
) {
    /** Absolute configuration ceiling for each resource, independent of scope. */
    public static final Map<BudgetKind, Long> HARD_MAXIMUMS = Map.ofEntries(
        Map.entry(BudgetKind.ACTION_NODES, 8_192L),
        Map.entry(BudgetKind.LOGICAL_PROJECTILES, 512L),
        Map.entry(BudgetKind.AUTHORITATIVE_ENTITIES, (long) TriggerRuntimeBudget.HARD_MAXIMUM),
        Map.entry(BudgetKind.TRIGGER_RELEASES, (long) TriggerRuntimeBudget.HARD_MAXIMUM),
        Map.entry(BudgetKind.ENTITY_SCANS, 16_384L),
        Map.entry(BudgetKind.BLOCK_CHECKS, 16_384L),
        Map.entry(BudgetKind.BLOCK_MUTATIONS, 2_048L),
        Map.entry(BudgetKind.NBT_BYTES, 262_144L),
        Map.entry(BudgetKind.NBT_NODES, 16_384L),
        Map.entry(BudgetKind.NETWORK_PACKETS, 64L),
        Map.entry(BudgetKind.NETWORK_BYTES, 262_144L),
        Map.entry(BudgetKind.VISUAL_EVENTS, 1_024L),
        Map.entry(BudgetKind.CROSS_TICK_JOB_STEPS, 4_096L),
        Map.entry(BudgetKind.PERSISTENT_JOBS, 64L)
    );

    private static final Map<BudgetKind, Long> DEFAULT_PER_CAST = Map.ofEntries(
        Map.entry(BudgetKind.ACTION_NODES, 2_048L),
        Map.entry(BudgetKind.LOGICAL_PROJECTILES, 128L),
        Map.entry(BudgetKind.AUTHORITATIVE_ENTITIES, 32L),
        Map.entry(BudgetKind.TRIGGER_RELEASES, 32L),
        Map.entry(BudgetKind.ENTITY_SCANS, 4_096L),
        Map.entry(BudgetKind.BLOCK_CHECKS, 4_096L),
        Map.entry(BudgetKind.BLOCK_MUTATIONS, 512L),
        Map.entry(BudgetKind.NBT_BYTES, 131_072L),
        Map.entry(BudgetKind.NBT_NODES, 8_192L),
        Map.entry(BudgetKind.NETWORK_PACKETS, 16L),
        Map.entry(BudgetKind.NETWORK_BYTES, 65_536L),
        Map.entry(BudgetKind.VISUAL_EVENTS, 256L),
        Map.entry(BudgetKind.CROSS_TICK_JOB_STEPS, 512L),
        Map.entry(BudgetKind.PERSISTENT_JOBS, 8L)
    );

    /** Conservative server baseline with a one-second (20 tick) rate window. */
    public static final BudgetLimits DEFAULT = createDefault();

    public BudgetLimits {
        perCast = BudgetValues.copyLimits(perCast, "perCast");
        requireWithinHardMaximums(perCast, "perCast");
        owner = Objects.requireNonNull(owner, "owner");
        chunk = Objects.requireNonNull(chunk, "chunk");
        dimension = Objects.requireNonNull(dimension, "dimension");
        global = Objects.requireNonNull(global, "global");
        if (windowTicks < 1L) {
            throw new IllegalArgumentException("windowTicks must be positive");
        }
    }

    /** Returns the configured value or the non-bypassable hard maximum. */
    static long effectiveLimit(Map<BudgetKind, Long> configured, BudgetKind kind) {
        return configured.getOrDefault(kind, HARD_MAXIMUMS.get(kind));
    }

    public static BudgetLimits unlimited() {
        ScopeLimits maximum = ScopeLimits.unlimited();
        return new BudgetLimits(HARD_MAXIMUMS, maximum, maximum, maximum, maximum, 1L);
    }

    private static BudgetLimits createDefault() {
        return new BudgetLimits(
            DEFAULT_PER_CAST,
            new ScopeLimits(scaleDefaults(2L), DEFAULT_PER_CAST, scaleDefaults(8L)),
            new ScopeLimits(scaleDefaults(4L), scaleDefaults(2L), scaleDefaults(16L)),
            new ScopeLimits(scaleDefaults(8L), scaleDefaults(4L), scaleDefaults(16L)),
            new ScopeLimits(scaleDefaults(16L), scaleDefaults(4L), scaleDefaults(16L)),
            20L
        );
    }

    private static Map<BudgetKind, Long> scaleDefaults(long factor) {
        EnumMap<BudgetKind, Long> scaled = new EnumMap<>(BudgetKind.class);
        for (BudgetKind kind : BudgetKind.values()) {
            long base = DEFAULT_PER_CAST.get(kind);
            long hardMaximum = HARD_MAXIMUMS.get(kind);
            scaled.put(kind, base > hardMaximum / factor ? hardMaximum : base * factor);
        }
        return Map.copyOf(scaled);
    }

    static void requireWithinHardMaximums(Map<BudgetKind, Long> values, String name) {
        for (Map.Entry<BudgetKind, Long> entry : values.entrySet()) {
            long hardMaximum = HARD_MAXIMUMS.get(entry.getKey());
            if (entry.getValue() > hardMaximum) {
                throw new IllegalArgumentException(name + " exceeds the hard maximum for " + entry.getKey());
            }
        }
    }

    public record ScopeLimits(
        Map<BudgetKind, Long> inFlight,
        Map<BudgetKind, Long> perTick,
        Map<BudgetKind, Long> perWindow
    ) {
        public ScopeLimits {
            inFlight = BudgetValues.copyLimits(inFlight, "inFlight");
            perTick = BudgetValues.copyLimits(perTick, "perTick");
            perWindow = BudgetValues.copyLimits(perWindow, "perWindow");
            requireWithinHardMaximums(inFlight, "inFlight");
            requireWithinHardMaximums(perTick, "perTick");
            requireWithinHardMaximums(perWindow, "perWindow");
        }

        public static ScopeLimits unlimited() {
            return new ScopeLimits(Map.of(), Map.of(), Map.of());
        }
    }
}
