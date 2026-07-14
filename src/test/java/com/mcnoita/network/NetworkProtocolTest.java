package com.mcnoita.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Hand;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("regression")
class NetworkProtocolTest {
    private static final String CATALOG_HASH = "a".repeat(NoitaNetworkProtocol.CATALOG_HASH_LENGTH);

    @Test
    void castRequestRoundTripsExactly() {
        WandCastRequest expected = new WandCastRequest(
            NoitaNetworkProtocol.VERSION, 7, Hand.MAIN_HAND, 3, 42, 19L, 4L, CATALOG_HASH
        );
        PacketByteBuf buf = PacketByteBufs.create();
        expected.write(buf);

        assertEquals(expected, WandCastRequest.read(buf).orElseThrow());
    }

    @Test
    void castRequestRejectsTrailingBytes() {
        PacketByteBuf buf = PacketByteBufs.create();
        new WandCastRequest(NoitaNetworkProtocol.VERSION, 1, Hand.MAIN_HAND, 0, 0, 0L, 0L, CATALOG_HASH).write(buf);
        buf.writeByte(1);

        assertTrue(WandCastRequest.read(buf).isEmpty());
    }

    @Test
    void castRequestRejectsInvalidBindingAndHudSnapshotRoundTripsExactly() {
        PacketByteBuf malformed = PacketByteBufs.create();
        malformed.writeVarInt(NoitaNetworkProtocol.VERSION);
        malformed.writeVarInt(1);
        malformed.writeByte(Hand.MAIN_HAND.ordinal());
        malformed.writeVarInt(0);
        malformed.writeInt(0);
        malformed.writeVarLong(0L);
        malformed.writeVarLong(0L);
        malformed.writeString("not-a-catalog-hash");
        assertTrue(WandCastRequest.read(malformed).isEmpty());

        WandCastHudSnapshot expected = new WandCastHudSnapshot(
            NoitaNetworkProtocol.VERSION, 1, 3, 8, 25.0f, 100, 19L, 4L, 42, CATALOG_HASH
        );
        PacketByteBuf hud = PacketByteBufs.create();
        expected.write(hud);
        assertEquals(expected, WandCastHudSnapshot.read(hud).orElseThrow());
    }

    @Test
    void canonicalCatalogHashRejectsWrongLengthAndUppercase() {
        assertTrue(NoitaNetworkProtocol.isCanonicalCatalogHash(CATALOG_HASH));
        assertFalse(NoitaNetworkProtocol.isCanonicalCatalogHash("a".repeat(NoitaNetworkProtocol.CATALOG_HASH_LENGTH - 1)));
        assertFalse(NoitaNetworkProtocol.isCanonicalCatalogHash("A".repeat(NoitaNetworkProtocol.CATALOG_HASH_LENGTH)));
    }

    @Test
    void hoverRequestRejectsNonFiniteInputAndClampsFiniteInput() {
        PacketByteBuf invalid = PacketByteBufs.create();
        invalid.writeVarInt(NoitaNetworkProtocol.VERSION);
        invalid.writeVarInt(1);
        invalid.writeBoolean(true);
        invalid.writeFloat(Float.NaN);
        invalid.writeFloat(0.0f);
        assertTrue(HoverInputRequest.read(invalid).isEmpty());

        PacketByteBuf finite = PacketByteBufs.create();
        new HoverInputRequest(NoitaNetworkProtocol.VERSION, 2, false, 10.0f, -10.0f).write(finite);
        HoverInputRequest decoded = HoverInputRequest.read(finite).orElseThrow();
        assertEquals(1.0f, decoded.sidewaysInput());
        assertEquals(-1.0f, decoded.forwardInput());
    }

    @Test
    void replayAndOutOfOrderSequencesAreRejected() {
        NoitaRequestGuard guard = new NoitaRequestGuard();
        UUID player = UUID.randomUUID();

        assertTrue(guard.acceptCast(player, 1, 0L));
        assertFalse(guard.acceptCast(player, 1, 1L));
        assertFalse(guard.acceptCast(player, 0, 2L));
        assertTrue(guard.acceptCast(player, 2, 3L));
    }

    @Test
    void castAndHoverUseIndependentTokenBucketsAndCleanup() {
        NoitaRequestGuard guard = new NoitaRequestGuard();
        UUID player = UUID.randomUUID();
        for (int sequence = 0; sequence < 8; sequence++) {
            assertTrue(guard.acceptCast(player, sequence, 0L));
        }
        assertFalse(guard.acceptCast(player, 8, 0L));
        assertTrue(guard.acceptHover(player, 0, 0L));

        guard.clear(player);
        assertTrue(guard.acceptCast(player, 0, 0L));
    }
}
