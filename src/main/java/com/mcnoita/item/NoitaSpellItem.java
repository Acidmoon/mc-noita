package com.mcnoita.item;

import com.mcnoita.spell.NoitaSpellTemplate;
import java.util.List;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

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
        return nbt.getInt(REMAINING_USES_KEY);
    }

    public static void consumeUse(ItemStack stack) {
        if (!(stack.getItem() instanceof NoitaSpellItem spellItem)) {
            return;
        }
        if (!spellItem.template.hasLimitedUses()) {
            return;
        }
        int remaining = getRemainingUses(stack);
        stack.getOrCreateNbt().putInt(REMAINING_USES_KEY, Math.max(0, remaining - 1));
    }

    public static boolean hasUsesRemaining(ItemStack stack) {
        int remaining = getRemainingUses(stack);
        return remaining != 0;
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.mc-noita.spell.type", Text.translatable(template.type().getTranslationKey())).formatted(Formatting.LIGHT_PURPLE));
        if (template.hasLimitedUses()) {
            int remaining = getRemainingUses(stack);
            tooltip.add(Text.translatable("tooltip.mc-noita.spell.remaining_uses", remaining, template.maxUses()).formatted(
                remaining > 0 ? Formatting.GRAY : Formatting.DARK_RED
            ));
        } else {
            tooltip.add(Text.translatable(
                "tooltip.mc-noita.spell.max_uses",
                Text.translatable("tooltip.mc-noita.spell.unlimited")
            ).formatted(Formatting.GRAY));
        }
        tooltip.add(Text.translatable("tooltip.mc-noita.spell.mana_drain", template.manaDrain()).formatted(Formatting.BLUE));

        addNonZero(tooltip, "tooltip.mc-noita.spell.damage", template.damage(), Formatting.RED);
        addPositive(tooltip, "tooltip.mc-noita.spell.explosion_radius", template.explosionRadius(), Formatting.RED);
        addNonZero(tooltip, "tooltip.mc-noita.spell.spread_degrees", template.spreadDegrees(), Formatting.GRAY);
        addPositive(tooltip, "tooltip.mc-noita.spell.speed", template.speed(), Formatting.GRAY);
        addPositive(tooltip, "tooltip.mc-noita.spell.trail_light", template.trailLightStacks(), Formatting.YELLOW);
        if (template.type() == com.mcnoita.spell.NoitaSpellType.MULTICAST && template.drawCount() > 0) {
            tooltip.add(Text.translatable("tooltip.mc-noita.spell.draw_count", template.drawCount()).formatted(Formatting.LIGHT_PURPLE));
        }
        if (template.triggerMode() != com.mcnoita.spell.NoitaSpellTriggerMode.NONE && template.triggerDrawCount() > 0) {
            tooltip.add(Text.translatable(
                "tooltip.mc-noita.spell.trigger",
                Text.translatable("spell_trigger.mc-noita." + template.triggerMode().name().toLowerCase(java.util.Locale.ROOT)),
                template.triggerDrawCount()
            ).formatted(Formatting.LIGHT_PURPLE));
            addPositive(tooltip, "tooltip.mc-noita.spell.trigger_delay", template.triggerDelayTicks(), Formatting.GRAY);
        }
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
