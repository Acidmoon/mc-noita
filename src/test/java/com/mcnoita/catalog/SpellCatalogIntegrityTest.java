package com.mcnoita.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("characterization")
class SpellCatalogIntegrityTest {
    private static final Path CATALOG_PATH = Path.of("docs", "baseline", "spell-catalog.json");

    @Test
    void catalogContainsEveryRegisteredSpellExactlyOnce() throws IOException {
        JsonArray spells = catalog().getAsJsonArray("spells");
        assertEquals(315, spells.size());

        Set<String> registryIds = new HashSet<>();
        for (JsonElement element : spells) {
            assertTrue(registryIds.add(element.getAsJsonObject().get("registry_id").getAsString()));
        }
    }

    @Test
    void catalogCarriesTraceabilityAndResourceStatusForEverySpell() throws IOException {
        for (JsonElement element : catalog().getAsJsonArray("spells")) {
            JsonObject spell = element.getAsJsonObject();
            assertTrue(spell.get("noita_id").getAsString().matches("[A-Z0-9_]+"));
            assertTrue(spell.get("wiki_source_url").getAsString().startsWith("https://noita.wiki.gg/"));
            assertTrue(spell.getAsJsonObject("model").get("exists").getAsBoolean());
            assertTrue(spell.getAsJsonObject("language").get("english_exists").getAsBoolean());
            assertTrue(spell.getAsJsonObject("language").get("chinese_exists").getAsBoolean());
            assertFalse(spell.getAsJsonObject("implementation").get("level").getAsString().isBlank());
        }
    }

    private static JsonObject catalog() throws IOException {
        return JsonParser.parseString(Files.readString(CATALOG_PATH)).getAsJsonObject();
    }
}
