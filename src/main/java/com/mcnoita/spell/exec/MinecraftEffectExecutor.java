package com.mcnoita.spell.exec;

import com.mcnoita.entity.SparkBoltProjectileEntity;
import com.mcnoita.MCNoita;
import com.mcnoita.spell.NoitaModifierEffect;
import com.mcnoita.spell.NoitaProjectileBehavior;
import com.mcnoita.spell.NoitaProjectilePayload;
import com.mcnoita.spell.NoitaSpellTriggerMode;
import com.mcnoita.spell.plan.EffectPlan;
import com.mcnoita.spell.plan.ProjectilePlan;
import com.mcnoita.spell.plan.RecoilPlan;
import com.mcnoita.spell.plan.SoundPlan;
import com.mcnoita.spell.plan.TriggerMode;
import com.mcnoita.wand.adapter.MinecraftTimeAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/** Executes frozen EffectPlan nodes after WandState was committed by the server. */
public final class MinecraftEffectExecutor {
    private static final double SPELL_SPAWN_FORWARD_OFFSET = 0.65;
    private static final double SPELL_SPAWN_RIGHT_OFFSET = 0.35;
    private static final double SPELL_SPAWN_DOWN_OFFSET = 0.25;
    private static final double NOITA_SPEED_TO_ARROW_SPEED = 300.0;
    private static final long FAILURE_LOG_INTERVAL_TICKS = 200L;
    private static final Map<String, Long> LAST_FAILURE_LOG_TICK = new HashMap<>();

    private MinecraftEffectExecutor() {
    }

    public static void execute(ServerPlayerEntity player, EffectPlan plan) {
        ServerWorld world = player.getServerWorld();
        Vec3d spawn = spellSpawnPosition(player);
        Vec3d direction = player.getRotationVec(1.0f);
        for (ProjectilePlan projectile : plan.projectiles()) {
            try {
                Vec3d resolvedDirection = direction.rotateY((float) Math.toRadians(-projectile.spreadOffsetDegrees()));
                SparkBoltProjectileEntity.spawnPayloadProjectile(world, player, spawn, resolvedDirection, payload(projectile));
            } catch (RuntimeException failure) {
                logFailure(world.getTime(), "projectile:" + projectile.itemPath(), player, failure);
            }
        }
        for (RecoilPlan recoil : plan.recoils()) {
            try {
                applyRecoil(player, direction, recoil.strength());
            } catch (RuntimeException failure) {
                logFailure(world.getTime(), "recoil", player, failure);
            }
        }
        for (SoundPlan sound : plan.sounds()) {
            try {
                if (sound.kind() == SoundPlan.SoundKind.FUSED_EXPLOSIVE_CAST) {
                    world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.PLAYERS, 0.8f, 1.0f);
                } else {
                    world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 0.6f, 1.35f);
                }
            } catch (RuntimeException failure) {
                logFailure(world.getTime(), "sound:" + sound.kind(), player, failure);
            }
        }
    }

    private static void logFailure(long worldTime, String node, ServerPlayerEntity player, RuntimeException failure) {
        Long previous = LAST_FAILURE_LOG_TICK.get(node);
        if (previous != null && worldTime - previous < FAILURE_LOG_INTERVAL_TICKS) {
            return;
        }
        LAST_FAILURE_LOG_TICK.put(node, worldTime);
        MCNoita.LOGGER.warn("EffectPlan {} failed for {} after WandState commit", node, player.getName().getString(), failure);
    }

    private static NoitaProjectilePayload payload(ProjectilePlan plan) {
        return new NoitaProjectilePayload(
            plan.itemPath(), behavior(plan.behavior()), (float) plan.damage(), (float) plan.criticalChancePercent(),
            MinecraftTimeAdapter.toMinecraftTicks(plan.lifetime(), 1), plan.trailLightStacks(), (float) plan.explosionRadius(),
            (float) Math.max(0.2, Math.min(8.0, plan.speed() / NOITA_SPEED_TO_ARROW_SPEED)), 0.0f, (float) plan.gravity(),
            (float) plan.drag(), (float) plan.bounceDamping(), (float) plan.renderScale(), (float) plan.knockbackForce(),
            plan.friendlyFire(), plan.piercing(), plan.projectileCount(), (float) plan.burstSpreadDegrees(), triggerMode(plan.triggerMode()),
            MinecraftTimeAdapter.toMinecraftTicks(plan.triggerDelay(), 0), plan.bounceCount(), effects(plan.effects()),
            plan.payloads().stream().map(MinecraftEffectExecutor::payload).toList()
        );
    }

    private static NoitaProjectileBehavior behavior(String name) {
        try {
            return NoitaProjectileBehavior.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return NoitaProjectileBehavior.BOLT;
        }
    }

    private static NoitaSpellTriggerMode triggerMode(TriggerMode mode) {
        return switch (mode) {
            case HIT -> NoitaSpellTriggerMode.HIT;
            case TIMER -> NoitaSpellTriggerMode.TIMER;
            case DEATH -> NoitaSpellTriggerMode.DEATH;
            case NONE -> NoitaSpellTriggerMode.NONE;
        };
    }

    private static List<NoitaModifierEffect> effects(List<String> names) {
        List<NoitaModifierEffect> effects = new ArrayList<>();
        for (String name : names) {
            try {
                effects.add(NoitaModifierEffect.valueOf(name));
            } catch (IllegalArgumentException ignored) {
                // Catalog epoch/hash already freezes mechanics; obsolete visual tags are safely ignored here.
            }
        }
        return List.copyOf(effects);
    }

    private static Vec3d spellSpawnPosition(ServerPlayerEntity player) {
        double yawRadians = Math.toRadians(player.getYaw());
        Vec3d forward = new Vec3d(-Math.sin(yawRadians), 0.0, Math.cos(yawRadians));
        Vec3d right = new Vec3d(-Math.cos(yawRadians), 0.0, -Math.sin(yawRadians));
        return new Vec3d(player.getX(), player.getEyeY(), player.getZ())
            .add(forward.multiply(SPELL_SPAWN_FORWARD_OFFSET))
            .add(right.multiply(SPELL_SPAWN_RIGHT_OFFSET))
            .add(0.0, -SPELL_SPAWN_DOWN_OFFSET, 0.0);
    }

    private static void applyRecoil(ServerPlayerEntity player, Vec3d direction, double recoil) {
        if (recoil == 0.0 || direction.lengthSquared() <= 1.0E-6) {
            return;
        }
        double strength = Math.max(-1.2, Math.min(1.2, recoil / 400.0));
        Vec3d impulse = direction.normalize().multiply(-strength);
        player.addVelocity(impulse.x, impulse.y * 0.45, impulse.z);
        player.velocityModified = true;
    }
}
