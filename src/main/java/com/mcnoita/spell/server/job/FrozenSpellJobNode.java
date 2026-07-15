package com.mcnoita.spell.server.job;

import com.mcnoita.persistence.NoitaNbtLimits;
import com.mcnoita.spell.plan.PersistentJobEffectNode;
import com.mcnoita.spell.server.budget.BudgetKind;
import com.mcnoita.spell.server.budget.BudgetLimits;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Frozen, Minecraft-independent job mechanics. Handlers receive this value
 * instead of resolving a catalog action again after the original cast commits.
 */
public record FrozenSpellJobNode(
    String nodePath,
    String jobType,
    int maximumSteps,
    boolean recoveryIdempotent,
    Map<BudgetKind, Long> perStepBudget,
    Map<String, String> parameters
) {
    public FrozenSpellJobNode {
        nodePath = requireBoundedNonBlank(nodePath, "nodePath");
        jobType = requireBoundedNonBlank(jobType, "jobType");
        if (maximumSteps < 1 || maximumSteps > NoitaNbtLimits.MAX_SPELL_JOB_STEPS) {
            throw new IllegalArgumentException("maximumSteps is outside the persisted job limit");
        }
        perStepBudget = copyBudget(perStepBudget, "perStepBudget", true);
        if (perStepBudget.getOrDefault(BudgetKind.CROSS_TICK_JOB_STEPS, 0L) != 1L) {
            // Every attempted handler call accounts for exactly one cross-tick
            // step, so a handler cannot bypass the central time-window ledger.
            throw new IllegalArgumentException("perStepBudget must reserve one CROSS_TICK_JOB_STEPS unit");
        }
        if (perStepBudget.containsKey(BudgetKind.PERSISTENT_JOBS)) {
            throw new IllegalArgumentException("PERSISTENT_JOBS is a record-lifetime cost, not a per-step cost");
        }
        parameters = copyParameters(parameters);
        // Validate the full lifetime here, rather than allowing a corrupt
        // late-cursor record to hide an originally over-limit job definition.
        for (Map.Entry<BudgetKind, Long> entry : perStepBudget.entrySet()) {
            multiplyBounded(entry.getKey(), entry.getValue(), maximumSteps);
        }
    }

    /** Copies the plan's explicit recovery declaration into durable mechanics. */
    public static FrozenSpellJobNode fromEffectNode(PersistentJobEffectNode node) {
        Objects.requireNonNull(node, "node");
        return new FrozenSpellJobNode(node.nodePath(), node.jobType(), node.maximumSteps(), node.recoveryIdempotent(),
            Map.of(BudgetKind.CROSS_TICK_JOB_STEPS, 1L), Map.of());
    }

    /** Initial hard budget is finite and never grows after a job is frozen. */
    public Map<BudgetKind, Long> initialRemainingHardBudget() {
        return remainingHardBudgetAfterSteps(0);
    }

    /** The persisted cursor is the only authority that may reduce a frozen job budget. */
    public Map<BudgetKind, Long> remainingHardBudgetAfterSteps(int consumedSteps) {
        if (consumedSteps < 0 || consumedSteps > maximumSteps) {
            throw new IllegalArgumentException("consumedSteps is outside the frozen job step range");
        }
        EnumMap<BudgetKind, Long> total = new EnumMap<>(BudgetKind.class);
        for (Map.Entry<BudgetKind, Long> entry : perStepBudget.entrySet()) {
            long amount = multiplyBounded(entry.getKey(), entry.getValue(), maximumSteps - consumedSteps);
            if (amount > 0L) {
                total.put(entry.getKey(), amount);
            }
        }
        return Map.copyOf(total);
    }

    static String requireBoundedNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank() || value.length() > NoitaNbtLimits.MAX_STRING_LENGTH) {
            throw new IllegalArgumentException(name + " must be nonblank and bounded");
        }
        return value;
    }

    static String requireBoundedText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.length() > NoitaNbtLimits.MAX_STRING_LENGTH) {
            throw new IllegalArgumentException(name + " exceeds the persisted string limit");
        }
        return value;
    }

    static Map<BudgetKind, Long> copyBudget(Map<BudgetKind, Long> source, String name, boolean requireStep) {
        Objects.requireNonNull(source, name);
        if (source.size() > BudgetKind.values().length) {
            throw new IllegalArgumentException(name + " has too many budget kinds");
        }
        EnumMap<BudgetKind, Long> copied = new EnumMap<>(BudgetKind.class);
        for (Map.Entry<BudgetKind, Long> entry : source.entrySet()) {
            BudgetKind kind = Objects.requireNonNull(entry.getKey(), name + " key");
            Long boxedAmount = Objects.requireNonNull(entry.getValue(), name + " value");
            long amount = boxedAmount;
            long maximum = BudgetLimits.HARD_MAXIMUMS.get(kind);
            if (amount < 1L || amount > maximum) {
                throw new IllegalArgumentException(name + " is outside the hard limit for " + kind);
            }
            copied.put(kind, amount);
        }
        if (requireStep && copied.isEmpty()) {
            throw new IllegalArgumentException(name + " must contain at least one cost");
        }
        return Map.copyOf(copied);
    }

    private static Map<String, String> copyParameters(Map<String, String> source) {
        Objects.requireNonNull(source, "parameters");
        if (source.size() > NoitaNbtLimits.MAX_SPELL_JOB_PARAMETERS) {
            throw new IllegalArgumentException("too many frozen job parameters");
        }
        Map<String, String> ordered = new TreeMap<>();
        for (Map.Entry<String, String> entry : source.entrySet()) {
            String key = requireBoundedNonBlank(entry.getKey(), "parameter key");
            String value = requireBoundedText(entry.getValue(), "parameter value");
            if (ordered.put(key, value) != null) {
                throw new IllegalArgumentException("duplicate frozen job parameter " + key);
            }
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(ordered));
    }

    private static long multiplyBounded(BudgetKind kind, long perStep, int maximumSteps) {
        if (maximumSteps == 0) {
            return 0L;
        }
        long maximum = BudgetLimits.HARD_MAXIMUMS.get(kind);
        if (perStep > maximum / maximumSteps) {
            throw new IllegalArgumentException("job lifetime budget exceeds the hard limit for " + kind);
        }
        return perStep * maximumSteps;
    }
}
