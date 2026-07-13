package com.mcnoita.client.item;

import com.mcnoita.item.NoitaProjectileSpellItem;
import com.mcnoita.item.NoitaSpellItem;
import com.mcnoita.item.NoitaWandItem;
import com.mcnoita.spell.NoitaSpellTemplate;
import com.mcnoita.spell.NoitaSpellTriggerMode;
import com.mcnoita.spell.NoitaSpellType;
import com.mcnoita.wand.NoitaWandTemplate;
import java.util.List;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Client-only tooltip projection keeps common item classes loadable on a dedicated server. */
public final class NoitaTooltipEvents {
    private NoitaTooltipEvents() {
    }

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
            if (stack.getItem() instanceof NoitaWandItem wand) {
                addWandTooltip(stack, wand, lines);
            } else if (stack.getItem() instanceof NoitaSpellItem spell) {
                addSpellTooltip(stack, spell, context.isAdvanced(), lines);
            }
        });
    }

    private static void addWandTooltip(ItemStack stack, NoitaWandItem wand, List<Text> lines) {
        NoitaWandTemplate template = wand.getTemplate(stack);
        lines.add(Text.translatable("tooltip.mc-noita.wand.shuffle", yesNo(template.shuffle())).formatted(Formatting.GRAY));
        lines.add(Text.translatable("tooltip.mc-noita.wand.spells_per_cast", template.spellsPerCast()).formatted(Formatting.GRAY));
        lines.add(Text.translatable("tooltip.mc-noita.wand.cast_delay", template.castDelaySeconds()).formatted(Formatting.GRAY));
        lines.add(Text.translatable("tooltip.mc-noita.wand.recharge_time", template.rechargeTimeSeconds()).formatted(Formatting.GRAY));
        lines.add(Text.translatable("tooltip.mc-noita.wand.mana_max", template.manaMax()).formatted(Formatting.BLUE));
        lines.add(Text.translatable("tooltip.mc-noita.wand.mana_charge_speed", template.manaChargeSpeed()).formatted(Formatting.BLUE));
        lines.add(Text.translatable("tooltip.mc-noita.wand.capacity", template.capacity()).formatted(Formatting.GRAY));
        lines.add(Text.translatable("tooltip.mc-noita.wand.loaded_spells", loadedSpellCount(stack, template.capacity()), template.capacity()).formatted(Formatting.AQUA));
        lines.add(Text.translatable("tooltip.mc-noita.wand.spread", template.spreadDegrees()).formatted(Formatting.GRAY));
        lines.add(Text.translatable("tooltip.mc-noita.wand.always_cast_count", template.alwaysCastSpells().size()).formatted(Formatting.LIGHT_PURPLE));
        lines.add(Text.translatable("tooltip.mc-noita.wand.speed_multiplier", template.speedMultiplier()).formatted(Formatting.DARK_GRAY));
    }

    private static void addSpellTooltip(ItemStack stack, NoitaSpellItem spell, boolean advanced, List<Text> lines) {
        NoitaSpellTemplate template = spell.getTemplate();
        lines.add(Text.translatable("tooltip.mc-noita.spell.type", Text.translatable(template.type().getTranslationKey())).formatted(Formatting.LIGHT_PURPLE));
        if (template.hasLimitedUses()) {
            int remaining = NoitaSpellItem.getRemainingUses(stack);
            lines.add(Text.translatable("tooltip.mc-noita.spell.remaining_uses", remaining, template.maxUses()).formatted(remaining > 0 ? Formatting.GRAY : Formatting.DARK_RED));
        } else {
            lines.add(Text.translatable("tooltip.mc-noita.spell.max_uses", Text.translatable("tooltip.mc-noita.spell.unlimited")).formatted(Formatting.GRAY));
        }
        lines.add(Text.translatable("tooltip.mc-noita.spell.mana_drain", template.manaDrain()).formatted(Formatting.BLUE));
        addNonZero(lines, "tooltip.mc-noita.spell.damage", template.damage(), Formatting.RED);
        addPositive(lines, "tooltip.mc-noita.spell.explosion_radius", template.explosionRadius(), Formatting.RED);
        addNonZero(lines, "tooltip.mc-noita.spell.spread_degrees", template.spreadDegrees(), Formatting.GRAY);
        addPositive(lines, "tooltip.mc-noita.spell.speed", template.speed(), Formatting.GRAY);
        addPositive(lines, "tooltip.mc-noita.spell.trail_light", template.trailLightStacks(), Formatting.YELLOW);
        if (template.type() == NoitaSpellType.MULTICAST && template.drawCount() > 0) {
            lines.add(Text.translatable("tooltip.mc-noita.spell.draw_count", template.drawCount()).formatted(Formatting.LIGHT_PURPLE));
        }
        if (template.triggerMode() != NoitaSpellTriggerMode.NONE && template.triggerDrawCount() > 0) {
            lines.add(Text.translatable("tooltip.mc-noita.spell.trigger", Text.translatable("spell_trigger.mc-noita." + template.triggerMode().name().toLowerCase(java.util.Locale.ROOT)), template.triggerDrawCount()).formatted(Formatting.LIGHT_PURPLE));
            addPositive(lines, "tooltip.mc-noita.spell.trigger_delay", template.triggerDelayTicks(), Formatting.GRAY);
        }
        addNonZero(lines, "tooltip.mc-noita.spell.cast_delay", template.castDelaySeconds(), Formatting.GRAY);
        addNonZero(lines, "tooltip.mc-noita.spell.recharge_time", template.rechargeTimeSeconds(), Formatting.GRAY);
        addNonZero(lines, "tooltip.mc-noita.spell.spread_modifier", template.spreadModifierDegrees(), Formatting.GRAY);
        if (template.speedMultiplier() != 1.0f) {
            lines.add(Text.translatable("tooltip.mc-noita.spell.speed_multiplier", template.speedMultiplier()).formatted(Formatting.GRAY));
        }
        addNonZero(lines, "tooltip.mc-noita.spell.critical_chance", template.criticalChancePercent(), Formatting.GOLD);
        if (advanced) {
            addPositive(lines, "tooltip.mc-noita.spell.lifetime", template.lifetimeTicks(), Formatting.DARK_GRAY);
            addPositive(lines, "tooltip.mc-noita.spell.max_lifetime", template.maxLifetimeTicks(), Formatting.DARK_GRAY);
            addNonZero(lines, "tooltip.mc-noita.spell.lifetime_modifier", template.lifetimeModifierTicks(), Formatting.DARK_GRAY);
            addNonZero(lines, "tooltip.mc-noita.spell.recoil", template.recoil(), Formatting.DARK_GRAY);
            addFlag(lines, "tooltip.mc-noita.spell.piercing", template.piercing());
            addFlag(lines, "tooltip.mc-noita.spell.friendly_fire", template.friendlyFire());
            if (spell instanceof NoitaProjectileSpellItem projectileSpell) {
                lines.add(Text.translatable("tooltip.mc-noita.spell.noita_id", projectileSpell.getProjectileSpec().noitaId()).formatted(Formatting.DARK_GRAY));
                lines.add(Text.translatable("tooltip.mc-noita.spell.behavior", projectileSpell.getProjectileSpec().behavior().name().toLowerCase(java.util.Locale.ROOT)).formatted(Formatting.DARK_GRAY));
            }
        }
    }

    private static int loadedSpellCount(ItemStack stack, int capacity) {
        int count = 0;
        for (ItemStack spell : NoitaWandItem.getSpellStacks(stack, capacity)) {
            if (!spell.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static Text yesNo(boolean value) {
        return Text.translatable(value ? "tooltip.mc-noita.yes" : "tooltip.mc-noita.no");
    }

    private static void addPositive(List<Text> lines, String key, int value, Formatting color) {
        if (value > 0) lines.add(Text.translatable(key, value).formatted(color));
    }

    private static void addPositive(List<Text> lines, String key, float value, Formatting color) {
        if (value > 0.0f) lines.add(Text.translatable(key, value).formatted(color));
    }

    private static void addNonZero(List<Text> lines, String key, int value, Formatting color) {
        if (value != 0) lines.add(Text.translatable(key, value).formatted(color));
    }

    private static void addNonZero(List<Text> lines, String key, float value, Formatting color) {
        if (value != 0.0f) lines.add(Text.translatable(key, value).formatted(color));
    }

    private static void addFlag(List<Text> lines, String key, boolean value) {
        if (value) lines.add(Text.translatable(key).formatted(Formatting.DARK_GRAY));
    }
}
