package com.mcnoita.persistence;

import com.mcnoita.MCNoita;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

/**
 * Centralizes the version boundary for every Noita-owned persistent structure.
 * Missing versions are legacy v0. Future data is never rewritten because doing so
 * would destroy a newer server's state when a player briefly joins with this build.
 */
public final class NoitaNbtSchema {
    public static final String VERSION_KEY = "SchemaVersion";
    public static final int CURRENT_VERSION = 5;

    private NoitaNbtSchema() {
    }

    /**
     * Returns the source version before migration. A present value with the
     * wrong NBT type is corrupt rather than an implicit v0 save.
     */
    public static int readStoredVersion(NbtCompound data) {
        if (!data.contains(VERSION_KEY)) {
            return 0;
        }
        return data.contains(VERSION_KEY, NbtElement.NUMBER_TYPE) ? data.getInt(VERSION_KEY) : -1;
    }

    public static boolean migrateToCurrent(NbtCompound data, Kind kind) {
        int version = readStoredVersion(data);
        if (version < 0 || version > CURRENT_VERSION) {
            MCNoita.LOGGER.warn("Refusing {} NBT schema version {} (supported through {})", kind.id, version, CURRENT_VERSION);
            return false;
        }

        while (version < CURRENT_VERSION) {
            switch (version) {
                case 0 -> migrateV0ToV1(data, kind);
                case 1 -> migrateV1ToV2(data, kind);
                case 2 -> migrateV2ToV3(data, kind);
                case 3 -> migrateV3ToV4(data, kind);
                case 4 -> migrateV4ToV5(data, kind);
                default -> throw new IllegalStateException("No migration registered for " + kind.id + " v" + version);
            }
            version++;
        }
        return true;
    }

    public static void writeCurrentVersion(NbtCompound data) {
        data.putInt(VERSION_KEY, CURRENT_VERSION);
    }

    private static void migrateV0ToV1(NbtCompound data, Kind kind) {
        // v0 used the same field names but did not state its schema explicitly.
        // Keeping the migration explicit makes the future v1 -> v2 chain auditable.
        data.putInt(VERSION_KEY, 1);
        MCNoita.LOGGER.debug("Migrated {} NBT from v0 to v1", kind.id);
    }

    private static void migrateV1ToV2(NbtCompound data, Kind kind) {
        if (kind == Kind.CAST_STATE) {
            // G01 persisted an empty Deck until Recharge elapsed. G02 instead
            // persists the seed-resolved next Deck at cast commit, so the adapter
            // needs one explicit marker to recognize and rebuild old G01 reloads.
            data.putBoolean("G02ReloadPrepared", false);
        }
        data.putInt(VERSION_KEY, 2);
        MCNoita.LOGGER.debug("Migrated {} NBT from v1 to v2", kind.id);
    }

    private static void migrateV2ToV3(NbtCompound data, Kind kind) {
        if ((kind == Kind.PROJECTILE_PAYLOAD || kind == Kind.ENTITY)
            && data.contains("TriggerMode", NbtElement.STRING_TYPE)
            && "DEATH".equals(data.getString("TriggerMode"))) {
            // G03 names the internal lifecycle event EXPIRATION. The persisted
            // DEATH spelling remains accepted by the payload decoder as well,
            // but migration makes the compatibility step explicit and auditable.
            data.putString("TriggerMode", "EXPIRATION");
        }
        if (kind == Kind.ENTITY && data.getBoolean("TriggerPayloadReleased")) {
            // Old state had one ambiguous flag. Conservatively preserve it as
            // a completed final release to prevent a save upgrade from firing a
            // one-shot payload again; legacy piercing continuation is not safe.
            data.putBoolean("G03LegacyTriggerReleased", true);
        }
        data.putInt(VERSION_KEY, 3);
        MCNoita.LOGGER.debug("Migrated {} NBT from v2 to v3", kind.id);
    }

    private static void migrateV3ToV4(NbtCompound data, Kind kind) {
        if (kind == Kind.PROJECTILE_PAYLOAD) {
            // Historic scalar payload damage was always interpreted by the
            // projectile path. Do not infer elemental damage from item names
            // while migrating frozen data from an older catalog snapshot.
            double legacyDamage = data.contains("Damage", NbtElement.NUMBER_TYPE) ? data.getDouble("Damage") : 0.0;
            data.put(DamageProfileNbtCodec.PROFILE_KEY,
                DamageProfileNbtCodec.legacyProjectileProfile(legacyDamage));
        }
        data.putInt(VERSION_KEY, 4);
        MCNoita.LOGGER.debug("Migrated {} NBT from v3 to v4", kind.id);
    }

    private static void migrateV4ToV5(NbtCompound data, Kind kind) {
        if (kind == Kind.SPELL_JOB) {
            // v4 had no durable job grammar. Mark an attempted legacy job inert
            // rather than guessing its world side effects during a save upgrade.
            data.putString("State", "INERT");
            data.putString("StateReason", "legacy job record has no frozen v5 definition");
        }
        data.putInt(VERSION_KEY, 5);
        MCNoita.LOGGER.debug("Migrated {} NBT from v4 to v5", kind.id);
    }

    public enum Kind {
        WAND_TEMPLATE("wand template"),
        WAND_SLOTS("wand slots"),
        CAST_STATE("wand cast state"),
        PROJECTILE_PAYLOAD("projectile payload"),
        ENTITY("entity persistence"),
        SPELL_JOB("spell job persistence");

        private final String id;

        Kind(String id) {
            this.id = id;
        }
    }
}
