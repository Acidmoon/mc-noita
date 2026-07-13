package com.mcnoita.item;

import com.mcnoita.MCNoita;
import com.mcnoita.spell.NoitaModifierEffect;
import com.mcnoita.spell.NoitaProjectileBehavior;
import com.mcnoita.spell.NoitaProjectileSpellSpec;
import com.mcnoita.spell.NoitaSpellTemplate;
import com.mcnoita.spell.NoitaSpellTriggerMode;
import com.mcnoita.spell.NoitaSpellType;
import com.mcnoita.wand.NoitaWandTemplate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModItems {
    private static final List<NoitaProjectileSpellItem> PROJECTILE_SPELLS_MUTABLE = new ArrayList<>();
    public static final List<NoitaProjectileSpellItem> PROJECTILE_SPELLS = Collections.unmodifiableList(PROJECTILE_SPELLS_MUTABLE);
    private static final List<NoitaProjectileSpellItem> STATIC_PROJECTILE_SPELLS_MUTABLE = new ArrayList<>();
    public static final List<NoitaProjectileSpellItem> STATIC_PROJECTILE_SPELLS = Collections.unmodifiableList(STATIC_PROJECTILE_SPELLS_MUTABLE);
    private static final List<NoitaSpellItem> MODIFIER_SPELLS_MUTABLE = new ArrayList<>();
    public static final List<NoitaSpellItem> MODIFIER_SPELLS = Collections.unmodifiableList(MODIFIER_SPELLS_MUTABLE);

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

    public static final NoitaProjectileSpellItem BOMB = registerProjectile(projectileSpec(
        "BOMB", "bomb", "Bomb", "炸弹", NoitaProjectileBehavior.FUSED_EXPLOSIVE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(3)
            .manaDrain(25)
            .explosionRadius(6.0f)
            .speed(200.0f)
            .castDelaySeconds(1.666667f)
            .lifetimeTicks(180)
            .friendlyFire(true)
            .build(),
        1, 0.0f, 0.05f, 0.99f, 0.65f, 1.25f)
    );
    public static final NoitaProjectileSpellItem LIGHT_BULLET = registerProjectile(projectileSpec(
        "LIGHT_BULLET", "spark_bolt", "Spark bolt", "火花弹", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .manaDrain(5)
            .damage(3.0f)
            .explosionRadius(1.0f)
            .speed(800.0f)
            .castDelaySeconds(0.05f)
            .spreadModifierDegrees(-1.0f)
            .criticalChancePercent(5.0f)
            .lifetimeTicks(40)
            .build(),
        1, 0.0f, 0.0f, 0.9932f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem LIGHT_BULLET_TRIGGER = registerProjectile(projectileSpec(
        "LIGHT_BULLET_TRIGGER", "spark_bolt_trigger", "Spark bolt with trigger", "带有触发的火花弹", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(100)
            .manaDrain(10)
            .damage(3.0f)
            .explosionRadius(1.0f)
            .speed(800.0f)
            .castDelaySeconds(0.05f)
            .criticalChancePercent(5.0f)
            .lifetimeTicks(40)
            .triggerMode(NoitaSpellTriggerMode.HIT)
            .triggerDrawCount(1)
            .build(),
        1, 0.0f, 0.0f, 0.9932f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem LIGHT_BULLET_TRIGGER_2 = registerProjectile(projectileSpec(
        "LIGHT_BULLET_TRIGGER_2", "light_bullet_trigger_2", "Spark bolt with double trigger", "带有双重触发的火花弹", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(100)
            .manaDrain(15)
            .damage(3.75f)
            .explosionRadius(2.0f)
            .speed(700.0f)
            .castDelaySeconds(0.066667f)
            .criticalChancePercent(5.0f)
            .lifetimeTicks(40)
            .triggerMode(NoitaSpellTriggerMode.HIT)
            .triggerDrawCount(2)
            .build(),
        1, 0.0f, 0.0f, 0.99f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem LIGHT_BULLET_TIMER = registerProjectile(projectileSpec(
        "LIGHT_BULLET_TIMER", "spark_bolt_timer", "Spark bolt with timer", "带有定时的火花弹", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(100)
            .manaDrain(10)
            .damage(3.0f)
            .explosionRadius(1.0f)
            .speed(800.0f)
            .castDelaySeconds(0.05f)
            .criticalChancePercent(5.0f)
            .lifetimeTicks(40)
            .triggerMode(NoitaSpellTriggerMode.TIMER)
            .triggerDrawCount(1)
            .triggerDelayTicks(10)
            .build(),
        1, 0.0f, 0.0f, 0.9932f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem BULLET = registerProjectile(projectileSpec(
        "BULLET", "bullet", "Magic arrow", "魔法箭", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .manaDrain(20)
            .damage(10.0f)
            .explosionRadius(1.0f)
            .spreadDegrees(2.9f)
            .speed(625.0f)
            .castDelaySeconds(0.066667f)
            .spreadModifierDegrees(2.0f)
            .criticalChancePercent(5.0f)
            .lifetimeTicks(40)
            .build(),
        1, 0.0f, 0.0f, 0.9952f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem BULLET_TRIGGER = registerProjectile(projectileSpec(
        "BULLET_TRIGGER", "bullet_trigger", "Magic arrow with trigger", "带有触发的魔法箭", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(80)
            .manaDrain(35)
            .damage(10.0f)
            .explosionRadius(1.0f)
            .spreadDegrees(2.9f)
            .speed(625.0f)
            .castDelaySeconds(0.066667f)
            .spreadModifierDegrees(2.0f)
            .criticalChancePercent(5.0f)
            .lifetimeTicks(40)
            .triggerMode(NoitaSpellTriggerMode.HIT)
            .triggerDrawCount(1)
            .build(),
        1, 0.0f, 0.0f, 0.9952f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem BULLET_TIMER = registerProjectile(projectileSpec(
        "BULLET_TIMER", "bullet_timer", "Magic arrow with timer", "带有定时的魔法箭", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(80)
            .manaDrain(35)
            .damage(10.0f)
            .explosionRadius(1.0f)
            .spreadDegrees(2.9f)
            .speed(625.0f)
            .castDelaySeconds(0.066667f)
            .spreadModifierDegrees(2.0f)
            .criticalChancePercent(5.0f)
            .lifetimeTicks(40)
            .triggerMode(NoitaSpellTriggerMode.TIMER)
            .triggerDrawCount(1)
            .triggerDelayTicks(10)
            .build(),
        1, 0.0f, 0.05f, 0.9952f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem HEAVY_BULLET = registerProjectile(projectileSpec(
        "HEAVY_BULLET", "heavy_bullet", "Magic bolt", "魔法弹", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(50)
            .manaDrain(30)
            .damage(12.75f)
            .explosionRadius(1.0f)
            .spreadDegrees(2.9f)
            .speed(675.0f)
            .castDelaySeconds(0.116667f)
            .spreadModifierDegrees(5.0f)
            .criticalChancePercent(5.0f)
            .lifetimeTicks(30)
            .build(),
        1, 0.0f, 0.05f, 0.9988f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem HEAVY_BULLET_TRIGGER = registerProjectile(projectileSpec(
        "HEAVY_BULLET_TRIGGER", "heavy_bullet_trigger", "Magic bolt with trigger", "带有触发的魔法弹", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(50)
            .manaDrain(40)
            .damage(12.75f)
            .explosionRadius(1.0f)
            .spreadDegrees(2.9f)
            .speed(675.0f)
            .castDelaySeconds(0.116667f)
            .spreadModifierDegrees(5.0f)
            .criticalChancePercent(5.0f)
            .lifetimeTicks(30)
            .triggerMode(NoitaSpellTriggerMode.HIT)
            .triggerDrawCount(1)
            .build(),
        1, 0.0f, 0.05f, 0.9988f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem HEAVY_BULLET_TIMER = registerProjectile(projectileSpec(
        "HEAVY_BULLET_TIMER", "heavy_bullet_timer", "Magic bolt with timer", "带有定时的魔法弹", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(50)
            .manaDrain(40)
            .damage(12.75f)
            .explosionRadius(1.0f)
            .spreadDegrees(2.9f)
            .speed(675.0f)
            .castDelaySeconds(0.116667f)
            .spreadModifierDegrees(5.0f)
            .criticalChancePercent(5.0f)
            .lifetimeTicks(30)
            .triggerMode(NoitaSpellTriggerMode.TIMER)
            .triggerDrawCount(1)
            .triggerDelayTicks(10)
            .build(),
        1, 0.0f, 0.05f, 0.9988f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem AIR_BULLET = registerProjectile(projectileSpec(
        "AIR_BULLET", "air_bullet", "Burst of air", "强气流", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(120)
            .manaDrain(5)
            .damage(5.75f)
            .speed(400.0f)
            .castDelaySeconds(0.05f)
            .spreadModifierDegrees(-2.0f)
            .lifetimeTicks(40)
            .build(),
        1, 0.0f, 0.0f, 0.9932f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem SLOW_BULLET = registerProjectile(projectileSpec(
        "SLOW_BULLET", "slow_bullet", "Energy orb", "能量球", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(50)
            .manaDrain(30)
            .damage(11.25f)
            .explosionRadius(1.5f)
            .spreadDegrees(1.7f)
            .speed(210.0f)
            .castDelaySeconds(0.1f)
            .spreadModifierDegrees(3.6f)
            .lifetimeTicks(50)
            .build(),
        1, 0.0f, 0.0f, 0.99f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem SLOW_BULLET_TRIGGER = registerProjectile(projectileSpec(
        "SLOW_BULLET_TRIGGER", "slow_bullet_trigger", "Energy orb with a trigger", "带有触发的能量球", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(50)
            .manaDrain(50)
            .damage(11.25f)
            .explosionRadius(1.5f)
            .spreadDegrees(1.7f)
            .speed(210.0f)
            .castDelaySeconds(0.416667f)
            .spreadModifierDegrees(10.0f)
            .lifetimeTicks(50)
            .triggerMode(NoitaSpellTriggerMode.HIT)
            .triggerDrawCount(1)
            .build(),
        1, 0.0f, 0.0f, 0.99f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem SLOW_BULLET_TIMER = registerProjectile(projectileSpec(
        "SLOW_BULLET_TIMER", "slow_bullet_timer", "Energy orb with a timer", "带有定时的能量球", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(50)
            .manaDrain(50)
            .damage(11.25f)
            .explosionRadius(1.5f)
            .spreadDegrees(1.7f)
            .speed(210.0f)
            .castDelaySeconds(0.1f)
            .spreadModifierDegrees(3.6f)
            .lifetimeTicks(50)
            .triggerMode(NoitaSpellTriggerMode.TIMER)
            .triggerDrawCount(1)
            .triggerDelayTicks(100)
            .build(),
        1, 0.0f, 0.0f, 0.99f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem HOOK = registerProjectile(projectileSpec(
        "HOOK", "hook", "Hookbolt", "飞钩", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .manaDrain(30)
            .damage(7.5f)
            .explosionRadius(1.0f)
            .speed(700.0f)
            .castDelaySeconds(0.2f)
            .lifetimeTicks(60)
            .build(),
        1, 0.0f, 0.03f, 0.9952f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem BLACK_HOLE = registerProjectile(projectileSpec(
        "BLACK_HOLE", "black_hole", "Black hole", "黑洞", NoitaProjectileBehavior.BLACK_HOLE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(3)
            .manaDrain(180)
            .explosionRadius(1.0f)
            .spreadDegrees(40.0f)
            .speed(40.0f)
            .castDelaySeconds(1.333333f)
            .lifetimeTicks(120)
            .build(),
        1, 0.0f, 0.0f, 1.0f, 0.65f, 1.3f)
    );
    public static final NoitaProjectileSpellItem BLACK_HOLE_DEATH_TRIGGER = registerProjectile(projectileSpec(
        "BLACK_HOLE_DEATH_TRIGGER", "black_hole_death_trigger", "Black Hole with Death Trigger", "带有死亡触发的黑洞", NoitaProjectileBehavior.BLACK_HOLE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(3)
            .manaDrain(200)
            .explosionRadius(1.0f)
            .spreadDegrees(40.0f)
            .speed(40.0f)
            .castDelaySeconds(1.5f)
            .lifetimeTicks(120)
            .triggerMode(NoitaSpellTriggerMode.DEATH)
            .triggerDrawCount(1)
            .build(),
        1, 0.0f, 0.0f, 1.0f, 0.65f, 1.3f)
    );
    public static final NoitaProjectileSpellItem WHITE_HOLE = registerProjectile(projectileSpec(
        "WHITE_HOLE", "white_hole", "White hole", "白洞", NoitaProjectileBehavior.WHITE_HOLE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(3)
            .manaDrain(180)
            .explosionRadius(1.0f)
            .spreadDegrees(40.0f)
            .speed(40.0f)
            .castDelaySeconds(1.333333f)
            .lifetimeTicks(120)
            .build(),
        1, 0.0f, 0.0f, 1.0f, 0.65f, 1.3f)
    );
    public static final NoitaProjectileSpellItem TENTACLE_PORTAL = registerProjectile(projectileSpec(
        "TENTACLE_PORTAL", "tentacle_portal", "Eldritch portal", "怪异传送门", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(5)
            .manaDrain(140)
            .damage(6.0f)
            .speed(700.0f)
            .castDelaySeconds(30.0f / 60.0f)
            .lifetimeTicks(60)
            .build(),
        1, 0.0f, 0.0f, 0.99f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem SPITTER = registerProjectile(projectileSpec(
        "SPITTER", "spitter", "Spitter bolt", "分裂弹", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(120)
            .manaDrain(5)
            .damage(7.5f)
            .explosionRadius(1.0f)
            .speed(500.0f)
            .castDelaySeconds(-0.016667f)
            .spreadModifierDegrees(6.0f)
            .lifetimeTicks(25)
            .build(),
        1, 0.0f, 0.05f, 0.9892f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem SPITTER_TIMER = registerProjectile(projectileSpec(
        "SPITTER_TIMER", "spitter_timer", "Spitter bolt with timer", "带有定时的分裂弹", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(120)
            .manaDrain(10)
            .damage(7.5f)
            .explosionRadius(1.0f)
            .speed(500.0f)
            .castDelaySeconds(-0.016667f)
            .spreadModifierDegrees(6.0f)
            .lifetimeTicks(25)
            .triggerMode(NoitaSpellTriggerMode.TIMER)
            .triggerDrawCount(1)
            .triggerDelayTicks(40)
            .build(),
        1, 0.0f, 0.05f, 0.9892f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem SPITTER_TIER_2 = registerProjectile(projectileSpec(
        "SPITTER_TIER_2", "spitter_tier_2", "Large spitter bolt", "大型分裂弹", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(120)
            .manaDrain(25)
            .damage(12.5f)
            .explosionRadius(1.0f)
            .speed(700.0f)
            .castDelaySeconds(-0.033333f)
            .spreadModifierDegrees(7.5f)
            .lifetimeTicks(30)
            .build(),
        1, 0.0f, 0.05f, 0.9892f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem SPITTER_TIER_2_TIMER = registerProjectile(projectileSpec(
        "SPITTER_TIER_2_TIMER", "spitter_tier_2_timer", "Large spitter bolt with timer", "带有定时的大型分裂弹", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(120)
            .manaDrain(30)
            .damage(12.5f)
            .explosionRadius(1.0f)
            .speed(700.0f)
            .castDelaySeconds(-0.033333f)
            .spreadModifierDegrees(7.5f)
            .lifetimeTicks(30)
            .triggerMode(NoitaSpellTriggerMode.TIMER)
            .triggerDrawCount(1)
            .triggerDelayTicks(40)
            .build(),
        1, 0.0f, 0.05f, 0.9892f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem SPITTER_TIER_3 = registerProjectile(projectileSpec(
        "SPITTER_TIER_3", "spitter_tier_3", "Giant spitter bolt", "巨型分裂弹", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(120)
            .manaDrain(40)
            .damage(15.0f)
            .explosionRadius(1.0f)
            .speed(900.0f)
            .castDelaySeconds(-0.066667f)
            .spreadModifierDegrees(9.0f)
            .lifetimeTicks(35)
            .build(),
        1, 0.0f, 0.05f, 0.9892f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem SPITTER_TIER_3_TIMER = registerProjectile(projectileSpec(
        "SPITTER_TIER_3_TIMER", "spitter_tier_3_timer", "Giant spitter bolt with timer", "带有定时的巨型分裂弹", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(120)
            .manaDrain(45)
            .damage(15.0f)
            .explosionRadius(1.0f)
            .speed(900.0f)
            .castDelaySeconds(-0.066667f)
            .spreadModifierDegrees(9.0f)
            .lifetimeTicks(35)
            .triggerMode(NoitaSpellTriggerMode.TIMER)
            .triggerDrawCount(1)
            .triggerDelayTicks(40)
            .build(),
        1, 0.0f, 0.05f, 0.9892f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem BUBBLESHOT = registerProjectile(projectileSpec(
        "BUBBLESHOT", "bubbleshot", "Bubble spark", "泡泡火花", NoitaProjectileBehavior.BOUNCY,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(120)
            .manaDrain(5)
            .damage(5.0f)
            .explosionRadius(1.0f)
            .spreadDegrees(22.9f)
            .speed(250.0f)
            .castDelaySeconds(-0.083333f)
            .lifetimeTicks(100)
            .build(),
        1, 0.0f, 0.0f, 0.996f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem BUBBLESHOT_TRIGGER = registerProjectile(projectileSpec(
        "BUBBLESHOT_TRIGGER", "bubbleshot_trigger", "Bubble spark with trigger", "带有触发的泡泡火花", NoitaProjectileBehavior.BOUNCY,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(120)
            .manaDrain(16)
            .damage(5.0f)
            .explosionRadius(1.0f)
            .spreadDegrees(22.9f)
            .speed(250.0f)
            .castDelaySeconds(-0.083333f)
            .lifetimeTicks(100)
            .triggerMode(NoitaSpellTriggerMode.HIT)
            .triggerDrawCount(1)
            .build(),
        1, 0.0f, 0.0f, 0.996f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem DISC_BULLET = registerProjectile(projectileSpec(
        "DISC_BULLET", "disc_bullet", "Disc projectile", "碟状投射物", NoitaProjectileBehavior.DISC,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(40)
            .manaDrain(20)
            .damage(20.0f)
            .spreadDegrees(0.6f)
            .speed(400.0f)
            .castDelaySeconds(0.166667f)
            .spreadModifierDegrees(2.0f)
            .lifetimeTicks(750)
            .build(),
        1, 0.0f, 0.0625f, 0.9976f, 0.65f, 1.1f)
    );
    public static final NoitaProjectileSpellItem DISC_BULLET_BIG = registerProjectile(projectileSpec(
        "DISC_BULLET_BIG", "disc_bullet_big", "Giga disc projectile", "巨型碟状投射物", NoitaProjectileBehavior.DISC,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(40)
            .manaDrain(38)
            .damage(30.0f)
            .spreadDegrees(0.6f)
            .speed(250.0f)
            .castDelaySeconds(0.333333f)
            .spreadModifierDegrees(3.4f)
            .lifetimeTicks(300)
            .build(),
        1, 0.0f, 0.0f, 1.0f, 0.65f, 1.4f)
    );
    public static final NoitaProjectileSpellItem DISC_BULLET_BIGGER = registerProjectile(projectileSpec(
        "DISC_BULLET_BIGGER", "disc_bullet_bigger", "Summon Omega Sawblade", "召唤终结锯刃", NoitaProjectileBehavior.DISC,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(40)
            .manaDrain(70)
            .damage(43.0f)
            .spreadDegrees(0.6f)
            .speed(150.0f)
            .castDelaySeconds(0.666667f)
            .spreadModifierDegrees(6.4f)
            .lifetimeTicks(500)
            .build(),
        1, 0.0f, 0.0f, 0.992f, 0.65f, 1.7f)
    );
    public static final NoitaProjectileSpellItem BOUNCY_ORB = registerProjectile(projectileSpec(
        "BOUNCY_ORB", "bouncy_orb", "Energy sphere", "能量球体", NoitaProjectileBehavior.BOUNCY,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(40)
            .manaDrain(20)
            .damage(5.0f)
            .explosionRadius(2.0f)
            .spreadDegrees(0.6f)
            .speed(450.0f)
            .castDelaySeconds(0.166667f)
            .lifetimeTicks(750)
            .build(),
        1, 0.0f, 0.0f, 0.99f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem BOUNCY_ORB_TIMER = registerProjectile(projectileSpec(
        "BOUNCY_ORB_TIMER", "bouncy_orb_timer", "Energy sphere with timer", "带有定时的能量球体", NoitaProjectileBehavior.BOUNCY,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(40)
            .manaDrain(50)
            .damage(5.0f)
            .explosionRadius(2.0f)
            .spreadDegrees(0.6f)
            .speed(450.0f)
            .castDelaySeconds(0.166667f)
            .lifetimeTicks(750)
            .triggerMode(NoitaSpellTriggerMode.TIMER)
            .triggerDrawCount(1)
            .triggerDelayTicks(200)
            .build(),
        1, 0.0f, 0.0f, 0.99f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem RUBBER_BALL = registerProjectile(projectileSpec(
        "RUBBER_BALL", "bouncing_burst", "Bouncing burst", "弹跳爆发", NoitaProjectileBehavior.BOUNCY,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(150)
            .manaDrain(5)
            .damage(3.0f)
            .explosionRadius(1.0f)
            .spreadDegrees(0.6f)
            .speed(700.0f)
            .castDelaySeconds(-0.033333f)
            .spreadModifierDegrees(-1.0f)
            .lifetimeTicks(750)
            .build(),
        1, 0.0f, 0.0625f, 0.9976f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem ARROW = registerProjectile(projectileSpec(
        "ARROW", "arrow", "Arrow", "箭矢", NoitaProjectileBehavior.ARROW,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(40)
            .manaDrain(15)
            .damage(10.0f)
            .explosionRadius(1.0f)
            .speed(600.0f)
            .castDelaySeconds(0.166667f)
            .spreadModifierDegrees(-20.0f)
            .lifetimeTicks(750)
            .build(),
        1, 0.0f, 0.0625f, 0.9968f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem POLLEN = registerProjectile(projectileSpec(
        "POLLEN", "pollen", "Pollen", "花粉", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(40)
            .manaDrain(10)
            .damage(5.0f)
            .explosionRadius(1.0f)
            .speed(200.0f)
            .castDelaySeconds(0.033333f)
            .spreadModifierDegrees(20.0f)
            .lifetimeTicks(750)
            .build(),
        1, 0.0f, 0.0025f, 0.9888f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem LANCE = registerProjectile(projectileSpec(
        "LANCE", "lance", "Glowing lance", "闪耀之枪", NoitaProjectileBehavior.ARROW,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(30)
            .manaDrain(30)
            .damage(35.0f)
            .speed(800.0f)
            .castDelaySeconds(0.333333f)
            .spreadModifierDegrees(-20.0f)
            .lifetimeTicks(80)
            .build(),
        1, 0.0f, 0.0f, 0.99f, 0.65f, 1.2f)
    );
    public static final NoitaProjectileSpellItem LANCE_HOLY = registerProjectile(projectileSpec(
        "LANCE_HOLY", "lance_holy", "Holy Lance", "圣枪", NoitaProjectileBehavior.ARROW,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(30)
            .manaDrain(120)
            .damage(80.0f)
            .speed(900.0f)
            .castDelaySeconds(0.5f)
            .spreadModifierDegrees(-10.0f)
            .lifetimeTicks(90)
            .build(),
        1, 0.0f, 0.0f, 0.99f, 0.65f, 1.35f)
    );
    public static final NoitaProjectileSpellItem ROCKET = registerProjectile(projectileSpec(
        "ROCKET", "rocket", "Magic missile", "魔法飞弹", NoitaProjectileBehavior.EXPLOSIVE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(10)
            .manaDrain(70)
            .damage(35.0f)
            .explosionRadius(1.5f)
            .speed(85.0f)
            .castDelaySeconds(1.0f)
            .lifetimeTicks(360)
            .friendlyFire(true)
            .build(),
        1, 0.0f, 0.0125f, 1.0f, 0.65f, 1.2f)
    );
    public static final NoitaProjectileSpellItem ROCKET_TIER_2 = registerProjectile(projectileSpec(
        "ROCKET_TIER_2", "rocket_tier_2", "Large magic missile", "大型魔法飞弹", NoitaProjectileBehavior.EXPLOSIVE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(8)
            .manaDrain(90)
            .damage(55.0f)
            .explosionRadius(3.2f)
            .speed(85.0f)
            .castDelaySeconds(1.5f)
            .lifetimeTicks(360)
            .friendlyFire(true)
            .build(),
        1, 0.0f, 0.0125f, 1.0f, 0.65f, 1.35f)
    );
    public static final NoitaProjectileSpellItem ROCKET_TIER_3 = registerProjectile(projectileSpec(
        "ROCKET_TIER_3", "rocket_tier_3", "Giant magic missile", "巨型魔法飞弹", NoitaProjectileBehavior.EXPLOSIVE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(6)
            .manaDrain(120)
            .damage(80.0f)
            .explosionRadius(4.2f)
            .speed(85.0f)
            .castDelaySeconds(2.0f)
            .lifetimeTicks(360)
            .friendlyFire(true)
            .build(),
        1, 0.0f, 0.0125f, 1.0f, 0.65f, 1.55f)
    );
    public static final NoitaProjectileSpellItem GRENADE = registerProjectile(projectileSpec(
        "GRENADE", "grenade", "Firebolt", "火焰弹", NoitaProjectileBehavior.EXPLOSIVE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(25)
            .manaDrain(50)
            .damage(45.0f)
            .explosionRadius(1.0f)
            .spreadDegrees(2.9f)
            .speed(265.0f)
            .castDelaySeconds(0.5f)
            .lifetimeTicks(500)
            .friendlyFire(true)
            .build(),
        1, 0.0f, 0.05f, 0.99f, 0.65f, 1.1f)
    );
    public static final NoitaProjectileSpellItem GRENADE_TRIGGER = registerProjectile(projectileSpec(
        "GRENADE_TRIGGER", "grenade_trigger", "Firebolt with trigger", "带有触发的火焰弹", NoitaProjectileBehavior.EXPLOSIVE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(25)
            .manaDrain(50)
            .damage(45.0f)
            .explosionRadius(1.0f)
            .spreadDegrees(2.9f)
            .speed(265.0f)
            .castDelaySeconds(0.5f)
            .lifetimeTicks(500)
            .friendlyFire(true)
            .triggerMode(NoitaSpellTriggerMode.HIT)
            .triggerDrawCount(1)
            .build(),
        1, 0.0f, 0.05f, 0.99f, 0.65f, 1.1f)
    );
    public static final NoitaProjectileSpellItem GRENADE_TIER_2 = registerProjectile(projectileSpec(
        "GRENADE_TIER_2", "grenade_tier_2", "Large firebolt", "大型火焰弹", NoitaProjectileBehavior.EXPLOSIVE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(20)
            .manaDrain(90)
            .damage(70.0f)
            .explosionRadius(2.5f)
            .spreadDegrees(2.9f)
            .speed(265.0f)
            .castDelaySeconds(0.833333f)
            .lifetimeTicks(500)
            .friendlyFire(true)
            .build(),
        1, 0.0f, 0.05f, 0.99f, 0.65f, 1.25f)
    );
    public static final NoitaProjectileSpellItem GRENADE_TIER_3 = registerProjectile(projectileSpec(
        "GRENADE_TIER_3", "grenade_tier_3", "Giant firebolt", "巨型火焰弹", NoitaProjectileBehavior.EXPLOSIVE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(20)
            .manaDrain(90)
            .damage(95.0f)
            .explosionRadius(4.0f)
            .spreadDegrees(2.9f)
            .speed(265.0f)
            .castDelaySeconds(1.333333f)
            .lifetimeTicks(500)
            .friendlyFire(true)
            .build(),
        1, 0.0f, 0.05f, 0.99f, 0.65f, 1.45f)
    );
    public static final NoitaProjectileSpellItem GRENADE_ANTI = registerProjectile(projectileSpec(
        "GRENADE_ANTI", "grenade_anti", "Odd Firebolt", "怪异火焰弹", NoitaProjectileBehavior.EXPLOSIVE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(25)
            .manaDrain(50)
            .damage(45.0f)
            .explosionRadius(1.9f)
            .spreadDegrees(2.9f)
            .speed(265.0f)
            .castDelaySeconds(0.5f)
            .lifetimeTicks(500)
            .friendlyFire(true)
            .build(),
        1, 0.0f, 0.0f, 0.98f, 0.65f, 1.1f)
    );
    public static final NoitaProjectileSpellItem GRENADE_LARGE = registerProjectile(projectileSpec(
        "GRENADE_LARGE", "grenade_large", "Dropper bolt", "坠落弹", NoitaProjectileBehavior.EXPLOSIVE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(35)
            .manaDrain(80)
            .damage(95.0f)
            .explosionRadius(1.6f)
            .spreadDegrees(2.9f)
            .speed(65.0f)
            .castDelaySeconds(0.666667f)
            .lifetimeTicks(500)
            .friendlyFire(true)
            .build(),
        1, 0.0f, 0.12f, 0.99f, 0.65f, 1.35f)
    );
    public static final NoitaProjectileSpellItem MINE = registerProjectile(projectileSpec(
        "MINE", "mine", "Unstable crystal", "不稳晶体", NoitaProjectileBehavior.MINE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(15)
            .manaDrain(20)
            .explosionRadius(3.0f)
            .spreadDegrees(2.9f)
            .speed(265.0f)
            .castDelaySeconds(0.5f)
            .speedMultiplier(0.75f)
            .lifetimeTicks(1200)
            .friendlyFire(true)
            .build(),
        1, 0.0f, 0.05f, 0.99f, 0.65f, 1.15f)
    );
    public static final NoitaProjectileSpellItem MINE_DEATH_TRIGGER = registerProjectile(projectileSpec(
        "MINE_DEATH_TRIGGER", "mine_death_trigger", "Unstable crystal with trigger", "带有触发的不稳晶体", NoitaProjectileBehavior.MINE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(15)
            .manaDrain(20)
            .explosionRadius(3.0f)
            .spreadDegrees(2.9f)
            .speed(265.0f)
            .castDelaySeconds(0.5f)
            .speedMultiplier(0.75f)
            .lifetimeTicks(1200)
            .friendlyFire(true)
            .triggerMode(NoitaSpellTriggerMode.DEATH)
            .triggerDrawCount(1)
            .build(),
        1, 0.0f, 0.05f, 0.99f, 0.65f, 1.15f)
    );
    public static final NoitaProjectileSpellItem PIPE_BOMB = registerProjectile(projectileSpec(
        "PIPE_BOMB", "pipe_bomb", "Dormant crystal", "休眠晶体", NoitaProjectileBehavior.MINE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(20)
            .manaDrain(20)
            .explosionRadius(3.0f)
            .spreadDegrees(2.9f)
            .speed(265.0f)
            .castDelaySeconds(0.5f)
            .speedMultiplier(0.75f)
            .lifetimeTicks(1200)
            .friendlyFire(true)
            .build(),
        1, 0.0f, 0.05f, 0.99f, 0.65f, 1.15f)
    );
    public static final NoitaProjectileSpellItem PIPE_BOMB_DEATH_TRIGGER = registerProjectile(projectileSpec(
        "PIPE_BOMB_DEATH_TRIGGER", "pipe_bomb_death_trigger", "Dormant crystal with trigger", "带有触发的休眠晶体", NoitaProjectileBehavior.MINE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(20)
            .manaDrain(20)
            .explosionRadius(3.0f)
            .spreadDegrees(2.9f)
            .speed(265.0f)
            .castDelaySeconds(0.5f)
            .speedMultiplier(0.75f)
            .lifetimeTicks(1200)
            .friendlyFire(true)
            .triggerMode(NoitaSpellTriggerMode.DEATH)
            .triggerDrawCount(1)
            .build(),
        1, 0.0f, 0.05f, 0.99f, 0.65f, 1.15f)
    );
    public static final NoitaProjectileSpellItem FISH = registerProjectile(projectileSpec(
        "FISH", "fish", "Summon fish", "召唤鱼类", NoitaProjectileBehavior.SUMMON,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(20)
            .manaDrain(90)
            .damage(8.0f)
            .speed(265.0f)
            .castDelaySeconds(80.0f / 60.0f)
            .lifetimeTicks(240)
            .build(),
        1, 0.0f, 0.04f, 0.99f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem EXPLODING_DEER = registerProjectile(projectileSpec(
        "EXPLODING_DEER", "exploding_deer", "Summon deercoy", "召唤鹿诱饵", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(10)
            .manaDrain(120)
            .damage(8.0f)
            .speed(320.0f)
            .castDelaySeconds(80.0f / 60.0f)
            .lifetimeTicks(240)
            .build(),
        1, 0.0f, 0.04f, 0.99f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem EXPLODING_DUCKS = registerProjectile(projectileSpec(
        "EXPLODING_DUCKS", "exploding_ducks", "Flock of Ducks", "鸭群", NoitaProjectileBehavior.EXPLOSIVE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(20)
            .manaDrain(100)
            .damage(8.0f)
            .explosionRadius(2.5f)
            .speed(265.0f)
            .castDelaySeconds(60.0f / 60.0f)
            .rechargeTimeSeconds(20.0f / 60.0f)
            .lifetimeTicks(300)
            .build(),
        3, 20.0f, 0.03f, 0.6f, 0.7f, 1.0f)
    );
    public static final NoitaProjectileSpellItem WORM_SHOT = registerProjectile(projectileSpec(
        "WORM_SHOT", "worm_shot", "Worm Launcher", "蠕虫发射器", NoitaProjectileBehavior.SUMMON,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(10)
            .manaDrain(150)
            .damage(8.0f)
            .explosionRadius(3.0f)
            .speed(250.0f)
            .castDelaySeconds(80.0f / 60.0f)
            .rechargeTimeSeconds(40.0f / 60.0f)
            .spreadModifierDegrees(20.0f)
            .lifetimeTicks(100)
            .build(),
        1, 0.0f, 0.05f, 1.0f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem LASER = registerProjectile(projectileSpec(
        "LASER", "laser", "Concentrated light", "汇聚之光", NoitaProjectileBehavior.BEAM,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(80)
            .manaDrain(30)
            .damage(10.0f)
            .speed(1400.0f)
            .castDelaySeconds(-0.366667f)
            .lifetimeTicks(3)
            .piercing(true)
            .build(),
        1, 0.0f, 0.0f, 0.99f, 0.65f, 1.25f)
    );
    public static final NoitaProjectileSpellItem MEGALASER = registerProjectile(projectileSpec(
        "MEGALASER", "megalaser", "Intense concentrated light", "强烈汇聚之光", NoitaProjectileBehavior.BEAM,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .manaDrain(110)
            .damage(18.0f)
            .speed(1.0f)
            .castDelaySeconds(90.0f / 60.0f)
            .lifetimeTicks(32)
            .piercing(true)
            .build(),
        1, 0.0f, 0.0f, 1.0f, 0.65f, 1.5f)
    );
    public static final NoitaProjectileSpellItem LIGHTNING = registerProjectile(projectileSpec(
        "LIGHTNING", "lightning", "Lightning bolt", "闪电弹", NoitaProjectileBehavior.BEAM,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(30)
            .manaDrain(70)
            .damage(25.0f)
            .explosionRadius(3.5f)
            .speed(1600.0f)
            .castDelaySeconds(0.833333f)
            .lifetimeTicks(2)
            .piercing(true)
            .build(),
        1, 0.0f, 0.0f, 0.99f, 0.65f, 1.3f)
    );
    public static final NoitaProjectileSpellItem BALL_LIGHTNING = registerProjectile(projectileSpec(
        "BALL_LIGHTNING", "ball_lightning", "Ball Lightning", "球状闪电", NoitaProjectileBehavior.BEAM,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .manaDrain(70)
            .damage(18.0f)
            .explosionRadius(1.2f)
            .speed(500.0f)
            .castDelaySeconds(0.833333f)
            .lifetimeTicks(80)
            .piercing(true)
            .build(),
        3, 20.0f, 0.0f, 0.99f, 0.65f, 1.1f)
    );
    public static final NoitaProjectileSpellItem LASER_EMITTER = registerProjectile(projectileSpec(
        "LASER_EMITTER", "laser_emitter", "Plasma beam", "电浆束", NoitaProjectileBehavior.BEAM,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .manaDrain(60)
            .damage(2.4f)
            .speed(40.0f)
            .castDelaySeconds(6.0f / 60.0f)
            .lifetimeTicks(100)
            .piercing(true)
            .build(),
        1, 0.0f, 0.0f, 0.93f, 0.65f, 1.2f)
    );
    public static final NoitaProjectileSpellItem LASER_EMITTER_FOUR = registerProjectile(projectileSpec(
        "LASER_EMITTER_FOUR", "laser_emitter_four", "Plasma Beam Cross", "电浆束十字", NoitaProjectileBehavior.BEAM,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .manaDrain(80)
            .damage(1.6f)
            .speed(40.0f)
            .castDelaySeconds(15.0f / 60.0f)
            .lifetimeTicks(100)
            .piercing(true)
            .build(),
        1, 0.0f, 0.0f, 0.93f, 0.65f, 1.2f)
    );
    public static final NoitaProjectileSpellItem LASER_EMITTER_CUTTER = registerProjectile(projectileSpec(
        "LASER_EMITTER_CUTTER", "laser_emitter_cutter", "Plasma Cutter", "电浆切割器", NoitaProjectileBehavior.BEAM,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .manaDrain(40)
            .damage(1.8f)
            .speed(40.0f)
            .rechargeTimeSeconds(10.0f / 60.0f)
            .lifetimeTicks(60)
            .piercing(true)
            .build(),
        1, 0.0f, 0.0f, 0.93f, 0.65f, 1.2f)
    );
    public static final NoitaProjectileSpellItem DIGGER = registerProjectile(projectileSpec(
        "DIGGER", "digger", "Digging bolt", "挖掘魔弹", NoitaProjectileBehavior.DRILL,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .manaDrain(0)
            .damage(4.0f)
            .speed(900.0f)
            .castDelaySeconds(0.016667f)
            .rechargeTimeSeconds(-0.166667f)
            .lifetimeTicks(10)
            .piercing(true)
            .build(),
        1, 0.0f, 0.0f, 0.99f, 0.65f, 1.2f)
    );
    public static final NoitaProjectileSpellItem POWERDIGGER = registerProjectile(projectileSpec(
        "POWERDIGGER", "powerdigger", "Digging blast", "挖掘爆破", NoitaProjectileBehavior.DRILL,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .manaDrain(0)
            .damage(8.0f)
            .explosionRadius(2.0f)
            .speed(800.0f)
            .castDelaySeconds(0.016667f)
            .rechargeTimeSeconds(-0.166667f)
            .lifetimeTicks(12)
            .piercing(true)
            .build(),
        1, 0.0f, 0.0f, 0.99f, 0.65f, 1.1f)
    );
    public static final NoitaProjectileSpellItem CHAINSAW = registerProjectile(projectileSpec(
        "CHAINSAW", "chainsaw", "Chainsaw", "链锯", NoitaProjectileBehavior.CHAINSAW,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(1000)
            .manaDrain(1)
            .damage(15.0f)
            .speed(500.0f)
            .rechargeTimeSeconds(-0.166667f)
            .spreadModifierDegrees(6.0f)
            .lifetimeTicks(2)
            .piercing(true)
            .build(),
        1, 0.0f, 0.0f, 0.99f, 0.65f, 1.25f)
    );
    public static final NoitaProjectileSpellItem LUMINOUS_DRILL = registerProjectile(projectileSpec(
        "LUMINOUS_DRILL", "luminous_drill", "Luminous drill", "光明穿凿", NoitaProjectileBehavior.DRILL,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(1000)
            .manaDrain(10)
            .damage(10.0f)
            .speed(1400.0f)
            .castDelaySeconds(-0.583333f)
            .rechargeTimeSeconds(-0.166667f)
            .lifetimeTicks(2)
            .piercing(true)
            .build(),
        1, 0.0f, 0.0f, 0.99f, 0.65f, 1.25f)
    );
    public static final NoitaProjectileSpellItem LASER_LUMINOUS_DRILL = registerProjectile(projectileSpec(
        "LASER_LUMINOUS_DRILL", "laser_luminous_drill", "Luminous drill with timer", "带有定时的光明穿凿", NoitaProjectileBehavior.DRILL,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(1000)
            .manaDrain(30)
            .damage(10.0f)
            .speed(1400.0f)
            .castDelaySeconds(-0.583333f)
            .rechargeTimeSeconds(-0.166667f)
            .lifetimeTicks(2)
            .piercing(true)
            .triggerMode(NoitaSpellTriggerMode.TIMER)
            .triggerDrawCount(1)
            .triggerDelayTicks(4)
            .build(),
        1, 0.0f, 0.0f, 0.99f, 0.65f, 1.25f)
    );
    public static final NoitaProjectileSpellItem TENTACLE = registerProjectile(projectileSpec(
        "TENTACLE", "tentacle", "Summon Tentacle", "召唤触手", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(40)
            .manaDrain(20)
            .damage(6.0f)
            .speed(8.0f)
            .castDelaySeconds(40.0f / 60.0f)
            .lifetimeTicks(60)
            .build(),
        1, 0.0f, 0.0f, 0.99f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem TENTACLE_TIMER = registerProjectile(projectileSpec(
        "TENTACLE_TIMER", "tentacle_timer", "Summon Tentacle with timer", "带有定时的触手", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(40)
            .manaDrain(20)
            .damage(6.0f)
            .speed(8.0f)
            .castDelaySeconds(40.0f / 60.0f)
            .lifetimeTicks(60)
            .triggerMode(NoitaSpellTriggerMode.TIMER)
            .triggerDrawCount(1)
            .triggerDelayTicks(20)
            .build(),
        1, 0.0f, 0.0f, 0.99f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem HEAL_BULLET = registerProjectile(projectileSpec(
        "HEAL_BULLET", "heal_bullet", "Healing bolt", "治疗魔弹", NoitaProjectileBehavior.HEAL,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(20)
            .manaDrain(15)
            .damage(8.0f)
            .explosionRadius(1.0f)
            .speed(625.0f)
            .castDelaySeconds(0.066667f)
            .spreadModifierDegrees(2.0f)
            .lifetimeTicks(60)
            .build(),
        1, 0.0f, 0.05f, 0.9952f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem ANTIHEAL = registerProjectile(projectileSpec(
        "ANTIHEAL", "antiheal", "Deadly heal", "致命治愈", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(20)
            .manaDrain(20)
            .damage(18.0f)
            .explosionRadius(1.0f)
            .speed(625.0f)
            .castDelaySeconds(0.133333f)
            .spreadModifierDegrees(3.0f)
            .lifetimeTicks(40)
            .build(),
        1, 0.0f, 0.05f, 0.9952f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem SPIRAL_SHOT = registerProjectile(projectileSpec(
        "SPIRAL_SHOT", "spiral_shot", "Spiral shot", "螺旋魔弹", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(15)
            .manaDrain(50)
            .damage(6.0f)
            .speed(110.0f)
            .castDelaySeconds(20.0f / 60.0f)
            .lifetimeTicks(100)
            .build(),
        1, 0.0f, 0.0f, 1.0f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem MAGIC_SHIELD = registerProjectile(projectileSpec(
        "MAGIC_SHIELD", "magic_shield", "Magic guard", "魔法护卫", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .manaDrain(40)
            .damage(6.0f)
            .speed(80.0f)
            .castDelaySeconds(20.0f / 60.0f)
            .lifetimeTicks(5)
            .build(),
        1, 0.0f, 0.0f, 1.0f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem BIG_MAGIC_SHIELD = registerProjectile(projectileSpec(
        "BIG_MAGIC_SHIELD", "big_magic_shield", "Big magic guard", "大型魔法护卫", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .manaDrain(60)
            .damage(6.0f)
            .speed(80.0f)
            .castDelaySeconds(30.0f / 60.0f)
            .lifetimeTicks(5)
            .build(),
        1, 0.0f, 0.0f, 1.0f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem CHAIN_BOLT = registerProjectile(projectileSpec(
        "CHAIN_BOLT", "chain_bolt", "Chain bolt", "连环魔弹", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .manaDrain(80)
            .damage(6.0f)
            .speed(40.0f)
            .castDelaySeconds(45.0f / 60.0f)
            .spreadModifierDegrees(14.0f)
            .lifetimeTicks(44)
            .build(),
        1, 0.0f, 0.0f, 0.99f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem FIREBALL = registerProjectile(projectileSpec(
        "FIREBALL", "fireball", "Fireball", "火球", NoitaProjectileBehavior.EXPLOSIVE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(15)
            .manaDrain(70)
            .damage(6.25f)
            .explosionRadius(1.5f)
            .speed(165.0f)
            .castDelaySeconds(0.833333f)
            .spreadModifierDegrees(4.0f)
            .lifetimeTicks(60)
            .friendlyFire(true)
            .build(),
        1, 0.0f, 0.025f, 0.99f, 0.65f, 1.25f)
    );
    public static final NoitaProjectileSpellItem METEOR = registerProjectile(projectileSpec(
        "METEOR", "meteor", "Meteor", "陨石", NoitaProjectileBehavior.EXPLOSIVE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(10)
            .manaDrain(150)
            .damage(56.25f)
            .explosionRadius(4.5f)
            .speed(350.0f)
            .lifetimeTicks(200)
            .friendlyFire(true)
            .build(),
        1, 0.0f, 0.0125f, 1.0f, 0.65f, 1.55f)
    );
    public static final NoitaProjectileSpellItem FLAMETHROWER = registerProjectile(projectileSpec(
        "FLAMETHROWER", "flamethrower", "Flamethrower", "火焰喷射器", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(60)
            .manaDrain(20)
            .damage(6.0f)
            .explosionRadius(1.0f)
            .speed(165.0f)
            .spreadModifierDegrees(4.0f)
            .lifetimeTicks(80)
            .build(),
        1, 0.0f, 0.025f, 0.99f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem ICEBALL = registerProjectile(projectileSpec(
        "ICEBALL", "iceball", "Iceball", "冰球", NoitaProjectileBehavior.EXPLOSIVE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(15)
            .manaDrain(90)
            .damage(6.0f)
            .explosionRadius(1.5f)
            .speed(165.0f)
            .castDelaySeconds(1.333333f)
            .spreadModifierDegrees(8.0f)
            .lifetimeTicks(60)
            .friendlyFire(true)
            .build(),
        1, 0.0f, 0.025f, 0.99f, 0.65f, 1.2f)
    );
    public static final NoitaProjectileSpellItem SLIMEBALL = registerProjectile(projectileSpec(
        "SLIMEBALL", "slimeball", "Slimeball", "粘液球", NoitaProjectileBehavior.ORB,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(50)
            .manaDrain(20)
            .damage(8.0f)
            .speed(300.0f)
            .castDelaySeconds(0.166667f)
            .spreadModifierDegrees(4.0f)
            .speedMultiplier(1.1f)
            .lifetimeTicks(100)
            .build(),
        1, 0.0f, 0.0f, 0.99f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem DARKFLAME = registerProjectile(projectileSpec(
        "DARKFLAME", "darkflame", "Path of dark flame", "黑焰之道", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(60)
            .manaDrain(90)
            .damage(6.0f)
            .explosionRadius(1.0f)
            .speed(250.0f)
            .castDelaySeconds(20.0f / 60.0f)
            .lifetimeTicks(100)
            .build(),
        1, 0.0f, 0.0f, 1.0f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem MISSILE = registerProjectile(projectileSpec(
        "MISSILE", "missile", "Summon missile", "召唤飞弹", NoitaProjectileBehavior.EXPLOSIVE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(20)
            .manaDrain(60)
            .damage(15.0f)
            .explosionRadius(1.0f)
            .speed(265.0f)
            .rechargeTimeSeconds(30.0f / 60.0f)
            .spreadModifierDegrees(3.0f)
            .lifetimeTicks(180)
            .friendlyFire(true)
            .build(),
        1, 0.0f, 0.05f, 0.99f, 0.65f, 1.2f)
    );
    public static final NoitaProjectileSpellItem FUNKY_SPELL = registerProjectile(projectileSpec(
        "FUNKY_SPELL", "funky_spell", "Funky_Spell", "FUNKY_SPELL", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .manaDrain(5)
            .damage(6.0f)
            .speed(700.0f)
            .castDelaySeconds(-3.0f / 60.0f)
            .spreadModifierDegrees(2.0f)
            .criticalChancePercent(1.0f)
            .lifetimeTicks(60)
            .build(),
        1, 0.0f, 0.0f, 0.99f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem PEBBLE = registerProjectile(projectileSpec(
        "PEBBLE", "pebble", "Summon rock spirit", "召唤岩石精灵", NoitaProjectileBehavior.SUMMON,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(10)
            .manaDrain(120)
            .damage(8.0f)
            .speed(230.0f)
            .castDelaySeconds(80.0f / 60.0f)
            .lifetimeTicks(60)
            .build(),
        1, 0.0f, 0.04f, 0.99f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem DYNAMITE = registerProjectile(projectileSpec(
        "DYNAMITE", "dynamite", "Dynamite", "炸药", NoitaProjectileBehavior.FUSED_EXPLOSIVE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(16)
            .manaDrain(50)
            .damage(15.0f)
            .explosionRadius(2.8f)
            .speed(800.0f)
            .castDelaySeconds(50.0f / 60.0f)
            .spreadModifierDegrees(6.0f)
            .lifetimeTicks(50)
            .friendlyFire(true)
            .build(),
        1, 0.0f, 0.05f, 0.99f, 0.65f, 1.2f)
    );
    public static final NoitaProjectileSpellItem GLITTER_BOMB = registerProjectile(projectileSpec(
        "GLITTER_BOMB", "glitter_bomb", "Glitter bomb", "闪烁炸弹", NoitaProjectileBehavior.FUSED_EXPLOSIVE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(16)
            .manaDrain(70)
            .damage(15.0f)
            .explosionRadius(1.0f)
            .speed(265.0f)
            .castDelaySeconds(50.0f / 60.0f)
            .spreadModifierDegrees(12.0f)
            .lifetimeTicks(180)
            .friendlyFire(true)
            .build(),
        1, 0.0f, 0.05f, 0.99f, 0.65f, 1.2f)
    );
    public static final NoitaProjectileSpellItem BUCKSHOT = registerProjectile(projectileSpec(
        "BUCKSHOT", "buckshot", "Triplicate bolt", "三联魔弹", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .manaDrain(25)
            .damage(8.0f)
            .explosionRadius(1.0f)
            .speed(550.0f)
            .castDelaySeconds(0.133333f)
            .spreadModifierDegrees(14.0f)
            .lifetimeTicks(120)
            .build(),
        3, 18.0f, 0.0f, 0.996f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem FREEZING_GAZE = registerProjectile(projectileSpec(
        "FREEZING_GAZE", "freezing_gaze", "Freezing gaze", "冰冷凝视", NoitaProjectileBehavior.BEAM,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(20)
            .manaDrain(45)
            .damage(6.0f)
            .speed(220.0f)
            .castDelaySeconds(0.333333f)
            .lifetimeTicks(25)
            .piercing(true)
            .build(),
        12, 30.0f, 0.0f, 1.0f, 0.65f, 1.1f)
    );
    public static final NoitaProjectileSpellItem GLOWING_BOLT = registerProjectile(projectileSpec(
        "GLOWING_BOLT", "glowing_bolt", "Pinpoint of light", "汇聚之光", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .manaDrain(65)
            .damage(6.0f)
            .speed(700.0f)
            .castDelaySeconds(40.0f / 60.0f)
            .spreadModifierDegrees(6.0f)
            .lifetimeTicks(60)
            .build(),
        1, 0.0f, 0.0f, 0.99f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem SPORE_POD = registerProjectile(projectileSpec(
        "SPORE_POD", "spore_pod", "Prickly Spore Pod", "多刺孢子荚", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .manaDrain(20)
            .damage(6.0f)
            .speed(225.0f)
            .castDelaySeconds(40.0f / 60.0f)
            .lifetimeTicks(100)
            .build(),
        1, 0.0f, 0.0f, 1.0f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem GLUE_SHOT = registerProjectile(projectileSpec(
        "GLUE_SHOT", "glue_shot", "Glue Ball", "胶球", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .manaDrain(25)
            .damage(6.0f)
            .speed(700.0f)
            .castDelaySeconds(30.0f / 60.0f)
            .spreadModifierDegrees(5.0f)
            .lifetimeTicks(60)
            .build(),
        1, 0.0f, 0.0f, 0.99f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem BOMB_HOLY = registerProjectile(projectileSpec(
        "BOMB_HOLY", "bomb_holy", "Holy Bomb", "神圣炸弹", NoitaProjectileBehavior.FUSED_EXPLOSIVE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(2)
            .manaDrain(300)
            .damage(15.0f)
            .explosionRadius(12.0f)
            .speed(265.0f)
            .castDelaySeconds(40.0f / 60.0f)
            .rechargeTimeSeconds(80.0f / 60.0f)
            .lifetimeTicks(170)
            .friendlyFire(true)
            .build(),
        1, 0.0f, 0.05f, 0.99f, 0.65f, 1.2f)
    );
    public static final NoitaProjectileSpellItem BOMB_HOLY_GIGA = registerProjectile(projectileSpec(
        "BOMB_HOLY_GIGA", "bomb_holy_giga", "Giga Holy Bomb", "巨型神圣炸弹", NoitaProjectileBehavior.FUSED_EXPLOSIVE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(2)
            .manaDrain(600)
            .damage(15.0f)
            .explosionRadius(12.0f)
            .speed(265.0f)
            .castDelaySeconds(120.0f / 60.0f)
            .rechargeTimeSeconds(160.0f / 60.0f)
            .lifetimeTicks(260)
            .friendlyFire(true)
            .build(),
        1, 0.0f, 0.05f, 0.99f, 0.65f, 1.2f)
    );
    public static final NoitaProjectileSpellItem PROPANE_TANK = registerProjectile(projectileSpec(
        "PROPANE_TANK", "propane_tank", "Propane tank", "丙烷罐", NoitaProjectileBehavior.FUSED_EXPLOSIVE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(10)
            .manaDrain(75)
            .damage(15.0f)
            .explosionRadius(6.0f)
            .speed(265.0f)
            .castDelaySeconds(100.0f / 60.0f)
            .lifetimeTicks(180)
            .friendlyFire(true)
            .build(),
        1, 0.0f, 0.05f, 0.99f, 0.65f, 1.2f)
    );
    public static final NoitaProjectileSpellItem BOMB_CART = registerProjectile(projectileSpec(
        "BOMB_CART", "bomb_cart", "Bomb cart", "炸弹矿车", NoitaProjectileBehavior.FUSED_EXPLOSIVE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(6)
            .manaDrain(75)
            .damage(15.0f)
            .explosionRadius(1.0f)
            .speed(265.0f)
            .castDelaySeconds(60.0f / 60.0f)
            .lifetimeTicks(180)
            .friendlyFire(true)
            .build(),
        1, 0.0f, 0.05f, 0.99f, 0.65f, 1.2f)
    );
    public static final NoitaProjectileSpellItem CURSED_ORB = registerProjectile(projectileSpec(
        "CURSED_ORB", "cursed_orb", "Cursed sphere", "诅咒之球", NoitaProjectileBehavior.ORB,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .manaDrain(40)
            .damage(6.0f)
            .explosionRadius(1.0f)
            .speed(1.5f)
            .castDelaySeconds(20.0f / 60.0f)
            .lifetimeTicks(120)
            .bounceCount(2)
            .build(),
        1, 0.0f, 0.0f, 1.02f, 0.25f, 1.0f)
    );
    public static final NoitaProjectileSpellItem EXPANDING_ORB = registerProjectile(projectileSpec(
        "EXPANDING_ORB", "expanding_orb", "Expanding Sphere", "扩张之球", NoitaProjectileBehavior.ORB,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .manaDrain(70)
            .damage(30.0f)
            .explosionRadius(1.8f)
            .speed(50.0f)
            .castDelaySeconds(30.0f / 60.0f)
            .lifetimeTicks(180)
            .build(),
        1, 0.0f, 0.0f, 1.0f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem CRUMBLING_EARTH = registerProjectile(projectileSpec(
        "CRUMBLING_EARTH", "crumbling_earth", "Earthquake", "地震", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(3)
            .manaDrain(240)
            .damage(6.0f)
            .explosionRadius(1.0f)
            .speed(110.0f)
            .lifetimeTicks(30)
            .build(),
        1, 0.0f, 0.0025f, 0.99f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem SUMMON_ROCK = registerProjectile(projectileSpec(
        "SUMMON_ROCK", "summon_rock", "Rock", "岩石", NoitaProjectileBehavior.SUMMON,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(3)
            .manaDrain(100)
            .damage(8.0f)
            .speed(320.0f)
            .lifetimeTicks(240)
            .build(),
        1, 0.0f, 0.04f, 0.99f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem SUMMON_EGG = registerProjectile(projectileSpec(
        "SUMMON_EGG", "summon_egg", "Summon egg", "召唤蛋", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(2)
            .manaDrain(100)
            .damage(8.0f)
            .speed(320.0f)
            .lifetimeTicks(240)
            .build(),
        1, 0.0f, 0.04f, 0.99f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem SUMMON_HOLLOW_EGG = registerProjectile(projectileSpec(
        "SUMMON_HOLLOW_EGG", "summon_hollow_egg", "Summon hollow egg", "召唤空的蛋", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .manaDrain(30)
            .damage(8.0f)
            .speed(320.0f)
            .castDelaySeconds(-12.0f / 60.0f)
            .lifetimeTicks(240)
            .triggerMode(NoitaSpellTriggerMode.DEATH)
            .triggerDrawCount(1)
            .build(),
        1, 0.0f, 0.04f, 0.99f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem TNTBOX = registerProjectile(projectileSpec(
        "TNTBOX", "tntbox", "Summon Explosive Box", "召唤炸药箱", NoitaProjectileBehavior.FUSED_EXPLOSIVE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(15)
            .manaDrain(40)
            .damage(15.0f)
            .explosionRadius(4.0f)
            .speed(265.0f)
            .castDelaySeconds(30.0f / 60.0f)
            .lifetimeTicks(180)
            .friendlyFire(true)
            .build(),
        1, 0.0f, 0.05f, 0.99f, 0.65f, 1.2f)
    );
    public static final NoitaProjectileSpellItem TNTBOX_BIG = registerProjectile(projectileSpec(
        "TNTBOX_BIG", "tntbox_big", "Summon Large Explosive Box", "召唤大号炸药箱", NoitaProjectileBehavior.FUSED_EXPLOSIVE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(15)
            .manaDrain(40)
            .damage(15.0f)
            .explosionRadius(1.0f)
            .speed(265.0f)
            .castDelaySeconds(30.0f / 60.0f)
            .lifetimeTicks(180)
            .friendlyFire(true)
            .build(),
        1, 0.0f, 0.05f, 0.99f, 0.65f, 1.2f)
    );
    public static final NoitaProjectileSpellItem ACIDSHOT = registerProjectile(projectileSpec(
        "ACIDSHOT", "acidshot", "Acid ball", "酸液球", NoitaProjectileBehavior.EXPLOSIVE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(20)
            .manaDrain(20)
            .damage(15.0f)
            .explosionRadius(1.0f)
            .speed(265.0f)
            .castDelaySeconds(10.0f / 60.0f)
            .lifetimeTicks(180)
            .friendlyFire(true)
            .build(),
        1, 0.0f, 0.05f, 0.99f, 0.65f, 1.2f)
    );
    public static final NoitaProjectileSpellItem THUNDERBALL = registerProjectile(projectileSpec(
        "THUNDERBALL", "thunderball", "Thunder charge", "雷霆放射", NoitaProjectileBehavior.EXPLOSIVE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(3)
            .manaDrain(120)
            .damage(15.0f)
            .explosionRadius(5.0f)
            .speed(110.0f)
            .castDelaySeconds(120.0f / 60.0f)
            .lifetimeTicks(100)
            .friendlyFire(true)
            .build(),
        1, 0.0f, 0.0f, 1.0f, 0.65f, 1.2f)
    );
    public static final NoitaProjectileSpellItem FIREBOMB = registerProjectile(projectileSpec(
        "FIREBOMB", "firebomb", "Firebomb", "火焰炸弹", NoitaProjectileBehavior.FUSED_EXPLOSIVE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(70)
            .manaDrain(10)
            .damage(15.0f)
            .explosionRadius(1.0f)
            .speed(130.0f)
            .lifetimeTicks(70)
            .friendlyFire(true)
            .build(),
        1, 0.0f, 0.0f, 1.0f, 0.65f, 1.2f)
    );
    public static final NoitaProjectileSpellItem DEATH_CROSS = registerProjectile(projectileSpec(
        "DEATH_CROSS", "death_cross", "Death cross", "死亡十字", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .manaDrain(80)
            .damage(6.0f)
            .explosionRadius(2.5f)
            .speed(80.0f)
            .castDelaySeconds(40.0f / 60.0f)
            .lifetimeTicks(68)
            .build(),
        1, 0.0f, 0.0f, 1.0f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem DEATH_CROSS_BIG = registerProjectile(projectileSpec(
        "DEATH_CROSS_BIG", "death_cross_big", "Giga death cross", "巨大死亡十字", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(8)
            .manaDrain(150)
            .damage(6.0f)
            .explosionRadius(3.5f)
            .speed(80.0f)
            .castDelaySeconds(70.0f / 60.0f)
            .lifetimeTicks(50)
            .build(),
        1, 0.0f, 0.0f, 1.0f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem INFESTATION = registerProjectile(projectileSpec(
        "INFESTATION", "infestation", "Infestation", "侵扰", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(150)
            .manaDrain(40)
            .damage(6.0f)
            .speed(550.0f)
            .castDelaySeconds(-2.0f / 60.0f)
            .spreadModifierDegrees(25.0f)
            .lifetimeTicks(600)
            .build(),
        10, 25.0f, 0.0f, 0.9976f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem MIST_RADIOACTIVE = registerProjectile(projectileSpec(
        "MIST_RADIOACTIVE", "mist_radioactive", "Toxic mist", "毒雾", NoitaProjectileBehavior.MIST,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(10)
            .manaDrain(40)
            .damage(2.0f)
            .speed(120.0f)
            .castDelaySeconds(10.0f / 60.0f)
            .lifetimeTicks(220)
            .build(),
        1, 0.0f, 0.0f, 0.94f, 0.65f, 1.4f)
    );
    public static final NoitaProjectileSpellItem MIST_ALCOHOL = registerProjectile(projectileSpec(
        "MIST_ALCOHOL", "mist_alcohol", "mist of spirits", "烈酒云雾", NoitaProjectileBehavior.MIST,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(10)
            .manaDrain(40)
            .damage(2.0f)
            .speed(120.0f)
            .castDelaySeconds(10.0f / 60.0f)
            .lifetimeTicks(220)
            .build(),
        1, 0.0f, 0.0f, 0.94f, 0.65f, 1.4f)
    );
    public static final NoitaProjectileSpellItem MIST_SLIME = registerProjectile(projectileSpec(
        "MIST_SLIME", "mist_slime", "Slime mist", "粘液雾", NoitaProjectileBehavior.MIST,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(10)
            .manaDrain(40)
            .damage(2.0f)
            .speed(120.0f)
            .castDelaySeconds(10.0f / 60.0f)
            .lifetimeTicks(220)
            .build(),
        1, 0.0f, 0.0f, 0.94f, 0.65f, 1.4f)
    );
    public static final NoitaProjectileSpellItem MIST_BLOOD = registerProjectile(projectileSpec(
        "MIST_BLOOD", "mist_blood", "Blood mist", "血雾", NoitaProjectileBehavior.MIST,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(10)
            .manaDrain(40)
            .damage(2.0f)
            .speed(120.0f)
            .castDelaySeconds(10.0f / 60.0f)
            .lifetimeTicks(220)
            .build(),
        1, 0.0f, 0.0f, 0.94f, 0.65f, 1.4f)
    );
    public static final NoitaProjectileSpellItem TELEPORT_PROJECTILE = registerProjectile(projectileSpec(
        "TELEPORT_PROJECTILE", "teleport_projectile", "Teleport bolt", "传送魔弹", NoitaProjectileBehavior.TELEPORT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(80)
            .manaDrain(40)
            .speed(600.0f)
            .castDelaySeconds(0.05f)
            .spreadModifierDegrees(-2.0f)
            .lifetimeTicks(80)
            .build(),
        1, 0.0f, 0.0f, 0.99f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem TELEPORT_PROJECTILE_SHORT = registerProjectile(projectileSpec(
        "TELEPORT_PROJECTILE_SHORT", "teleport_projectile_short", "Small Teleport Bolt", "小传送魔弹", NoitaProjectileBehavior.TELEPORT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(80)
            .manaDrain(20)
            .speed(800.0f)
            .spreadModifierDegrees(-2.0f)
            .lifetimeTicks(20)
            .build(),
        1, 0.0f, 0.0f, 0.99f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem TELEPORT_PROJECTILE_STATIC = registerProjectile(projectileSpec(
        "TELEPORT_PROJECTILE_STATIC", "teleport_projectile_static", "Return", "返回", NoitaProjectileBehavior.TELEPORT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(80)
            .manaDrain(40)
            .explosionRadius(1.0f)
            .speed(80.0f)
            .castDelaySeconds(0.05f)
            .spreadModifierDegrees(-2.0f)
            .lifetimeTicks(240)
            .build(),
        1, 0.0f, 0.0f, 0.9932f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem SWAPPER_PROJECTILE = registerProjectile(projectileSpec(
        "SWAPPER_PROJECTILE", "swapper_projectile", "Swapper", "交换者", NoitaProjectileBehavior.SWAPPER,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .manaDrain(5)
            .speed(600.0f)
            .castDelaySeconds(0.05f)
            .spreadModifierDegrees(-2.0f)
            .criticalChancePercent(5.0f)
            .lifetimeTicks(80)
            .build(),
        1, 0.0f, 0.0f, 0.99f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem TELEPORT_PROJECTILE_CLOSER = registerProjectile(projectileSpec(
        "TELEPORT_PROJECTILE_CLOSER", "teleport_projectile_closer", "Homebringer Teleport Bolt", "“归家”传送魔弹", NoitaProjectileBehavior.TELEPORT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .manaDrain(20)
            .speed(600.0f)
            .spreadModifierDegrees(-2.0f)
            .lifetimeTicks(80)
            .build(),
        1, 0.0f, 0.0f, 0.99f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem NUKE = registerProjectile(projectileSpec(
        "NUKE", "nuke", "Nuke", "核弹", NoitaProjectileBehavior.FUSED_EXPLOSIVE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(1)
            .manaDrain(200)
            .damage(15.0f)
            .explosionRadius(12.0f)
            .speed(265.0f)
            .castDelaySeconds(20.0f / 60.0f)
            .rechargeTimeSeconds(600.0f / 60.0f)
            .speedMultiplier(0.75f)
            .lifetimeTicks(180)
            .friendlyFire(true)
            .build(),
        1, 0.0f, 0.05f, 0.99f, 0.65f, 1.2f)
    );
    public static final NoitaProjectileSpellItem NUKE_GIGA = registerProjectile(projectileSpec(
        "NUKE_GIGA", "nuke_giga", "Giga Nuke", "巨型核弹", NoitaProjectileBehavior.FUSED_EXPLOSIVE,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(1)
            .manaDrain(500)
            .damage(15.0f)
            .explosionRadius(18.0f)
            .speed(265.0f)
            .castDelaySeconds(50.0f / 60.0f)
            .rechargeTimeSeconds(800.0f / 60.0f)
            .speedMultiplier(0.5f)
            .lifetimeTicks(180)
            .friendlyFire(true)
            .build(),
        1, 0.0f, 0.05f, 0.99f, 0.65f, 1.2f)
    );
    public static final NoitaProjectileSpellItem FIREWORK = registerProjectile(projectileSpec(
        "FIREWORK", "firework", "Fireworks!", "烟火！", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .maxUses(25)
            .manaDrain(70)
            .damage(6.0f)
            .explosionRadius(1.5f)
            .speed(75.0f)
            .castDelaySeconds(60.0f / 60.0f)
            .lifetimeTicks(30)
            .build(),
        1, 0.0f, 0.0125f, 1.0f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem RANDOM_PROJECTILE = registerProjectile(projectileSpec(
        "RANDOM_PROJECTILE", "random_projectile", "Random projectile spell", "随机投射物法术", NoitaProjectileBehavior.RANDOM,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.PROJECTILE)
            .manaDrain(20)
            .damage(6.0f)
            .speed(700.0f)
            .lifetimeTicks(60)
            .build(),
        1, 0.0f, 0.0f, 0.99f, 0.65f, 1.0f)
    );

    public static final NoitaProjectileSpellItem BLACK_HOLE_BIG = registerStaticProjectile(projectileSpec(
        "BLACK_HOLE_BIG", "black_hole_big", "Giga black hole", "巨大黑洞", NoitaProjectileBehavior.BLACK_HOLE,
        staticProjectileTemplate(6, 240, 0.0f, 1.0f, 40.0f, 180, 80.0f, 0.0f),
        1, 0.0f, 0.0f, 1.0f, 0.65f, 1.7f)
    );
    public static final NoitaProjectileSpellItem WHITE_HOLE_BIG = registerStaticProjectile(projectileSpec(
        "WHITE_HOLE_BIG", "white_hole_big", "Giga white hole", "巨大白洞", NoitaProjectileBehavior.WHITE_HOLE,
        staticProjectileTemplate(6, 240, 0.0f, 1.0f, 40.0f, 180, 80.0f, 0.0f),
        1, 0.0f, 0.0f, 1.0f, 0.65f, 1.7f)
    );
    public static final NoitaProjectileSpellItem BLACK_HOLE_GIGA = registerStaticProjectile(projectileSpec(
        "BLACK_HOLE_GIGA", "black_hole_giga", "Omega Black Hole", "终结黑洞", NoitaProjectileBehavior.BLACK_HOLE,
        staticProjectileTemplate(6, 500, 0.0f, 1.0f, 28.0f, 240, 120.0f, 100.0f),
        1, 0.0f, 0.0f, 1.0f, 0.65f, 2.2f)
    );
    public static final NoitaProjectileSpellItem WHITE_HOLE_GIGA = registerStaticProjectile(projectileSpec(
        "WHITE_HOLE_GIGA", "white_hole_giga", "Omega white hole", "终结白洞", NoitaProjectileBehavior.WHITE_HOLE,
        staticProjectileTemplate(6, 500, 0.0f, 1.0f, 28.0f, 240, 120.0f, 100.0f),
        1, 0.0f, 0.0f, 1.0f, 0.65f, 2.2f)
    );
    public static final NoitaProjectileSpellItem BOMB_DETONATOR = registerStaticProjectile(projectileSpec(
        "BOMB_DETONATOR", "bomb_detonator", "Explosive Detonator", "炸药引爆器", NoitaProjectileBehavior.BOLT,
        staticProjectileTemplate(-1, 50, 0.0f, 0.0f, 700.0f, 28, 0.0f, 0.0f),
        1, 0.0f, 0.0f, 0.99f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem SWARM_FLY = registerStaticProjectile(projectileSpec(
        "SWARM_FLY", "swarm_fly", "Summon fly swarm", "召唤苍蝇群", NoitaProjectileBehavior.BOLT,
        staticProjectileTemplate(-1, 60, 1.5f, 0.0f, 360.0f, 80, 60.0f, 20.0f),
        4, 6.0f, 0.0f, 0.99f, 0.65f, 0.85f)
    );
    public static final NoitaProjectileSpellItem SWARM_FIREBUG = registerStaticProjectile(projectileSpec(
        "SWARM_FIREBUG", "swarm_firebug", "Summon Firebug swarm", "召唤萤火虫群", NoitaProjectileBehavior.BOLT,
        staticProjectileTemplate(-1, 70, 2.0f, 0.0f, 340.0f, 90, 60.0f, 20.0f),
        3, 12.0f, 0.0f, 0.99f, 0.65f, 0.85f)
    );
    public static final NoitaProjectileSpellItem SWARM_WASP = registerStaticProjectile(projectileSpec(
        "SWARM_WASP", "swarm_wasp", "Summon Wasp swarm", "召唤黄蜂群", NoitaProjectileBehavior.BOLT,
        staticProjectileTemplate(-1, 80, 2.5f, 0.0f, 380.0f, 100, 60.0f, 20.0f),
        5, 24.0f, 0.0f, 0.99f, 0.65f, 0.85f)
    );
    public static final NoitaProjectileSpellItem FRIEND_FLY = registerStaticProjectile(projectileSpec(
        "FRIEND_FLY", "friend_fly", "Summon Friendly fly", "召唤友好苍蝇", NoitaProjectileBehavior.BOLT,
        staticProjectileTemplate(-1, 120, 1.0f, 0.0f, 300.0f, 200, 80.0f, 40.0f),
        1, 24.0f, 0.0f, 0.99f, 0.65f, 0.9f)
    );
    public static final NoitaProjectileSpellItem WALL_HORIZONTAL = registerStaticProjectile(projectileSpec(
        "WALL_HORIZONTAL", "wall_horizontal", "Horizontal barrier", "水平屏障", NoitaProjectileBehavior.BOLT,
        staticProjectileTemplate(80, 70, 0.0f, 0.0f, 220.0f, 120, 5.0f, 0.0f),
        1, 0.0f, 0.0f, 0.96f, 0.65f, 1.3f)
    );
    public static final NoitaProjectileSpellItem WALL_VERTICAL = registerStaticProjectile(projectileSpec(
        "WALL_VERTICAL", "wall_vertical", "Vertical barrier", "垂直屏障", NoitaProjectileBehavior.BOLT,
        staticProjectileTemplate(80, 70, 0.0f, 0.0f, 220.0f, 120, 5.0f, 0.0f),
        1, 0.0f, 0.0f, 0.96f, 0.65f, 1.3f)
    );
    public static final NoitaProjectileSpellItem WALL_SQUARE = registerStaticProjectile(projectileSpec(
        "WALL_SQUARE", "wall_square", "Square barrier", "方形屏障", NoitaProjectileBehavior.BOLT,
        staticProjectileTemplate(20, 70, 0.0f, 0.0f, 180.0f, 140, 20.0f, 0.0f),
        1, 0.0f, 0.0f, 0.96f, 0.65f, 1.5f)
    );
    public static final NoitaProjectileSpellItem PURPLE_EXPLOSION_FIELD = registerStaticProjectile(projectileSpec(
        "PURPLE_EXPLOSION_FIELD", "purple_explosion_field", "Glittering field", "盛大场面", NoitaProjectileBehavior.BOLT,
        staticProjectileTemplate(20, 90, 3.0f, 2.0f, 60.0f, 120, 10.0f, 0.0f),
        1, 0.0f, 0.0f, 0.88f, 0.65f, 1.4f)
    );
    public static final NoitaProjectileSpellItem DELAYED_SPELL = registerStaticProjectile(projectileSpec(
        "DELAYED_SPELL", "delayed_spell", "Delayed spellcast", "延迟施法", NoitaProjectileBehavior.BOLT,
        NoitaSpellTemplate.builder()
            .type(NoitaSpellType.STATIC_PROJECTILE)
            .manaDrain(20)
            .speed(80.0f)
            .castDelaySeconds(10.0f / 60.0f)
            .lifetimeTicks(60)
            .triggerMode(NoitaSpellTriggerMode.DEATH)
            .triggerDrawCount(3)
            .build(),
        1, 0.0f, 0.0f, 0.92f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem DESTRUCTION = registerStaticProjectile(projectileSpec(
        "DESTRUCTION", "destruction", "Destruction", "破坏", NoitaProjectileBehavior.BOLT,
        staticProjectileTemplate(5, 240, 0.0f, 6.0f, 0.0f, 24, 150.0f, 240.0f),
        1, 0.0f, 0.0f, 1.0f, 0.65f, 1.6f)
    );
    public static final NoitaProjectileSpellItem MASS_POLYMORPH = registerStaticProjectile(projectileSpec(
        "MASS_POLYMORPH", "mass_polymorph", "Muodonmuutos", "墨东姆托", NoitaProjectileBehavior.BOLT,
        staticProjectileTemplate(3, 220, 0.0f, 0.0f, 0.0f, 100, 140.0f, 240.0f),
        1, 0.0f, 0.0f, 1.0f, 0.65f, 1.5f)
    );
    public static final NoitaProjectileSpellItem EXPLOSION = registerStaticProjectile(projectileSpec(
        "EXPLOSION", "explosion", "Explosion", "爆炸", NoitaProjectileBehavior.EXPLOSIVE,
        staticProjectileTemplate(30, 80, 0.0f, 3.6f, 360.0f, 18, 3.0f, 0.0f),
        1, 0.0f, 0.0f, 0.98f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem EXPLOSION_LIGHT = registerStaticProjectile(projectileSpec(
        "EXPLOSION_LIGHT", "explosion_light", "Magical Explosion", "魔法爆炸", NoitaProjectileBehavior.EXPLOSIVE,
        staticProjectileTemplate(30, 80, 0.0f, 2.6f, 360.0f, 18, 3.0f, 0.0f),
        1, 0.0f, 0.0f, 0.98f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem FIRE_BLAST = registerStaticProjectile(projectileSpec(
        "FIRE_BLAST", "fireblast", "Explosion of brimstone", "火焰爆炸", NoitaProjectileBehavior.EXPLOSIVE,
        staticProjectileTemplate(30, 10, 0.0f, 1.8f, 360.0f, 16, 3.0f, 0.0f),
        1, 0.0f, 0.0f, 0.98f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem POISON_BLAST = registerStaticProjectile(projectileSpec(
        "POISON_BLAST", "poison_blast", "Explosion of poison", "毒素爆炸", NoitaProjectileBehavior.EXPLOSIVE,
        staticProjectileTemplate(30, 30, 0.0f, 2.0f, 360.0f, 16, 3.0f, 0.0f),
        1, 0.0f, 0.0f, 0.98f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem ALCOHOL_BLAST = registerStaticProjectile(projectileSpec(
        "ALCOHOL_BLAST", "alcohol_blast", "Explosion of spirits", "烈酒爆炸", NoitaProjectileBehavior.EXPLOSIVE,
        staticProjectileTemplate(30, 30, 0.0f, 2.0f, 360.0f, 16, 3.0f, 0.0f),
        1, 0.0f, 0.0f, 0.98f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem THUNDER_BLAST = registerStaticProjectile(projectileSpec(
        "THUNDER_BLAST", "thunder_blast", "Explosion of thunder", "雷霆爆炸", NoitaProjectileBehavior.EXPLOSIVE,
        staticProjectileTemplate(30, 110, 0.0f, 3.0f, 360.0f, 16, 15.0f, 0.0f),
        1, 0.0f, 0.0f, 0.98f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem BERSERK_FIELD = registerStaticProjectile(projectileSpec(
        "BERSERK_FIELD", "berserk_field", "Circle of fervour", "激情之环", NoitaProjectileBehavior.BOLT,
        staticProjectileTemplate(15, 30, 0.0f, 0.0f, 90.0f, 160, 15.0f, 0.0f),
        1, 0.0f, 0.0f, 0.9f, 0.65f, 1.2f)
    );
    public static final NoitaProjectileSpellItem POLYMORPH_FIELD = registerStaticProjectile(projectileSpec(
        "POLYMORPH_FIELD", "polymorph_field", "Circle of transmogrification", "变形之环", NoitaProjectileBehavior.BOLT,
        staticProjectileTemplate(5, 50, 0.0f, 0.0f, 90.0f, 160, 15.0f, 0.0f),
        1, 0.0f, 0.0f, 0.9f, 0.65f, 1.2f)
    );
    public static final NoitaProjectileSpellItem CHAOS_POLYMORPH_FIELD = registerStaticProjectile(projectileSpec(
        "CHAOS_POLYMORPH_FIELD", "chaos_polymorph_field", "Circle of unstable metamorphosis", "不稳变形之环", NoitaProjectileBehavior.BOLT,
        staticProjectileTemplate(10, 20, 0.0f, 0.0f, 90.0f, 160, 15.0f, 0.0f),
        1, 0.0f, 0.0f, 0.9f, 0.65f, 1.2f)
    );
    public static final NoitaProjectileSpellItem ELECTROCUTION_FIELD = registerStaticProjectile(projectileSpec(
        "ELECTROCUTION_FIELD", "electrocution_field", "Circle of thunder", "雷霆之环", NoitaProjectileBehavior.BOLT,
        staticProjectileTemplate(15, 60, 2.0f, 0.0f, 90.0f, 160, 15.0f, 0.0f),
        1, 0.0f, 0.0f, 0.9f, 0.65f, 1.2f)
    );
    public static final NoitaProjectileSpellItem FREEZE_FIELD = registerStaticProjectile(projectileSpec(
        "FREEZE_FIELD", "freeze_field", "Circle of stillness", "静止之环", NoitaProjectileBehavior.BOLT,
        staticProjectileTemplate(15, 50, 0.0f, 0.0f, 90.0f, 160, 15.0f, 0.0f),
        1, 0.0f, 0.0f, 0.9f, 0.65f, 1.2f)
    );
    public static final NoitaProjectileSpellItem REGENERATION_FIELD = registerStaticProjectile(projectileSpec(
        "REGENERATION_FIELD", "regeneration_field", "Circle of vigour", "活力之环", NoitaProjectileBehavior.BOLT,
        staticProjectileTemplate(2, 80, 0.0f, 0.0f, 90.0f, 160, 15.0f, 0.0f),
        1, 0.0f, 0.0f, 0.9f, 0.65f, 1.2f)
    );
    public static final NoitaProjectileSpellItem TELEPORTATION_FIELD = registerStaticProjectile(projectileSpec(
        "TELEPORTATION_FIELD", "teleportation_field", "Circle of displacement", "位移之环", NoitaProjectileBehavior.TELEPORT,
        staticProjectileTemplate(15, 30, 0.0f, 0.0f, 90.0f, 160, 15.0f, 0.0f),
        1, 0.0f, 0.0f, 0.9f, 0.65f, 1.2f)
    );
    public static final NoitaProjectileSpellItem LEVITATION_FIELD = registerStaticProjectile(projectileSpec(
        "LEVITATION_FIELD", "levitation_field", "Circle of buoyancy", "浮力之环", NoitaProjectileBehavior.BOLT,
        staticProjectileTemplate(15, 10, 0.0f, 0.0f, 90.0f, 160, 15.0f, 0.0f),
        1, 0.0f, 0.0f, 0.9f, 0.65f, 1.2f)
    );
    public static final NoitaProjectileSpellItem SHIELD_FIELD = registerStaticProjectile(projectileSpec(
        "SHIELD_FIELD", "shield_field", "Circle of shielding", "遮蔽之环", NoitaProjectileBehavior.BOLT,
        staticProjectileTemplate(10, 20, 0.0f, 0.0f, 90.0f, 160, 15.0f, 0.0f),
        1, 0.0f, 0.0f, 0.9f, 0.65f, 1.2f)
    );
    public static final NoitaProjectileSpellItem PROJECTILE_TRANSMUTATION_FIELD = registerStaticProjectile(projectileSpec(
        "PROJECTILE_TRANSMUTATION_FIELD", "projectile_transmutation_field", "Projectile transmutation field", "投射物转化领域", NoitaProjectileBehavior.BOLT,
        staticProjectileTemplate(6, 120, 0.0f, 0.0f, 90.0f, 180, 15.0f, 0.0f),
        1, 0.0f, 0.0f, 0.9f, 0.65f, 1.25f)
    );
    public static final NoitaProjectileSpellItem PROJECTILE_THUNDER_FIELD = registerStaticProjectile(projectileSpec(
        "PROJECTILE_THUNDER_FIELD", "projectile_thunder_field", "Projectile thunder field", "投射物雷电领域", NoitaProjectileBehavior.BOLT,
        staticProjectileTemplate(6, 140, 2.0f, 0.0f, 90.0f, 180, 15.0f, 0.0f),
        1, 0.0f, 0.0f, 0.9f, 0.65f, 1.25f)
    );
    public static final NoitaProjectileSpellItem PROJECTILE_GRAVITY_FIELD = registerStaticProjectile(projectileSpec(
        "PROJECTILE_GRAVITY_FIELD", "projectile_gravity_field", "Projectile gravity field", "投射物重力领域", NoitaProjectileBehavior.BOLT,
        staticProjectileTemplate(6, 120, 0.0f, 0.0f, 90.0f, 180, 15.0f, 0.0f),
        1, 0.0f, 0.0f, 0.9f, 0.65f, 1.25f)
    );
    public static final NoitaProjectileSpellItem VACUUM_POWDER = registerStaticProjectile(projectileSpec(
        "VACUUM_POWDER", "vacuum_powder", "Powder Vacuum Field", "粉末真空场", NoitaProjectileBehavior.BLACK_HOLE,
        staticProjectileTemplate(20, 40, 0.0f, 0.0f, 80.0f, 120, 10.0f, 0.0f),
        1, 0.0f, 0.0f, 0.92f, 0.65f, 1.2f)
    );
    public static final NoitaProjectileSpellItem VACUUM_LIQUID = registerStaticProjectile(projectileSpec(
        "VACUUM_LIQUID", "vacuum_liquid", "Liquid Vacuum Field", "液体真空场", NoitaProjectileBehavior.BLACK_HOLE,
        staticProjectileTemplate(20, 40, 0.0f, 0.0f, 80.0f, 120, 10.0f, 0.0f),
        1, 0.0f, 0.0f, 0.92f, 0.65f, 1.2f)
    );
    public static final NoitaProjectileSpellItem VACUUM_ENTITIES = registerStaticProjectile(projectileSpec(
        "VACUUM_ENTITIES", "vacuum_entities", "Vacuum Field", "真空场", NoitaProjectileBehavior.BLACK_HOLE,
        staticProjectileTemplate(20, 50, 0.0f, 0.0f, 80.0f, 120, 10.0f, 0.0f),
        1, 0.0f, 0.0f, 0.92f, 0.65f, 1.2f)
    );
    public static final NoitaProjectileSpellItem CLOUD_WATER = registerStaticProjectile(projectileSpec(
        "CLOUD_WATER", "cloud_water", "Rain cloud", "雨云", NoitaProjectileBehavior.BOLT,
        staticProjectileTemplate(10, 30, 0.0f, 0.0f, 80.0f, 160, 15.0f, 0.0f),
        1, 0.0f, 0.0f, 0.94f, 0.65f, 1.1f)
    );
    public static final NoitaProjectileSpellItem CLOUD_OIL = registerStaticProjectile(projectileSpec(
        "CLOUD_OIL", "cloud_oil", "Oil cloud", "油脂之云", NoitaProjectileBehavior.BOLT,
        staticProjectileTemplate(15, 20, 0.0f, 0.0f, 80.0f, 160, 15.0f, 0.0f),
        1, 0.0f, 0.0f, 0.94f, 0.65f, 1.1f)
    );
    public static final NoitaProjectileSpellItem CLOUD_BLOOD = registerStaticProjectile(projectileSpec(
        "CLOUD_BLOOD", "cloud_blood", "Blood cloud", "血云", NoitaProjectileBehavior.BOLT,
        staticProjectileTemplate(3, 60, 0.0f, 0.0f, 80.0f, 160, 30.0f, 0.0f),
        1, 0.0f, 0.0f, 0.94f, 0.65f, 1.1f)
    );
    public static final NoitaProjectileSpellItem CLOUD_ACID = registerStaticProjectile(projectileSpec(
        "CLOUD_ACID", "cloud_acid", "Acid cloud", "酸云", NoitaProjectileBehavior.BOLT,
        staticProjectileTemplate(8, 90, 1.5f, 0.0f, 80.0f, 160, 15.0f, 0.0f),
        1, 0.0f, 0.0f, 0.94f, 0.65f, 1.1f)
    );
    public static final NoitaProjectileSpellItem CLOUD_THUNDER = registerStaticProjectile(projectileSpec(
        "CLOUD_THUNDER", "cloud_thunder", "Thundercloud", "雷云", NoitaProjectileBehavior.BOLT,
        staticProjectileTemplate(5, 90, 2.0f, 0.0f, 80.0f, 160, 30.0f, 0.0f),
        1, 0.0f, 0.0f, 0.94f, 0.65f, 1.1f)
    );
    public static final NoitaProjectileSpellItem RANDOM_STATIC_PROJECTILE = registerStaticProjectile(projectileSpec(
        "RANDOM_STATIC_PROJECTILE", "random_static_projectile", "Random static projectile spell", "随机静态投射物法术", NoitaProjectileBehavior.RANDOM,
        staticProjectileTemplate(-1, 20, 0.0f, 0.0f, 90.0f, 120, 0.0f, 0.0f),
        1, 0.0f, 0.0f, 0.94f, 0.65f, 1.0f)
    );
    public static final NoitaProjectileSpellItem METEOR_RAIN = registerStaticProjectile(projectileSpec(
        "METEOR_RAIN", "meteor_rain", "Meteorisade", "陨石雨", NoitaProjectileBehavior.BOLT,
        staticProjectileTemplate(2, 225, 6.0f, 3.0f, 0.0f, 140, 100.0f, 60.0f),
        1, 0.0f, 0.0f, 1.0f, 0.65f, 1.3f)
    );
    public static final NoitaProjectileSpellItem WORM_RAIN = registerStaticProjectile(projectileSpec(
        "WORM_RAIN", "worm_rain", "Matosade", "蠕虫雨", NoitaProjectileBehavior.BOLT,
        staticProjectileTemplate(2, 225, 4.0f, 0.0f, 0.0f, 140, 100.0f, 60.0f),
        1, 0.0f, 0.0f, 1.0f, 0.65f, 1.3f)
    );

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
    public static final NoitaSpellItem SPREAD_REDUCE = registerModifier("spread_reduce", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(1)
        .spreadModifierDegrees(-60.0f)
        .build()
    );
    public static final NoitaSpellItem HEAVY_SPREAD = registerModifier("heavy_spread", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .manaDrain(2)
        .castDelaySeconds(-7.0f / 60.0f)
        .rechargeTimeSeconds(-15.0f / 60.0f)
        .spreadModifierDegrees(720.0f)
        .build()
    );
    public static final NoitaSpellItem RECHARGE = registerModifier("recharge", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(12)
        .castDelaySeconds(-10.0f / 60.0f)
        .rechargeTimeSeconds(-20.0f / 60.0f)
        .build()
    );
    public static final NoitaSpellItem LIFETIME = registerModifier("lifetime", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(40)
        .castDelaySeconds(13.0f / 60.0f)
        .lifetimeModifierTicks(75)
        .build()
    );
    public static final NoitaSpellItem LIFETIME_DOWN = registerModifier("lifetime_down", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(10)
        .castDelaySeconds(-15.0f / 60.0f)
        .lifetimeModifierTicks(-42)
        .build()
    );
    public static final NoitaSpellItem NOLLA = registerModifier("nolla", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(1)
        .castDelaySeconds(-15.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.NOLLA)
        .build()
    );
    public static final NoitaSpellItem EXPLOSION_REMOVE = registerModifier("explosion_remove", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(0)
        .castDelaySeconds(-15.0f / 60.0f)
        .explosionRadius(-30.0f)
        .modifierEffect(NoitaModifierEffect.REMOVE_EXPLOSION)
        .build()
    );
    public static final NoitaSpellItem EXPLOSION_TINY = registerModifier("explosion_tiny", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(40)
        .castDelaySeconds(15.0f / 60.0f)
        .explosionRadius(-30.0f)
        .build()
    );
    public static final NoitaSpellItem ADD_MANA = registerModifier("add_mana", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(-30)
        .castDelaySeconds(10.0f / 60.0f)
        .build()
    );
    public static final NoitaSpellItem GRAVITY = registerModifier("gravity", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(100)
        .manaDrain(1)
        .gravity(600.0f)
        .build()
    );
    public static final NoitaSpellItem GRAVITY_ANTI = registerModifier("gravity_anti", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(100)
        .manaDrain(1)
        .gravity(-600.0f)
        .build()
    );
    public static final NoitaSpellItem SINEWAVE = registerModifier("sinewave", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(0)
        .speedMultiplier(2.0f)
        .modifierEffect(NoitaModifierEffect.SINEWAVE)
        .build()
    );
    public static final NoitaSpellItem CHAOTIC_ARC = registerModifier("chaotic_arc", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(0)
        .speedMultiplier(2.0f)
        .modifierEffect(NoitaModifierEffect.CHAOTIC_ARC)
        .build()
    );
    public static final NoitaSpellItem PINGPONG_PATH = registerModifier("pingpong_path", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(0)
        .lifetimeModifierTicks(25)
        .modifierEffect(NoitaModifierEffect.PINGPONG_PATH)
        .build()
    );
    public static final NoitaSpellItem AVOIDING_ARC = registerModifier("avoiding_arc", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(0)
        .castDelaySeconds(10.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.AVOIDING_ARC)
        .build()
    );
    public static final NoitaSpellItem FLOATING_ARC = registerModifier("floating_arc", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(0)
        .castDelaySeconds(10.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.FLOATING_ARC)
        .build()
    );
    public static final NoitaSpellItem FLY_DOWNWARDS = registerModifier("fly_downwards", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(0)
        .castDelaySeconds(-8.0f / 60.0f)
        .speedMultiplier(1.2f)
        .modifierEffect(NoitaModifierEffect.FLY_DOWNWARDS)
        .build()
    );
    public static final NoitaSpellItem FLY_UPWARDS = registerModifier("fly_upwards", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(0)
        .castDelaySeconds(-8.0f / 60.0f)
        .speedMultiplier(1.2f)
        .modifierEffect(NoitaModifierEffect.FLY_UPWARDS)
        .build()
    );
    public static final NoitaSpellItem HORIZONTAL_ARC = registerModifier("horizontal_arc", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(0)
        .damage(1.8f)
        .castDelaySeconds(-6.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.HORIZONTAL_ARC)
        .build()
    );
    public static final NoitaSpellItem LINE_ARC = registerModifier("line_arc", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(0)
        .damage(1.2f)
        .castDelaySeconds(-4.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.LINE_ARC)
        .build()
    );
    public static final NoitaSpellItem ORBIT_SHOT = registerModifier("orbit_shot", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(0)
        .damage(0.6f)
        .castDelaySeconds(-6.0f / 60.0f)
        .lifetimeModifierTicks(25)
        .modifierEffect(NoitaModifierEffect.ORBIT_SHOT)
        .build()
    );
    public static final NoitaSpellItem SPIRALING_SHOT = registerModifier("spiraling_shot", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(0)
        .damage(0.6f)
        .castDelaySeconds(-6.0f / 60.0f)
        .lifetimeModifierTicks(50)
        .modifierEffect(NoitaModifierEffect.SPIRALING_SHOT)
        .build()
    );
    public static final NoitaSpellItem PHASING_ARC = registerModifier("phasing_arc", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(2)
        .castDelaySeconds(-12.0f / 60.0f)
        .lifetimeModifierTicks(80)
        .speedMultiplier(0.33f)
        .modifierEffect(NoitaModifierEffect.PHASING_ARC)
        .build()
    );
    public static final NoitaSpellItem TRUE_ORBIT = registerModifier("true_orbit", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(2)
        .damage(0.6f)
        .castDelaySeconds(-20.0f / 60.0f)
        .lifetimeModifierTicks(80)
        .modifierEffect(NoitaModifierEffect.TRUE_ORBIT)
        .build()
    );
    public static final NoitaSpellItem BOUNCE = registerModifier("bounce", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(0)
        .bounceCount(10)
        .build()
    );
    public static final NoitaSpellItem REMOVE_BOUNCE = registerModifier("remove_bounce", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(0)
        .modifierEffect(NoitaModifierEffect.REMOVE_BOUNCE)
        .build()
    );
    public static final NoitaSpellItem HOMING = registerModifier("homing", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(100)
        .manaDrain(70)
        .modifierEffect(NoitaModifierEffect.HOMING)
        .build()
    );
    public static final NoitaSpellItem ANTI_HOMING = registerModifier("anti_homing", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(100)
        .manaDrain(1)
        .castDelaySeconds(-20.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.ANTI_HOMING)
        .build()
    );
    public static final NoitaSpellItem HOMING_WAND = registerModifier("homing_wand", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(100)
        .manaDrain(200)
        .modifierEffect(NoitaModifierEffect.HOMING_WAND)
        .build()
    );
    public static final NoitaSpellItem HOMING_SHORT = registerModifier("homing_short", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(100)
        .manaDrain(40)
        .modifierEffect(NoitaModifierEffect.HOMING_SHORT)
        .build()
    );
    public static final NoitaSpellItem HOMING_ROTATE = registerModifier("homing_rotate", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(100)
        .manaDrain(40)
        .modifierEffect(NoitaModifierEffect.HOMING_ROTATE)
        .build()
    );
    public static final NoitaSpellItem HOMING_SHOOTER = registerModifier("homing_shooter", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(100)
        .manaDrain(10)
        .modifierEffect(NoitaModifierEffect.HOMING_SHOOTER)
        .build()
    );
    public static final NoitaSpellItem HOMING_ACCELERATING = registerModifier("homing_accelerating", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(100)
        .manaDrain(60)
        .modifierEffect(NoitaModifierEffect.HOMING_ACCELERATING)
        .build()
    );
    public static final NoitaSpellItem HOMING_CURSOR = registerModifier("homing_cursor", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(100)
        .manaDrain(30)
        .modifierEffect(NoitaModifierEffect.HOMING_CURSOR)
        .build()
    );
    public static final NoitaSpellItem HOMING_AREA = registerModifier("homing_area", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(100)
        .manaDrain(60)
        .castDelaySeconds(8.0f / 60.0f)
        .spreadModifierDegrees(6.0f)
        .speedMultiplier(0.75f)
        .modifierEffect(NoitaModifierEffect.HOMING_AREA)
        .build()
    );
    public static final NoitaSpellItem PIERCING_SHOT = registerModifier("piercing_shot", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(100)
        .manaDrain(140)
        .damage(-3.6f)
        .piercing(true)
        .friendlyFire(true)
        .modifierEffect(NoitaModifierEffect.PIERCING_SHOT)
        .build()
    );
    public static final NoitaSpellItem DAMAGE = registerModifier("damage", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(50)
        .manaDrain(5)
        .damage(2.4f)
        .castDelaySeconds(5.0f / 60.0f)
        .build()
    );
    public static final NoitaSpellItem BLOODLUST = registerModifier("bloodlust", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(100)
        .manaDrain(2)
        .damage(7.8f)
        .castDelaySeconds(8.0f / 60.0f)
        .spreadModifierDegrees(6.0f)
        .friendlyFire(true)
        .build()
    );
    public static final NoitaSpellItem CRITICAL_HIT = registerModifier("critical_hit", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(50)
        .manaDrain(5)
        .criticalChancePercent(15.0f)
        .build()
    );
    public static final NoitaSpellItem HEAVY_SHOT = registerModifier("heavy_shot", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(50)
        .manaDrain(7)
        .damage(10.5f)
        .castDelaySeconds(10.0f / 60.0f)
        .speedMultiplier(0.3f)
        .build()
    );
    public static final NoitaSpellItem LIGHT_SHOT = registerModifier("light_shot", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(50)
        .manaDrain(5)
        .damage(-6.0f)
        .explosionRadius(-10.0f)
        .castDelaySeconds(-3.0f / 60.0f)
        .spreadModifierDegrees(-6.0f)
        .speedMultiplier(7.5f)
        .build()
    );
    public static final NoitaSpellItem KNOCKBACK = registerModifier("knockback", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(5)
        .knockbackForce(5.0f)
        .build()
    );
    public static final NoitaSpellItem SPEED = registerModifier("speed", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(100)
        .manaDrain(3)
        .speedMultiplier(2.5f)
        .build()
    );
    public static final NoitaSpellItem ACCELERATING_SHOT = registerModifier("accelerating_shot", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(50)
        .manaDrain(20)
        .castDelaySeconds(8.0f / 60.0f)
        .speedMultiplier(0.32f)
        .modifierEffect(NoitaModifierEffect.ACCELERATING_SHOT)
        .build()
    );
    public static final NoitaSpellItem DECELERATING_SHOT = registerModifier("decelerating_shot", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(50)
        .manaDrain(10)
        .castDelaySeconds(-8.0f / 60.0f)
        .speedMultiplier(1.68f)
        .modifierEffect(NoitaModifierEffect.DECELERATING_SHOT)
        .build()
    );
    public static final NoitaSpellItem EXPLOSIVE_PROJECTILE = registerModifier("explosive_projectile", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(50)
        .manaDrain(30)
        .explosionRadius(15.0f)
        .castDelaySeconds(40.0f / 60.0f)
        .speedMultiplier(0.75f)
        .build()
    );
    public static final NoitaSpellItem ELECTRIC_CHARGE = registerModifier("electric_charge", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(50)
        .manaDrain(8)
        .modifierEffect(NoitaModifierEffect.ELECTRIC_CHARGE)
        .build()
    );
    public static final NoitaSpellItem FREEZE = registerModifier("freeze", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(50)
        .manaDrain(10)
        .modifierEffect(NoitaModifierEffect.FREEZE_CHARGE)
        .build()
    );
    public static final NoitaSpellItem ACID_TRAIL = registerModifier("acid_trail", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(50)
        .manaDrain(15)
        .modifierEffect(NoitaModifierEffect.ACID_TRAIL)
        .build()
    );
    public static final NoitaSpellItem POISON_TRAIL = registerModifier("poison_trail", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(50)
        .manaDrain(10)
        .modifierEffect(NoitaModifierEffect.POISON_TRAIL)
        .build()
    );
    public static final NoitaSpellItem OIL_TRAIL = registerModifier("oil_trail", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(50)
        .manaDrain(10)
        .modifierEffect(NoitaModifierEffect.OIL_TRAIL)
        .build()
    );
    public static final NoitaSpellItem WATER_TRAIL = registerModifier("water_trail", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(50)
        .manaDrain(10)
        .modifierEffect(NoitaModifierEffect.WATER_TRAIL)
        .build()
    );
    public static final NoitaSpellItem GUNPOWDER_TRAIL = registerModifier("gunpowder_trail", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(50)
        .manaDrain(10)
        .modifierEffect(NoitaModifierEffect.GUNPOWDER_TRAIL)
        .build()
    );
    public static final NoitaSpellItem FIRE_TRAIL = registerModifier("fire_trail", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(50)
        .manaDrain(10)
        .modifierEffect(NoitaModifierEffect.FIRE_TRAIL)
        .build()
    );
    public static final NoitaSpellItem BURN_TRAIL = registerModifier("burn_trail", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(120)
        .manaDrain(5)
        .modifierEffect(NoitaModifierEffect.BURN_TRAIL)
        .build()
    );
    public static final NoitaSpellItem LIGHT = registerModifier("light", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .manaDrain(1)
        .trailLightStacks(1)
        .modifierEffect(NoitaModifierEffect.LIGHT)
        .build()
    );
    public static final NoitaSpellItem RAINBOW_TRAIL = registerModifier("rainbow_trail", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(50)
        .manaDrain(0)
        .modifierEffect(NoitaModifierEffect.RAINBOW_TRAIL)
        .build()
    );
    public static final NoitaSpellItem SLOW_BUT_STEADY = registerModifier("slow_but_steady", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .manaDrain(0)
        .rechargeTimeSeconds(90.0f / 60.0f)
        .recoil(-80.0f)
        .build()
    );
    public static final NoitaSpellItem LASER_EMITTER_WIDER = registerModifier("laser_emitter_wider", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(120)
        .manaDrain(10)
        .modifierEffect(NoitaModifierEffect.LASER_EMITTER_WIDER)
        .build()
    );
    public static final NoitaSpellItem QUANTUM_SPLIT = registerModifier("quantum_split", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .manaDrain(10)
        .castDelaySeconds(5.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.QUANTUM_SPLIT)
        .build()
    );
    public static final NoitaSpellItem AUTOAIM = registerModifier("autoaim", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .manaDrain(25)
        .modifierEffect(NoitaModifierEffect.AUTOAIM)
        .build()
    );
    public static final NoitaSpellItem CLIPPING_SHOT = registerModifier("clipping_shot", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(100)
        .manaDrain(160)
        .castDelaySeconds(50.0f / 60.0f)
        .rechargeTimeSeconds(40.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.CLIPPING_SHOT)
        .build()
    );
    public static final NoitaSpellItem DAMAGE_RANDOM = registerModifier("damage_random", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(50)
        .manaDrain(15)
        .damage(2.4f)
        .castDelaySeconds(5.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.DAMAGE_RANDOM)
        .build()
    );
    public static final NoitaSpellItem DAMAGE_FOREVER = registerModifier("damage_forever", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(20)
        .manaDrain(0)
        .damage(3.0f)
        .castDelaySeconds(15.0f / 60.0f)
        .rechargeTimeSeconds(10.0f / 60.0f)
        .recoil(10.0f)
        .build()
    );
    public static final NoitaSpellItem AREA_DAMAGE = registerModifier("area_damage", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(100)
        .manaDrain(30)
        .modifierEffect(NoitaModifierEffect.AREA_DAMAGE)
        .build()
    );
    public static final NoitaSpellItem SPELLS_TO_POWER = registerModifier("spells_to_power", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(20)
        .manaDrain(110)
        .castDelaySeconds(40.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.SPELLS_TO_POWER)
        .build()
    );
    public static final NoitaSpellItem ESSENCE_TO_POWER = registerModifier("essence_to_power", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(20)
        .manaDrain(110)
        .castDelaySeconds(20.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.ESSENCE_TO_POWER)
        .build()
    );
    public static final NoitaSpellItem ZERO_DAMAGE = registerModifier("zero_damage", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(50)
        .manaDrain(5)
        .damage(-1000.0f)
        .explosionRadius(-1000.0f)
        .criticalChancePercent(-1000.0f)
        .castDelaySeconds(-5.0f / 60.0f)
        .lifetimeModifierTicks(280)
        .recoil(-10.0f)
        .modifierEffect(NoitaModifierEffect.ZERO_DAMAGE)
        .build()
    );
    public static final NoitaSpellItem RECOIL = registerModifier("recoil", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(5)
        .recoil(200.0f)
        .build()
    );
    public static final NoitaSpellItem RECOIL_DAMPER = registerModifier("recoil_damper", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(5)
        .recoil(-200.0f)
        .build()
    );
    public static final NoitaSpellItem CLUSTERMOD = registerModifier("clustermod", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(50)
        .manaDrain(30)
        .explosionRadius(4.0f)
        .castDelaySeconds(20.0f / 60.0f)
        .recoil(10.0f)
        .modifierEffect(NoitaModifierEffect.CLUSTERMOD)
        .build()
    );
    public static final NoitaSpellItem WATER_TO_POISON = registerModifier("water_to_poison", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(50)
        .manaDrain(30)
        .castDelaySeconds(10.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.WATER_TO_POISON)
        .build()
    );
    public static final NoitaSpellItem BLOOD_TO_ACID = registerModifier("blood_to_acid", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(50)
        .manaDrain(30)
        .castDelaySeconds(10.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.BLOOD_TO_ACID)
        .build()
    );
    public static final NoitaSpellItem LAVA_TO_BLOOD = registerModifier("lava_to_blood", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(50)
        .manaDrain(30)
        .castDelaySeconds(10.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.LAVA_TO_BLOOD)
        .build()
    );
    public static final NoitaSpellItem LIQUID_TO_EXPLOSION = registerModifier("liquid_to_explosion", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(50)
        .manaDrain(40)
        .castDelaySeconds(20.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.LIQUID_TO_EXPLOSION)
        .build()
    );
    public static final NoitaSpellItem TOXIC_TO_ACID = registerModifier("toxic_to_acid", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(50)
        .manaDrain(50)
        .castDelaySeconds(10.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.TOXIC_TO_ACID)
        .build()
    );
    public static final NoitaSpellItem STATIC_TO_SAND = registerModifier("static_to_sand", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(8)
        .manaDrain(70)
        .castDelaySeconds(60.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.STATIC_TO_SAND)
        .build()
    );
    public static final NoitaSpellItem TRANSMUTATION = registerModifier("transmutation", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(8)
        .manaDrain(80)
        .castDelaySeconds(20.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.TRANSMUTATION)
        .build()
    );
    public static final NoitaSpellItem RANDOM_EXPLOSION = registerModifier("random_explosion", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(30)
        .manaDrain(120)
        .castDelaySeconds(40.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.RANDOM_EXPLOSION)
        .build()
    );
    public static final NoitaSpellItem NECROMANCY = registerModifier("necromancy", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(50)
        .manaDrain(20)
        .castDelaySeconds(10.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.NECROMANCY)
        .build()
    );
    public static final NoitaSpellItem MATTER_EATER = registerModifier("matter_eater", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(10)
        .manaDrain(120)
        .modifierEffect(NoitaModifierEffect.MATTER_EATER)
        .build()
    );
    public static final NoitaSpellItem HITFX_BURNING_CRITICAL_HIT = registerModifier("hitfx_burning_critical_hit", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(50)
        .manaDrain(10)
        .modifierEffect(NoitaModifierEffect.HITFX_BURNING_CRITICAL_HIT)
        .build()
    );
    public static final NoitaSpellItem HITFX_CRITICAL_WATER = registerModifier("hitfx_critical_water", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(50)
        .manaDrain(10)
        .modifierEffect(NoitaModifierEffect.HITFX_CRITICAL_WATER)
        .build()
    );
    public static final NoitaSpellItem HITFX_CRITICAL_OIL = registerModifier("hitfx_critical_oil", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(50)
        .manaDrain(10)
        .modifierEffect(NoitaModifierEffect.HITFX_CRITICAL_OIL)
        .build()
    );
    public static final NoitaSpellItem HITFX_CRITICAL_BLOOD = registerModifier("hitfx_critical_blood", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(50)
        .manaDrain(10)
        .modifierEffect(NoitaModifierEffect.HITFX_CRITICAL_BLOOD)
        .build()
    );
    public static final NoitaSpellItem HITFX_TOXIC_CHARM = registerModifier("hitfx_toxic_charm", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(50)
        .manaDrain(70)
        .modifierEffect(NoitaModifierEffect.HITFX_TOXIC_CHARM)
        .build()
    );
    public static final NoitaSpellItem HITFX_EXPLOSION_SLIME = registerModifier("hitfx_explosion_slime", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(50)
        .manaDrain(20)
        .modifierEffect(NoitaModifierEffect.HITFX_EXPLOSION_SLIME)
        .build()
    );
    public static final NoitaSpellItem HITFX_EXPLOSION_SLIME_GIGA = registerModifier("hitfx_explosion_slime_giga", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(20)
        .manaDrain(200)
        .modifierEffect(NoitaModifierEffect.HITFX_EXPLOSION_SLIME_GIGA)
        .build()
    );
    public static final NoitaSpellItem HITFX_EXPLOSION_ALCOHOL = registerModifier("hitfx_explosion_alcohol", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(50)
        .manaDrain(20)
        .modifierEffect(NoitaModifierEffect.HITFX_EXPLOSION_ALCOHOL)
        .build()
    );
    public static final NoitaSpellItem HITFX_EXPLOSION_ALCOHOL_GIGA = registerModifier("hitfx_explosion_alcohol_giga", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(20)
        .manaDrain(200)
        .modifierEffect(NoitaModifierEffect.HITFX_EXPLOSION_ALCOHOL_GIGA)
        .build()
    );
    public static final NoitaSpellItem HITFX_PETRIFY = registerModifier("hitfx_petrify", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .manaDrain(10)
        .modifierEffect(NoitaModifierEffect.HITFX_PETRIFY)
        .build()
    );
    public static final NoitaSpellItem ROCKET_DOWNWARDS = registerModifier("rocket_downwards", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .manaDrain(90)
        .castDelaySeconds(25.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.ROCKET_DOWNWARDS)
        .build()
    );
    public static final NoitaSpellItem ROCKET_OCTAGON = registerModifier("rocket_octagon", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .manaDrain(100)
        .castDelaySeconds(20.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.ROCKET_OCTAGON)
        .build()
    );
    public static final NoitaSpellItem FIZZLE = registerModifier("fizzle", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(0)
        .castDelaySeconds(-10.0f / 60.0f)
        .speedMultiplier(1.2f)
        .modifierEffect(NoitaModifierEffect.FIZZLE)
        .build()
    );
    public static final NoitaSpellItem BOUNCE_EXPLOSION = registerModifier("bounce_explosion", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(20)
        .castDelaySeconds(25.0f / 60.0f)
        .recoil(20.0f)
        .bounceCount(1)
        .modifierEffect(NoitaModifierEffect.BOUNCE_EXPLOSION)
        .build()
    );
    public static final NoitaSpellItem BOUNCE_SPARK = registerModifier("bounce_spark", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(20)
        .castDelaySeconds(8.0f / 60.0f)
        .recoil(5.0f)
        .bounceCount(1)
        .modifierEffect(NoitaModifierEffect.BOUNCE_SPARK)
        .build()
    );
    public static final NoitaSpellItem BOUNCE_LASER = registerModifier("bounce_laser", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(30)
        .castDelaySeconds(12.0f / 60.0f)
        .recoil(5.0f)
        .bounceCount(1)
        .modifierEffect(NoitaModifierEffect.BOUNCE_LASER)
        .build()
    );
    public static final NoitaSpellItem BOUNCE_LASER_EMITTER = registerModifier("bounce_laser_emitter", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(40)
        .castDelaySeconds(12.0f / 60.0f)
        .recoil(5.0f)
        .bounceCount(1)
        .modifierEffect(NoitaModifierEffect.BOUNCE_LASER_EMITTER)
        .build()
    );
    public static final NoitaSpellItem BOUNCE_LARPA = registerModifier("bounce_larpa", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(80)
        .castDelaySeconds(32.0f / 60.0f)
        .recoil(10.0f)
        .bounceCount(1)
        .modifierEffect(NoitaModifierEffect.BOUNCE_LARPA)
        .build()
    );
    public static final NoitaSpellItem BOUNCE_SMALL_EXPLOSION = registerModifier("bounce_small_explosion", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(10)
        .castDelaySeconds(9.0f / 60.0f)
        .recoil(10.0f)
        .bounceCount(1)
        .modifierEffect(NoitaModifierEffect.BOUNCE_SMALL_EXPLOSION)
        .build()
    );
    public static final NoitaSpellItem BOUNCE_LIGHTNING = registerModifier("bounce_lightning", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(150)
        .manaDrain(40)
        .castDelaySeconds(25.0f / 60.0f)
        .recoil(10.0f)
        .bounceCount(1)
        .modifierEffect(NoitaModifierEffect.BOUNCE_LIGHTNING)
        .build()
    );
    public static final NoitaSpellItem BOUNCE_HOLE = registerModifier("bounce_hole", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(20)
        .manaDrain(60)
        .castDelaySeconds(40.0f / 60.0f)
        .recoil(10.0f)
        .bounceCount(1)
        .modifierEffect(NoitaModifierEffect.BOUNCE_HOLE)
        .build()
    );
    public static final NoitaSpellItem FIREBALL_RAY = registerModifier("fireball_ray", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(16)
        .manaDrain(110)
        .modifierEffect(NoitaModifierEffect.FIREBALL_RAY)
        .build()
    );
    public static final NoitaSpellItem LIGHTNING_RAY = registerModifier("lightning_ray", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(16)
        .manaDrain(110)
        .modifierEffect(NoitaModifierEffect.LIGHTNING_RAY)
        .build()
    );
    public static final NoitaSpellItem TENTACLE_RAY = registerModifier("tentacle_ray", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(16)
        .manaDrain(110)
        .modifierEffect(NoitaModifierEffect.TENTACLE_RAY)
        .build()
    );
    public static final NoitaSpellItem LASER_EMITTER_RAY = registerModifier("laser_emitter_ray", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(16)
        .manaDrain(110)
        .modifierEffect(NoitaModifierEffect.LASER_EMITTER_RAY)
        .build()
    );
    public static final NoitaSpellItem FIREBALL_RAY_LINE = registerModifier("fireball_ray_line", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(20)
        .manaDrain(130)
        .modifierEffect(NoitaModifierEffect.FIREBALL_RAY_LINE)
        .build()
    );
    public static final NoitaSpellItem FIREBALL_RAY_ENEMY = registerModifier("fireball_ray_enemy", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(20)
        .manaDrain(90)
        .modifierEffect(NoitaModifierEffect.FIREBALL_RAY_ENEMY)
        .build()
    );
    public static final NoitaSpellItem LIGHTNING_RAY_ENEMY = registerModifier("lightning_ray_enemy", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(20)
        .manaDrain(90)
        .modifierEffect(NoitaModifierEffect.LIGHTNING_RAY_ENEMY)
        .build()
    );
    public static final NoitaSpellItem TENTACLE_RAY_ENEMY = registerModifier("tentacle_ray_enemy", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(20)
        .manaDrain(90)
        .modifierEffect(NoitaModifierEffect.TENTACLE_RAY_ENEMY)
        .build()
    );
    public static final NoitaSpellItem GRAVITY_FIELD_ENEMY = registerModifier("gravity_field_enemy", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(20)
        .manaDrain(110)
        .modifierEffect(NoitaModifierEffect.GRAVITY_FIELD_ENEMY)
        .build()
    );
    public static final NoitaSpellItem CURSE = registerModifier("curse", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .manaDrain(30)
        .modifierEffect(NoitaModifierEffect.CURSE)
        .build()
    );
    public static final NoitaSpellItem CURSE_WITHER_PROJECTILE = registerModifier("curse_wither_projectile", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .manaDrain(50)
        .modifierEffect(NoitaModifierEffect.CURSE_WITHER_PROJECTILE)
        .build()
    );
    public static final NoitaSpellItem CURSE_WITHER_EXPLOSION = registerModifier("curse_wither_explosion", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .manaDrain(50)
        .modifierEffect(NoitaModifierEffect.CURSE_WITHER_EXPLOSION)
        .build()
    );
    public static final NoitaSpellItem CURSE_WITHER_MELEE = registerModifier("curse_wither_melee", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .manaDrain(50)
        .modifierEffect(NoitaModifierEffect.CURSE_WITHER_MELEE)
        .build()
    );
    public static final NoitaSpellItem CURSE_WITHER_ELECTRICITY = registerModifier("curse_wither_electricity", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .manaDrain(50)
        .modifierEffect(NoitaModifierEffect.CURSE_WITHER_ELECTRICITY)
        .build()
    );
    public static final NoitaSpellItem ORBIT_DISCS = registerModifier("orbit_discs", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .manaDrain(70)
        .modifierEffect(NoitaModifierEffect.ORBIT_DISCS)
        .build()
    );
    public static final NoitaSpellItem ORBIT_FIREBALLS = registerModifier("orbit_fireballs", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .manaDrain(40)
        .modifierEffect(NoitaModifierEffect.ORBIT_FIREBALLS)
        .build()
    );
    public static final NoitaSpellItem ORBIT_NUKES = registerModifier("orbit_nukes", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(3)
        .manaDrain(250)
        .modifierEffect(NoitaModifierEffect.ORBIT_NUKES)
        .build()
    );
    public static final NoitaSpellItem ORBIT_LASERS = registerModifier("orbit_lasers", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .manaDrain(100)
        .modifierEffect(NoitaModifierEffect.ORBIT_LASERS)
        .build()
    );
    public static final NoitaSpellItem ORBIT_LARPA = registerModifier("orbit_larpa", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .manaDrain(90)
        .modifierEffect(NoitaModifierEffect.ORBIT_LARPA)
        .build()
    );
    public static final NoitaSpellItem CHAIN_SHOT = registerModifier("chain_shot", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(100)
        .manaDrain(70)
        .damage(-1.2f)
        .explosionRadius(-5.0f)
        .spreadModifierDegrees(10.0f)
        .lifetimeModifierTicks(-30)
        .modifierEffect(NoitaModifierEffect.CHAIN_SHOT)
        .build()
    );
    public static final NoitaSpellItem ARC_ELECTRIC = registerModifier("arc_electric", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(15)
        .manaDrain(15)
        .modifierEffect(NoitaModifierEffect.ARC_ELECTRIC)
        .build()
    );
    public static final NoitaSpellItem ARC_FIRE = registerModifier("arc_fire", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(15)
        .manaDrain(15)
        .modifierEffect(NoitaModifierEffect.ARC_FIRE)
        .build()
    );
    public static final NoitaSpellItem ARC_GUNPOWDER = registerModifier("arc_gunpowder", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(15)
        .manaDrain(15)
        .modifierEffect(NoitaModifierEffect.ARC_GUNPOWDER)
        .build()
    );
    public static final NoitaSpellItem ARC_POISON = registerModifier("arc_poison", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(15)
        .manaDrain(15)
        .modifierEffect(NoitaModifierEffect.ARC_POISON)
        .build()
    );
    public static final NoitaSpellItem CRUMBLING_EARTH_PROJECTILE = registerModifier("crumbling_earth_projectile", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(15)
        .manaDrain(45)
        .modifierEffect(NoitaModifierEffect.CRUMBLING_EARTH_PROJECTILE)
        .build()
    );
    public static final NoitaSpellItem UNSTABLE_GUNPOWDER = registerModifier("unstable_gunpowder", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(20)
        .manaDrain(15)
        .modifierEffect(NoitaModifierEffect.UNSTABLE_GUNPOWDER)
        .build()
    );
    public static final NoitaSpellItem ENERGY_SHIELD_SHOT = registerModifier("energy_shield_shot", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .manaDrain(5)
        .speedMultiplier(0.4f)
        .modifierEffect(NoitaModifierEffect.ENERGY_SHIELD_SHOT)
        .build()
    );
    public static final NoitaSpellItem RANDOM_MODIFIER = registerModifier("random_modifier", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .manaDrain(20)
        .damage(1.2f)
        .criticalChancePercent(10.0f)
        .speedMultiplier(1.1f)
        .modifierEffects(NoitaModifierEffect.DAMAGE_RANDOM, NoitaModifierEffect.AUTOAIM)
        .build()
    );
    public static final NoitaSpellItem LARPA_CHAOS = registerModifier("larpa_chaos", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(20)
        .manaDrain(100)
        .castDelaySeconds(15.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.LARPA_CHAOS)
        .build()
    );
    public static final NoitaSpellItem LARPA_DOWNWARDS = registerModifier("larpa_downwards", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(20)
        .manaDrain(120)
        .castDelaySeconds(15.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.LARPA_DOWNWARDS)
        .build()
    );
    public static final NoitaSpellItem LARPA_UPWARDS = registerModifier("larpa_upwards", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(20)
        .manaDrain(120)
        .castDelaySeconds(15.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.LARPA_UPWARDS)
        .build()
    );
    public static final NoitaSpellItem LARPA_CHAOS_2 = registerModifier("larpa_chaos_2", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(20)
        .manaDrain(150)
        .castDelaySeconds(20.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.LARPA_CHAOS_2)
        .build()
    );
    public static final NoitaSpellItem LARPA_DEATH = registerModifier("larpa_death", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(30)
        .manaDrain(90)
        .castDelaySeconds(15.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.LARPA_DEATH)
        .build()
    );
    public static final NoitaSpellItem COLOUR_RED = registerModifier("colour_red", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(100)
        .manaDrain(0)
        .castDelaySeconds(-8.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.COLOUR_RED)
        .build()
    );
    public static final NoitaSpellItem COLOUR_ORANGE = registerModifier("colour_orange", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(100)
        .manaDrain(0)
        .castDelaySeconds(-8.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.COLOUR_ORANGE)
        .build()
    );
    public static final NoitaSpellItem COLOUR_GREEN = registerModifier("colour_green", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(100)
        .manaDrain(0)
        .castDelaySeconds(-8.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.COLOUR_GREEN)
        .build()
    );
    public static final NoitaSpellItem COLOUR_YELLOW = registerModifier("colour_yellow", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(100)
        .manaDrain(0)
        .castDelaySeconds(-8.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.COLOUR_YELLOW)
        .build()
    );
    public static final NoitaSpellItem COLOUR_PURPLE = registerModifier("colour_purple", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(100)
        .manaDrain(0)
        .castDelaySeconds(-8.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.COLOUR_PURPLE)
        .build()
    );
    public static final NoitaSpellItem COLOUR_BLUE = registerModifier("colour_blue", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(100)
        .manaDrain(0)
        .castDelaySeconds(-8.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.COLOUR_BLUE)
        .build()
    );
    public static final NoitaSpellItem COLOUR_RAINBOW = registerModifier("colour_rainbow", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(100)
        .manaDrain(0)
        .castDelaySeconds(-8.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.COLOUR_RAINBOW)
        .build()
    );
    public static final NoitaSpellItem COLOUR_INVIS = registerModifier("colour_invis", NoitaSpellTemplate.builder()
        .type(NoitaSpellType.PROJECTILE_MODIFIER)
        .maxUses(100)
        .manaDrain(0)
        .castDelaySeconds(-8.0f / 60.0f)
        .modifierEffect(NoitaModifierEffect.COLOUR_INVIS)
        .build()
    );

    private ModItems() {
    }

    public static void register() {
        MCNoita.LOGGER.info("Registering MC Noita items");
    }

    private static NoitaProjectileSpellSpec projectileSpec(
        String noitaId,
        String itemPath,
        String englishName,
        String chineseName,
        NoitaProjectileBehavior behavior,
        NoitaSpellTemplate template,
        int projectileCount,
        float burstSpreadDegrees,
        float gravity,
        float drag,
        float bounceDamping,
        float renderScale
    ) {
        return new NoitaProjectileSpellSpec(noitaId, itemPath, englishName, chineseName, behavior, template, projectileCount,
            burstSpreadDegrees, gravity, drag, bounceDamping, renderScale);
    }

    private static NoitaSpellTemplate staticProjectileTemplate(
        int maxUses,
        int manaDrain,
        float damage,
        float explosionRadius,
        float speed,
        int lifetimeTicks,
        float castDelayFrames,
        float rechargeFrames
    ) {
        NoitaSpellTemplate.Builder builder = NoitaSpellTemplate.builder()
            .type(NoitaSpellType.STATIC_PROJECTILE)
            .manaDrain(manaDrain)
            .damage(damage)
            .explosionRadius(explosionRadius)
            .speed(speed)
            .lifetimeTicks(lifetimeTicks)
            .castDelaySeconds(castDelayFrames / 60.0f)
            .rechargeTimeSeconds(rechargeFrames / 60.0f);
        if (maxUses != NoitaSpellTemplate.UNLIMITED_USES) {
            builder.maxUses(maxUses);
        }
        return builder.build();
    }

    private static NoitaProjectileSpellItem registerProjectile(NoitaProjectileSpellSpec spec) {
        NoitaProjectileSpellItem item = register(spec.itemPath(), new NoitaProjectileSpellItem(spec, new Item.Settings().maxCount(16)));
        PROJECTILE_SPELLS_MUTABLE.add(item);
        return item;
    }

    private static NoitaProjectileSpellItem registerStaticProjectile(NoitaProjectileSpellSpec spec) {
        NoitaProjectileSpellItem item = register(spec.itemPath(), new NoitaProjectileSpellItem(spec, new Item.Settings().maxCount(16)));
        PROJECTILE_SPELLS_MUTABLE.add(item);
        STATIC_PROJECTILE_SPELLS_MUTABLE.add(item);
        return item;
    }

    private static NoitaSpellItem registerModifier(String path, NoitaSpellTemplate template) {
        NoitaSpellItem item = register(path, new NoitaSpellItem(template, new Item.Settings().maxCount(16)));
        MODIFIER_SPELLS_MUTABLE.add(item);
        return item;
    }

    private static <T extends Item> T register(String path, T item) {
        return Registry.register(Registries.ITEM, MCNoita.id(path), item);
    }
}
