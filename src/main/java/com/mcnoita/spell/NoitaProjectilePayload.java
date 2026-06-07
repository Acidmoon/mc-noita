package com.mcnoita.spell;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

public record NoitaProjectilePayload(
    ProjectileKind kind,
    float damage,
    float criticalChancePercent,
    int lifetimeTicks,
    int trailLightStacks,
    float explosionRadius,
    float speed,
    float divergence,
    NoitaSpellTriggerMode triggerMode,
    int triggerDelayTicks,
    List<NoitaProjectilePayload> payloads
) {
    private static final String KIND_KEY = "Kind";
    private static final String DAMAGE_KEY = "Damage";
    private static final String CRITICAL_CHANCE_PERCENT_KEY = "CriticalChancePercent";
    private static final String LIFETIME_TICKS_KEY = "LifetimeTicks";
    private static final String TRAIL_LIGHT_STACKS_KEY = "TrailLightStacks";
    private static final String EXPLOSION_RADIUS_KEY = "ExplosionRadius";
    private static final String SPEED_KEY = "Speed";
    private static final String DIVERGENCE_KEY = "Divergence";
    private static final String TRIGGER_MODE_KEY = "TriggerMode";
    private static final String TRIGGER_DELAY_TICKS_KEY = "TriggerDelayTicks";
    private static final String PAYLOADS_KEY = "Payloads";

    public NoitaProjectilePayload {
        payloads = List.copyOf(payloads);
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString(KIND_KEY, kind.name());
        nbt.putFloat(DAMAGE_KEY, damage);
        nbt.putFloat(CRITICAL_CHANCE_PERCENT_KEY, criticalChancePercent);
        nbt.putInt(LIFETIME_TICKS_KEY, lifetimeTicks);
        nbt.putInt(TRAIL_LIGHT_STACKS_KEY, trailLightStacks);
        nbt.putFloat(EXPLOSION_RADIUS_KEY, explosionRadius);
        nbt.putFloat(SPEED_KEY, speed);
        nbt.putFloat(DIVERGENCE_KEY, divergence);
        nbt.putString(TRIGGER_MODE_KEY, triggerMode.name());
        nbt.putInt(TRIGGER_DELAY_TICKS_KEY, triggerDelayTicks);
        nbt.put(PAYLOADS_KEY, toNbtList(payloads));
        return nbt;
    }

    public static NoitaProjectilePayload fromNbt(NbtCompound nbt) {
        return new NoitaProjectilePayload(
            readEnum(nbt, KIND_KEY, ProjectileKind.SPARK_BOLT),
            nbt.getFloat(DAMAGE_KEY),
            nbt.getFloat(CRITICAL_CHANCE_PERCENT_KEY),
            Math.max(1, nbt.getInt(LIFETIME_TICKS_KEY)),
            Math.max(0, nbt.getInt(TRAIL_LIGHT_STACKS_KEY)),
            Math.max(0.0f, nbt.getFloat(EXPLOSION_RADIUS_KEY)),
            Math.max(0.0f, nbt.getFloat(SPEED_KEY)),
            Math.max(0.0f, nbt.getFloat(DIVERGENCE_KEY)),
            readEnum(nbt, TRIGGER_MODE_KEY, NoitaSpellTriggerMode.NONE),
            Math.max(0, nbt.getInt(TRIGGER_DELAY_TICKS_KEY)),
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

    public enum ProjectileKind {
        SPARK_BOLT,
        BOMB
    }
}
