package com.mcnoita.spell.exec;

import com.mcnoita.spell.plan.SummonEffectNode;

/**
 * Explicitly rejects generic summons until the persistent-job subsystem owns
 * lifetime expiry, reload recovery, and an entity allowlist. Spawning a generic
 * registry type here would leave durable mobs without those guarantees.
 */
final class SummonEffectNodeExecutor implements EffectNodeExecutor<SummonEffectNode> {
    @Override
    public Class<SummonEffectNode> nodeType() {
        return SummonEffectNode.class;
    }

    @Override
    public void execute(SummonEffectNode node, EffectExecutionContext context) {
        context.reportDeferred(node, "summon lifetime and allowlist policy are not available for generic entity ids");
    }

    @Override
    public boolean isDeferred() {
        return true;
    }
}
