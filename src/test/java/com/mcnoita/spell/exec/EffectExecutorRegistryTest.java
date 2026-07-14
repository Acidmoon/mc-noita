package com.mcnoita.spell.exec;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mcnoita.spell.plan.BlockMutationEffectNode;
import com.mcnoita.spell.plan.ExplosionEffectNode;
import com.mcnoita.spell.plan.FieldEffectNode;
import com.mcnoita.spell.plan.PersistentJobEffectNode;
import com.mcnoita.spell.plan.ProjectileEffectNode;
import com.mcnoita.spell.plan.ProjectilePlan;
import com.mcnoita.spell.plan.RecoilEffectNode;
import com.mcnoita.spell.plan.RecoilPlan;
import com.mcnoita.spell.plan.SoundEffectNode;
import com.mcnoita.spell.plan.SoundPlan;
import com.mcnoita.spell.plan.SummonEffectNode;
import com.mcnoita.spell.plan.TeleportEffectNode;
import com.mcnoita.wand.model.NoitaDuration;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("regression")
class EffectExecutorRegistryTest {
    @Test
    void defaultRegistryHasAnExplicitSafePathForEveryCurrentNodeType() {
        EffectExecutorRegistry registry = EffectExecutorRegistry.createDefault();

        assertTrue(registry.supports(new ProjectileEffectNode(projectile())));
        assertTrue(registry.supports(new SoundEffectNode("sound", new SoundPlan(SoundPlan.SoundKind.PROJECTILE_CAST))));
        assertTrue(registry.supports(new RecoilEffectNode("recoil", new RecoilPlan(1.0))));
        assertTrue(registry.supports(new ExplosionEffectNode("explosion", 1.0, 1.0, false)));
        assertTrue(registry.supports(new FieldEffectNode("field", FieldEffectNode.FieldKind.GENERIC, 1.0, NoitaDuration.frames(1))));
        assertTrue(registry.supports(new SummonEffectNode("summon", "minecraft:pig", 1, NoitaDuration.frames(1))));
        assertTrue(registry.supports(new TeleportEffectNode("teleport", 1.0, true)));
        assertTrue(registry.supports(new BlockMutationEffectNode("mutation", BlockMutationEffectNode.MutationKind.BREAK, 1, 1.0)));
        assertTrue(registry.supports(new PersistentJobEffectNode("job", "field_tick", 1, NoitaDuration.frames(1))));
    }

    @Test
    void duplicateExecutorRegistrationIsRejected() {
        EffectExecutorRegistry registry = new EffectExecutorRegistry();
        registry.register(new DeferredEffectNodeExecutor<>(ExplosionEffectNode.class));

        assertThrows(IllegalArgumentException.class, () -> registry.register(new DeferredEffectNodeExecutor<>(ExplosionEffectNode.class)));
    }

    private static ProjectilePlan projectile() {
        return new ProjectilePlan("projectile", "spark_bolt", "BOLT", 1.0, 0.0, NoitaDuration.frames(60), 0,
            0.0, 400.0, 0.0, 0.0, 0.99, 0.65, 1.0, 0.0, false, false, 1, 0.0, null, 0, List.of());
    }
}
