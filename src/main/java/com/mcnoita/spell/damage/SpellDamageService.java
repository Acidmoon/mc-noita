package com.mcnoita.spell.damage;

import com.mcnoita.MCNoita;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;

/**
 * The only server boundary that turns frozen Noita damage channels into
 * Minecraft DamageSource instances. It never accepts a client-provided amount
 * and retains both direct projectile and original owner attribution.
 */
public final class SpellDamageService {
    private static final Map<DamageChannel, RegistryKey<DamageType>> DAMAGE_TYPES = damageTypes();

    private SpellDamageService() {
    }

    /**
     * Applies every positive frozen channel independently. Status effects such
     * as burning, freezing and poison remain explicit executor/entity effects;
     * they are not smuggled into a negative or generic damage number.
     */
    public static boolean apply(
        Entity target, Entity directEntity, Entity owner, DamageProfile profile, boolean allowFriendlyFire
    ) {
        return apply(target, directEntity, owner, profile, allowFriendlyFire, allowFriendlyFire);
    }

    /**
     * Applies a frozen profile with explicit self and team policy. Some legacy
     * area effects intentionally include their owner without opening damage to
     * the owner's team, so one friendly-fire flag is not expressive enough.
     */
    public static boolean apply(
        Entity target, Entity directEntity, Entity owner, DamageProfile profile,
        boolean allowSelfDamage, boolean allowTeammateDamage
    ) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(directEntity, "directEntity");
        Objects.requireNonNull(profile, "profile");
        if (!(target.getWorld() instanceof ServerWorld world) || profile.isEmpty()
            || !canHarm(target, owner, allowSelfDamage, allowTeammateDamage)) {
            return false;
        }

        boolean applied = false;
        for (DamageChannel channel : DamageChannel.values()) {
            double rawAmount = profile.amount(channel);
            if (rawAmount <= 0.0) {
                continue;
            }
            float amount = (float) Math.min(Float.MAX_VALUE, rawAmount);
            applied |= applyOne(target, source(world, channel, directEntity, owner), amount);
        }
        return applied;
    }

    /** Compatibility helper for legacy secondary effects that are projectile damage. */
    public static boolean applyProjectile(
        Entity target, Entity directEntity, Entity owner, double amount, boolean allowFriendlyFire
    ) {
        return apply(target, directEntity, owner, DamageProfile.legacyProjectile(amount), allowFriendlyFire);
    }

    public static boolean applyProjectile(
        Entity target, Entity directEntity, Entity owner, double amount,
        boolean allowSelfDamage, boolean allowTeammateDamage
    ) {
        return apply(target, directEntity, owner, DamageProfile.legacyProjectile(amount), allowSelfDamage,
            allowTeammateDamage);
    }

    /**
     * Shared owner/team gate for hostile side effects such as knockback, fire,
     * and status conditions that are deliberately resolved outside direct
     * damage. It does not inspect invulnerability frames.
     */
    public static boolean isTargetAllowed(Entity target, Entity owner, boolean allowFriendlyFire) {
        Objects.requireNonNull(target, "target");
        return canHarm(target, owner, allowFriendlyFire, allowFriendlyFire);
    }

    /**
     * A rejected non-empty DamageProfile must not become a route around the
     * owner/team and damage-immunity policy through knockback or debuffs.
     * Empty profiles remain valid carriers for intentionally status-only
     * Noita effects, whose target admission was checked separately.
     */
    public static boolean shouldApplyHarmfulFollowUp(DamageProfile profile, boolean directDamageApplied) {
        Objects.requireNonNull(profile, "profile");
        return profile.isEmpty() || directDamageApplied;
    }

    /** Public for tests and future effect executors that need one frozen channel. */
    public static DamageSource source(ServerWorld world, DamageChannel channel, Entity directEntity, Entity owner) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(directEntity, "directEntity");
        RegistryEntry<DamageType> type = world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE)
            .entryOf(DAMAGE_TYPES.get(channel));
        return new DamageSource(type, directEntity, owner);
    }

    private static boolean canHarm(
        Entity target, Entity owner, boolean allowSelfDamage, boolean allowTeammateDamage
    ) {
        if (owner == null) {
            return true;
        }
        if (target == owner) {
            return allowSelfDamage;
        }
        return allowTeammateDamage || !target.isTeammate(owner);
    }

    /** Preserves existing Noita-style repeated-hit behavior without exposing it to entities. */
    private static boolean applyOne(Entity target, DamageSource source, float amount) {
        if (!(target instanceof LivingEntity livingTarget)) {
            return target.damage(source, amount);
        }
        int previousTimeUntilRegen = livingTarget.timeUntilRegen;
        livingTarget.timeUntilRegen = 0;
        boolean damaged = livingTarget.damage(source, amount);
        if (damaged) {
            livingTarget.timeUntilRegen = 0;
        } else {
            livingTarget.timeUntilRegen = previousTimeUntilRegen;
        }
        return damaged;
    }

    private static Map<DamageChannel, RegistryKey<DamageType>> damageTypes() {
        EnumMap<DamageChannel, RegistryKey<DamageType>> keys = new EnumMap<>(DamageChannel.class);
        for (DamageChannel channel : DamageChannel.values()) {
            keys.put(channel, RegistryKey.of(RegistryKeys.DAMAGE_TYPE, MCNoita.id(channel.damageTypePath())));
        }
        return Map.copyOf(keys);
    }
}
