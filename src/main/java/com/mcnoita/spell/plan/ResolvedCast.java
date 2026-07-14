package com.mcnoita.spell.plan;

import com.mcnoita.wand.model.CardRef;
import com.mcnoita.wand.model.NoitaDuration;
import com.mcnoita.wand.model.WandState;
import com.mcnoita.wand.eval.DrawOutcome;
import com.mcnoita.wand.eval.EvaluationTrace;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** The evaluator's only output; it deliberately has no Minecraft runtime references. */
public record ResolvedCast(
    Status status,
    WandState nextState,
    EffectPlan effectPlan,
    Map<CardRef, Integer> remainingUses,
    List<DrawOutcome> drawOutcomes,
    NoitaDuration castDelay,
    NoitaDuration rechargeTime,
    long randomSeed,
    long catalogEpoch,
    String catalogHash,
    BudgetUsage budgetUsage,
    List<CastDiagnostic> diagnostics,
    EvaluationTrace trace
) {
    public enum Status {
        ACCEPTED,
        REJECTED,
        TRUNCATED
    }

    public ResolvedCast {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(nextState, "nextState");
        Objects.requireNonNull(effectPlan, "effectPlan");
        Objects.requireNonNull(castDelay, "castDelay");
        Objects.requireNonNull(rechargeTime, "rechargeTime");
        Objects.requireNonNull(catalogHash, "catalogHash");
        Objects.requireNonNull(budgetUsage, "budgetUsage");
        Objects.requireNonNull(trace, "trace");
        remainingUses = Collections.unmodifiableMap(new LinkedHashMap<>(remainingUses));
        drawOutcomes = List.copyOf(drawOutcomes);
        diagnostics = List.copyOf(diagnostics);
    }
}
