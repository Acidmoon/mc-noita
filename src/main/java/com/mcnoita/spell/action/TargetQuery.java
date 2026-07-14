package com.mcnoita.spell.action;

import java.util.List;
import java.util.Set;

/** Declarative target search used by Call, Copy, Divide and Add Trigger. */
public record TargetQuery(
    List<Source> sources,
    Direction direction,
    Set<SpellCategory> categories,
    Set<String> excludedSpellIds,
    boolean recursiveTargetsAllowed,
    boolean requireRelatedProjectile,
    int limit
) {
    public enum Source { DECK, HAND, DISCARD, EXTERNAL }
    public enum Direction { FIRST, LAST, ALL }

    public TargetQuery {
        sources = List.copyOf(sources);
        categories = Set.copyOf(categories);
        excludedSpellIds = Set.copyOf(excludedSpellIds);
        if (sources.isEmpty() || limit < 1) {
            throw new IllegalArgumentException("target query needs a source and positive limit");
        }
    }

    public static TargetQuery alpha() {
        return new TargetQuery(List.of(Source.DISCARD, Source.HAND, Source.DECK), Direction.FIRST,
            Set.of(), Set.of(), true, false, 1);
    }

    public static TargetQuery gamma() {
        return new TargetQuery(List.of(Source.DECK, Source.HAND), Direction.LAST,
            Set.of(), Set.of(), true, false, 1);
    }

    public static TargetQuery tau() {
        return new TargetQuery(List.of(Source.DECK), Direction.FIRST, Set.of(), Set.of(), true, false, 2);
    }

    public static TargetQuery allWand(Set<SpellCategory> categories) {
        return new TargetQuery(List.of(Source.DISCARD, Source.HAND, Source.DECK), Direction.ALL,
            categories, Set.of(), true, false, Integer.MAX_VALUE);
    }

    public static TargetQuery externalRandom() {
        return new TargetQuery(List.of(Source.EXTERNAL), Direction.ALL, Set.of(), Set.of(), true, false,
            Integer.MAX_VALUE);
    }
}
