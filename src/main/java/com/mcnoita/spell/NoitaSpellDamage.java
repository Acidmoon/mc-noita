package com.mcnoita.spell;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;

public final class NoitaSpellDamage {
    private NoitaSpellDamage() {
    }

    public static boolean apply(Entity target, DamageSource source, float amount) {
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
}
