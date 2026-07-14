package com.mcnoita.spell.exec;

import com.mcnoita.spell.plan.RecoilEffectNode;
import net.minecraft.util.math.Vec3d;

/** Applies the existing bounded recoil conversion through the typed registry. */
final class RecoilEffectNodeExecutor implements EffectNodeExecutor<RecoilEffectNode> {
    @Override
    public Class<RecoilEffectNode> nodeType() {
        return RecoilEffectNode.class;
    }

    @Override
    public void execute(RecoilEffectNode node, EffectExecutionContext context) {
        Vec3d direction = context.direction();
        double recoil = node.recoil().strength();
        if (recoil == 0.0 || direction.lengthSquared() <= 1.0E-6) {
            return;
        }
        double strength = Math.max(-1.2, Math.min(1.2, recoil / 400.0));
        Vec3d impulse = direction.normalize().multiply(-strength);
        context.player().addVelocity(impulse.x, impulse.y * 0.45, impulse.z);
        context.player().velocityModified = true;
    }
}
