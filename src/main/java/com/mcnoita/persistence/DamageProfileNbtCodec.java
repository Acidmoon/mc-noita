package com.mcnoita.persistence;

import com.mcnoita.spell.damage.DamageChannel;
import com.mcnoita.spell.damage.DamageProfile;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

/**
 * Versioned payload storage for frozen multi-channel direct damage. Keeping
 * this codec in persistence prevents the pure damage model from depending on
 * Minecraft NBT while making migration and normal decoding use one grammar.
 */
public final class DamageProfileNbtCodec {
    public static final String PROFILE_KEY = "DamageProfile";

    private DamageProfileNbtCodec() {
    }

    public static NbtCompound toNbt(DamageProfile profile) {
        Objects.requireNonNull(profile, "profile");
        NbtCompound nbt = new NbtCompound();
        for (DamageChannel channel : DamageChannel.values()) {
            double amount = profile.amount(channel);
            if (amount > 0.0) {
                nbt.putDouble(channel.name(), amount);
            }
        }
        return nbt;
    }

    /**
     * Rejects unknown, nonnumeric, nonfinite, negative, and oversized channel
     * amounts before a persisted payload reaches an executor.
     */
    public static DamageProfile fromNbt(NbtCompound nbt) {
        Objects.requireNonNull(nbt, "nbt");
        EnumMap<DamageChannel, Double> amounts = new EnumMap<>(DamageChannel.class);
        for (String key : nbt.getKeys()) {
            DamageChannel channel;
            try {
                channel = DamageChannel.valueOf(key);
            } catch (IllegalArgumentException failure) {
                throw new IllegalArgumentException("unknown damage channel " + key, failure);
            }
            if (!nbt.contains(key, NbtElement.NUMBER_TYPE)) {
                throw new IllegalArgumentException("damage channel " + key + " must be numeric");
            }
            double amount = nbt.getDouble(key);
            if (!Double.isFinite(amount) || amount < 0.0
                || amount > NoitaNbtLimits.MAX_ABSOLUTE_PROJECTILE_DAMAGE) {
                throw new IllegalArgumentException("damage channel " + key + " is outside frozen payload bounds");
            }
            if (amount > 0.0) {
                amounts.put(channel, amount);
            }
        }
        return amounts.isEmpty() ? DamageProfile.EMPTY : new DamageProfile(Map.copyOf(amounts));
    }

    /** Migration bridge for historic scalar payload damage. */
    public static NbtCompound legacyProjectileProfile(double legacyDamage) {
        double bounded = Double.isFinite(legacyDamage)
            ? Math.max(0.0, Math.min(NoitaNbtLimits.MAX_ABSOLUTE_PROJECTILE_DAMAGE, legacyDamage)) : 0.0;
        return toNbt(DamageProfile.legacyProjectile(bounded));
    }
}
