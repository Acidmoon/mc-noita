package com.mcnoita.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mcnoita.spell.action.CallSpellAction;
import com.mcnoita.spell.action.DrawAction;
import com.mcnoita.spell.action.SpellCategory;
import com.mcnoita.spell.action.SpellDefinition;
import com.mcnoita.spell.action.TargetQuery;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("regression")
class SpellCatalogServiceTest {
    @Test
    void canonicalSha256IsStableAcrossDefinitionAndSetInsertionOrder() {
        SpellDefinition alphaForward = callDefinition("mc-noita:alpha", 10, false);
        SpellDefinition beta = new SpellDefinition("mc-noita:beta", SpellCategory.MULTICAST, 5, false, List.of(new DrawAction(2)));
        Map<String, SpellDefinition> forward = new LinkedHashMap<>();
        forward.put(alphaForward.id(), alphaForward);
        forward.put(beta.id(), beta);

        SpellDefinition alphaReverse = callDefinition("mc-noita:alpha", 10, true);
        Map<String, SpellDefinition> reverse = new LinkedHashMap<>();
        reverse.put(beta.id(), beta);
        reverse.put(alphaReverse.id(), alphaReverse);

        String stable = CanonicalCatalogHasher.sha256(forward);
        assertEquals(stable, CanonicalCatalogHasher.sha256(reverse));
        assertTrue(stable.matches("[0-9a-f]{64}"));

        Map<String, SpellDefinition> changed = new LinkedHashMap<>(forward);
        changed.put(beta.id(), new SpellDefinition("mc-noita:beta", SpellCategory.MULTICAST, 5, false, List.of(new DrawAction(3))));
        assertNotEquals(stable, CanonicalCatalogHasher.sha256(changed));
    }

    @Test
    void validReloadAtomicallyPublishesNewEpochAndHash() {
        SpellCatalogService service = initializedService();
        CatalogSnapshot before = service.current();
        SpellDefinition override = callDefinition("mc-noita:alpha", 25, false);

        SpellCatalogService.ReloadResult result = service.reload(Map.of(override.id(), override));

        assertTrue(result.accepted());
        assertTrue(result.changed());
        assertEquals(before.epoch() + 1L, result.snapshot().epoch());
        assertNotEquals(before.hash(), result.snapshot().hash());
        assertSame(result.snapshot(), service.current());
        assertEquals(25, service.current().catalog().require(override.id()).manaCost());
        assertEquals(5, service.current().catalog().require("mc-noita:beta").manaCost());
    }

    @Test
    void unknownReloadIdKeepsThePreviousSnapshot() {
        SpellCatalogService service = initializedService();
        CatalogSnapshot before = service.current();
        SpellDefinition unknown = new SpellDefinition("mc-noita:not_registered", SpellCategory.UTILITY, 0, false, List.of());

        SpellCatalogService.ReloadResult result = service.reload(Map.of(unknown.id(), unknown));

        assertFalse(result.accepted());
        assertFalse(result.changed());
        assertSame(before, result.snapshot());
        assertSame(before, service.current());
        assertEquals(before.epoch(), service.current().epoch());
        assertEquals(before.hash(), service.current().hash());
    }

    @Test
    void sameContentReloadRetainsTheExistingEpoch() {
        SpellCatalogService service = initializedService();
        CatalogSnapshot before = service.current();
        SpellDefinition equivalent = callDefinition("mc-noita:alpha", 10, true);

        SpellCatalogService.ReloadResult result = service.reload(Map.of(equivalent.id(), equivalent));

        assertTrue(result.accepted());
        assertFalse(result.changed());
        assertSame(before, result.snapshot());
        assertSame(before, service.current());
    }

    private static SpellCatalogService initializedService() {
        SpellDefinition alpha = callDefinition("mc-noita:alpha", 10, false);
        SpellDefinition beta = new SpellDefinition("mc-noita:beta", SpellCategory.MULTICAST, 5, false, List.of(new DrawAction(2)));
        SpellCatalogService service = new SpellCatalogService();
        service.initialize(Map.of(beta.id(), beta, alpha.id(), alpha));
        return service;
    }

    private static SpellDefinition callDefinition(String id, int manaCost, boolean reverseSetOrder) {
        Set<SpellCategory> categories = new LinkedHashSet<>();
        Set<String> excluded = new LinkedHashSet<>();
        if (reverseSetOrder) {
            categories.add(SpellCategory.UTILITY);
            categories.add(SpellCategory.PROJECTILE);
            excluded.add("mc-noita:beta");
            excluded.add("mc-noita:alpha");
        } else {
            categories.add(SpellCategory.PROJECTILE);
            categories.add(SpellCategory.UTILITY);
            excluded.add("mc-noita:alpha");
            excluded.add("mc-noita:beta");
        }
        TargetQuery query = new TargetQuery(List.of(TargetQuery.Source.DECK), TargetQuery.Direction.FIRST, categories,
            excluded, true, false, 1);
        return new SpellDefinition(id, SpellCategory.OTHER, manaCost, true, List.of(new CallSpellAction(query)));
    }
}
