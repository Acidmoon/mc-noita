package com.mcnoita.item;

import com.mcnoita.MCNoita;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

public final class ModItemGroups {
    public static final ItemGroup MAIN = Registry.register(
        Registries.ITEM_GROUP,
        MCNoita.id("main"),
        FabricItemGroup.builder()
            .displayName(Text.translatable("itemGroup.mc-noita.main"))
            .icon(() -> new ItemStack(ModItems.STARTER_WAND))
            .entries((context, entries) -> {
                entries.add(ModItems.STARTER_WAND);
                for (NoitaProjectileSpellItem spell : ModItems.PROJECTILE_SPELLS) {
                    entries.add(spell);
                }
                for (NoitaSpellItem spell : ModItems.MODIFIER_SPELLS) {
                    entries.add(spell);
                }
                entries.add(ModItems.DOUBLE_SPELL);
                entries.add(ModItems.DUPLICATE);
                entries.add(ModItems.WAND_REFRESH);
                entries.add(ModItems.ALPHA);
                entries.add(ModItems.GAMMA);
                entries.add(ModItems.TAU);
                entries.add(ModItems.OMEGA);
                entries.add(ModItems.MU);
                entries.add(ModItems.PHI);
                entries.add(ModItems.SIGMA);
                entries.add(ModItems.ZETA);
                entries.add(ModItems.DIVIDE_2);
                entries.add(ModItems.DIVIDE_3);
                entries.add(ModItems.DIVIDE_4);
                entries.add(ModItems.DIVIDE_10);
                entries.add(ModItems.ADD_TRIGGER);
                entries.add(ModItems.ADD_TIMER);
                entries.add(ModItems.ADD_DEATH_TRIGGER);
            })
            .build()
    );

    private ModItemGroups() {
    }

    public static void register() {
        MCNoita.LOGGER.info("Registering MC Noita item groups");
    }
}
