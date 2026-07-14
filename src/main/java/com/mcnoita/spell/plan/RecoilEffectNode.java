package com.mcnoita.spell.plan;

import java.util.Objects;

/** Typed player recoil request associated with a committed cast. */
public record RecoilEffectNode(String nodePath, RecoilPlan recoil) implements EffectNode {
    public RecoilEffectNode {
        EffectNode.requireNodePath(nodePath);
        recoil = Objects.requireNonNull(recoil, "recoil");
    }
}
