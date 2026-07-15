package com.mcnoita.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mcnoita.spell.NoitaProjectileBehavior;
import com.mcnoita.spell.NoitaProjectilePayload;
import com.mcnoita.spell.NoitaSpellTriggerMode;
import com.mcnoita.wand.NoitaWandTemplate;
import java.util.List;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("regression")
class NoitaNbtMigrationTest {
    @Test
    void latestWandTemplateRoundTripsWithSchemaVersion() {
        NoitaWandTemplate original = template();
        NbtCompound nbt = original.toNbt();

        assertEquals(NoitaNbtSchema.CURRENT_VERSION, nbt.getInt(NoitaNbtSchema.VERSION_KEY));
        assertEquals(original, NoitaWandTemplate.tryFromNbt(nbt).orElseThrow());
    }

    @Test
    void missingVersionMigratesFromV0ToV1() {
        NbtCompound legacy = template().toNbt();
        legacy.remove(NoitaNbtSchema.VERSION_KEY);

        assertEquals(template(), NoitaWandTemplate.tryFromNbt(legacy).orElseThrow());
    }

    @Test
    void g01CastStateMigratesToThePreparedReloadSchema() {
        NbtCompound g01 = new NbtCompound();
        g01.putInt(NoitaNbtSchema.VERSION_KEY, 1);
        g01.putLong("RechargeEndTick", 120L);
        NbtList discarded = new NbtList();
        discarded.add(net.minecraft.nbt.NbtInt.of(0));
        discarded.add(net.minecraft.nbt.NbtInt.of(1));
        g01.put("Discarded", discarded);

        assertTrue(NoitaNbtSchema.migrateToCurrent(g01, NoitaNbtSchema.Kind.CAST_STATE));
        assertEquals(NoitaNbtSchema.CURRENT_VERSION, g01.getInt(NoitaNbtSchema.VERSION_KEY));
        assertFalse(g01.getBoolean("G02ReloadPrepared"));
        assertEquals(2, g01.getList("Discarded", net.minecraft.nbt.NbtElement.INT_TYPE).size());
    }

    @Test
    void futureTemplateVersionIsRejectedWithoutMutatingTheInput() {
        NbtCompound future = template().toNbt();
        future.putInt(NoitaNbtSchema.VERSION_KEY, 99);

        assertTrue(NoitaWandTemplate.tryFromNbt(future).isEmpty());
        assertEquals(99, future.getInt(NoitaNbtSchema.VERSION_KEY));
    }

    @Test
    void malformedSlotListsAreRejectedBeforeItemStackDecode() {
        NbtList entries = new NbtList();
        for (int i = 0; i < NoitaNbtLimits.MAX_WAND_SLOT_ENTRIES + 1; i++) {
            NbtCompound entry = new NbtCompound();
            entry.putInt("Slot", i % 2); // Includes duplicate slots as well as an oversized list.
            entries.add(entry);
        }

        assertFalse(NoitaNbtSafety.hasUniqueBoundedSlots(entries, template().capacity()));
    }

    @Test
    void deeplyNestedPayloadIsRejectedBeforeRecursiveDecode() {
        NbtCompound root = legacyTriggerPayload();
        NbtCompound current = root;
        for (int i = 0; i <= NoitaNbtLimits.MAX_PAYLOAD_DEPTH; i++) {
            NbtCompound child = legacyTriggerPayload();
            NbtList children = new NbtList();
            children.add(child);
            current.put("Payloads", children);
            current = child;
        }

        assertTrue(NoitaProjectilePayload.tryFromNbt(root).isEmpty());
    }

    @Test
    void oversizedPayloadListIsRejected() {
        NbtCompound root = legacyTriggerPayload();
        NbtList children = new NbtList();
        for (int i = 0; i < NoitaNbtLimits.MAX_PAYLOAD_CHILDREN + 1; i++) {
            children.add(legacyTriggerPayload());
        }
        root.put("Payloads", children);

        assertFalse(NoitaProjectilePayload.tryFromNbt(root).isPresent());
    }

    @Test
    void invalidPayloadEnumIsRejectedInsteadOfFallingBackToAnotherSpellBehavior() {
        NbtCompound invalid = payload().toNbt();
        invalid.putString("Behavior", "NOT_A_BEHAVIOR");

        assertTrue(NoitaProjectilePayload.tryFromNbt(invalid).isEmpty());
    }

    private static NoitaWandTemplate template() {
        return NoitaWandTemplate.builder().capacity(8).manaMax(250).manaChargeSpeed(50).build();
    }

    private static NoitaProjectilePayload payload() {
        return new NoitaProjectilePayload(
            "spark_bolt", NoitaProjectileBehavior.BOLT, 2.0f, 5.0f, 20, 0, 0.0f,
            1.0f, 0.0f, 0.0f, 0.99f, 0.65f, 1.0f, 0.0f, false, false,
            1, 0.0f, NoitaSpellTriggerMode.NONE, 0, 0, List.of(), List.of()
        );
    }

    private static NbtCompound legacyTriggerPayload() {
        NbtCompound nbt = payload().toNbt();
        nbt.putInt(NoitaNbtSchema.VERSION_KEY, 2);
        nbt.remove("TriggerPlan");
        nbt.putString("TriggerMode", "HIT");
        nbt.putInt("TriggerDelayTicks", 0);
        return nbt;
    }
}
