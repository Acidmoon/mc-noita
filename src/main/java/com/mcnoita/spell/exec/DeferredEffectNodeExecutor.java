package com.mcnoita.spell.exec;

import com.mcnoita.spell.plan.EffectNode;
import java.util.Objects;

/** Safe placeholder for nodes whose world-policy executor has not been implemented. */
final class DeferredEffectNodeExecutor<N extends EffectNode> implements EffectNodeExecutor<N> {
    private final Class<N> nodeType;

    DeferredEffectNodeExecutor(Class<N> nodeType) {
        this.nodeType = Objects.requireNonNull(nodeType, "nodeType");
    }

    @Override
    public Class<N> nodeType() {
        return nodeType;
    }

    @Override
    public void execute(N node, EffectExecutionContext context) {
        context.reportDeferred(node);
    }

    @Override
    public boolean isDeferred() {
        return true;
    }
}
