package com.mcnoita.wand;

import com.mcnoita.item.NoitaWandItem;
import com.mcnoita.wand.adapter.MinecraftWandAdapter;
import com.mcnoita.wand.server.CastIntent;
import com.mcnoita.wand.server.CastResult;
import com.mcnoita.wand.server.CastTransaction;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;

/**
 * Compatibility facade for the formal server transaction coordinator. No direct
 * cast path may mutate a wand before validate/evaluate/reserve/revalidate pass.
 */
public final class NoitaWandCaster {
    private static final CastTransaction TRANSACTION = CastTransaction.createProduction();

    private NoitaWandCaster() {
    }

    public static boolean canCast(ServerPlayerEntity player) {
        return player != null && TRANSACTION.canCast(player, CastIntent.mainHand(player.getInventory().selectedSlot));
    }

    public static CastResult cast(ServerPlayerEntity player) {
        return cast(player, CastIntent.SERVER_INITIATED_SEQUENCE);
    }

    /** Existing packet fields may supply their sequence without changing the wire format. */
    public static CastResult cast(ServerPlayerEntity player, long sequence) {
        if (player == null) {
            return CastResult.rejected(CastResult.Status.VALIDATION_REJECTED, "player is missing", null, null, null);
        }
        return TRANSACTION.cast(player, new CastIntent(Hand.MAIN_HAND, player.getInventory().selectedSlot, sequence));
    }

    public static CastHudState getHudState(ServerPlayerEntity player) {
        ItemStack wandStack = player.getStackInHand(Hand.MAIN_HAND);
        // HUD polling must never trigger legacy NBT migration on the real
        // stack: that would change a transaction binding without a cast.
        if (!(wandStack.getItem() instanceof NoitaWandItem wandItem) || !wandItem.hasSupportedNbt(wandStack.copy())) {
            return CastHudState.empty();
        }
        long now = player.getServerWorld().getTime();
        MinecraftWandAdapter.LoadedWand loaded = MinecraftWandAdapter.readReadOnly(wandStack, wandItem, now, 0L);
        if (loaded == null) {
            return CastHudState.empty();
        }
        float mana = (float) Math.min(loaded.definition().manaMax(), loaded.state().mana()
            + loaded.elapsed().frames() / 60.0 * loaded.definition().manaChargePerSecond());
        if (!loaded.state().rechargeRemaining().isZero()) {
            int total = Math.max(1, com.mcnoita.wand.adapter.MinecraftTimeAdapter.toMinecraftTicks(loaded.state().rechargeRemaining(), 1));
            return CastHudState.recharge(0, total, mana, loaded.definition().manaMax());
        }
        if (!loaded.state().castDelayRemaining().isZero()) {
            int total = Math.max(1, com.mcnoita.wand.adapter.MinecraftTimeAdapter.toMinecraftTicks(loaded.state().castDelayRemaining(), 1));
            return CastHudState.castDelay(0, total, mana, loaded.definition().manaMax());
        }
        return CastHudState.idle(mana, loaded.definition().manaMax());
    }

    public record CastHudState(int mode, int progressTicks, int totalTicks, float currentMana, int manaMax) {
        public static final int MODE_EMPTY = 0;
        public static final int MODE_CAST_DELAY = 1;
        public static final int MODE_RECHARGE = 2;

        private static CastHudState empty() {
            return new CastHudState(MODE_EMPTY, 0, 0, 0.0f, 0);
        }

        private static CastHudState idle(float currentMana, int manaMax) {
            return new CastHudState(MODE_EMPTY, 0, 0, Math.max(0.0f, currentMana), Math.max(0, manaMax));
        }

        private static CastHudState castDelay(int progressTicks, int totalTicks, float currentMana, int manaMax) {
            return new CastHudState(MODE_CAST_DELAY, Math.max(0, progressTicks), Math.max(1, totalTicks), Math.max(0.0f, currentMana), Math.max(0, manaMax));
        }

        private static CastHudState recharge(int progressTicks, int totalTicks, float currentMana, int manaMax) {
            return new CastHudState(MODE_RECHARGE, Math.max(0, progressTicks), Math.max(1, totalTicks), Math.max(0.0f, currentMana), Math.max(0, manaMax));
        }
    }
}
