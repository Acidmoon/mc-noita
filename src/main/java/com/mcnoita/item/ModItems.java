package com.mcnoita.item;

import com.mcnoita.MCNoita;
import com.mcnoita.spell.NoitaSpellTemplate;
import com.mcnoita.spell.NoitaSpellType;
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

    public static final NoitaSpellItem SPARK_BOLT = register("spark_bolt", new NoitaSpellItem(
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .manaDrain(5)
            .damage(3.0f)
            .explosionRadius(2.0f)
            .speed(1600.0f)
            .castDelaySeconds(0.05f)
            .rechargeTimeSeconds(0.03f)
            .spreadModifierDegrees(-1.0f)
            .criticalChancePercent(5.0f)
            .lifetimeTicks(40)
            .build(),
        new Item.Settings().maxCount(16)
    ));
    public static final NoitaSpellItem BOUNCING_BURST = register("bouncing_burst", new NoitaSpellItem(
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .manaDrain(5)
            .damage(3.0f)
            .spreadDegrees(2.0f)
            .speed(400.0f)
            .castDelaySeconds(-0.03f)
            .rechargeTimeSeconds(0.0f)
            .lifetimeTicks(80)
            .build(),
        new Item.Settings().maxCount(16)
    ));
    public static final NoitaSpellItem LIGHT_BULLET = register("light_bullet", new NoitaSpellItem(
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .manaDrain(5)
            .damage(2.0f)
            .spreadDegrees(0.0f)
            .speed(1000.0f)
            .castDelaySeconds(-0.02f)
            .rechargeTimeSeconds(0.0f)
            .lifetimeTicks(40)
            .build(),
        new Item.Settings().maxCount(16)
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
