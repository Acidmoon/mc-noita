package com.mcnoita.wand;

import com.mcnoita.item.NoitaWandItem;
import com.mcnoita.spell.action.SpellCatalog;
import com.mcnoita.spell.exec.MinecraftEffectExecutor;
import com.mcnoita.spell.plan.ResolvedCast;
import com.mcnoita.wand.adapter.LegacySpellCatalogAdapter;
import com.mcnoita.wand.adapter.MinecraftWandAdapter;
import com.mcnoita.wand.adapter.MinecraftExternalSpellPoolAdapter;
import com.mcnoita.wand.eval.WandCastEvaluator;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;

/**
 * Server orchestration boundary. It validates and converts the held ItemStack,
 * commits a successful pure result, then executes its already-frozen plan.
 */
public final class NoitaWandCaster {
    private static final WandCastEvaluator EVALUATOR = new WandCastEvaluator();

    private NoitaWandCaster() {
    }

    public static boolean canCast(ServerPlayerEntity player) {
        ItemStack wandStack = player.getMainHandStack();
        return wandStack.getItem() instanceof NoitaWandItem wandItem
            && wandItem.hasSupportedNbt(wandStack)
            && MinecraftWandAdapter.canCast(wandStack, player.getServerWorld().getTime());
    }

    public static void cast(ServerPlayerEntity player) {
        ItemStack wandStack = player.getStackInHand(Hand.MAIN_HAND);
        if (!(wandStack.getItem() instanceof NoitaWandItem wandItem) || !wandItem.hasSupportedNbt(wandStack)) {
            return;
        }
        long now = player.getServerWorld().getTime();
        if (!MinecraftWandAdapter.canCast(wandStack, now)) {
            return;
        }

        long randomSeed = player.getRandom().nextLong();
        MinecraftWandAdapter.LoadedWand loaded = MinecraftWandAdapter.read(wandStack, wandItem, now, randomSeed);
        if (loaded == null) {
            return;
        }
        SpellCatalog catalog = LegacySpellCatalogAdapter.createCatalog();
        ResolvedCast resolved = EVALUATOR.evaluate(loaded.definition(), loaded.state(), catalog, loaded.elapsed(),
            randomSeed, MinecraftExternalSpellPoolAdapter.fromOtherHotbarWands(player, wandStack));
        if (resolved.status() != ResolvedCast.Status.ACCEPTED) {
            return;
        }

        // State is durable before any entity allocation. Executor failures never roll it back.
        MinecraftWandAdapter.write(wandStack, loaded, resolved.nextState(), now);
        MinecraftEffectExecutor.execute(player, resolved);
    }

    public static CastHudState getHudState(ServerPlayerEntity player) {
        ItemStack wandStack = player.getStackInHand(Hand.MAIN_HAND);
        if (!(wandStack.getItem() instanceof NoitaWandItem wandItem) || !wandItem.hasSupportedNbt(wandStack)) {
            return CastHudState.empty();
        }
        long now = player.getServerWorld().getTime();
        MinecraftWandAdapter.LoadedWand loaded = MinecraftWandAdapter.read(wandStack, wandItem, now, 0L);
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
