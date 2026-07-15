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
import com.mcnoita.spell.server.job.SpellJobSink;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Type-safe dispatch table that isolates one committed effect-node failure. */
public final class EffectExecutorRegistry {
    private final Map<Class<? extends EffectNode>, EffectNodeExecutor<? extends EffectNode>> executors = new LinkedHashMap<>();

    public static EffectExecutorRegistry createDefault() {
        return createDefault(SpellJobSink.rejecting());
    }

    /**
     * The server persistence layer can supply a handler-backed sink. The
     * default deliberately rejects jobs rather than keeping unimplemented work
     * alive after the cast transaction closes its root reservation.
     */
    public static EffectExecutorRegistry createDefault(SpellJobSink jobSink) {
        Objects.requireNonNull(jobSink, "jobSink");
        EffectExecutorRegistry registry = new EffectExecutorRegistry();
        registry.register(new ProjectileEffectNodeExecutor());
        registry.register(new SoundEffectNodeExecutor());
        registry.register(new RecoilEffectNodeExecutor());
        registry.register(new ExplosionEffectNodeExecutor());
        registry.register(new DeferredEffectNodeExecutor<>(FieldEffectNode.class));
        registry.register(new SummonEffectNodeExecutor());
        registry.register(new TeleportEffectNodeExecutor());
        registry.register(new BlockMutationEffectNodeExecutor());
        registry.register(new PersistentJobEffectNodeExecutor(jobSink));
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

    /** Exposes the deliberate no-op status so tests and diagnostics never mistake registration for completion. */
    public boolean isDeferred(EffectNode node) {
        if (node == null) {
            return false;
        }
        EffectNodeExecutor<? extends EffectNode> executor = executors.get(node.getClass());
        return executor != null && executor.isDeferred();
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
        executeIsolated(() -> {
            EffectNodeExecutor<N> executor = (EffectNodeExecutor<N>) rawExecutor;
            executor.execute((N) node, context);
            if (executor.isDeferred()) {
                context.releaseUnusedBudget(node, "deferred");
            }
        }, failure -> {
            context.reportNodeFailure(node, "executor", failure);
            context.releaseUnusedBudget(node, "executor");
        });
    }

    /** Package-visible pure isolation primitive used by the registry regression tests. */
    static boolean executeIsolated(Runnable action, java.util.function.Consumer<RuntimeException> onFailure) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(onFailure, "onFailure");
        try {
            action.run();
            return true;
        } catch (RuntimeException failure) {
            onFailure.accept(failure);
            return false;
        }
    }
}
