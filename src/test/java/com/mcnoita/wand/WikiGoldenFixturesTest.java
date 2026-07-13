package com.mcnoita.wand;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("wiki-golden")
class WikiGoldenFixturesTest {
    @Test
    @Disabled("G01 must move the evaluator out of NoitaWandCaster before this Wiki golden fixture can run.")
    void damagePlusAppliesToBothProjectilesInTheSameMulticastShotState() {
        assertEquals(2, 0, "Wiki: https://noita.wiki.gg/wiki/Guide_To_Wand_Mechanics, checked 2026-07-13");
    }

    @Test
    @Disabled("G01 must expose deterministic Draw and Wrap state before this Wiki golden fixture can run.")
    void initialSpellsPerCastDoesNotInitiateWrap() {
        assertEquals("no_wrap", "pending", "Wiki: https://noita.wiki.gg/wiki/Guide_To_Wand_Mechanics, checked 2026-07-13");
    }

    @Test
    @Disabled("G01 must expose Call policy before this Wiki golden fixture can run.")
    void gammaCallDoesNotSpendTheTargetsManaOrUses() {
        assertEquals(0, 1, "Wiki: https://noita.wiki.gg/wiki/Gamma, checked 2026-07-13");
    }
}
