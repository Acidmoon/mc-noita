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
                entries.add(ModItems.SPARK_BOLT);
                entries.add(ModItems.SPARK_BOLT_TRIGGER);
                entries.add(ModItems.SPARK_BOLT_TIMER);
                entries.add(ModItems.BOUNCING_BURST);
                entries.add(ModItems.LIGHT_BULLET);
                entries.add(ModItems.BOMB);
                entries.add(ModItems.BOMB_DEATH_TRIGGER);
                entries.add(ModItems.DOUBLE_SPELL);
                entries.add(ModItems.DUPLICATE);
                entries.add(ModItems.WAND_REFRESH);
                entries.add(ModItems.ALPHA);
                entries.add(ModItems.GAMMA);
                entries.add(ModItems.LIGHT);
                entries.add(ModItems.ADD_MANA);
            })
            .build()
    );

    private ModItemGroups() {
    }

    public static void register() {
        MCNoita.LOGGER.info("Registering MC Noita item groups");
    }
}
