package com.mcnoita.wand.adapter;

import com.mcnoita.item.NoitaSpellItem;
import com.mcnoita.item.NoitaWandItem;
import com.mcnoita.wand.eval.ExternalSpellPool;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;

/** Builds Zeta candidates authoritatively from other server-side hotbar wands. */
public final class MinecraftExternalSpellPoolAdapter {
    private static final int HOTBAR_SIZE = 9;
    private static final int MAX_EXTERNAL_CANDIDATES = 256;

    private MinecraftExternalSpellPoolAdapter() {
    }

    public static ExternalSpellPool fromOtherHotbarWands(ServerPlayerEntity player, ItemStack activeWand) {
        List<String> spellIds = new ArrayList<>();
        for (int slot = 0; slot < HOTBAR_SIZE && spellIds.size() < MAX_EXTERNAL_CANDIDATES; slot++) {
            if (slot == player.getInventory().selectedSlot) {
                continue;
            }
            ItemStack candidateWand = player.getInventory().getStack(slot);
            if (candidateWand == activeWand || candidateWand.isEmpty()
                || !(candidateWand.getItem() instanceof NoitaWandItem wandItem)
                || !wandItem.hasSupportedNbt(candidateWand)) {
                continue;
            }
            int capacity = wandItem.getTemplate(candidateWand).capacity();
            for (ItemStack spellStack : NoitaWandItem.getSpellStacks(candidateWand, capacity)) {
                if (spellIds.size() >= MAX_EXTERNAL_CANDIDATES) {
                    break;
                }
                if (!spellStack.isEmpty() && spellStack.getItem() instanceof NoitaSpellItem) {
                    String id = Registries.ITEM.getId(spellStack.getItem()).toString();
                    if (Registries.ITEM.containsId(Registries.ITEM.getId(spellStack.getItem()))) {
                        spellIds.add(id);
                    }
                }
            }
        }
        return new ExternalSpellPool(spellIds);
    }
}
