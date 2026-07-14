package com.mcnoita.catalog;

import com.mcnoita.spell.action.SpellCatalog;
import java.util.Objects;

/** Immutable catalog generation selected by one atomic service reference. */
public record CatalogSnapshot(SpellCatalog catalog) {
    public CatalogSnapshot {
        Objects.requireNonNull(catalog, "catalog");
    }

    public long epoch() {
        return catalog.epoch();
    }

    public String hash() {
        return catalog.hash();
    }
}
