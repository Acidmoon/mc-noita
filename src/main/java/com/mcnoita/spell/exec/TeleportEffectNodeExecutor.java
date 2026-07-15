package com.mcnoita.spell.exec;

import com.mcnoita.spell.plan.TeleportEffectNode;
import com.mcnoita.world.mutation.WorldMutationPolicy;
import com.mcnoita.world.mutation.WorldMutationService;
import net.minecraft.util.math.Vec3d;

/** Teleports only the bound owner to a loaded, collision-safe server-derived destination. */
final class TeleportEffectNodeExecutor implements EffectNodeExecutor<TeleportEffectNode> {
    private static final double MAX_TELEPORT_DISTANCE = WorldMutationPolicy.MAX_QUERY_DIAMETER;

    @Override
    public Class<TeleportEffectNode> nodeType() {
        return TeleportEffectNode.class;
    }

    @Override
    public void execute(TeleportEffectNode node, EffectExecutionContext context) {
        if (!node.requireSafeDestination()) {
            context.rejectNode(node, "unsafe teleport destinations are not permitted by the server executor");
            return;
        }
        if (node.maximumDistance() <= 0.0 || node.maximumDistance() > MAX_TELEPORT_DISTANCE) {
            context.rejectNode(node, "teleport distance is outside the policy-bounded range");
            return;
        }

        Vec3d destination = context.player().getPos().add(context.direction().normalize().multiply(node.maximumDistance()));
        if (!WorldMutationService.teleportOwner(context.worldMutationContext(node), context.player(), destination)) {
            context.rejectNode(node, "no loaded collision-safe teleport destination was available");
        }
    }
}
