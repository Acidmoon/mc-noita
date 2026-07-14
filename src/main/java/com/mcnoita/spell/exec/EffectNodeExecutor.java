package com.mcnoita.spell.exec;

import com.mcnoita.spell.plan.EffectNode;

/** Server-side translator for one pure effect-node type. */
public interface EffectNodeExecutor<N extends EffectNode> {
    Class<N> nodeType();

    void execute(N node, EffectExecutionContext context);

    /** Deferred nodes are intentionally no-op world effects and release their reserved slice. */
    default boolean isDeferred() {
        return false;
    }
}
