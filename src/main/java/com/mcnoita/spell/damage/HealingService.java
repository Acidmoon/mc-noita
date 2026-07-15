package com.mcnoita.spell.damage;

import java.util.Objects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;

/** Server-side healing boundary. Healing is never represented as negative damage. */
public final class HealingService {
    private HealingService() {
    }

    /**
     * Owners and their team may receive healing by default; non-allies require
     * the caller to opt in. This keeps a healing projectile from becoming an
     * accidental hostile-target heal while preserving self-healing.
     */
    public static boolean heal(Entity target, Entity owner, double amount, boolean allowNonAllies) {
        Objects.requireNonNull(target, "target");
        if (!(target instanceof LivingEntity livingTarget) || !(target.getWorld() instanceof ServerWorld)
            || !Double.isFinite(amount) || amount <= 0.0) {
            return false;
        }
        if (!isTargetAllowed(target, owner, allowNonAllies)) {
            return false;
        }
        livingTarget.heal((float) Math.min(Float.MAX_VALUE, amount));
        return true;
    }

    /** Shared target gate for healing effects that also carry non-healing side effects. */
    public static boolean isTargetAllowed(Entity target, Entity owner, boolean allowNonAllies) {
        Objects.requireNonNull(target, "target");
        if (!(target instanceof LivingEntity)) {
            return false;
        }
        if (owner == null) {
            return allowNonAllies;
        }
        return target == owner || target.isTeammate(owner) || allowNonAllies;
    }
}
