package com.mcnoita.item;

import com.mcnoita.wand.NoitaWandTemplate;
import java.util.List;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

public class NoitaWandItem extends Item {
    private static final String TEMPLATE_KEY = "NoitaWandTemplate";

    private final NoitaWandTemplate defaultTemplate;

    public NoitaWandItem(NoitaWandTemplate defaultTemplate, Settings settings) {
        super(settings);
        this.defaultTemplate = defaultTemplate;
    }

    @Override
    public ItemStack getDefaultStack() {
        ItemStack stack = super.getDefaultStack();
        setTemplate(stack, defaultTemplate);
        return stack;
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        NoitaWandTemplate template = getTemplate(stack);

        tooltip.add(Text.translatable("tooltip.mc-noita.wand.shuffle", yesNo(template.shuffle())).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.mc-noita.wand.spells_per_cast", template.spellsPerCast()).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.mc-noita.wand.cast_delay", template.castDelaySeconds()).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.mc-noita.wand.recharge_time", template.rechargeTimeSeconds()).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.mc-noita.wand.mana_max", template.manaMax()).formatted(Formatting.BLUE));
        tooltip.add(Text.translatable("tooltip.mc-noita.wand.mana_charge_speed", template.manaChargeSpeed()).formatted(Formatting.BLUE));
        tooltip.add(Text.translatable("tooltip.mc-noita.wand.capacity", template.capacity()).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.mc-noita.wand.spread", template.spreadDegrees()).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.mc-noita.wand.always_cast_count", template.alwaysCastSpells().size()).formatted(Formatting.LIGHT_PURPLE));
        tooltip.add(Text.translatable("tooltip.mc-noita.wand.speed_multiplier", template.speedMultiplier()).formatted(Formatting.DARK_GRAY));
    }

    public NoitaWandTemplate getTemplate(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(TEMPLATE_KEY)) {
            return defaultTemplate;
        }

        return NoitaWandTemplate.fromNbt(nbt.getCompound(TEMPLATE_KEY));
    }

    public static void setTemplate(ItemStack stack, NoitaWandTemplate template) {
        stack.getOrCreateNbt().put(TEMPLATE_KEY, template.toNbt());
    }

    private static Text yesNo(boolean value) {
        return Text.translatable(value ? "tooltip.mc-noita.yes" : "tooltip.mc-noita.no");
    }
}
