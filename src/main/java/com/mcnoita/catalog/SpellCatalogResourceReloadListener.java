package com.mcnoita.catalog;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcnoita.MCNoita;
import com.mcnoita.spell.action.SpellDefinition;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

/**
 * Applies one validated batch of safe catalog overrides from
 * {@code data/<namespace>/spell_overrides/<name>.json}. Invalid resources never publish a
 * partial catalog: the service keeps the prior immutable snapshot instead.
 */
public final class SpellCatalogResourceReloadListener implements SimpleSynchronousResourceReloadListener {
    private static final String DIRECTORY = "spell_overrides";
    private static boolean registered;

    public static synchronized void register() {
        if (registered) {
            return;
        }
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SpellCatalogResourceReloadListener());
        registered = true;
    }

    @Override
    public Identifier getFabricId() {
        return MCNoita.id("spell_catalog_overrides");
    }

    @Override
    public void reload(ResourceManager manager) {
        SpellCatalogService service = SpellCatalogService.getInstance();
        CatalogSnapshot before;
        try {
            before = service.current();
        } catch (IllegalStateException notInitialized) {
            // The listener is registered only after initialization, but this
            // guard keeps an early Fabric lifecycle call from exposing a half-built catalog.
            MCNoita.LOGGER.warn("Ignoring spell catalog reload before startup initialization", notInitialized);
            return;
        }

        ParsedOverrides parsed = parse(manager, service.builtInDefinitions());
        if (!parsed.errors().isEmpty()) {
            MCNoita.LOGGER.warn("Rejected spell catalog reload; retaining epoch {}: {}", before.epoch(),
                String.join("; ", parsed.errors()));
            return;
        }

        SpellCatalogService.ReloadResult result = service.reload(parsed.overrides());
        if (!result.accepted()) {
            MCNoita.LOGGER.warn("Rejected spell catalog reload; retaining epoch {}: {}", before.epoch(),
                String.join("; ", result.errors()));
        } else if (result.changed()) {
            MCNoita.LOGGER.info("Published spell catalog epoch {} with hash {}", result.snapshot().epoch(), result.snapshot().hash());
        }
    }

    /** Each reload file is decoded against immutable built-ins, never a prior override generation. */
    static ParsedOverrides parse(ResourceManager manager, Map<String, SpellDefinition> builtInDefinitions) {
        Map<String, SpellDefinition> overrides = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        for (Map.Entry<Identifier, Resource> entry : manager.findResources(DIRECTORY,
            id -> id.getPath().endsWith(".json")).entrySet()) {
            String source = entry.getKey().toString();
            try (Reader reader = new InputStreamReader(entry.getValue().getInputStream(), StandardCharsets.UTF_8)) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject()) {
                    throw new IllegalArgumentException(source + " must contain a JSON object");
                }
                JsonObject json = root.getAsJsonObject();
                String id = SpellCatalogOverrideDecoder.readId(json, source);
                SpellDefinition base = builtInDefinitions.get(id);
                if (base == null) {
                    throw new IllegalArgumentException(source + " may not add unregistered spell id: " + id);
                }
                SpellDefinition override = SpellCatalogOverrideDecoder.decode(json, source, base);
                if (overrides.putIfAbsent(id, override) != null) {
                    throw new IllegalArgumentException(source + " duplicates an override for " + id);
                }
            } catch (RuntimeException | java.io.IOException failure) {
                errors.add(failure.getMessage() == null ? source + " could not be decoded" : failure.getMessage());
            }
        }
        return new ParsedOverrides(Map.copyOf(overrides), List.copyOf(errors));
    }

    record ParsedOverrides(Map<String, SpellDefinition> overrides, List<String> errors) {
    }
}
