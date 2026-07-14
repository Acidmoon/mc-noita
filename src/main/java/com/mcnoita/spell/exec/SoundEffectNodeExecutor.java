package com.mcnoita.spell.exec;

import com.mcnoita.spell.plan.SoundEffectNode;
import com.mcnoita.spell.plan.SoundPlan;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

/** Executes the existing cast sounds through the typed node registry. */
final class SoundEffectNodeExecutor implements EffectNodeExecutor<SoundEffectNode> {
    @Override
    public Class<SoundEffectNode> nodeType() {
        return SoundEffectNode.class;
    }

    @Override
    public void execute(SoundEffectNode node, EffectExecutionContext context) {
        if (node.sound().kind() == SoundPlan.SoundKind.FUSED_EXPLOSIVE_CAST) {
            context.world().playSound(null, context.player().getX(), context.player().getY(), context.player().getZ(),
                SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.PLAYERS, 0.8f, 1.0f);
        } else {
            context.world().playSound(null, context.player().getX(), context.player().getY(), context.player().getZ(),
                SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 0.6f, 1.35f);
        }
    }
}
