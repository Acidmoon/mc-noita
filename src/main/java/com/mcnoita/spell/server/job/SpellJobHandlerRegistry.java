package com.mcnoita.spell.server.job;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Explicit registry prevents a persisted job type from falling back to another handler. */
public final class SpellJobHandlerRegistry {
    private final Map<String, SpellJobHandler> handlers = new LinkedHashMap<>();

    public synchronized void register(SpellJobHandler handler) {
        Objects.requireNonNull(handler, "handler");
        String jobType = FrozenSpellJobNode.requireBoundedNonBlank(handler.jobType(), "handler jobType");
        if (handlers.putIfAbsent(jobType, handler) != null) {
            throw new IllegalArgumentException("spell job handler already registered: " + jobType);
        }
    }

    public synchronized Optional<SpellJobHandler> find(String jobType) {
        return Optional.ofNullable(handlers.get(jobType));
    }

    public synchronized boolean supports(String jobType) {
        return handlers.containsKey(jobType);
    }
}
