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
    public static final int CURRENT_VERSION = 1;

    private NoitaNbtSchema() {
    }

    public static boolean migrateToCurrent(NbtCompound data, Kind kind) {
        int version = data.contains(VERSION_KEY, NbtElement.NUMBER_TYPE) ? data.getInt(VERSION_KEY) : 0;
        if (version < 0 || version > CURRENT_VERSION) {
            MCNoita.LOGGER.warn("Refusing {} NBT schema version {} (supported through {})", kind.id, version, CURRENT_VERSION);
            return false;
        }

        while (version < CURRENT_VERSION) {
            switch (version) {
                case 0 -> migrateV0ToV1(data, kind);
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

    public enum Kind {
        WAND_TEMPLATE("wand template"),
        WAND_SLOTS("wand slots"),
        CAST_STATE("wand cast state"),
        PROJECTILE_PAYLOAD("projectile payload"),
        ENTITY("entity persistence");

        private final String id;

        Kind(String id) {
            this.id = id;
        }
    }
}
