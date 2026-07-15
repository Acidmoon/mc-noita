package com.mcnoita.catalog;

import com.mcnoita.spell.action.SpellCatalog;
import com.mcnoita.spell.action.SpellDefinition;
import com.mcnoita.wand.adapter.LegacySpellCatalogAdapter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Owns the immutable catalog generation used by server casts. A reload prepares
 * and validates an entire replacement before a single reference swap exposes it.
 */
public final class SpellCatalogService {
    private static final SpellCatalogService INSTANCE = new SpellCatalogService(new SpellCatalogValidator());

    private final SpellCatalogValidator validator;
    private final AtomicReference<CatalogSnapshot> current = new AtomicReference<>();
    /** Startup definitions never change; every datapack reload starts from this baseline. */
    private Map<String, SpellDefinition> builtInDefinitions = Map.of();
    private Set<String> registeredIds = Set.of();

    public SpellCatalogService() {
        this(new SpellCatalogValidator());
    }

    SpellCatalogService(SpellCatalogValidator validator) {
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    public static SpellCatalogService getInstance() {
        return INSTANCE;
    }

    /** Builds the initial immutable snapshot only after all built-in Item IDs exist. */
    public synchronized CatalogSnapshot initializeFromLegacy() {
        return initialize(LegacySpellCatalogAdapter.createDefinitions());
    }

    public synchronized CatalogSnapshot initialize(Map<String, SpellDefinition> definitions) {
        if (current.get() != null) {
            throw new IllegalStateException("spell catalog service is already initialized");
        }
        Map<String, SpellDefinition> candidate = copyDefinitions(definitions);
        SpellCatalogValidator.ValidationResult validation = validator.validateInitial(candidate);
        if (!validation.isValid()) {
            throw new IllegalStateException("invalid initial spell catalog: " + String.join("; ", validation.errors()));
        }

        Map<String, SpellDefinition> ordered = orderedDefinitions(candidate);
        CatalogSnapshot snapshot = snapshot(0L, ordered);
        builtInDefinitions = ordered;
        registeredIds = Set.copyOf(ordered.keySet());
        current.set(snapshot);
        return snapshot;
    }

    /**
     * Applies only overrides for IDs captured at initialization. Invalid input
     * never reaches the current reference, and equal content retains its epoch.
     */
    public synchronized ReloadResult reload(Map<String, SpellDefinition> overrides) {
        CatalogSnapshot previous = current();
        Map<String, SpellDefinition> candidateOverrides = copyDefinitions(overrides);
        SpellCatalogValidator.ValidationResult validation = validator.validateOverrides(registeredIds, candidateOverrides);
        if (!validation.isValid()) {
            return ReloadResult.rejected(previous, validation.errors());
        }

        // A resource reload represents the complete current override set. It
        // must rebuild from startup definitions, not merge into the previous
        // snapshot, so removing a datapack file restores its built-in value.
        Map<String, SpellDefinition> nextDefinitions = new LinkedHashMap<>(builtInDefinitions);
        nextDefinitions.putAll(candidateOverrides);
        SpellCatalogValidator.ValidationResult mergedValidation = validator.validateInitial(nextDefinitions);
        if (!mergedValidation.isValid()) {
            return ReloadResult.rejected(previous, mergedValidation.errors());
        }

        Map<String, SpellDefinition> ordered = orderedDefinitions(nextDefinitions);
        String hash = CanonicalCatalogHasher.sha256(ordered);
        if (previous.hash().equals(hash)) {
            return ReloadResult.accepted(previous, false);
        }

        CatalogSnapshot next = new CatalogSnapshot(new SpellCatalog(previous.epoch() + 1L, hash, ordered));
        current.set(next);
        return ReloadResult.accepted(next, true);
    }

    public CatalogSnapshot current() {
        CatalogSnapshot snapshot = current.get();
        if (snapshot == null) {
            throw new IllegalStateException("spell catalog service has not been initialized");
        }
        return snapshot;
    }

    /** Listener-only read of immutable startup definitions for partial override decoding. */
    synchronized Map<String, SpellDefinition> builtInDefinitions() {
        if (current.get() == null) {
            throw new IllegalStateException("spell catalog service has not been initialized");
        }
        return builtInDefinitions;
    }

    private static CatalogSnapshot snapshot(long epoch, Map<String, SpellDefinition> definitions) {
        String hash = CanonicalCatalogHasher.sha256(definitions);
        return new CatalogSnapshot(new SpellCatalog(epoch, hash, definitions));
    }

    /** Lexical ID order is part of deterministic random-candidate evaluation. */
    private static Map<String, SpellDefinition> orderedDefinitions(Map<String, SpellDefinition> definitions) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(new TreeMap<>(definitions)));
    }

    private static Map<String, SpellDefinition> copyDefinitions(Map<String, SpellDefinition> definitions) {
        return definitions == null ? null : new LinkedHashMap<>(definitions);
    }

    public record ReloadResult(CatalogSnapshot snapshot, boolean accepted, boolean changed, List<String> errors) {
        public ReloadResult {
            Objects.requireNonNull(snapshot, "snapshot");
            errors = List.copyOf(Objects.requireNonNull(errors, "errors"));
        }

        private static ReloadResult accepted(CatalogSnapshot snapshot, boolean changed) {
            return new ReloadResult(snapshot, true, changed, List.of());
        }

        private static ReloadResult rejected(CatalogSnapshot snapshot, List<String> errors) {
            return new ReloadResult(snapshot, false, false, errors);
        }
    }
}
