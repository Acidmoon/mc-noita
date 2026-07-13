package com.mcnoita.network;

import java.util.Optional;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Hand;

public record WandCastRequest(int protocolVersion, int sequence, Hand hand, int slot, int stateHash) {
    public void write(PacketByteBuf buf) {
        buf.writeVarInt(protocolVersion);
        buf.writeVarInt(sequence);
        buf.writeByte(hand.ordinal());
        buf.writeVarInt(slot);
        buf.writeInt(stateHash);
    }

    public static Optional<WandCastRequest> read(PacketByteBuf buf) {
        if (buf.readableBytes() < 1 || buf.readableBytes() > NoitaNetworkProtocol.MAX_CAST_PACKET_BYTES) {
            return Optional.empty();
        }
        try {
            int protocolVersion = buf.readVarInt();
            int sequence = buf.readVarInt();
            int handOrdinal = buf.readUnsignedByte();
            int slot = buf.readVarInt();
            int stateHash = buf.readInt();
            if (buf.isReadable() || handOrdinal < 0 || handOrdinal >= Hand.values().length || sequence < 0) {
                return Optional.empty();
            }
            return Optional.of(new WandCastRequest(protocolVersion, sequence, Hand.values()[handOrdinal], slot, stateHash));
        } catch (IndexOutOfBoundsException | IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
