package com.mcnoita.wand.eval;

import java.util.HashMap;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * Deterministic RNG with named substreams. Adding a random visual choice to one
 * domain cannot change Deck ordering or random-spell selection in another.
 */
public final class CastRng {
    private final long rootSeed;
    private final Map<String, Long> positions = new HashMap<>();

    public CastRng(long rootSeed) {
        this.rootSeed = rootSeed;
    }

    public long rootSeed() {
        return rootSeed;
    }

    public int nextInt(String domain, int bound) {
        if (bound < 1) {
            throw new IllegalArgumentException("random bound must be positive");
        }
        return random(domain).nextInt(bound);
    }

    public double nextDouble(String domain) {
        return random(domain).nextDouble();
    }

    private SplittableRandom random(String domain) {
        long position = positions.merge(domain, 1L, Long::sum) - 1L;
        return new SplittableRandom(mix64(rootSeed ^ mix64(domain.hashCode()) ^ mix64(position)));
    }

    private static long mix64(long value) {
        long mixed = value + 0x9E3779B97F4A7C15L;
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        return mixed ^ (mixed >>> 31);
    }
}
