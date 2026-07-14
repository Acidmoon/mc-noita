package com.mcnoita.spell.exec;

import com.mcnoita.spell.plan.BlockMutationEffectNode;
import com.mcnoita.spell.plan.EffectNode;
import com.mcnoita.spell.plan.ExplosionEffectNode;
import com.mcnoita.spell.plan.FieldEffectNode;
import com.mcnoita.spell.plan.PersistentJobEffectNode;
import com.mcnoita.spell.plan.ProjectileEffectNode;
import com.mcnoita.spell.plan.RecoilEffectNode;
import com.mcnoita.spell.plan.SoundEffectNode;
import com.mcnoita.spell.plan.SummonEffectNode;
import com.mcnoita.spell.plan.TeleportEffectNode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Type-safe dispatch table that isolates one committed effect-node failure. */
public final class EffectExecutorRegistry {
    private final Map<Class<? extends EffectNode>, EffectNodeExecutor<? extends EffectNode>> executors = new LinkedHashMap<>();

    public static EffectExecutorRegistry createDefault() {
        EffectExecutorRegistry registry = new EffectExecutorRegistry();
        registry.register(new ProjectileEffectNodeExecutor());
        registry.register(new SoundEffectNodeExecutor());
        registry.register(new RecoilEffectNodeExecutor());
        registry.register(new DeferredEffectNodeExecutor<>(ExplosionEffectNode.class));
        registry.register(new DeferredEffectNodeExecutor<>(FieldEffectNode.class));
        registry.register(new DeferredEffectNodeExecutor<>(SummonEffectNode.class));
        registry.register(new DeferredEffectNodeExecutor<>(TeleportEffectNode.class));
        registry.register(new DeferredEffectNodeExecutor<>(BlockMutationEffectNode.class));
        registry.register(new DeferredEffectNodeExecutor<>(PersistentJobEffectNode.class));
        return registry;
    }

    public <N extends EffectNode> void register(EffectNodeExecutor<N> executor) {
        Objects.requireNonNull(executor, "executor");
        Class<N> nodeType = Objects.requireNonNull(executor.nodeType(), "executor node type");
        if (executors.putIfAbsent(nodeType, executor) != null) {
            throw new IllegalArgumentException("effect node executor already registered: " + nodeType.getName());
        }
    }

    public boolean supports(EffectNode node) {
        return node != null && executors.containsKey(node.getClass());
    }

    public void execute(EffectNode node, EffectExecutionContext context) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(context, "context");
        EffectNodeExecutor<? extends EffectNode> executor = executors.get(node.getClass());
        if (executor == null) {
            context.reportNodeFailure(node, "missing-executor", new IllegalStateException("no executor is registered"));
            context.releaseUnusedBudget(node, "missing-executor");
            return;
        }
        executeIsolated(executor, node, context);
    }

    @SuppressWarnings("unchecked")
    private static <N extends EffectNode> void executeIsolated(
        EffectNodeExecutor<? extends EffectNode> rawExecutor, EffectNode node, EffectExecutionContext context
    ) {
        try {
            EffectNodeExecutor<N> executor = (EffectNodeExecutor<N>) rawExecutor;
            executor.execute((N) node, context);
            if (executor.isDeferred()) {
                context.releaseUnusedBudget(node, "deferred");
            }
        } catch (RuntimeException failure) {
            context.reportNodeFailure(node, "executor", failure);
            context.releaseUnusedBudget(node, "executor");
        }
    }
}
