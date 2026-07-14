package com.mcnoita.network;

import java.util.Objects;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import com.mcnoita.wand.adapter.MinecraftWandAdapter;

public final class NoitaNetworkProtocol {
    public static final int VERSION = 2;
    public static final int CATALOG_HASH_LENGTH = 64;
    public static final int MAX_CAST_PACKET_BYTES = 128;
    public static final int MAX_HOVER_PACKET_BYTES = 24;
    public static final int MAX_HUD_PACKET_BYTES = 128;

    private NoitaNetworkProtocol() {
    }

    /**
     * This detects a client request made against an obsolete stack, not a proof
     * of authority. The server still reads the held stack and evaluates every
     * mana, cooldown, spell, and projectile value from its own current state.
     */
    public static int wandStateHash(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        return Objects.hash(
            Registries.ITEM.getId(stack.getItem()), stack.getCount(),
            MinecraftWandAdapter.canonicalNbtStateHash(stack.getNbt())
        );
    }

    /** Reject malformed catalog identities before a packet can reach server state. */
    public static boolean isCanonicalCatalogHash(String hash) {
        if (hash == null || hash.length() != CATALOG_HASH_LENGTH) {
            return false;
        }
        for (int index = 0; index < hash.length(); index++) {
            char value = hash.charAt(index);
            if (!((value >= '0' && value <= '9') || (value >= 'a' && value <= 'f'))) {
                return false;
            }
        }
        return true;
    }
}
