package com.mcnoita.item;

import com.mcnoita.MCNoita;
import com.mcnoita.spell.NoitaSpellTemplate;
import com.mcnoita.spell.NoitaSpellTriggerMode;
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
    public static final NoitaSpellItem SPARK_BOLT_TRIGGER = register("spark_bolt_trigger", new NoitaSpellItem(
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .manaDrain(10)
            .damage(3.0f)
            .explosionRadius(2.0f)
            .speed(1600.0f)
            .castDelaySeconds(0.05f)
            .rechargeTimeSeconds(0.03f)
            .criticalChancePercent(5.0f)
            .lifetimeTicks(40)
            .triggerMode(NoitaSpellTriggerMode.HIT)
            .triggerDrawCount(1)
            .build(),
        new Item.Settings().maxCount(16)
    ));
    public static final NoitaSpellItem SPARK_BOLT_TIMER = register("spark_bolt_timer", new NoitaSpellItem(
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .manaDrain(10)
            .damage(3.0f)
            .explosionRadius(2.0f)
            .speed(1600.0f)
            .castDelaySeconds(0.05f)
            .rechargeTimeSeconds(0.03f)
            .criticalChancePercent(5.0f)
            .lifetimeTicks(40)
            .triggerMode(NoitaSpellTriggerMode.TIMER)
            .triggerDrawCount(1)
            .triggerDelayTicks(10)
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
    public static final NoitaSpellItem BOMB = register("bomb", new NoitaSpellItem(
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(3)
            .manaDrain(25)
            .damage(125.0f)
            .explosionRadius(4.0f)
            .speed(200.0f)
            .castDelaySeconds(1.67f)
            .rechargeTimeSeconds(0.0f)
            .spreadDegrees(0.0f)
            .lifetimeTicks(60)
            .friendlyFire(true)
            .build(),
        new Item.Settings().maxCount(16)
    ));
    public static final NoitaSpellItem BOMB_DEATH_TRIGGER = register("bomb_death_trigger", new NoitaSpellItem(
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(3)
            .manaDrain(35)
            .damage(125.0f)
            .explosionRadius(4.0f)
            .speed(200.0f)
            .castDelaySeconds(1.67f)
            .rechargeTimeSeconds(0.0f)
            .spreadDegrees(0.0f)
            .lifetimeTicks(60)
            .friendlyFire(true)
            .triggerMode(NoitaSpellTriggerMode.DEATH)
            .triggerDrawCount(1)
            .build(),
        new Item.Settings().maxCount(16)
    ));
    public static final NoitaSpellItem DOUBLE_SPELL = register("double_spell", new NoitaSpellItem(
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.MULTICAST)
            .manaDrain(0)
            .drawCount(2)
            .build(),
        new Item.Settings().maxCount(16)
    ));
    public static final NoitaSpellItem DUPLICATE = register("duplicate", new NoitaSpellItem(
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.OTHER)
            .manaDrain(250)
            .castDelaySeconds(20.0f / 60.0f)
            .rechargeTimeSeconds(20.0f / 60.0f)
            .build(),
        new Item.Settings().maxCount(16)
    ));
    public static final NoitaSpellItem WAND_REFRESH = register("wand_refresh", new NoitaSpellItem(
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.UTILITY)
            .manaDrain(20)
            .rechargeTimeSeconds(-25.0f / 60.0f)
            .build(),
        new Item.Settings().maxCount(16)
    ));
    public static final NoitaSpellItem ALPHA = register("alpha", new NoitaSpellItem(
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.OTHER)
            .manaDrain(40)
            .castDelaySeconds(15.0f / 60.0f)
            .build(),
        new Item.Settings().maxCount(16)
    ));
    public static final NoitaSpellItem GAMMA = register("gamma", new NoitaSpellItem(
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.OTHER)
            .manaDrain(40)
            .castDelaySeconds(15.0f / 60.0f)
            .build(),
        new Item.Settings().maxCount(16)
    ));
    public static final NoitaSpellItem LIGHT = register("light", new NoitaSpellItem(
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE_MODIFIER)
            .manaDrain(0)
            .trailLightStacks(1)
            .build(),
        new Item.Settings().maxCount(16)
    ));
    public static final NoitaSpellItem ADD_MANA = register("add_mana", new NoitaSpellItem(
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE_MODIFIER)
            .manaDrain(-30)
            .castDelaySeconds(10.0f / 60.0f)
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
