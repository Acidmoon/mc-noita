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
            })
            .build()
    );

    private ModItemGroups() {
    }

    public static void register() {
        MCNoita.LOGGER.info("Registering MC Noita item groups");
    }
}
