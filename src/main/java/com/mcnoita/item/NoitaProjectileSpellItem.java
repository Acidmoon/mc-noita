package com.mcnoita.item;

import com.mcnoita.spell.NoitaProjectileSpellSpec;
import java.util.List;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

public class NoitaProjectileSpellItem extends NoitaSpellItem {
    private final NoitaProjectileSpellSpec projectileSpec;

    public NoitaProjectileSpellItem(NoitaProjectileSpellSpec projectileSpec, Settings settings) {
        super(projectileSpec.template(), settings);
        this.projectileSpec = projectileSpec;
    }

    public NoitaProjectileSpellSpec getProjectileSpec() {
        return projectileSpec;
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        if (context.isAdvanced()) {
            tooltip.add(Text.translatable("tooltip.mc-noita.spell.noita_id", projectileSpec.noitaId()).formatted(Formatting.DARK_GRAY));
            tooltip.add(Text.translatable("tooltip.mc-noita.spell.behavior", projectileSpec.behavior().name().toLowerCase(java.util.Locale.ROOT)).formatted(Formatting.DARK_GRAY));
        }
    }
}
