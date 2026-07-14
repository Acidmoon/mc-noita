package com.mcnoita.wand.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** The transaction result cannot expose an impossible accepted/rejected state. */
@Tag("regression")
class CastResultTest {
    @Test
    void rejectedResultCannotCarryACommittedCast() {
        assertFalse(CastResult.rejected(CastResult.Status.VALIDATION_REJECTED, "invalid", null, null, null).accepted());
        assertThrows(IllegalArgumentException.class, () -> new CastResult(CastResult.Status.ACCEPTED,
            "invalid", null, null, null, null));
    }
}
