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
    @Test
    void castRequestRoundTripsExactly() {
        WandCastRequest expected = new WandCastRequest(NoitaNetworkProtocol.VERSION, 7, Hand.MAIN_HAND, 3, 42);
        PacketByteBuf buf = PacketByteBufs.create();
        expected.write(buf);

        assertEquals(expected, WandCastRequest.read(buf).orElseThrow());
    }

    @Test
    void castRequestRejectsTrailingBytes() {
        PacketByteBuf buf = PacketByteBufs.create();
        new WandCastRequest(NoitaNetworkProtocol.VERSION, 1, Hand.MAIN_HAND, 0, 0).write(buf);
        buf.writeByte(1);

        assertTrue(WandCastRequest.read(buf).isEmpty());
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
