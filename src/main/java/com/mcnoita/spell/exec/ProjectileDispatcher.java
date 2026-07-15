package com.mcnoita.spell.exec;

import com.mcnoita.entity.SparkBoltProjectileEntity;
import com.mcnoita.spell.NoitaProjectilePayload;
import com.mcnoita.spell.trigger.TriggerRuntimeBudget;
import com.mcnoita.world.mutation.WorldMutationBudget;
import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Shared authoritative spawn gateway for root plans and frozen Trigger releases. */
public final class ProjectileDispatcher {
    private ProjectileDispatcher() {
    }

    public static void spawn(
        World world, LivingEntity owner, Vec3d position, Vec3d direction, NoitaProjectilePayload payload,
        List<TriggerRuntimeBudget> childBudgets, int releaseSequence
    ) {
        SparkBoltProjectileEntity.spawnPayloadProjectile(world, owner, position, direction, payload, childBudgets, releaseSequence);
    }

    /** Root nodes pass their committed local entity slice; delayed payloads pass null and re-reserve centrally. */
    public static void spawn(
        World world, LivingEntity owner, Vec3d position, Vec3d direction, NoitaProjectilePayload payload,
        List<TriggerRuntimeBudget> childBudgets, int releaseSequence, WorldMutationBudget spawnBudget
    ) {
        SparkBoltProjectileEntity.spawnPayloadProjectile(world, owner, position, direction, payload, childBudgets,
            releaseSequence, spawnBudget);
    }
}
