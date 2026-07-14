package com.mcnoita.spell.plan;

/** One immutable, world-independent operation selected during wand evaluation. */
public sealed interface EffectNode permits ProjectileEffectNode, SoundEffectNode, RecoilEffectNode,
    ExplosionEffectNode, FieldEffectNode, SummonEffectNode, TeleportEffectNode, BlockMutationEffectNode,
    PersistentJobEffectNode {
    String nodePath();

    static String requireNodePath(String nodePath) {
        if (nodePath == null || nodePath.isBlank()) {
            throw new IllegalArgumentException("effect node path must not be blank");
        }
        return nodePath;
    }
}
