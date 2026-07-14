package com.mcnoita.world.mutation;

import com.mcnoita.MCNoita;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;

/** Tags used by the conservative default terrain policy. */
public final class ModSpellBlockTags {
    public static final TagKey<Block> SPELL_UNBREAKABLE = TagKey.of(RegistryKeys.BLOCK, MCNoita.id("spell_unbreakable"));

    private ModSpellBlockTags() {
    }
}
