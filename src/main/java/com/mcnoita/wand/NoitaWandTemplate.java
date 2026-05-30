package com.mcnoita.wand;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;

public record NoitaWandTemplate(
    boolean shuffle,
    int spellsPerCast,
    float castDelaySeconds,
    float rechargeTimeSeconds,
    int manaMax,
    int manaChargeSpeed,
    int capacity,
    float spreadDegrees,
    List<Identifier> alwaysCastSpells,
    float speedMultiplier
) {
    private static final String SHUFFLE_KEY = "shuffle";
    private static final String SPELLS_PER_CAST_KEY = "spells_per_cast";
    private static final String CAST_DELAY_SECONDS_KEY = "cast_delay_seconds";
    private static final String RECHARGE_TIME_SECONDS_KEY = "recharge_time_seconds";
    private static final String MANA_MAX_KEY = "mana_max";
    private static final String MANA_CHARGE_SPEED_KEY = "mana_charge_speed";
    private static final String CAPACITY_KEY = "capacity";
    private static final String SPREAD_DEGREES_KEY = "spread_degrees";
    private static final String ALWAYS_CAST_SPELLS_KEY = "always_cast_spells";
    private static final String SPEED_MULTIPLIER_KEY = "speed_multiplier";

    public NoitaWandTemplate {
        if (spellsPerCast < 1) {
            throw new IllegalArgumentException("spellsPerCast must be at least 1");
        }
        if (castDelaySeconds < 0.0f) {
            throw new IllegalArgumentException("castDelaySeconds must not be negative");
        }
        if (rechargeTimeSeconds < 0.0f) {
            throw new IllegalArgumentException("rechargeTimeSeconds must not be negative");
        }
        if (manaMax < 0) {
            throw new IllegalArgumentException("manaMax must not be negative");
        }
        if (manaChargeSpeed < 0) {
            throw new IllegalArgumentException("manaChargeSpeed must not be negative");
        }
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be at least 1");
        }
        if (speedMultiplier <= 0.0f) {
            throw new IllegalArgumentException("speedMultiplier must be greater than 0");
        }

        alwaysCastSpells = List.copyOf(alwaysCastSpells);
    }

    public static Builder builder() {
        return new Builder();
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean(SHUFFLE_KEY, shuffle);
        nbt.putInt(SPELLS_PER_CAST_KEY, spellsPerCast);
        nbt.putFloat(CAST_DELAY_SECONDS_KEY, castDelaySeconds);
        nbt.putFloat(RECHARGE_TIME_SECONDS_KEY, rechargeTimeSeconds);
        nbt.putInt(MANA_MAX_KEY, manaMax);
        nbt.putInt(MANA_CHARGE_SPEED_KEY, manaChargeSpeed);
        nbt.putInt(CAPACITY_KEY, capacity);
        nbt.putFloat(SPREAD_DEGREES_KEY, spreadDegrees);
        nbt.putFloat(SPEED_MULTIPLIER_KEY, speedMultiplier);

        NbtList alwaysCast = new NbtList();
        for (Identifier spellId : alwaysCastSpells) {
            alwaysCast.add(NbtString.of(spellId.toString()));
        }
        nbt.put(ALWAYS_CAST_SPELLS_KEY, alwaysCast);

        return nbt;
    }

    public static NoitaWandTemplate fromNbt(NbtCompound nbt) {
        Builder builder = builder()
            .shuffle(nbt.getBoolean(SHUFFLE_KEY))
            .spellsPerCast(readPositiveInt(nbt, SPELLS_PER_CAST_KEY, 1))
            .castDelaySeconds(readNonNegativeFloat(nbt, CAST_DELAY_SECONDS_KEY, 0.0f))
            .rechargeTimeSeconds(readNonNegativeFloat(nbt, RECHARGE_TIME_SECONDS_KEY, 0.0f))
            .manaMax(readNonNegativeInt(nbt, MANA_MAX_KEY, 0))
            .manaChargeSpeed(readNonNegativeInt(nbt, MANA_CHARGE_SPEED_KEY, 0))
            .capacity(readPositiveInt(nbt, CAPACITY_KEY, 1))
            .spreadDegrees(nbt.getFloat(SPREAD_DEGREES_KEY))
            .speedMultiplier(Math.max(0.01f, nbt.getFloat(SPEED_MULTIPLIER_KEY)));

        NbtList alwaysCast = nbt.getList(ALWAYS_CAST_SPELLS_KEY, NbtElement.STRING_TYPE);
        for (int i = 0; i < alwaysCast.size(); i++) {
            Identifier spellId = Identifier.tryParse(alwaysCast.getString(i));
            if (spellId != null) {
                builder.addAlwaysCastSpell(spellId);
            }
        }

        return builder.build();
    }

    private static int readPositiveInt(NbtCompound nbt, String key, int fallback) {
        return nbt.contains(key, NbtElement.NUMBER_TYPE) ? Math.max(1, nbt.getInt(key)) : fallback;
    }

    private static int readNonNegativeInt(NbtCompound nbt, String key, int fallback) {
        return nbt.contains(key, NbtElement.NUMBER_TYPE) ? Math.max(0, nbt.getInt(key)) : fallback;
    }

    private static float readNonNegativeFloat(NbtCompound nbt, String key, float fallback) {
        return nbt.contains(key, NbtElement.NUMBER_TYPE) ? Math.max(0.0f, nbt.getFloat(key)) : fallback;
    }

    public static final class Builder {
        private boolean shuffle;
        private int spellsPerCast = 1;
        private float castDelaySeconds = 0.17f;
        private float rechargeTimeSeconds = 0.5f;
        private int manaMax = 100;
        private int manaChargeSpeed = 50;
        private int capacity = 4;
        private float spreadDegrees;
        private final List<Identifier> alwaysCastSpells = new ArrayList<>();
        private float speedMultiplier = 1.0f;

        private Builder() {
        }

        public Builder shuffle(boolean shuffle) {
            this.shuffle = shuffle;
            return this;
        }

        public Builder spellsPerCast(int spellsPerCast) {
            this.spellsPerCast = spellsPerCast;
            return this;
        }

        public Builder castDelaySeconds(float castDelaySeconds) {
            this.castDelaySeconds = castDelaySeconds;
            return this;
        }

        public Builder rechargeTimeSeconds(float rechargeTimeSeconds) {
            this.rechargeTimeSeconds = rechargeTimeSeconds;
            return this;
        }

        public Builder manaMax(int manaMax) {
            this.manaMax = manaMax;
            return this;
        }

        public Builder manaChargeSpeed(int manaChargeSpeed) {
            this.manaChargeSpeed = manaChargeSpeed;
            return this;
        }

        public Builder capacity(int capacity) {
            this.capacity = capacity;
            return this;
        }

        public Builder spreadDegrees(float spreadDegrees) {
            this.spreadDegrees = spreadDegrees;
            return this;
        }

        public Builder addAlwaysCastSpell(Identifier spellId) {
            this.alwaysCastSpells.add(spellId);
            return this;
        }

        public Builder speedMultiplier(float speedMultiplier) {
            this.speedMultiplier = speedMultiplier;
            return this;
        }

        public NoitaWandTemplate build() {
            return new NoitaWandTemplate(
                shuffle,
                spellsPerCast,
                castDelaySeconds,
                rechargeTimeSeconds,
                manaMax,
                manaChargeSpeed,
                capacity,
                spreadDegrees,
                alwaysCastSpells,
                speedMultiplier
            );
        }
    }
}
