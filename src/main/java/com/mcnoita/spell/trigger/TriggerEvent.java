package com.mcnoita.spell.trigger;

import java.util.Objects;

/** An immutable input to the runtime trigger state machine. */
public record TriggerEvent(Kind kind, CollisionKey collision, ProjectileTerminationCause terminationCause) {
    public enum Kind {
        COLLISION,
        TIMER_EXPIRED,
        TERMINATED
    }

    public TriggerEvent {
        Objects.requireNonNull(kind, "kind");
        if (kind == Kind.COLLISION && collision == null) {
            throw new IllegalArgumentException("collision events require a CollisionKey");
        }
        if (kind == Kind.TERMINATED && terminationCause == null) {
            throw new IllegalArgumentException("termination events require a cause");
        }
    }

    public static TriggerEvent collision(CollisionKey collision) {
        return new TriggerEvent(Kind.COLLISION, collision, null);
    }

    public static TriggerEvent timerExpired() {
        return new TriggerEvent(Kind.TIMER_EXPIRED, null, null);
    }

    public static TriggerEvent terminated(ProjectileTerminationCause cause) {
        return new TriggerEvent(Kind.TERMINATED, null, cause);
    }
}
