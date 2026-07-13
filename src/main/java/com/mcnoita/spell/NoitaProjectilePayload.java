package com.mcnoita.spell;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

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
    NoitaSpellTriggerMode triggerMode,
    int triggerDelayTicks,
    int bounceCount,
    List<NoitaModifierEffect> modifierEffects,
    List<NoitaProjectilePayload> payloads
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
    private static final String TRIGGER_MODE_KEY = "TriggerMode";
    private static final String TRIGGER_DELAY_TICKS_KEY = "TriggerDelayTicks";
    private static final String BOUNCE_COUNT_KEY = "BounceCount";
    private static final String MODIFIER_EFFECTS_KEY = "ModifierEffects";
    private static final String PAYLOADS_KEY = "Payloads";

    public NoitaProjectilePayload {
        if (itemPath == null || itemPath.isBlank()) {
            itemPath = "spark_bolt";
        }
        behavior = behavior == null ? NoitaProjectileBehavior.BOLT : behavior;
        lifetimeTicks = Math.max(1, lifetimeTicks);
        trailLightStacks = Math.max(0, trailLightStacks);
        explosionRadius = Math.max(0.0f, explosionRadius);
        speed = Math.max(0.0f, speed);
        divergence = Math.max(0.0f, divergence);
        drag = Math.max(0.0f, drag);
        bounceDamping = Math.max(0.0f, bounceDamping);
        renderScale = renderScale <= 0.0f ? 1.0f : renderScale;
        projectileCount = Math.max(1, projectileCount);
        burstSpreadDegrees = Math.max(0.0f, burstSpreadDegrees);
        triggerMode = triggerMode == null ? NoitaSpellTriggerMode.NONE : triggerMode;
        triggerDelayTicks = Math.max(0, triggerDelayTicks);
        bounceCount = Math.max(0, bounceCount);
        modifierEffects = List.copyOf(modifierEffects);
        payloads = List.copyOf(payloads);
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
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
        nbt.putString(TRIGGER_MODE_KEY, triggerMode.name());
        nbt.putInt(TRIGGER_DELAY_TICKS_KEY, triggerDelayTicks);
        nbt.putInt(BOUNCE_COUNT_KEY, bounceCount);
        nbt.put(MODIFIER_EFFECTS_KEY, toEffectNbtList(modifierEffects));
        nbt.put(PAYLOADS_KEY, toNbtList(payloads));
        return nbt;
    }

    public static NoitaProjectilePayload fromNbt(NbtCompound nbt) {
        return new NoitaProjectilePayload(
            readItemPath(nbt),
            readBehavior(nbt),
            nbt.getFloat(DAMAGE_KEY),
            nbt.getFloat(CRITICAL_CHANCE_PERCENT_KEY),
            Math.max(1, nbt.getInt(LIFETIME_TICKS_KEY)),
            Math.max(0, nbt.getInt(TRAIL_LIGHT_STACKS_KEY)),
            Math.max(0.0f, nbt.getFloat(EXPLOSION_RADIUS_KEY)),
            Math.max(0.0f, nbt.getFloat(SPEED_KEY)),
            Math.max(0.0f, nbt.getFloat(DIVERGENCE_KEY)),
            nbt.getFloat(GRAVITY_KEY),
            nbt.contains(DRAG_KEY, NbtElement.NUMBER_TYPE) ? Math.max(0.0f, nbt.getFloat(DRAG_KEY)) : 0.99f,
            nbt.contains(BOUNCE_DAMPING_KEY, NbtElement.NUMBER_TYPE) ? Math.max(0.0f, nbt.getFloat(BOUNCE_DAMPING_KEY)) : 0.65f,
            nbt.contains(RENDER_SCALE_KEY, NbtElement.NUMBER_TYPE) ? Math.max(0.1f, nbt.getFloat(RENDER_SCALE_KEY)) : 1.0f,
            nbt.contains(KNOCKBACK_FORCE_KEY, NbtElement.NUMBER_TYPE) ? nbt.getFloat(KNOCKBACK_FORCE_KEY) : 0.0f,
            nbt.contains(FRIENDLY_FIRE_KEY, NbtElement.BYTE_TYPE) && nbt.getBoolean(FRIENDLY_FIRE_KEY),
            nbt.contains(PIERCING_KEY, NbtElement.BYTE_TYPE) && nbt.getBoolean(PIERCING_KEY),
            nbt.contains(PROJECTILE_COUNT_KEY, NbtElement.NUMBER_TYPE) ? Math.max(1, nbt.getInt(PROJECTILE_COUNT_KEY)) : 1,
            nbt.contains(BURST_SPREAD_DEGREES_KEY, NbtElement.NUMBER_TYPE) ? Math.max(0.0f, nbt.getFloat(BURST_SPREAD_DEGREES_KEY)) : 0.0f,
            readEnum(nbt, TRIGGER_MODE_KEY, NoitaSpellTriggerMode.NONE),
            Math.max(0, nbt.getInt(TRIGGER_DELAY_TICKS_KEY)),
            nbt.contains(BOUNCE_COUNT_KEY, NbtElement.NUMBER_TYPE) ? Math.max(0, nbt.getInt(BOUNCE_COUNT_KEY)) : 0,
            fromEffectNbtList(nbt.getList(MODIFIER_EFFECTS_KEY, NbtElement.STRING_TYPE)),
            fromNbtList(nbt.getList(PAYLOADS_KEY, NbtElement.COMPOUND_TYPE))
        );
    }

    public static NbtList toNbtList(List<NoitaProjectilePayload> payloads) {
        NbtList nbtList = new NbtList();
        for (NoitaProjectilePayload payload : payloads) {
            nbtList.add(payload.toNbt());
        }
        return nbtList;
    }

    public static List<NoitaProjectilePayload> fromNbtList(NbtList nbtList) {
        List<NoitaProjectilePayload> payloads = new ArrayList<>(nbtList.size());
        for (int i = 0; i < nbtList.size(); i++) {
            payloads.add(fromNbt(nbtList.getCompound(i)));
        }
        return payloads;
    }

    public static NbtList toEffectNbtList(List<NoitaModifierEffect> modifierEffects) {
        NbtList nbtList = new NbtList();
        for (NoitaModifierEffect modifierEffect : modifierEffects) {
            nbtList.add(net.minecraft.nbt.NbtString.of(modifierEffect.name()));
        }
        return nbtList;
    }

    public static List<NoitaModifierEffect> fromEffectNbtList(NbtList nbtList) {
        List<NoitaModifierEffect> modifierEffects = new ArrayList<>(nbtList.size());
        for (int i = 0; i < nbtList.size(); i++) {
            try {
                modifierEffects.add(NoitaModifierEffect.valueOf(nbtList.getString(i)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return modifierEffects;
    }

    private static String readItemPath(NbtCompound nbt) {
        if (nbt.contains(ITEM_PATH_KEY, NbtElement.STRING_TYPE)) {
            return nbt.getString(ITEM_PATH_KEY);
        }
        return "BOMB".equals(nbt.getString(LEGACY_KIND_KEY)) ? "bomb" : "spark_bolt";
    }

    private static NoitaProjectileBehavior readBehavior(NbtCompound nbt) {
        if (nbt.contains(BEHAVIOR_KEY, NbtElement.STRING_TYPE)) {
            try {
                return NoitaProjectileBehavior.valueOf(nbt.getString(BEHAVIOR_KEY));
            } catch (IllegalArgumentException ignored) {
                return NoitaProjectileBehavior.BOLT;
            }
        }
        return "BOMB".equals(nbt.getString(LEGACY_KIND_KEY)) ? NoitaProjectileBehavior.FUSED_EXPLOSIVE : NoitaProjectileBehavior.BOLT;
    }

    private static <E extends Enum<E>> E readEnum(NbtCompound nbt, String key, E fallback) {
        if (!nbt.contains(key, NbtElement.STRING_TYPE)) {
            return fallback;
        }
        try {
            return Enum.valueOf(fallback.getDeclaringClass(), nbt.getString(key));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
