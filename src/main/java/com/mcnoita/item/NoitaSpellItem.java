package com.mcnoita.item;

import com.mcnoita.spell.NoitaSpellTemplate;
import java.util.List;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

public class NoitaSpellItem extends Item {
    private final NoitaSpellTemplate template;

    public NoitaSpellItem(NoitaSpellTemplate template, Settings settings) {
        super(settings);
        this.template = template;
    }

    public NoitaSpellTemplate getTemplate() {
        return template;
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.mc-noita.spell.type", Text.translatable(template.type().getTranslationKey())).formatted(Formatting.LIGHT_PURPLE));
        tooltip.add(Text.translatable(
            "tooltip.mc-noita.spell.max_uses",
            template.hasLimitedUses() ? template.maxUses() : Text.translatable("tooltip.mc-noita.spell.unlimited")
        ).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.mc-noita.spell.mana_drain", template.manaDrain()).formatted(Formatting.BLUE));

        addNonZero(tooltip, "tooltip.mc-noita.spell.damage", template.damage(), Formatting.RED);
        addPositive(tooltip, "tooltip.mc-noita.spell.explosion_radius", template.explosionRadius(), Formatting.RED);
        addNonZero(tooltip, "tooltip.mc-noita.spell.spread_degrees", template.spreadDegrees(), Formatting.GRAY);
        addPositive(tooltip, "tooltip.mc-noita.spell.speed", template.speed(), Formatting.GRAY);
        addPositive(tooltip, "tooltip.mc-noita.spell.trail_light", template.trailLightStacks(), Formatting.YELLOW);
        addNonZero(tooltip, "tooltip.mc-noita.spell.cast_delay", template.castDelaySeconds(), Formatting.GRAY);
        addNonZero(tooltip, "tooltip.mc-noita.spell.recharge_time", template.rechargeTimeSeconds(), Formatting.GRAY);
        addNonZero(tooltip, "tooltip.mc-noita.spell.spread_modifier", template.spreadModifierDegrees(), Formatting.GRAY);
        if (template.speedMultiplier() != 1.0f) {
            tooltip.add(Text.translatable("tooltip.mc-noita.spell.speed_multiplier", template.speedMultiplier()).formatted(Formatting.GRAY));
        }
        addNonZero(tooltip, "tooltip.mc-noita.spell.critical_chance", template.criticalChancePercent(), Formatting.GOLD);

        if (context.isAdvanced()) {
            addPositive(tooltip, "tooltip.mc-noita.spell.lifetime", template.lifetimeTicks(), Formatting.DARK_GRAY);
            addPositive(tooltip, "tooltip.mc-noita.spell.max_lifetime", template.maxLifetimeTicks(), Formatting.DARK_GRAY);
            addNonZero(tooltip, "tooltip.mc-noita.spell.lifetime_modifier", template.lifetimeModifierTicks(), Formatting.DARK_GRAY);
            addNonZero(tooltip, "tooltip.mc-noita.spell.recoil", template.recoil(), Formatting.DARK_GRAY);
            addFlag(tooltip, "tooltip.mc-noita.spell.piercing", template.piercing());
            addFlag(tooltip, "tooltip.mc-noita.spell.friendly_fire", template.friendlyFire());
        }
    }

    private static void addPositive(List<Text> tooltip, String translationKey, int value, Formatting formatting) {
        if (value > 0) {
            tooltip.add(Text.translatable(translationKey, value).formatted(formatting));
        }
    }

    private static void addPositive(List<Text> tooltip, String translationKey, float value, Formatting formatting) {
        if (value > 0.0f) {
            tooltip.add(Text.translatable(translationKey, value).formatted(formatting));
        }
    }

    private static void addNonZero(List<Text> tooltip, String translationKey, int value, Formatting formatting) {
        if (value != 0) {
            tooltip.add(Text.translatable(translationKey, value).formatted(formatting));
        }
    }

    private static void addNonZero(List<Text> tooltip, String translationKey, float value, Formatting formatting) {
        if (value != 0.0f) {
            tooltip.add(Text.translatable(translationKey, value).formatted(formatting));
        }
    }

    private static void addFlag(List<Text> tooltip, String translationKey, boolean value) {
        if (value) {
            tooltip.add(Text.translatable(translationKey).formatted(Formatting.DARK_GRAY));
        }
    }
}
