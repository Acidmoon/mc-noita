package com.mcnoita.spell;

import com.mcnoita.persistence.NoitaNbtLimits;
import com.mcnoita.persistence.NoitaNbtSafety;
import com.mcnoita.persistence.NoitaNbtSchema;
import com.mcnoita.spell.trigger.TriggerRuntimeBudget;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

/**
 * A fully frozen projectile node. Runtime code may consume its TriggerPlan, but
 * it must never re-open a wand deck, charge mana, or resolve a catalog ID.
 */
public record NoitaProjectilePayload(
    String itemPath,
    NoitaProjectileBehavior behavior,
    float damage,
    float criticalChancePercent,
    int lifetimeTicks,
    int trailLightStacks,
    float explosionRadius,
    float speed,
    float divergence,
    float gravity,
    float drag,
    float bounceDamping,
    float renderScale,
    float knockbackForce,
    boolean friendlyFire,
    boolean piercing,
    int projectileCount,
    float burstSpreadDegrees,
    int bounceCount,
    List<NoitaModifierEffect> modifierEffects,
    NoitaTriggerPlan triggerPlan,
    NoitaExecutionIdentity executionIdentity,
    TriggerRuntimeBudget runtimeBudget
) {
    private static final String ITEM_PATH_KEY = "ItemPath";
    private static final String BEHAVIOR_KEY = "Behavior";
    private static final String LEGACY_KIND_KEY = "Kind";
    private static final String DAMAGE_KEY = "Damage";
    private static final String CRITICAL_CHANCE_PERCENT_KEY = "CriticalChancePercent";
    private static final String LIFETIME_TICKS_KEY = "LifetimeTicks";
    private static final String TRAIL_LIGHT_STACKS_KEY = "TrailLightStacks";
    private static final String EXPLOSION_RADIUS_KEY = "ExplosionRadius";
    private static final String SPEED_KEY = "Speed";
    private static final String DIVERGENCE_KEY = "Divergence";
    private static final String GRAVITY_KEY = "Gravity";
    private static final String DRAG_KEY = "Drag";
    private static final String BOUNCE_DAMPING_KEY = "BounceDamping";
    private static final String RENDER_SCALE_KEY = "RenderScale";
    private static final String KNOCKBACK_FORCE_KEY = "KnockbackForce";
    private static final String FRIENDLY_FIRE_KEY = "FriendlyFire";
    private static final String PIERCING_KEY = "Piercing";
    private static final String PROJECTILE_COUNT_KEY = "ProjectileCount";
    private static final String BURST_SPREAD_DEGREES_KEY = "BurstSpreadDegrees";
    private static final String BOUNCE_COUNT_KEY = "BounceCount";
    private static final String MODIFIER_EFFECTS_KEY = "ModifierEffects";
    private static final String TRIGGER_PLAN_KEY = "TriggerPlan";
    private static final String TRIGGER_MODE_KEY = "TriggerMode";
    private static final String TRIGGER_DELAY_TICKS_KEY = "TriggerDelayTicks";
    private static final String PAYLOADS_KEY = "Payloads";
    private static final String NODE_PATH_KEY = "NodePath";
    private static final String EXECUTION_ID_KEY = "ExecutionId";
    private static final String CATALOG_EPOCH_KEY = "CatalogEpoch";
    private static final String CATALOG_HASH_KEY = "CatalogHash";
    private static final String RUNTIME_BUDGET_KEY = "RuntimeBudget";
    private static final String REMAINING_RELEASE_EVENTS_KEY = "RemainingReleaseEvents";
    private static final String REMAINING_SPAWNED_ENTITIES_KEY = "RemainingSpawnedEntities";
    private static final String TIMER_DELAY_KEY = "TimerDelayTicks";
    private static final String PAYLOAD_DEPTH_KEY = "PayloadDepth";
    private static final String RELEASE_POLICY_KEY = "ReleasePolicy";
    private static final String PROJECTILES_KEY = "Projectiles";
    private static final Pattern ITEM_PATH_PATTERN = Pattern.compile("[a-z0-9._/-]+");

    public NoitaProjectilePayload {
        itemPath = itemPath == null || itemPath.isBlank() ? "spark_bolt" : boundedItemPath(itemPath);
        behavior = behavior == null ? NoitaProjectileBehavior.BOLT : behavior;
        damage = boundedFinite(damage, "damage", -NoitaNbtLimits.MAX_ABSOLUTE_PROJECTILE_DAMAGE,
            NoitaNbtLimits.MAX_ABSOLUTE_PROJECTILE_DAMAGE);
        criticalChancePercent = boundedFinite(criticalChancePercent, "criticalChancePercent", 0.0f,
            NoitaNbtLimits.MAX_CRITICAL_CHANCE_PERCENT);
        explosionRadius = boundedFinite(explosionRadius, "explosionRadius", 0.0f, NoitaNbtLimits.MAX_EXPLOSION_RADIUS);
        speed = boundedFinite(speed, "speed", 0.0f, NoitaNbtLimits.MAX_PROJECTILE_SPEED);
        divergence = boundedFinite(divergence, "divergence", 0.0f, NoitaNbtLimits.MAX_DIVERGENCE_DEGREES);
        gravity = boundedFinite(gravity, "gravity", -NoitaNbtLimits.MAX_ABSOLUTE_GRAVITY,
            NoitaNbtLimits.MAX_ABSOLUTE_GRAVITY);
        drag = boundedFinite(drag, "drag", 0.0f, NoitaNbtLimits.MAX_DRAG);
        bounceDamping = boundedFinite(bounceDamping, "bounceDamping", 0.0f, NoitaNbtLimits.MAX_BOUNCE_DAMPING);
        renderScale = boundedFinite(renderScale, "renderScale", 0.1f, NoitaNbtLimits.MAX_RENDER_SCALE);
        knockbackForce = boundedFinite(knockbackForce, "knockbackForce", -NoitaNbtLimits.MAX_ABSOLUTE_KNOCKBACK_FORCE,
            NoitaNbtLimits.MAX_ABSOLUTE_KNOCKBACK_FORCE);
        burstSpreadDegrees = boundedFinite(burstSpreadDegrees, "burstSpreadDegrees", 0.0f,
            NoitaNbtLimits.MAX_DIVERGENCE_DEGREES);
        lifetimeTicks = Math.min(NoitaNbtLimits.MAX_PROJECTILE_LIFETIME_TICKS, Math.max(1, lifetimeTicks));
        trailLightStacks = Math.min(NoitaNbtLimits.MAX_TRAIL_LIGHT_STACKS, Math.max(0, trailLightStacks));
        projectileCount = Math.min(NoitaNbtLimits.MAX_PROJECTILE_COUNT, Math.max(1, projectileCount));
        bounceCount = Math.min(NoitaNbtLimits.MAX_BOUNCE_COUNT, Math.max(0, bounceCount));
        modifierEffects = List.copyOf(modifierEffects == null ? List.of() : modifierEffects);
        if (modifierEffects.size() > NoitaNbtLimits.MAX_MODIFIER_EFFECTS) {
            throw new IllegalArgumentException("projectile modifier effects exceed frozen payload limit");
        }
        executionIdentity = executionIdentity == null ? NoitaExecutionIdentity.unbound("root") : executionIdentity;
        triggerPlan = triggerPlan == null ? NoitaTriggerPlan.none(executionIdentity.nodePath()) : triggerPlan;
        runtimeBudget = runtimeBudget == null ? TriggerRuntimeBudget.DEFAULT : runtimeBudget;
    }

    /**
     * Source-compatible constructor for pre-G03 callers. It converts the old
     * flat payload list into one PayloadPlan and marks the identity unbound.
     */
    public NoitaProjectilePayload(
        String itemPath,
        NoitaProjectileBehavior behavior,
        float damage,
        float criticalChancePercent,
        int lifetimeTicks,
        int trailLightStacks,
        float explosionRadius,
        float speed,
        float divergence,
        float gravity,
        float drag,
        float bounceDamping,
        float renderScale,
        float knockbackForce,
        boolean friendlyFire,
        boolean piercing,
        int projectileCount,
        float burstSpreadDegrees,
        NoitaSpellTriggerMode triggerMode,
        int triggerDelayTicks,
        int bounceCount,
        List<NoitaModifierEffect> modifierEffects,
        List<NoitaProjectilePayload> payloads
    ) {
        this(itemPath, behavior, damage, criticalChancePercent, lifetimeTicks, trailLightStacks, explosionRadius,
            speed, divergence, gravity, drag, bounceDamping, renderScale, knockbackForce, friendlyFire, piercing,
            projectileCount, burstSpreadDegrees, bounceCount, modifierEffects,
            NoitaTriggerPlan.legacy(triggerMode, triggerDelayTicks, payloads == null ? List.of() : payloads, "root"),
            NoitaExecutionIdentity.unbound("root"), TriggerRuntimeBudget.DEFAULT);
    }

    /** Old accessors remain until callers are fully migrated to triggerPlan(). */
    public NoitaSpellTriggerMode triggerMode() {
        return triggerPlan.mode();
    }

    public int triggerDelayTicks() {
        return triggerPlan.timerDelayTicks();
    }

    /**
     * Returns the legacy flattened view. New callers must preserve payload-shot
     * boundaries through triggerPlan().payloads().
     */
    public List<NoitaProjectilePayload> payloads() {
        List<NoitaProjectilePayload> flattened = new ArrayList<>();
        for (NoitaPayloadPlan payload : triggerPlan.payloads()) {
            flattened.addAll(payload.projectiles());
        }
        return List.copyOf(flattened);
    }

    public NoitaProjectilePayload withRuntimeBudget(TriggerRuntimeBudget nextBudget) {
        return new NoitaProjectilePayload(itemPath, behavior, damage, criticalChancePercent, lifetimeTicks,
            trailLightStacks, explosionRadius, speed, divergence, gravity, drag, bounceDamping, renderScale,
            knockbackForce, friendlyFire, piercing, projectileCount, burstSpreadDegrees, bounceCount, modifierEffects,
            triggerPlan, executionIdentity, nextBudget);
    }

    /**
     * Binds a legacy runtime tree at its server spawn boundary. Old call sites
     * can still construct payloads without catalog metadata, but once an entity
     * exists every descendant must persist one non-zero execution identity.
     */
    public NoitaProjectilePayload withTreeIdentity(UUID executionId, long catalogEpoch, String catalogHash) {
        return withTreeIdentity(executionId, catalogEpoch, catalogHash, executionIdentity.nodePath());
    }

    private NoitaProjectilePayload withTreeIdentity(
        UUID executionId, long catalogEpoch, String catalogHash, String nodePath
    ) {
        NoitaExecutionIdentity identity = new NoitaExecutionIdentity(executionId, nodePath,
            catalogEpoch, catalogHash);
        List<NoitaPayloadPlan> boundPayloads = new ArrayList<>(triggerPlan.payloads().size());
        for (int payloadIndex = 0; payloadIndex < triggerPlan.payloads().size(); payloadIndex++) {
            NoitaPayloadPlan payload = triggerPlan.payloads().get(payloadIndex);
            String payloadPath = nodePath + "/trigger/" + payloadIndex;
            List<NoitaProjectilePayload> projectiles = new ArrayList<>(payload.projectiles().size());
            for (int projectileIndex = 0; projectileIndex < payload.projectiles().size(); projectileIndex++) {
                NoitaProjectilePayload projectile = payload.projectiles().get(projectileIndex);
                projectiles.add(projectile.withTreeIdentity(executionId, catalogEpoch, catalogHash,
                    payloadPath + "/" + projectileIndex));
            }
            boundPayloads.add(new NoitaPayloadPlan(payloadPath, projectiles));
        }
        NoitaTriggerPlan boundTrigger = new NoitaTriggerPlan(triggerPlan.mode(), triggerPlan.timerDelayTicks(),
            boundPayloads, nodePath + "/trigger", triggerPlan.payloadDepth(), triggerPlan.releasePolicy());
        return new NoitaProjectilePayload(itemPath, behavior, damage, criticalChancePercent, lifetimeTicks,
            trailLightStacks, explosionRadius, speed, divergence, gravity, drag, bounceDamping, renderScale,
            knockbackForce, friendlyFire, piercing, projectileCount, burstSpreadDegrees, bounceCount, modifierEffects,
            boundTrigger, identity, runtimeBudget);
    }

    /**
     * A logical plan node may fan out into multiple Minecraft entities. Derive
     * an instance path for each one, then rebase its frozen descendants so
     * collision diagnostics and persisted reload state cannot collide.
     */
    public NoitaProjectilePayload withRuntimeBudgetAndInstance(
        TriggerRuntimeBudget nextBudget, int releaseSequence, int entityIndex
    ) {
        if (releaseSequence < 0 || entityIndex < 0) {
            throw new IllegalArgumentException("release and entity indexes must not be negative");
        }
        return rebase(executionIdentity.nodePath() + "/release/" + releaseSequence + "/entity/" + entityIndex, nextBudget);
    }

    private NoitaProjectilePayload rebase(String nodePath, TriggerRuntimeBudget nextBudget) {
        NoitaExecutionIdentity identity = new NoitaExecutionIdentity(executionIdentity.executionId(), nodePath,
            executionIdentity.catalogEpoch(), executionIdentity.catalogHash());
        List<NoitaPayloadPlan> rebasedPayloads = new ArrayList<>(triggerPlan.payloads().size());
        for (int payloadIndex = 0; payloadIndex < triggerPlan.payloads().size(); payloadIndex++) {
            NoitaPayloadPlan payload = triggerPlan.payloads().get(payloadIndex);
            String payloadPath = nodePath + "/trigger/" + payloadIndex;
            List<NoitaProjectilePayload> projectiles = new ArrayList<>(payload.projectiles().size());
            for (int projectileIndex = 0; projectileIndex < payload.projectiles().size(); projectileIndex++) {
                NoitaProjectilePayload projectile = payload.projectiles().get(projectileIndex);
                projectiles.add(projectile.rebase(payloadPath + "/" + projectileIndex, projectile.runtimeBudget()));
            }
            rebasedPayloads.add(new NoitaPayloadPlan(payloadPath, projectiles));
        }
        NoitaTriggerPlan rebasedTrigger = new NoitaTriggerPlan(triggerPlan.mode(), triggerPlan.timerDelayTicks(),
            rebasedPayloads, nodePath + "/trigger", triggerPlan.payloadDepth(), triggerPlan.releasePolicy());
        return new NoitaProjectilePayload(itemPath, behavior, damage, criticalChancePercent, lifetimeTicks,
            trailLightStacks, explosionRadius, speed, divergence, gravity, drag, bounceDamping, renderScale,
            knockbackForce, friendlyFire, piercing, projectileCount, burstSpreadDegrees, bounceCount, modifierEffects,
            rebasedTrigger, identity, nextBudget);
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        NoitaNbtSchema.writeCurrentVersion(nbt);
        nbt.putString(ITEM_PATH_KEY, itemPath);
        nbt.putString(BEHAVIOR_KEY, behavior.name());
        nbt.putFloat(DAMAGE_KEY, damage);
        nbt.putFloat(CRITICAL_CHANCE_PERCENT_KEY, criticalChancePercent);
        nbt.putInt(LIFETIME_TICKS_KEY, lifetimeTicks);
        nbt.putInt(TRAIL_LIGHT_STACKS_KEY, trailLightStacks);
        nbt.putFloat(EXPLOSION_RADIUS_KEY, explosionRadius);
        nbt.putFloat(SPEED_KEY, speed);
        nbt.putFloat(DIVERGENCE_KEY, divergence);
        nbt.putFloat(GRAVITY_KEY, gravity);
        nbt.putFloat(DRAG_KEY, drag);
        nbt.putFloat(BOUNCE_DAMPING_KEY, bounceDamping);
        nbt.putFloat(RENDER_SCALE_KEY, renderScale);
        nbt.putFloat(KNOCKBACK_FORCE_KEY, knockbackForce);
        nbt.putBoolean(FRIENDLY_FIRE_KEY, friendlyFire);
        nbt.putBoolean(PIERCING_KEY, piercing);
        nbt.putInt(PROJECTILE_COUNT_KEY, projectileCount);
        nbt.putFloat(BURST_SPREAD_DEGREES_KEY, burstSpreadDegrees);
        nbt.putInt(BOUNCE_COUNT_KEY, bounceCount);
        nbt.put(MODIFIER_EFFECTS_KEY, toEffectNbtList(modifierEffects));
        nbt.put(TRIGGER_PLAN_KEY, triggerPlanToNbt(triggerPlan));
        nbt.putString(EXECUTION_ID_KEY, executionIdentity.executionId().toString());
        nbt.putString(NODE_PATH_KEY, executionIdentity.nodePath());
        nbt.putLong(CATALOG_EPOCH_KEY, executionIdentity.catalogEpoch());
        nbt.putString(CATALOG_HASH_KEY, executionIdentity.catalogHash());
        nbt.put(RUNTIME_BUDGET_KEY, runtimeBudgetToNbt(runtimeBudget));
        return nbt;
    }

    public static NoitaProjectilePayload fromNbt(NbtCompound nbt) {
        return tryFromNbt(nbt).orElseThrow(() -> new IllegalArgumentException("Invalid Noita projectile payload NBT"));
    }

    /**
     * Decodes one entire frozen tree with one shared context. A malformed child
     * rejects the parent instead of silently running a partial trigger tree.
     */
    public static Optional<NoitaProjectilePayload> tryFromNbt(NbtCompound rawNbt) {
        NbtCompound nbt = rawNbt.copy();
        if (nbt.getSizeInBytes() > NoitaNbtLimits.MAX_PAYLOAD_NBT_BYTES
            || !NoitaNbtSafety.validateTree(nbt, NoitaNbtLimits.MAX_PAYLOAD_NBT_DEPTH, NoitaNbtLimits.MAX_PAYLOAD_NBT_NODES,
                NoitaNbtLimits.MAX_NBT_LIST_ENTRIES)) {
            return Optional.empty();
        }
        try {
            return Optional.of(readPayload(nbt, new DecodeContext(), "root", 0, false));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public static NbtList toNbtList(List<NoitaProjectilePayload> payloads) {
        NbtList nbtList = new NbtList();
        for (NoitaProjectilePayload payload : payloads) {
            nbtList.add(payload.toNbt());
        }
        return nbtList;
    }

    /** Legacy list API now rejects the complete list when any child is malformed. */
    public static List<NoitaProjectilePayload> fromNbtList(NbtList nbtList) {
        return tryFromNbtList(nbtList).orElse(List.of());
    }

    public static Optional<List<NoitaProjectilePayload>> tryFromNbtList(NbtList nbtList) {
        if (nbtList.size() > NoitaNbtLimits.MAX_PAYLOAD_CHILDREN) {
            return Optional.empty();
        }
        try {
            DecodeContext context = new DecodeContext();
            List<NoitaProjectilePayload> payloads = new ArrayList<>(nbtList.size());
            for (int index = 0; index < nbtList.size(); index++) {
                NbtCompound child = nbtList.getCompound(index).copy();
                if (child.getSizeInBytes() > NoitaNbtLimits.MAX_PAYLOAD_NBT_BYTES
                    || !NoitaNbtSafety.validateTree(child, NoitaNbtLimits.MAX_PAYLOAD_NBT_DEPTH,
                        NoitaNbtLimits.MAX_PAYLOAD_NBT_NODES, NoitaNbtLimits.MAX_NBT_LIST_ENTRIES)) {
                    return Optional.empty();
                }
                payloads.add(readPayload(child, context, "root/" + index, 0, false));
            }
            return Optional.of(List.copyOf(payloads));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public static NbtList toEffectNbtList(List<NoitaModifierEffect> modifierEffects) {
        if (modifierEffects.size() > NoitaNbtLimits.MAX_MODIFIER_EFFECTS) {
            throw new IllegalArgumentException("too many modifier effects");
        }
        NbtList nbtList = new NbtList();
        for (NoitaModifierEffect modifierEffect : modifierEffects) {
            nbtList.add(net.minecraft.nbt.NbtString.of(modifierEffect.name()));
        }
        return nbtList;
    }

    public static List<NoitaModifierEffect> fromEffectNbtList(NbtList nbtList) {
        if (nbtList.size() > NoitaNbtLimits.MAX_MODIFIER_EFFECTS) {
            throw new IllegalArgumentException("too many modifier effects");
        }
        List<NoitaModifierEffect> modifierEffects = new ArrayList<>(nbtList.size());
        for (int index = 0; index < nbtList.size(); index++) {
            try {
                modifierEffects.add(NoitaModifierEffect.valueOf(nbtList.getString(index)));
            } catch (IllegalArgumentException failure) {
                throw new IllegalArgumentException("invalid modifier effect", failure);
            }
        }
        return List.copyOf(modifierEffects);
    }

    private static NoitaProjectilePayload readPayload(
        NbtCompound nbt, DecodeContext context, String fallbackNodePath, int currentPayloadDepth,
        boolean currentSchemaAncestor
    ) {
        // Every nested compound is an independently versioned payload. Checking
        // it here closes the old list-decoder gap where a future/corrupt child
        // could bypass the root migration and be accepted as a partial tree.
        int sourceVersion = NoitaNbtSchema.readStoredVersion(nbt);
        boolean currentSchema = sourceVersion == NoitaNbtSchema.CURRENT_VERSION;
        // A v3 parent promises a fully frozen tree. Accepting a v2 descendant
        // after migrating it in memory would silently reintroduce legacy
        // defaults below an otherwise strict v3 entity payload.
        if (currentSchemaAncestor && !currentSchema) {
            throw new IllegalArgumentException("current frozen payload tree contains a legacy child");
        }
        if (sourceVersion < 0 || nbt.getSizeInBytes() > NoitaNbtLimits.MAX_PAYLOAD_NBT_BYTES
            || !NoitaNbtSchema.migrateToCurrent(nbt, NoitaNbtSchema.Kind.PROJECTILE_PAYLOAD)
            || !NoitaNbtSafety.validateTree(nbt, NoitaNbtLimits.MAX_PAYLOAD_NBT_DEPTH,
                NoitaNbtLimits.MAX_PAYLOAD_NBT_NODES, NoitaNbtLimits.MAX_NBT_LIST_ENTRIES)) {
            throw new IllegalArgumentException("invalid nested frozen projectile payload");
        }
        if (currentSchema) {
            requireCurrentPayloadShape(nbt);
        }
        context.enterProjectile(fallbackNodePath);
        NoitaExecutionIdentity identity = readIdentity(nbt, fallbackNodePath, currentSchema);
        context.replaceProjectilePath(fallbackNodePath, identity.nodePath());
        context.bindIdentity(identity);
        NoitaTriggerPlan triggerPlan = readTriggerPlan(nbt, context, identity.nodePath(), currentPayloadDepth, currentSchema,
            currentSchemaAncestor || currentSchema);
        return new NoitaProjectilePayload(
            readItemPath(nbt), readBehavior(nbt), finite(nbt, DAMAGE_KEY, 0.0f),
            finite(nbt, CRITICAL_CHANCE_PERCENT_KEY, 0.0f), boundedInt(nbt, LIFETIME_TICKS_KEY, 1,
                NoitaNbtLimits.MAX_PROJECTILE_LIFETIME_TICKS, 60), nonNegative(nbt, TRAIL_LIGHT_STACKS_KEY, 0),
            nonNegativeFloat(nbt, EXPLOSION_RADIUS_KEY, 0.0f), nonNegativeFloat(nbt, SPEED_KEY, 0.0f),
            nonNegativeFloat(nbt, DIVERGENCE_KEY, 0.0f), finite(nbt, GRAVITY_KEY, 0.0f),
            nonNegativeFloat(nbt, DRAG_KEY, 0.99f), nonNegativeFloat(nbt, BOUNCE_DAMPING_KEY, 0.65f),
            positiveFloat(nbt, RENDER_SCALE_KEY, 1.0f), finite(nbt, KNOCKBACK_FORCE_KEY, 0.0f),
            nbt.getBoolean(FRIENDLY_FIRE_KEY), nbt.getBoolean(PIERCING_KEY), boundedInt(nbt, PROJECTILE_COUNT_KEY, 1,
                NoitaNbtLimits.MAX_PROJECTILE_COUNT, 1), nonNegativeFloat(nbt, BURST_SPREAD_DEGREES_KEY, 0.0f),
            nonNegative(nbt, BOUNCE_COUNT_KEY, 0), fromEffectNbtList(nbt.getList(MODIFIER_EFFECTS_KEY, NbtElement.STRING_TYPE)),
            triggerPlan, identity, readRuntimeBudget(nbt, currentSchema)
        );
    }

    private static NoitaTriggerPlan readTriggerPlan(
        NbtCompound nbt, DecodeContext context, String ownerNodePath, int ownerPayloadDepth, boolean currentSchema,
        boolean requireCurrentChildren
    ) {
        if (!nbt.contains(TRIGGER_PLAN_KEY, NbtElement.COMPOUND_TYPE)) {
            if (currentSchema) {
                throw new IllegalArgumentException("current frozen payload is missing TriggerPlan");
            }
            NoitaSpellTriggerMode mode = readTriggerMode(nbt, TRIGGER_MODE_KEY, NoitaSpellTriggerMode.NONE);
            NbtList legacyPayloads = nbt.getList(PAYLOADS_KEY, NbtElement.COMPOUND_TYPE);
            int childPayloadDepth = mode == NoitaSpellTriggerMode.NONE ? ownerPayloadDepth : ownerPayloadDepth + 1;
            requirePayloadDepth(childPayloadDepth);
            List<NoitaProjectilePayload> projectiles = readLegacyPayloads(legacyPayloads, context,
                ownerNodePath + "/trigger/0", childPayloadDepth);
            return NoitaTriggerPlan.legacy(mode, boundedInt(nbt, TRIGGER_DELAY_TICKS_KEY, 0,
                NoitaNbtLimits.MAX_TRIGGER_DELAY_TICKS, 0), projectiles, ownerNodePath);
        }

        NbtCompound planNbt = nbt.getCompound(TRIGGER_PLAN_KEY);
        if (currentSchema) {
            requireCurrentTriggerPlanShape(planNbt);
        }
        String planPath = readString(planNbt, NODE_PATH_KEY, ownerNodePath + "/trigger");
        context.enterPlanPath(planPath);
        NoitaSpellTriggerMode mode = readTriggerMode(planNbt, TRIGGER_MODE_KEY, NoitaSpellTriggerMode.NONE);
        NoitaTriggerReleasePolicy policy = readPolicy(planNbt, mode);
        int delay = boundedInt(planNbt, TIMER_DELAY_KEY, 0, NoitaNbtLimits.MAX_TRIGGER_DELAY_TICKS, 0);
        int depth = boundedInt(planNbt, PAYLOAD_DEPTH_KEY, 0, NoitaNbtLimits.MAX_PAYLOAD_DEPTH, 0);
        NbtList payloadNbt = planNbt.getList(PAYLOADS_KEY, NbtElement.COMPOUND_TYPE);
        if (payloadNbt.size() > NoitaNbtLimits.MAX_PAYLOAD_CHILDREN) {
            throw new IllegalArgumentException("too many trigger payload shots");
        }
        if (mode == NoitaSpellTriggerMode.NONE) {
            if (depth != 0 || !payloadNbt.isEmpty()) {
                throw new IllegalArgumentException("NONE trigger plans cannot carry payload state");
            }
        } else if (depth != ownerPayloadDepth + 1) {
            throw new IllegalArgumentException("payload depth does not match its parent tree position");
        }
        List<NoitaPayloadPlan> payloads = new ArrayList<>(payloadNbt.size());
        for (int index = 0; index < payloadNbt.size(); index++) {
            payloads.add(readPayloadPlan(payloadNbt.getCompound(index), context, planPath + "/" + index, depth,
                currentSchema, requireCurrentChildren));
        }
        return new NoitaTriggerPlan(mode, delay, payloads, planPath, depth, policy);
    }

    private static List<NoitaProjectilePayload> readLegacyPayloads(
        NbtList entries, DecodeContext context, String basePath, int childPayloadDepth
    ) {
        if (entries.size() > NoitaNbtLimits.MAX_PAYLOAD_CHILDREN) {
            throw new IllegalArgumentException("too many legacy trigger payloads");
        }
        List<NoitaProjectilePayload> payloads = new ArrayList<>(entries.size());
        for (int index = 0; index < entries.size(); index++) {
            payloads.add(readPayload(entries.getCompound(index), context, basePath + "/" + index, childPayloadDepth, false));
        }
        return List.copyOf(payloads);
    }

    private static NoitaPayloadPlan readPayloadPlan(
        NbtCompound nbt, DecodeContext context, String fallbackNodePath, int payloadDepth, boolean currentSchema,
        boolean requireCurrentPayloads
    ) {
        if (currentSchema) {
            requireCurrentPayloadPlanShape(nbt);
        }
        String path = readString(nbt, NODE_PATH_KEY, fallbackNodePath);
        context.enterPlanPath(path);
        NbtList projectilesNbt = nbt.getList(PROJECTILES_KEY, NbtElement.COMPOUND_TYPE);
        if (projectilesNbt.size() > NoitaNbtLimits.MAX_PAYLOAD_CHILDREN) {
            throw new IllegalArgumentException("too many payload projectiles");
        }
        List<NoitaProjectilePayload> projectiles = new ArrayList<>(projectilesNbt.size());
        for (int index = 0; index < projectilesNbt.size(); index++) {
            projectiles.add(readPayload(projectilesNbt.getCompound(index), context, path + "/" + index, payloadDepth,
                requireCurrentPayloads));
        }
        return new NoitaPayloadPlan(path, projectiles);
    }

    private static NbtCompound triggerPlanToNbt(NoitaTriggerPlan plan) {
        NbtCompound nbt = new NbtCompound();
        nbt.putString(TRIGGER_MODE_KEY, plan.mode().name());
        nbt.putInt(TIMER_DELAY_KEY, plan.timerDelayTicks());
        nbt.putString(NODE_PATH_KEY, plan.nodePath());
        nbt.putInt(PAYLOAD_DEPTH_KEY, plan.payloadDepth());
        nbt.putString(RELEASE_POLICY_KEY, plan.releasePolicy().name());
        NbtList payloads = new NbtList();
        for (NoitaPayloadPlan payload : plan.payloads()) {
            NbtCompound payloadNbt = new NbtCompound();
            payloadNbt.putString(NODE_PATH_KEY, payload.nodePath());
            payloadNbt.put(PROJECTILES_KEY, toNbtList(payload.projectiles()));
            payloads.add(payloadNbt);
        }
        nbt.put(PAYLOADS_KEY, payloads);
        return nbt;
    }

    private static NbtCompound runtimeBudgetToNbt(TriggerRuntimeBudget budget) {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt(REMAINING_RELEASE_EVENTS_KEY, budget.remainingReleaseEvents());
        nbt.putInt(REMAINING_SPAWNED_ENTITIES_KEY, budget.remainingSpawnedEntities());
        return nbt;
    }

    private static NoitaExecutionIdentity readIdentity(NbtCompound nbt, String fallbackNodePath, boolean currentSchema) {
        if (currentSchema) {
            require(nbt, EXECUTION_ID_KEY, NbtElement.STRING_TYPE);
            require(nbt, NODE_PATH_KEY, NbtElement.STRING_TYPE);
            require(nbt, CATALOG_EPOCH_KEY, NbtElement.NUMBER_TYPE);
            require(nbt, CATALOG_HASH_KEY, NbtElement.STRING_TYPE);
        }
        String nodePath = readString(nbt, NODE_PATH_KEY, fallbackNodePath);
        UUID executionId = NoitaExecutionIdentity.UNBOUND_EXECUTION_ID;
        if (nbt.contains(EXECUTION_ID_KEY, NbtElement.STRING_TYPE)) {
            try {
                executionId = UUID.fromString(nbt.getString(EXECUTION_ID_KEY));
            } catch (IllegalArgumentException failure) {
                throw new IllegalArgumentException("invalid execution UUID", failure);
            }
        }
        NoitaExecutionIdentity identity = new NoitaExecutionIdentity(executionId, nodePath,
            nbt.contains(CATALOG_EPOCH_KEY, NbtElement.NUMBER_TYPE) ? nbt.getLong(CATALOG_EPOCH_KEY) : 0L,
            readString(nbt, CATALOG_HASH_KEY, "legacy"));
        if (currentSchema && !identity.isBound()) {
            throw new IllegalArgumentException("current frozen payload has no server execution identity");
        }
        return identity;
    }

    private static TriggerRuntimeBudget readRuntimeBudget(NbtCompound nbt, boolean currentSchema) {
        if (!nbt.contains(RUNTIME_BUDGET_KEY, NbtElement.COMPOUND_TYPE)) {
            if (currentSchema) {
                throw new IllegalArgumentException("current frozen payload is missing RuntimeBudget");
            }
            return TriggerRuntimeBudget.DEFAULT;
        }
        NbtCompound budget = nbt.getCompound(RUNTIME_BUDGET_KEY);
        if (currentSchema) {
            require(budget, REMAINING_RELEASE_EVENTS_KEY, NbtElement.NUMBER_TYPE);
            require(budget, REMAINING_SPAWNED_ENTITIES_KEY, NbtElement.NUMBER_TYPE);
        }
        return new TriggerRuntimeBudget(
            boundedInt(budget, REMAINING_RELEASE_EVENTS_KEY, 0, TriggerRuntimeBudget.DEFAULT.remainingReleaseEvents(),
                TriggerRuntimeBudget.DEFAULT.remainingReleaseEvents()),
            boundedInt(budget, REMAINING_SPAWNED_ENTITIES_KEY, 0, TriggerRuntimeBudget.DEFAULT.remainingSpawnedEntities(),
                TriggerRuntimeBudget.DEFAULT.remainingSpawnedEntities())
        );
    }

    private static NoitaSpellTriggerMode readTriggerMode(NbtCompound nbt, String key, NoitaSpellTriggerMode fallback) {
        if (!nbt.contains(key, NbtElement.STRING_TYPE)) {
            return fallback;
        }
        try {
            return NoitaTriggerPlan.normalize(NoitaSpellTriggerMode.fromPersisted(nbt.getString(key)));
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("invalid trigger mode", failure);
        }
    }

    private static NoitaTriggerReleasePolicy readPolicy(NbtCompound nbt, NoitaSpellTriggerMode mode) {
        if (!nbt.contains(RELEASE_POLICY_KEY, NbtElement.STRING_TYPE)) {
            return NoitaTriggerReleasePolicy.forMode(mode);
        }
        try {
            return NoitaTriggerReleasePolicy.valueOf(nbt.getString(RELEASE_POLICY_KEY));
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("invalid trigger release policy", failure);
        }
    }

    private static String readItemPath(NbtCompound nbt) {
        if (nbt.contains(ITEM_PATH_KEY, NbtElement.STRING_TYPE)) {
            return boundedItemPath(nbt.getString(ITEM_PATH_KEY));
        }
        return "BOMB".equals(nbt.getString(LEGACY_KIND_KEY)) ? "bomb" : "spark_bolt";
    }

    private static NoitaProjectileBehavior readBehavior(NbtCompound nbt) {
        if (nbt.contains(BEHAVIOR_KEY, NbtElement.STRING_TYPE)) {
            try {
                return NoitaProjectileBehavior.valueOf(nbt.getString(BEHAVIOR_KEY));
            } catch (IllegalArgumentException failure) {
                throw new IllegalArgumentException("invalid projectile behavior", failure);
            }
        }
        return "BOMB".equals(nbt.getString(LEGACY_KIND_KEY))
            ? NoitaProjectileBehavior.FUSED_EXPLOSIVE : NoitaProjectileBehavior.BOLT;
    }

    private static String readString(NbtCompound nbt, String key, String fallback) {
        return nbt.contains(key, NbtElement.STRING_TYPE) ? bounded(nbt.getString(key), key) : fallback;
    }

    private static int boundedInt(NbtCompound nbt, String key, int min, int max, int fallback) {
        if (!nbt.contains(key, NbtElement.NUMBER_TYPE)) {
            return fallback;
        }
        return Math.max(min, Math.min(max, nbt.getInt(key)));
    }

    private static void requirePayloadDepth(int depth) {
        if (depth < 0 || depth > NoitaNbtLimits.MAX_PAYLOAD_DEPTH) {
            throw new IllegalArgumentException("frozen payload depth exceeds the configured limit");
        }
    }

    private static int nonNegative(NbtCompound nbt, String key, int fallback) {
        return boundedInt(nbt, key, 0, Integer.MAX_VALUE, fallback);
    }

    private static float finite(NbtCompound nbt, String key, float fallback) {
        return NoitaNbtSafety.finiteFloat(nbt, key, fallback, -Float.MAX_VALUE, Float.MAX_VALUE);
    }

    private static float nonNegativeFloat(NbtCompound nbt, String key, float fallback) {
        return NoitaNbtSafety.finiteFloat(nbt, key, fallback, 0.0f, Float.MAX_VALUE);
    }

    private static float positiveFloat(NbtCompound nbt, String key, float fallback) {
        return Math.max(0.1f, NoitaNbtSafety.finiteFloat(nbt, key, fallback, 0.1f, Float.MAX_VALUE));
    }

    private static String bounded(String value, String name) {
        if (value == null || value.isBlank() || value.length() > NoitaNbtLimits.MAX_STRING_LENGTH) {
            throw new IllegalArgumentException(name + " must be nonblank and bounded");
        }
        return value;
    }

    /** Item paths become registry identifiers in the entity adapter, so reject invalid characters before that boundary. */
    public static boolean isValidItemPath(String value) {
        return value != null && !value.isBlank() && value.length() <= NoitaNbtLimits.MAX_STRING_LENGTH
            && ITEM_PATH_PATTERN.matcher(value).matches();
    }

    private static String boundedItemPath(String value) {
        if (!isValidItemPath(value)) {
            throw new IllegalArgumentException("itemPath must be a lowercase Minecraft path");
        }
        return value;
    }

    private static float boundedFinite(float value, String name, float min, float max) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
        return Math.max(min, Math.min(max, value));
    }

    private static void requireCurrentPayloadShape(NbtCompound nbt) {
        require(nbt, ITEM_PATH_KEY, NbtElement.STRING_TYPE);
        require(nbt, BEHAVIOR_KEY, NbtElement.STRING_TYPE);
        require(nbt, DAMAGE_KEY, NbtElement.NUMBER_TYPE);
        require(nbt, CRITICAL_CHANCE_PERCENT_KEY, NbtElement.NUMBER_TYPE);
        require(nbt, LIFETIME_TICKS_KEY, NbtElement.NUMBER_TYPE);
        require(nbt, TRAIL_LIGHT_STACKS_KEY, NbtElement.NUMBER_TYPE);
        require(nbt, EXPLOSION_RADIUS_KEY, NbtElement.NUMBER_TYPE);
        require(nbt, SPEED_KEY, NbtElement.NUMBER_TYPE);
        require(nbt, DIVERGENCE_KEY, NbtElement.NUMBER_TYPE);
        require(nbt, GRAVITY_KEY, NbtElement.NUMBER_TYPE);
        require(nbt, DRAG_KEY, NbtElement.NUMBER_TYPE);
        require(nbt, BOUNCE_DAMPING_KEY, NbtElement.NUMBER_TYPE);
        require(nbt, RENDER_SCALE_KEY, NbtElement.NUMBER_TYPE);
        require(nbt, KNOCKBACK_FORCE_KEY, NbtElement.NUMBER_TYPE);
        require(nbt, FRIENDLY_FIRE_KEY, NbtElement.BYTE_TYPE);
        require(nbt, PIERCING_KEY, NbtElement.BYTE_TYPE);
        require(nbt, PROJECTILE_COUNT_KEY, NbtElement.NUMBER_TYPE);
        require(nbt, BURST_SPREAD_DEGREES_KEY, NbtElement.NUMBER_TYPE);
        require(nbt, BOUNCE_COUNT_KEY, NbtElement.NUMBER_TYPE);
        require(nbt, MODIFIER_EFFECTS_KEY, NbtElement.LIST_TYPE);
        require(nbt, TRIGGER_PLAN_KEY, NbtElement.COMPOUND_TYPE);
        require(nbt, EXECUTION_ID_KEY, NbtElement.STRING_TYPE);
        require(nbt, NODE_PATH_KEY, NbtElement.STRING_TYPE);
        require(nbt, CATALOG_EPOCH_KEY, NbtElement.NUMBER_TYPE);
        require(nbt, CATALOG_HASH_KEY, NbtElement.STRING_TYPE);
        require(nbt, RUNTIME_BUDGET_KEY, NbtElement.COMPOUND_TYPE);
    }

    private static void requireCurrentTriggerPlanShape(NbtCompound nbt) {
        require(nbt, TRIGGER_MODE_KEY, NbtElement.STRING_TYPE);
        require(nbt, TIMER_DELAY_KEY, NbtElement.NUMBER_TYPE);
        require(nbt, NODE_PATH_KEY, NbtElement.STRING_TYPE);
        require(nbt, PAYLOAD_DEPTH_KEY, NbtElement.NUMBER_TYPE);
        require(nbt, RELEASE_POLICY_KEY, NbtElement.STRING_TYPE);
        require(nbt, PAYLOADS_KEY, NbtElement.LIST_TYPE);
    }

    private static void requireCurrentPayloadPlanShape(NbtCompound nbt) {
        require(nbt, NODE_PATH_KEY, NbtElement.STRING_TYPE);
        require(nbt, PROJECTILES_KEY, NbtElement.LIST_TYPE);
    }

    private static void require(NbtCompound nbt, String key, byte type) {
        if (!nbt.contains(key, type)) {
            throw new IllegalArgumentException("current frozen payload is missing or has invalid " + key);
        }
    }

    private static final class DecodeContext {
        private int projectileNodes;
        private final Set<String> nodePaths = new HashSet<>();
        private UUID executionId;
        private Long catalogEpoch;
        private String catalogHash;

        /**
         * The 128-node payload limit is an evaluator limit for frozen projectile
         * nodes, not for bookkeeping TriggerPlan/PayloadPlan compounds. The
         * latter remain bounded by the separate structural NBT limits.
         */
        private void enterProjectile(String nodePath) {
            if (++projectileNodes > NoitaNbtLimits.MAX_PAYLOAD_NODES
                || !nodePaths.add(NoitaExecutionIdentity.requireNodePath(nodePath))) {
                throw new IllegalArgumentException("invalid or duplicate frozen payload node path");
            }
        }

        private void replaceProjectilePath(String fallback, String actual) {
            if (!fallback.equals(actual)) {
                nodePaths.remove(fallback);
                if (!nodePaths.add(NoitaExecutionIdentity.requireNodePath(actual))) {
                    throw new IllegalArgumentException("duplicate frozen payload node path");
                }
            }
        }

        /** All descendants of one frozen tree must remain in the cast snapshot that produced its root. */
        private void bindIdentity(NoitaExecutionIdentity identity) {
            if (executionId == null) {
                executionId = identity.executionId();
                catalogEpoch = identity.catalogEpoch();
                catalogHash = identity.catalogHash();
                return;
            }
            if (!executionId.equals(identity.executionId()) || catalogEpoch.longValue() != identity.catalogEpoch()
                || !catalogHash.equals(identity.catalogHash())) {
                throw new IllegalArgumentException("frozen payload tree mixes execution or catalog identities");
            }
        }

        private void enterPlanPath(String nodePath) {
            if (!nodePaths.add(NoitaExecutionIdentity.requireNodePath(nodePath))) {
                throw new IllegalArgumentException("invalid or duplicate frozen payload node path");
            }
        }
    }
}
