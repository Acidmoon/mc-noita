package com.mcnoita.spell.exec;

import com.mcnoita.spell.NoitaProjectilePayload;
import com.mcnoita.spell.plan.ProjectileEffectNode;
import com.mcnoita.spell.trigger.TriggerRuntimeBudget;
import java.util.List;
import net.minecraft.util.math.Vec3d;

/** Materializes root projectile nodes through the shared projectile dispatcher. */
final class ProjectileEffectNodeExecutor implements EffectNodeExecutor<ProjectileEffectNode> {
    @Override
    public Class<ProjectileEffectNode> nodeType() {
        return ProjectileEffectNode.class;
    }

    @Override
    public void execute(ProjectileEffectNode node, EffectExecutionContext context) {
        List<TriggerRuntimeBudget> childBudgets = context.requireRootBudgets(node);
        Vec3d direction = context.direction().rotateY((float) Math.toRadians(-node.projectile().spreadOffsetDegrees()));
        NoitaProjectilePayload payload = ProjectilePlanPayloadAdapter.payload(node.projectile(), context);
        ProjectileDispatcher.spawn(context.world(), context.player(), context.spawnPosition(), direction, payload, childBudgets, 0);
    }
}
