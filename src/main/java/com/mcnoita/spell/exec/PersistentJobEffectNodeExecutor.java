package com.mcnoita.spell.exec;

import com.mcnoita.spell.plan.PersistentJobEffectNode;
import com.mcnoita.spell.server.budget.ChunkBudgetKey;
import com.mcnoita.spell.server.job.SpellJobPersistentState;
import com.mcnoita.spell.server.job.SpellJobSink;
import java.util.Objects;

/**
 * Offers a frozen typed job to a server-owned sink. The default sink rejects
 * every job inertly until a world persistence service supplies real handlers;
 * no unsupported plan node can become live background work by accident.
 */
final class PersistentJobEffectNodeExecutor implements EffectNodeExecutor<PersistentJobEffectNode> {
    private final SpellJobSink jobSink;

    PersistentJobEffectNodeExecutor(SpellJobSink jobSink) {
        this.jobSink = Objects.requireNonNull(jobSink, "jobSink");
    }

    @Override
    public Class<PersistentJobEffectNode> nodeType() {
        return PersistentJobEffectNode.class;
    }

    @Override
    public void execute(PersistentJobEffectNode node, EffectExecutionContext context) {
        String dimensionId = context.world().getRegistryKey().getValue().toString();
        ChunkBudgetKey originChunk = new ChunkBudgetKey(dimensionId, context.player().getChunkPos().x,
            context.player().getChunkPos().z);
        SpellJobPersistentState job = SpellJobPersistentState.fromEffectNode(node, context.executionId(),
            context.player().getUuid(), dimensionId, originChunk, context.catalogEpoch(), context.catalogHash(),
            context.player().getServer().getTicks());
        SpellJobSink.Submission submission = jobSink.submit(job, context.reservation());
        if (!submission.accepted()) {
            context.reportNodeFailure(node, "persistent-job", new IllegalStateException(submission.reason()));
            context.releaseUnusedBudget(node, "persistent-job");
        }
    }
}
