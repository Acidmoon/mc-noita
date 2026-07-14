package com.mcnoita.spell.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mcnoita.wand.model.NoitaDuration;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("regression")
class EffectPlanTest {
    @Test
    void compatibilityConstructorBuildsTypedNodesAndPreservesLegacyProjections() {
        ProjectilePlan projectile = projectile("root/projectile/0");
        SoundPlan sound = new SoundPlan(SoundPlan.SoundKind.PROJECTILE_CAST);
        RecoilPlan recoil = new RecoilPlan(20.0);

        EffectPlan plan = new EffectPlan(List.of(projectile), List.of(sound), List.of(recoil));

        assertEquals(List.of(projectile), plan.projectiles());
        assertEquals(List.of(sound), plan.sounds());
        assertEquals(List.of(recoil), plan.recoils());
        assertInstanceOf(ProjectileEffectNode.class, plan.nodes().get(0));
        assertInstanceOf(RecoilEffectNode.class, plan.nodes().get(1));
        assertInstanceOf(SoundEffectNode.class, plan.nodes().get(2));
        assertThrows(UnsupportedOperationException.class, () -> plan.nodes().add(new ExplosionEffectNode("extra", 1.0, 1.0, false)));
    }

    @Test
    void explicitTypedNodesKeepLegacyViewsEmptyWhenTheyDoNotContainLegacyTypes() {
        EffectPlan plan = new EffectPlan(List.of(
            new ExplosionEffectNode("root/explosion", 2.0, 10.0, true),
            new TeleportEffectNode("root/teleport", 32.0, true)
        ));

        assertEquals(List.of(), plan.projectiles());
        assertEquals(List.of(), plan.sounds());
        assertEquals(List.of(), plan.recoils());
        assertEquals(2, plan.nodes().size());
    }

    @Test
    void duplicateNodePathsAreRejectedAcrossTypedNodeKinds() {
        assertThrows(IllegalArgumentException.class, () -> new EffectPlan(List.of(
            new ExplosionEffectNode("root/shared", 1.0, 1.0, false),
            new FieldEffectNode("root/shared", FieldEffectNode.FieldKind.GENERIC, 1.0, NoitaDuration.frames(30))
        )));
    }

    private static ProjectilePlan projectile(String nodePath) {
        return new ProjectilePlan(nodePath, "spark_bolt", "BOLT", 1.0, 0.0, NoitaDuration.frames(60), 0,
            0.0, 400.0, 0.0, 0.0, 0.99, 0.65, 1.0, 0.0, false, false, 1, 0.0, null, 0, List.of());
    }
}
