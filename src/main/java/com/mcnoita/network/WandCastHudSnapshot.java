package com.mcnoita.network;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.PacketByteBuf;

/**
 * Server-to-client cast HUD projection plus the binding that a subsequent C2S
 * cast intent must echo. The client never derives these values from authority.
 */
public record WandCastHudSnapshot(
    int protocolVersion,
    int mode,
    int progressTicks,
    int totalTicks,
    float currentMana,
    int manaMax,
    long wandRevision,
    long catalogEpoch,
    int stateHash,
    String catalogHash
) {
    private static final int MAX_TICKS = 72_000;
    private static final int MAX_MANA = 1_000_000;

    public WandCastHudSnapshot {
        catalogHash = Objects.requireNonNull(catalogHash, "catalogHash");
        if (protocolVersion < 0 || mode < 0 || mode > 2 || progressTicks < 0 || totalTicks < 1 || totalTicks > MAX_TICKS
            || progressTicks > totalTicks || !Float.isFinite(currentMana) || currentMana < 0.0f || manaMax < 0
            || manaMax > MAX_MANA || currentMana > manaMax || wandRevision < 0L || catalogEpoch < 0L
            || !NoitaNetworkProtocol.isCanonicalCatalogHash(catalogHash)) {
            throw new IllegalArgumentException("invalid wand HUD snapshot");
        }
    }

    public void write(PacketByteBuf buf) {
        buf.writeVarInt(protocolVersion);
        buf.writeVarInt(mode);
        buf.writeVarInt(progressTicks);
        buf.writeVarInt(totalTicks);
        buf.writeFloat(currentMana);
        buf.writeVarInt(manaMax);
        buf.writeVarLong(wandRevision);
        buf.writeVarLong(catalogEpoch);
        buf.writeInt(stateHash);
        buf.writeString(catalogHash, NoitaNetworkProtocol.CATALOG_HASH_LENGTH);
    }

    public static Optional<WandCastHudSnapshot> read(PacketByteBuf buf) {
        if (buf.readableBytes() < 1 || buf.readableBytes() > NoitaNetworkProtocol.MAX_HUD_PACKET_BYTES) {
            return Optional.empty();
        }
        try {
            int protocolVersion = buf.readVarInt();
            int mode = buf.readVarInt();
            int progressTicks = buf.readVarInt();
            int totalTicks = buf.readVarInt();
            float currentMana = buf.readFloat();
            int manaMax = buf.readVarInt();
            long wandRevision = buf.readVarLong();
            long catalogEpoch = buf.readVarLong();
            int stateHash = buf.readInt();
            String catalogHash = buf.readString(NoitaNetworkProtocol.CATALOG_HASH_LENGTH);
            if (buf.isReadable()) {
                return Optional.empty();
            }
            return Optional.of(new WandCastHudSnapshot(
                protocolVersion, mode, progressTicks, totalTicks, currentMana, manaMax,
                wandRevision, catalogEpoch, stateHash, catalogHash
            ));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }
}
