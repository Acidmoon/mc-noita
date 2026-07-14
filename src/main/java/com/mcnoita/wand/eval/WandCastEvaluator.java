package com.mcnoita.wand.eval;

import com.mcnoita.spell.action.SpellCatalog;
import com.mcnoita.spell.plan.CastBudget;
import com.mcnoita.spell.plan.ResolvedCast;
import com.mcnoita.wand.model.NoitaDuration;
import com.mcnoita.wand.model.WandDefinition;
import com.mcnoita.wand.model.WandState;
import java.util.Objects;

/** Entry point for world-independent, deterministic wand evaluation. */
public final class WandCastEvaluator {
    private final CastBudget budget;

    public WandCastEvaluator() {
        this(CastBudget.DEFAULT);
    }

    public WandCastEvaluator(CastBudget budget) {
        this.budget = Objects.requireNonNull(budget, "budget");
    }

    public ResolvedCast evaluate(
        WandDefinition wand,
        WandState state,
        SpellCatalog catalog,
        NoitaDuration elapsed,
        long randomSeed
    ) {
        return evaluate(wand, state, catalog, elapsed, randomSeed, ExternalSpellPool.EMPTY);
    }

    public ResolvedCast evaluate(
        WandDefinition wand,
        WandState state,
        SpellCatalog catalog,
        NoitaDuration elapsed,
        long randomSeed,
        ExternalSpellPool externalSpellPool
    ) {
        Objects.requireNonNull(wand, "wand");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(elapsed, "elapsed");
        Objects.requireNonNull(externalSpellPool, "externalSpellPool");

        return new WandCastSession(wand, state, catalog, elapsed, new CastRng(randomSeed), budget,
            externalSpellPool).evaluate();
    }
}
