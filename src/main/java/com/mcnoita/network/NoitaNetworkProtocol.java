package com.mcnoita.network;

import java.util.Objects;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

public final class NoitaNetworkProtocol {
    public static final int VERSION = 1;
    public static final int MAX_CAST_PACKET_BYTES = 24;
    public static final int MAX_HOVER_PACKET_BYTES = 24;
    public static final int MAX_HUD_PACKET_BYTES = 32;

    private NoitaNetworkProtocol() {
    }

    /**
     * This detects a client request made against an obsolete stack, not a proof
     * of authority. The server still reads the held stack and evaluates every
     * mana, cooldown, spell, and projectile value from its own current state.
     */
    public static int wandStateHash(ItemStack stack) {
        return Objects.hash(Registries.ITEM.getId(stack.getItem()), stack.getCount(), stack.getNbt());
    }
}
