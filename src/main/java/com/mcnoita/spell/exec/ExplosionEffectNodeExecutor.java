package com.mcnoita.spell.exec;

import com.mcnoita.spell.plan.ExplosionEffectNode;
import com.mcnoita.spell.damage.DamageChannel;
import com.mcnoita.spell.damage.DamageProfile;
import com.mcnoita.world.mutation.WorldMutationPolicy;
import com.mcnoita.world.mutation.WorldMutationService;

/**
 * Executes the bounded Minecraft-equivalent explosion path. The plan's scalar
 * damage is applied through a bounded entity collection with the player as
 * both direct source and owner attribution; vanilla's unbounded entity phase
 * is never invoked.
 */
final class ExplosionEffectNodeExecutor implements EffectNodeExecutor<ExplosionEffectNode> {
    @Override
    public Class<ExplosionEffectNode> nodeType() {
        return ExplosionEffectNode.class;
    }

    @Override
    public void execute(ExplosionEffectNode node, EffectExecutionContext context) {
        if (node.radius() <= 0.0 || node.radius() > WorldMutationPolicy.MAX_EXPLOSION_RADIUS) {
            context.rejectNode(node, "explosion radius is outside the policy-bounded range");
            return;
        }

        boolean executed = WorldMutationService.explode(context.worldMutationContext(node), context.player(), context.player(),
            context.spawnPosition(), (float) node.radius(), DamageProfile.of(DamageChannel.EXPLOSION, node.damage()), false,
            false, node.terrainRequested());
        if (!executed) {
            context.rejectNode(node, "world mutation policy rejected the explosion");
        }
    }
}
