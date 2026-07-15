package com.mcnoita.spell.damage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Pure regression coverage for frozen multi-channel damage mechanics. */
@Tag("regression")
class DamageProfileTest {
    @Test
    void legacyScalarMapsOnlyToProjectileAndProfilesComposeDeterministically() {
        DamageProfile legacy = DamageProfile.legacyProjectile(3.0);
        DamageProfile elemental = new DamageProfile(Map.of(DamageChannel.FIRE, 2.5, DamageChannel.ELECTRICITY, 1.0));
        DamageProfile combined = legacy.plus(elemental).scale(2.0);

        assertEquals(3.0, legacy.projectileDamage());
        assertEquals(0.0, legacy.amount(DamageChannel.FIRE));
        assertEquals(6.0, combined.amount(DamageChannel.PROJECTILE));
        assertEquals(5.0, combined.amount(DamageChannel.FIRE));
        assertEquals(2.0, combined.amount(DamageChannel.ELECTRICITY));
        assertEquals(13.0, combined.totalDamage());
        assertFalse(combined.isEmpty());
    }

    @Test
    void zeroAmountsAreCanonicalizedAndInvalidValuesAreRejected() {
        assertTrue(DamageProfile.legacyProjectile(0.0).isEmpty());
        assertEquals(DamageProfile.EMPTY, new DamageProfile(Map.of(DamageChannel.ICE, 0.0)));
        assertThrows(IllegalArgumentException.class, () -> new DamageProfile(Map.of(DamageChannel.ICE, Double.NaN)));
        assertThrows(IllegalArgumentException.class, () -> DamageProfile.legacyProjectile(1.0).scale(-1.0));
    }

    @Test
    void replacingOneChannelDoesNotMutateTheFrozenSourceProfile() {
        DamageProfile source = new DamageProfile(Map.of(DamageChannel.PROJECTILE, 2.0, DamageChannel.FIRE, 1.0));
        DamageProfile replaced = source.withAmount(DamageChannel.PROJECTILE, 4.0);
        DamageProfile removed = replaced.withAmount(DamageChannel.FIRE, 0.0);

        assertEquals(2.0, source.amount(DamageChannel.PROJECTILE));
        assertEquals(1.0, source.amount(DamageChannel.FIRE));
        assertEquals(4.0, replaced.amount(DamageChannel.PROJECTILE));
        assertEquals(0.0, removed.amount(DamageChannel.FIRE));
    }

    @Test
    void runtimeLegacyScalarProjectionPreservesOtherFrozenChannels() {
        DamageProfile source = new DamageProfile(Map.of(DamageChannel.PROJECTILE, 2.0, DamageChannel.FIRE, 1.5));
        DamageProfile updated = source.withProjectileDamage(2.08);

        assertEquals(2.0, source.projectileDamage());
        assertEquals(2.08, updated.projectileDamage());
        assertEquals(1.5, updated.amount(DamageChannel.FIRE));
    }
}
