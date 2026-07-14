package com.mcnoita.spell.plan;

import java.util.Objects;

/** Typed sound request that is executed only after a cast is committed. */
public record SoundEffectNode(String nodePath, SoundPlan sound) implements EffectNode {
    public SoundEffectNode {
        EffectNode.requireNodePath(nodePath);
        sound = Objects.requireNonNull(sound, "sound");
    }
}
