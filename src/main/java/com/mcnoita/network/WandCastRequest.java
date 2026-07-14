package com.mcnoita.network;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Hand;

/** The client can only echo the server-observed binding; it cannot submit cast mechanics. */
public record WandCastRequest(
    int protocolVersion,
    int sequence,
    Hand hand,
    int slot,
    int stateHash,
    long wandRevision,
    long catalogEpoch,
    String catalogHash
) {
    public WandCastRequest {
        hand = Objects.requireNonNull(hand, "hand");
        catalogHash = Objects.requireNonNull(catalogHash, "catalogHash");
        if (protocolVersion < 0 || sequence < 0 || slot < -1 || slot >= 9 || wandRevision < 0L || catalogEpoch < 0L
            || !NoitaNetworkProtocol.isCanonicalCatalogHash(catalogHash)) {
            throw new IllegalArgumentException("invalid cast request binding");
        }
    }

    public void write(PacketByteBuf buf) {
        buf.writeVarInt(protocolVersion);
        buf.writeVarInt(sequence);
        buf.writeByte(hand.ordinal());
        buf.writeVarInt(slot);
        buf.writeInt(stateHash);
        buf.writeVarLong(wandRevision);
        buf.writeVarLong(catalogEpoch);
        buf.writeString(catalogHash, NoitaNetworkProtocol.CATALOG_HASH_LENGTH);
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
            long wandRevision = buf.readVarLong();
            long catalogEpoch = buf.readVarLong();
            String catalogHash = buf.readString(NoitaNetworkProtocol.CATALOG_HASH_LENGTH);
            if (buf.isReadable() || handOrdinal < 0 || handOrdinal >= Hand.values().length) {
                return Optional.empty();
            }
            return Optional.of(new WandCastRequest(
                protocolVersion, sequence, Hand.values()[handOrdinal], slot, stateHash, wandRevision, catalogEpoch, catalogHash
            ));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }
}
