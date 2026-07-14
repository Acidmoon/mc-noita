package com.mcnoita.spell.trigger;

import com.mcnoita.MCNoita;
import com.mcnoita.entity.SparkBoltProjectileEntity;
import com.mcnoita.spell.NoitaProjectilePayload;
import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Server execution adapter for an already-committed ReleaseDecision. Individual
 * child failures are isolated: the controller must not retry the whole event
 * because successful siblings would otherwise be duplicated.
 */
public final class TriggerPayloadSpawner {
    private TriggerPayloadSpawner() {
    }

    public static void spawn(World world, LivingEntity owner, Vec3d position, Vec3d direction, ReleaseDecision decision) {
        if (!decision.shouldRelease()) {
            return;
        }
        for (PayloadRelease payloadRelease : decision.payloads()) {
            int budgetIndex = 0;
            for (NoitaProjectilePayload payload : payloadRelease.payload().projectiles()) {
                int entityCount = payload.projectileCount();
                List<TriggerRuntimeBudget> childBudgets = payloadRelease.childBudgets().subList(budgetIndex,
                    budgetIndex + entityCount);
                budgetIndex += entityCount;
                try {
                    SparkBoltProjectileEntity.spawnPayloadProjectile(world, owner, position, direction, payload, childBudgets,
                        decision.releaseSequence());
                } catch (RuntimeException failure) {
                    MCNoita.LOGGER.warn("Frozen trigger payload {} release {} failed after state commit", decision.nodePath(),
                        decision.releaseSequence(), failure);
                }
            }
        }
    }
}
