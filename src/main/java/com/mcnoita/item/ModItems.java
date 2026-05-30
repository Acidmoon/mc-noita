package com.mcnoita.item;

import com.mcnoita.MCNoita;
import com.mcnoita.wand.NoitaWandTemplate;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModItems {
    public static final NoitaWandItem STARTER_WAND = register("starter_wand", new NoitaWandItem(
        NoitaWandTemplate.builder()
            .shuffle(false)
            .spellsPerCast(1)
            .castDelaySeconds(0.17f)
            .rechargeTimeSeconds(0.50f)
            .manaMax(100)
            .manaChargeSpeed(50)
            .capacity(4)
            .spreadDegrees(0.0f)
            .speedMultiplier(1.0f)
            .build(),
        new Item.Settings().maxCount(1)
    ));

    private ModItems() {
    }

    public static void register() {
        MCNoita.LOGGER.info("Registering MC Noita items");
    }

    private static <T extends Item> T register(String path, T item) {
        return Registry.register(Registries.ITEM, MCNoita.id(path), item);
    }
}
