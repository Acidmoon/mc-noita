package com.mcnoita.spell.damage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Regression coverage for the damage-result boundary before harmful follow-up effects. */
@Tag("regression")
class SpellDamageServiceTest {
    @Test
    void rejectedDirectDamageCannotApplyHarmfulFollowUpEffects() {
        DamageProfile damagingProfile = DamageProfile.of(DamageChannel.PROJECTILE, 1.0);

        assertFalse(SpellDamageService.shouldApplyHarmfulFollowUp(damagingProfile, false));
        assertTrue(SpellDamageService.shouldApplyHarmfulFollowUp(damagingProfile, true));
    }

    @Test
    void explicitZeroDamageProfileCanStillCarryStatusOnlyEffects() {
        assertTrue(SpellDamageService.shouldApplyHarmfulFollowUp(DamageProfile.EMPTY, false));
    }
}
