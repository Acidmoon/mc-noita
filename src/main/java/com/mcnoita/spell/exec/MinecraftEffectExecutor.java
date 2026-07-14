package com.mcnoita.spell.exec;

import com.mcnoita.entity.SparkBoltProjectileEntity;
import com.mcnoita.MCNoita;
import com.mcnoita.spell.NoitaModifierEffect;
import com.mcnoita.spell.NoitaExecutionIdentity;
import com.mcnoita.spell.NoitaPayloadPlan;
import com.mcnoita.spell.NoitaProjectileBehavior;
import com.mcnoita.spell.NoitaProjectilePayload;
import com.mcnoita.spell.NoitaSpellTriggerMode;
import com.mcnoita.spell.NoitaTriggerPlan;
import com.mcnoita.spell.NoitaTriggerReleasePolicy;
import com.mcnoita.spell.plan.EffectPlan;
import com.mcnoita.spell.plan.ProjectilePlan;
import com.mcnoita.spell.plan.RecoilPlan;
import com.mcnoita.spell.plan.SoundPlan;
import com.mcnoita.spell.plan.TriggerMode;
import com.mcnoita.spell.plan.ResolvedCast;
import com.mcnoita.spell.trigger.TriggerRuntimeBudget;
import com.mcnoita.wand.adapter.MinecraftTimeAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

    /**
     * Executes a committed pure result. The execution UUID is generated on the
     * logical server after state commit, while catalog metadata comes directly
     * from ResolvedCast and is frozen into every persisted payload node.
     */
    public static void execute(ServerPlayerEntity player, ResolvedCast resolvedCast) {
        EffectPlan plan = resolvedCast.effectPlan();
        ServerWorld world = player.getServerWorld();
        Vec3d spawn = spellSpawnPosition(player);
        Vec3d direction = player.getRotationVec(1.0f);
        ExecutionContext context = new ExecutionContext(UUID.randomUUID(), resolvedCast.catalogEpoch(), resolvedCast.catalogHash());
        int rootEntityCount = plan.projectiles().stream().mapToInt(ProjectilePlan::projectileCount).sum();
        if (rootEntityCount > TriggerRuntimeBudget.DEFAULT.remainingSpawnedEntities()) {
            // This should be rejected by the pure spawned-entity budget before
            // WandState commit. Keep a defensive executor guard for old/custom
            // evaluators so a malformed accepted plan cannot allocate 128 roots.
            logFailure(world.getTime(), "root-entity-budget", player,
                new IllegalStateException("root entity budget exceeded: " + rootEntityCount));
            return;
        }
        List<RootRequirement> rootRequirements = rootRequirements(plan.projectiles());
        List<TriggerRuntimeBudget> rootBudgets = reserveRootBudgets(rootEntityCount, rootRequirements);
        if (rootBudgets == null) {
            logFailure(world.getTime(), "trigger-tree-budget", player,
                new IllegalStateException("accepted effect plan exceeds frozen trigger runtime capacity"));
            return;
        }
        int budgetIndex = 0;
        for (ProjectilePlan projectile : plan.projectiles()) {
            try {
                Vec3d resolvedDirection = direction.rotateY((float) Math.toRadians(-projectile.spreadOffsetDegrees()));
                int entityCount = projectile.projectileCount();
                List<TriggerRuntimeBudget> nodeBudgets = rootBudgets.subList(budgetIndex, budgetIndex + entityCount);
                budgetIndex += entityCount;
                SparkBoltProjectileEntity.spawnPayloadProjectile(world, player, spawn, resolvedDirection, payload(projectile, context),
                    nodeBudgets, 0);
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

    /**
     * Each physical root receives its nested tree's one-release minimum before
     * spare capacity is shared for Piercing repeats. Equal partitioning alone
     * can starve a payload-heavy root while an unrelated root holds its slots.
     */
    private static List<RootRequirement> rootRequirements(List<ProjectilePlan> projectiles) {
        List<RootRequirement> requirements = new ArrayList<>();
        for (ProjectilePlan projectile : projectiles) {
            int futureEntities = cappedInt(projectile.futureEntityFootprintPerInstance());
            int releaseEvents = cappedInt(projectile.staticReleaseEventFootprintPerInstance());
            for (int index = 0; index < projectile.projectileCount(); index++) {
                requirements.add(new RootRequirement(releaseEvents, futureEntities));
            }
        }
        return List.copyOf(requirements);
    }

    private static List<TriggerRuntimeBudget> reserveRootBudgets(int rootEntityCount, List<RootRequirement> requirements) {
        if (requirements.size() != rootEntityCount) {
            return null;
        }
        int requiredEvents = 0;
        int requiredEntities = 0;
        for (RootRequirement requirement : requirements) {
            requiredEvents = addCapped(requiredEvents, requirement.releaseEvents());
            requiredEntities = addCapped(requiredEntities, requirement.futureEntities());
        }
        int availableEntities = TriggerRuntimeBudget.DEFAULT.remainingSpawnedEntities() - rootEntityCount;
        int availableEvents = TriggerRuntimeBudget.DEFAULT.remainingReleaseEvents();
        if (requiredEntities > availableEntities || requiredEvents > availableEvents) {
            return null;
        }

        int[] events = new int[rootEntityCount];
        int[] entities = new int[rootEntityCount];
        for (int index = 0; index < rootEntityCount; index++) {
            RootRequirement requirement = requirements.get(index);
            events[index] = requirement.releaseEvents();
            entities[index] = requirement.futureEntities();
        }
        distribute(events, availableEvents - requiredEvents);
        distribute(entities, availableEntities - requiredEntities);

        List<TriggerRuntimeBudget> budgets = new ArrayList<>(rootEntityCount);
        for (int index = 0; index < rootEntityCount; index++) {
            budgets.add(new TriggerRuntimeBudget(events[index], entities[index]));
        }
        return List.copyOf(budgets);
    }

    private static void distribute(int[] values, int extras) {
        if (values.length == 0) {
            return;
        }
        for (int index = 0; index < extras; index++) {
            values[index % values.length]++;
        }
    }

    private static int addCapped(int left, int right) {
        return right > Integer.MAX_VALUE - left ? Integer.MAX_VALUE : left + right;
    }

    private static int cappedInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private record RootRequirement(int releaseEvents, int futureEntities) {
    }

    private static NoitaProjectilePayload payload(ProjectilePlan plan, ExecutionContext context) {
        NoitaExecutionIdentity identity = new NoitaExecutionIdentity(context.executionId(), plan.nodePath(), context.catalogEpoch(), context.catalogHash());
        NoitaTriggerPlan triggerPlan = triggerPlan(plan, context, identity.nodePath());
        return new NoitaProjectilePayload(
            plan.itemPath(), behavior(plan.behavior()), (float) plan.damage(), (float) plan.criticalChancePercent(),
            MinecraftTimeAdapter.toMinecraftTicks(plan.lifetime(), 1), plan.trailLightStacks(), (float) plan.explosionRadius(),
            (float) Math.max(0.2, Math.min(8.0, plan.speed() / NOITA_SPEED_TO_ARROW_SPEED)), 0.0f, (float) plan.gravity(),
            (float) plan.drag(), (float) plan.bounceDamping(), (float) plan.renderScale(), (float) plan.knockbackForce(),
            plan.friendlyFire(), plan.piercing(), plan.projectileCount(), (float) plan.burstSpreadDegrees(), plan.bounceCount(),
            effects(plan.effects()), triggerPlan, identity, TriggerRuntimeBudget.DEFAULT
        );
    }

    private static NoitaTriggerPlan triggerPlan(ProjectilePlan plan, ExecutionContext context, String ownerNodePath) {
        if (!plan.hasTrigger()) {
            return NoitaTriggerPlan.none(ownerNodePath);
        }
        com.mcnoita.spell.plan.TriggerPlan trigger = plan.trigger();
        NoitaSpellTriggerMode mode = triggerMode(trigger.mode());
        List<NoitaProjectilePayload> projectiles = trigger.payload().projectiles().stream()
            .map(payload -> payload(payload, context)).toList();
        // The pure plan shares one readable path for the TriggerPlan and its
        // sole PayloadPlan. NBT nodes need distinct identities for duplicate
        // detection, so the adapter gives the payload-shot its own suffix.
        NoitaPayloadPlan payload = new NoitaPayloadPlan(trigger.nodePath() + "/payload", projectiles);
        return new NoitaTriggerPlan(mode, MinecraftTimeAdapter.toMinecraftTicks(trigger.timerDelay(), 0), List.of(payload),
            trigger.nodePath(), trigger.payloadDepth(), NoitaTriggerReleasePolicy.forMode(mode));
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
            case EXPIRATION, DEATH -> NoitaSpellTriggerMode.EXPIRATION;
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

    private record ExecutionContext(UUID executionId, long catalogEpoch, String catalogHash) {
    }
}
