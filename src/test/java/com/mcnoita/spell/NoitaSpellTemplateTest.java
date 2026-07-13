package com.mcnoita.spell;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("characterization")
class NoitaSpellTemplateTest {
    @Test
    void templateRejectsNonFiniteNumericValues() {
        assertThrows(IllegalArgumentException.class, () -> NoitaSpellTemplate.builder().speed(Float.NaN).build());
        assertThrows(IllegalArgumentException.class, () -> NoitaSpellTemplate.builder().speedMultiplier(Float.POSITIVE_INFINITY).build());
    }

    @Test
    void projectilePayloadRejectsNonFiniteNumericValues() {
        assertThrows(IllegalArgumentException.class, () -> new NoitaProjectilePayload(
            "spark_bolt", NoitaProjectileBehavior.BOLT, Float.NaN, 0.0f, 20, 0, 0.0f,
            1.0f, 0.0f, 0.0f, 1.0f, 0.65f, 1.0f, 0.0f, false, false,
            1, 0.0f, NoitaSpellTriggerMode.NONE, 0, 0, java.util.List.of(), java.util.List.of()
        ));
    }
}
