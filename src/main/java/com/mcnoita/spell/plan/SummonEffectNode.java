package com.mcnoita.spell.plan;

import com.mcnoita.wand.model.NoitaDuration;
import java.util.Objects;

/** Frozen summon intent. Entity resolution and spawn permission stay server-side. */
public record SummonEffectNode(String nodePath, String entityTypeId, int count, NoitaDuration lifetime) implements EffectNode {
    public SummonEffectNode {
        EffectNode.requireNodePath(nodePath);
        if (entityTypeId == null || entityTypeId.isBlank() || count < 1) {
            throw new IllegalArgumentException("summon node requires an entity id and positive count");
        }
        lifetime = Objects.requireNonNull(lifetime, "lifetime");
    }
}
