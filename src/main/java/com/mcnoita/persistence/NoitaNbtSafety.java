package com.mcnoita.persistence;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

public final class NoitaNbtSafety {
    private NoitaNbtSafety() {
    }

    public static boolean isFinite(float value) {
        return Float.isFinite(value);
    }

    public static boolean validateTree(NbtElement root, int maxDepth, int maxNodes, int maxListLength) {
        return validate(root, 0, maxDepth, maxNodes, maxListLength, new int[] {0});
    }

    public static float finiteFloat(NbtCompound nbt, String key, float fallback, float min, float max) {
        if (!nbt.contains(key, NbtElement.NUMBER_TYPE)) {
            return fallback;
        }
        float value = nbt.getFloat(key);
        if (!Float.isFinite(value)) {
            return fallback;
        }
        return Math.max(min, Math.min(max, value));
    }

    public static boolean hasUniqueBoundedSlots(NbtList entries, int capacity) {
        int safeCapacity = Math.min(NoitaNbtLimits.MAX_CAST_STATE_SLOTS, Math.max(1, capacity));
        if (entries.size() > safeCapacity) {
            return false;
        }
        boolean[] occupied = new boolean[safeCapacity];
        for (int i = 0; i < entries.size(); i++) {
            NbtCompound entry = entries.getCompound(i);
            if (!entry.contains("Slot", NbtElement.NUMBER_TYPE)) {
                return false;
            }
            int slot = entry.getInt("Slot");
            if (slot < 0 || slot >= safeCapacity || occupied[slot]) {
                return false;
            }
            occupied[slot] = true;
        }
        return true;
    }

    public static <E extends Enum<E>> boolean hasValidEnumIfPresent(NbtCompound nbt, String key, Class<E> enumType) {
        if (!nbt.contains(key, NbtElement.STRING_TYPE)) {
            return true;
        }
        try {
            Enum.valueOf(enumType, nbt.getString(key));
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static boolean validate(NbtElement value, int depth, int maxDepth, int maxNodes, int maxListLength, int[] nodes) {
        if (++nodes[0] > maxNodes || depth > maxDepth) {
            return false;
        }
        if (value instanceof NbtString string) {
            return string.asString().length() <= NoitaNbtLimits.MAX_STRING_LENGTH;
        }
        if (value instanceof NbtFloat floating) {
            return Float.isFinite(floating.floatValue());
        }
        if (value instanceof NbtDouble floating) {
            return Double.isFinite(floating.doubleValue());
        }
        if (value instanceof NbtList list) {
            if (list.size() > maxListLength) {
                return false;
            }
            for (int i = 0; i < list.size(); i++) {
                if (!validate(list.get(i), depth + 1, maxDepth, maxNodes, maxListLength, nodes)) {
                    return false;
                }
            }
        }
        if (value instanceof NbtCompound compound) {
            for (String key : compound.getKeys()) {
                if (key.length() > NoitaNbtLimits.MAX_STRING_LENGTH || !validate(compound.get(key), depth + 1, maxDepth, maxNodes, maxListLength, nodes)) {
                    return false;
                }
            }
        }
        return true;
    }
}
