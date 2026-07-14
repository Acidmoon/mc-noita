package com.mcnoita.spell.trigger;

import com.mcnoita.persistence.NoitaNbtLimits;
import java.util.Optional;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

/** NBT adapter for the otherwise Minecraft-independent TriggerRuntimeState. */
public final class TriggerRuntimeStateNbtCodec {
    private static final String RELEASE_SEQUENCE_KEY = "ReleaseSequence";
    private static final String TIMER_ELAPSED_TICKS_KEY = "TimerElapsedTicks";
    private static final String TIMER_EXPIRED_KEY = "TimerExpired";
    private static final String EXPIRATION_RELEASED_KEY = "ExpirationReleased";
    private static final String INERT_KEY = "Inert";
    private static final String REMAINING_RELEASE_EVENTS_KEY = "RemainingReleaseEvents";
    private static final String REMAINING_SPAWNED_ENTITIES_KEY = "RemainingSpawnedEntities";
    private static final String LATEST_COLLISION_KEY = "LatestCollision";
    private static final String SERVER_TICK_KEY = "ServerTick";
    private static final String TARGET_KEY = "Target";
    private static final String FACE_KEY = "Face";
    private static final String NODE_PATH_KEY = "NodePath";

    private TriggerRuntimeStateNbtCodec() {
    }

    public static NbtCompound toNbt(TriggerRuntimeState state) {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt(RELEASE_SEQUENCE_KEY, state.releaseSequence());
        nbt.putInt(TIMER_ELAPSED_TICKS_KEY, state.timerElapsedTicks());
        nbt.putBoolean(TIMER_EXPIRED_KEY, state.timerExpired());
        nbt.putBoolean(EXPIRATION_RELEASED_KEY, state.expirationReleased());
        nbt.putBoolean(INERT_KEY, state.inert());
        nbt.putInt(REMAINING_RELEASE_EVENTS_KEY, state.remainingBudget().remainingReleaseEvents());
        nbt.putInt(REMAINING_SPAWNED_ENTITIES_KEY, state.remainingBudget().remainingSpawnedEntities());
        if (state.latestCollision() != null) {
            NbtCompound collision = new NbtCompound();
            collision.putLong(SERVER_TICK_KEY, state.latestCollision().serverTick());
            collision.putString(TARGET_KEY, state.latestCollision().target());
            collision.putString(FACE_KEY, state.latestCollision().face());
            collision.putString(NODE_PATH_KEY, state.latestCollision().nodePath());
            nbt.put(LATEST_COLLISION_KEY, collision);
        }
        return nbt;
    }

    public static Optional<TriggerRuntimeState> tryFromNbt(NbtCompound nbt, TriggerRuntimeBudget fallback) {
        return decode(nbt, fallback, false);
    }

    /**
     * v3 entities must carry every runtime flag. Falling back to fresh state on
     * a missing final-event flag could replay a Timer or Expiration payload.
     */
    public static Optional<TriggerRuntimeState> tryFromCurrentNbt(NbtCompound nbt, TriggerRuntimeBudget fallback) {
        return decode(nbt, fallback, true);
    }

    private static Optional<TriggerRuntimeState> decode(NbtCompound nbt, TriggerRuntimeBudget fallback, boolean requireComplete) {
        try {
            if (requireComplete) {
                require(nbt, RELEASE_SEQUENCE_KEY, NbtElement.NUMBER_TYPE);
                require(nbt, TIMER_ELAPSED_TICKS_KEY, NbtElement.NUMBER_TYPE);
                require(nbt, TIMER_EXPIRED_KEY, NbtElement.BYTE_TYPE);
                require(nbt, EXPIRATION_RELEASED_KEY, NbtElement.BYTE_TYPE);
                require(nbt, INERT_KEY, NbtElement.BYTE_TYPE);
                require(nbt, REMAINING_RELEASE_EVENTS_KEY, NbtElement.NUMBER_TYPE);
                require(nbt, REMAINING_SPAWNED_ENTITIES_KEY, NbtElement.NUMBER_TYPE);
            }
            int sequence = bounded(nbt, RELEASE_SEQUENCE_KEY, 0, Integer.MAX_VALUE, 0);
            TriggerRuntimeBudget budget = new TriggerRuntimeBudget(
                bounded(nbt, REMAINING_RELEASE_EVENTS_KEY, 0, fallback.remainingReleaseEvents(), fallback.remainingReleaseEvents()),
                bounded(nbt, REMAINING_SPAWNED_ENTITIES_KEY, 0, fallback.remainingSpawnedEntities(), fallback.remainingSpawnedEntities())
            );
            CollisionKey collision = null;
            if (nbt.contains(LATEST_COLLISION_KEY, NbtElement.COMPOUND_TYPE)) {
                NbtCompound collisionNbt = nbt.getCompound(LATEST_COLLISION_KEY);
                collision = new CollisionKey(collisionNbt.getLong(SERVER_TICK_KEY), boundedString(collisionNbt, TARGET_KEY),
                    boundedString(collisionNbt, FACE_KEY), boundedString(collisionNbt, NODE_PATH_KEY));
            }
            return Optional.of(new TriggerRuntimeState(sequence,
                bounded(nbt, TIMER_ELAPSED_TICKS_KEY, 0, Integer.MAX_VALUE, 0), nbt.getBoolean(TIMER_EXPIRED_KEY),
                nbt.getBoolean(EXPIRATION_RELEASED_KEY), nbt.getBoolean(INERT_KEY), collision, budget));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private static void require(NbtCompound nbt, String key, byte type) {
        if (!nbt.contains(key, type)) {
            throw new IllegalArgumentException("missing current trigger runtime field " + key);
        }
    }

    private static int bounded(NbtCompound nbt, String key, int min, int max, int fallback) {
        if (!nbt.contains(key, NbtElement.NUMBER_TYPE)) {
            return fallback;
        }
        return Math.max(min, Math.min(max, nbt.getInt(key)));
    }

    private static String boundedString(NbtCompound nbt, String key) {
        if (!nbt.contains(key, NbtElement.STRING_TYPE)) {
            throw new IllegalArgumentException("missing collision key " + key);
        }
        String value = nbt.getString(key);
        if (value.isBlank() || value.length() > NoitaNbtLimits.MAX_STRING_LENGTH) {
            throw new IllegalArgumentException("invalid collision key " + key);
        }
        return value;
    }
}
