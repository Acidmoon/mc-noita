package com.mcnoita.wand.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Regression coverage for the no-mutation NBT binding fingerprint. */
@Tag("regression")
class MinecraftWandAdapterFingerprintTest {
    @Test
    void canonicalHashIgnoresCompoundInsertionOrderWithoutChangingEitherTree() {
        NbtCompound first = tree(false);
        NbtCompound second = tree(true);
        NbtCompound firstBefore = first.copy();
        NbtCompound secondBefore = second.copy();

        assertEquals(MinecraftWandAdapter.canonicalNbtStateHash(first), MinecraftWandAdapter.canonicalNbtStateHash(second));
        assertEquals(firstBefore, first, "hashing a source stack must not run NBT migration or write tags");
        assertEquals(secondBefore, second, "hashing a source stack must not reorder or mutate tags");

        second.getCompound("nested").putInt("value", 8);
        assertNotEquals(MinecraftWandAdapter.canonicalNbtStateHash(first), MinecraftWandAdapter.canonicalNbtStateHash(second));
    }

    private static NbtCompound tree(boolean reverseOrder) {
        NbtCompound root = new NbtCompound();
        NbtCompound nested = new NbtCompound();
        nested.putInt("value", 7);
        nested.putString("name", "spark");
        NbtList entries = new NbtList();
        entries.add(NbtString.of("first"));
        entries.add(NbtString.of("second"));

        if (reverseOrder) {
            root.put("entries", entries);
            root.put("nested", nested);
        } else {
            root.put("nested", nested);
            root.put("entries", entries);
        }
        return root;
    }
}
