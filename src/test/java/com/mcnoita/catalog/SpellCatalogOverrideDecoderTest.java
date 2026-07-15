package com.mcnoita.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonParser;
import com.mcnoita.spell.action.DrawAction;
import com.mcnoita.spell.action.SpellCategory;
import com.mcnoita.spell.action.SpellDefinition;
import com.mcnoita.spell.action.UseConsumptionPolicy;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("regression")
class SpellCatalogOverrideDecoderTest {
    private static final SpellDefinition BASE = new SpellDefinition(
        "mc-noita:sample", SpellCategory.MULTICAST, 10, false, List.of(new DrawAction(2)),
        UseConsumptionPolicy.WHEN_PROJECTILE_SHOT, "spark_bolt"
    );

    @Test
    void safeOverridePreservesTheFrozenActionAndCategory() {
        SpellDefinition result = SpellCatalogOverrideDecoder.decode(JsonParser.parseString("""
            {"id":"mc-noita:sample","mana_cost":25,"recursive":true,
             "use_consumption_policy":"NEVER","related_projectile":"bomb"}
            """).getAsJsonObject(), "test", BASE);

        assertEquals(25, result.manaCost());
        assertEquals(SpellCategory.MULTICAST, result.category());
        assertEquals(BASE.actions(), result.actions());
        assertEquals(UseConsumptionPolicy.NEVER, result.useConsumptionPolicy());
        assertEquals("bomb", result.relatedProjectile());
    }

    @Test
    void malformedOrUnsafeFieldsAreRejectedBeforeServiceReload() {
        assertThrows(IllegalArgumentException.class, () -> SpellCatalogOverrideDecoder.decode(
            JsonParser.parseString("{\"id\":\"mc-noita:sample\",\"actions\":[]}").getAsJsonObject(), "test", BASE));
        assertThrows(IllegalArgumentException.class, () -> SpellCatalogOverrideDecoder.decode(
            JsonParser.parseString("{\"id\":\"mc-noita:sample\",\"mana_cost\":1.5}").getAsJsonObject(), "test", BASE));
        assertThrows(IllegalArgumentException.class, () -> SpellCatalogOverrideDecoder.decode(
            JsonParser.parseString("{\"id\":\"mc-noita:other\"}").getAsJsonObject(), "test", BASE));
    }
}
