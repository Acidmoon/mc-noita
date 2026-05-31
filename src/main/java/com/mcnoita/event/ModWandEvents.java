package com.mcnoita.event;

import com.mcnoita.item.NoitaWandItem;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;

public final class ModWandEvents {
    private ModWandEvents() {
    }

    public static void register() {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) ->
            isCastingWand(player.getStackInHand(hand)) ? ActionResult.FAIL : ActionResult.PASS
        );
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
            isCastingWand(player.getStackInHand(hand)) ? ActionResult.FAIL : ActionResult.PASS
        );
    }

    private static boolean isCastingWand(ItemStack stack) {
        return stack.getItem() instanceof NoitaWandItem;
    }
}
