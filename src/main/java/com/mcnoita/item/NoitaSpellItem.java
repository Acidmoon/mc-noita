package com.mcnoita.item;

import com.mcnoita.spell.NoitaSpellTemplate;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

public class NoitaSpellItem extends Item {
    private static final String REMAINING_USES_KEY = "RemainingUses";
    private final NoitaSpellTemplate template;

    public NoitaSpellItem(NoitaSpellTemplate template, Settings settings) {
        super(settings);
        this.template = template;
    }

    public NoitaSpellTemplate getTemplate() {
        return template;
    }

    public static int getRemainingUses(ItemStack stack) {
        if (!(stack.getItem() instanceof NoitaSpellItem spellItem)) {
            return 0;
        }
        if (!spellItem.template.hasLimitedUses()) {
            return -1;
        }
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(REMAINING_USES_KEY, NbtElement.INT_TYPE)) {
            return spellItem.template.maxUses();
        }
        return Math.max(0, nbt.getInt(REMAINING_USES_KEY));
    }

    public static void consumeUse(ItemStack stack) {
        if (!(stack.getItem() instanceof NoitaSpellItem spellItem) || !spellItem.template.hasLimitedUses()) {
            return;
        }
        stack.getOrCreateNbt().putInt(REMAINING_USES_KEY, Math.max(0, getRemainingUses(stack) - 1));
    }

    public static boolean hasUsesRemaining(ItemStack stack) {
        return getRemainingUses(stack) != 0;
    }
}
