package com.mcnoita.network;

import java.util.Optional;
import net.minecraft.network.PacketByteBuf;

public record HoverInputRequest(int protocolVersion, int sequence, boolean jumpHeld, float sidewaysInput, float forwardInput) {
    public void write(PacketByteBuf buf) {
        buf.writeVarInt(protocolVersion);
        buf.writeVarInt(sequence);
        buf.writeBoolean(jumpHeld);
        buf.writeFloat(sidewaysInput);
        buf.writeFloat(forwardInput);
    }

    public static Optional<HoverInputRequest> read(PacketByteBuf buf) {
        if (buf.readableBytes() < 1 || buf.readableBytes() > NoitaNetworkProtocol.MAX_HOVER_PACKET_BYTES) {
            return Optional.empty();
        }
        try {
            int protocolVersion = buf.readVarInt();
            int sequence = buf.readVarInt();
            boolean jumpHeld = buf.readBoolean();
            float sidewaysInput = buf.readFloat();
            float forwardInput = buf.readFloat();
            if (buf.isReadable() || sequence < 0 || !Float.isFinite(sidewaysInput) || !Float.isFinite(forwardInput)) {
                return Optional.empty();
            }
            return Optional.of(new HoverInputRequest(
                protocolVersion,
                sequence,
                jumpHeld,
                clampMovementInput(sidewaysInput),
                clampMovementInput(forwardInput)
            ));
        } catch (IndexOutOfBoundsException | IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private static float clampMovementInput(float input) {
        return Math.max(-1.0f, Math.min(1.0f, input));
    }
}
